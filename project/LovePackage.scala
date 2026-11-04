import java.io.{ByteArrayInputStream, InputStream}
import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8

import scala.collection.JavaConverters._

import org.apache.commons.compress.archivers.tar.{TarArchiveEntry, TarArchiveOutputStream}
import org.apache.commons.compress.archivers.zip.{ZipArchiveEntry, ZipArchiveOutputStream, ZipFile}
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import sbt._
import sbt.io.Using

/** Packages a LÖVE game with LÖVE's own builds, for players who do not have LÖVE
  * (https://love2d.org/wiki/Game_Distribution).
  *
  * LÖVE 12 (for its HTTPS module) has no release yet: its builds are the artifacts of a run of
  * LÖVE's continuous integration ([[Build]]), which GitHub only gives with a token (`GITHUB_TOKEN`
  * or `GH_TOKEN`, any token of a GitHub account: LÖVE's repository is public), and only for 90
  * days. They are downloaded once, into the cache (`target/love-12.0`); they may also be put there
  * by hand (`<artifact>.zip`, as GitHub's page of the run gives them).
  *
  * LÖVE's archives are not unpacked: their entries are copied to the packages, keeping their Unix
  * modes (and so the symbolic links of the macOS app's frameworks).
  */
object LovePackage {
  val loveVersion = "12.0"

  /** The run of LÖVE's continuous integration (on its main branch, where 12.0 is made) whose builds
    * we package, with the ids of its artifacts. To take a newer one: its id is in the URL of its
    * page (https://github.com/love2d/love/actions), and the ids of its artifacts are given by
    * https://api.github.com/repos/love2d/love/actions/runs/<id>/artifacts.
    */
  object Build {
    val run     = 40249789777L // commit ef388d4371, 2026-10-26
    val windows = Artifact("love-windows-x64", 12620500299L)
    val macos   = Artifact("love-macos", 12619719960L)
    val linux   = Artifact("love-linux-X64.AppImage", 12620014816L)
  }

  final case class Artifact(name: String, id: Long)

  /** A file added to a package: its path, content and Unix mode. */
  private final case class Added(path: String, content: Array[Byte], mode: Int = 420 /* 0644 */)

  /** The packages of the game directory `game`, in `out`, LÖVE's builds being downloaded once in
    * `cache`. `title` names the macOS app; in camel case, the other files.
    */
  def apply(game: File, title: String, cache: File, out: File, log: Logger): Seq[File] = {
    val name     = title.split(" ").map(_.capitalize).mkString
    val loveFile = out / s"$name.love"
    IO.delete(out)
    IO.createDirectory(out)
    // A .love is a zip of the game directory, main.lua at its root
    IO.zip(Path.allSubpaths(game), loveFile, None)
    val love = IO.readBytes(loveFile)
    val packages = Seq(
      windows(unwrapped(download(Build.windows, cache, log), cache), name, love, out),
      macos(unwrapped(download(Build.macos, cache, log), cache), title, name, love, out),
      linux(appImage(cache, log), name, love, out)
    )
    (loveFile +: packages).foreach(file => log.info(s"Packaged $file"))
    loveFile +: packages
  }

  /** LÖVE's AppImage (for Linux x64), downloaded once in `cache` (also to run the game in dev). */
  def appImage(cache: File, log: Logger): File = {
    val target = cache / "love.AppImage"
    if (!target.exists()) {
      val found = withZip(download(Build.linux, cache, log))(zip =>
        zip.getEntries.asScala.find(_.getName.endsWith(".AppImage")).map(entry => IO.readBytes(zip.getInputStream(entry)))
      )
      IO.write(target, found.getOrElse(sys.error(s"No AppImage in ${Build.linux.name}")))
      target.setExecutable(true)
    }
    target
  }

  /** The zip of the artifact (GitHub wraps what was uploaded in a zip). */
  private def download(artifact: Artifact, cache: File, log: Logger): File = {
    val target = cache / s"${artifact.name}.zip"
    if (!target.exists()) {
      val token = sys.env.get("GITHUB_TOKEN").orElse(sys.env.get("GH_TOKEN")).filter(_.nonEmpty).getOrElse(
        sys.error(
          s"LÖVE ${loveVersion}'s builds come from GitHub, which needs a token: set GITHUB_TOKEN (any token of a GitHub " +
            s"account), or put ${artifact.name}.zip (from https://github.com/love2d/love/actions/runs/${Build.run}) in $cache"
        )
      )
      import java.net.http.{HttpClient, HttpRequest, HttpResponse}
      val client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()
      val api    = s"https://api.github.com/repos/love2d/love/actions/artifacts/${artifact.id}/zip"
      log.info(s"Downloading ${artifact.name} (LÖVE's run ${Build.run})")
      // GitHub answers with a redirection to where the zip is, which must be fetched without the token
      val redirect = client.send(
        HttpRequest.newBuilder(new URI(api)).header("Authorization", s"Bearer $token").build(),
        HttpResponse.BodyHandlers.discarding()
      )
      // (not Optional.orElse, which evaluates its argument first)
      val location = Option(redirect.headers().firstValue("location").orElse(null)).getOrElse(
        sys.error(s"GitHub did not give ${artifact.name} (HTTP ${redirect.statusCode()}): it may have expired")
      )
      val part = cache / s"${artifact.name}.zip.part"
      IO.createDirectory(cache)
      val response = client.send(HttpRequest.newBuilder(new URI(location)).build(), HttpResponse.BodyHandlers.ofFile(part.toPath))
      if (response.statusCode() != 200) sys.error(s"Downloading ${artifact.name}: HTTP ${response.statusCode()}")
      IO.move(part, target)
    }
    target
  }

  /** The zip that was uploaded in the artifact, when there is one (e.g. `love-12.0-win64.zip`, next
    * to the installer), otherwise the artifact itself.
    */
  private def unwrapped(artifact: File, cache: File): File = withZip(artifact) { zip =>
    zip.getEntries.asScala.find(entry => !entry.isDirectory && entry.getName.endsWith(".zip")) match {
      case Some(uploaded) =>
        val inner = cache / s"${artifact.base}-inner.zip"
        if (!inner.exists()) IO.write(inner, IO.readBytes(zip.getInputStream(uploaded)))
        inner
      case None => artifact
    }
  }

  /** The folder (possibly none) of the zip's entry whose path ends with `suffix`. */
  private def folderOf(loveZip: File, suffix: String): String = withZip(loveZip) { zip =>
    zip.getEntries.asScala
      .map(_.getName)
      .find(path => path == suffix || path.endsWith("/" + suffix))
      .map(_.stripSuffix(suffix))
      .getOrElse(sys.error(s"$suffix not found in $loveZip"))
  }

  /** love.exe with the game appended (LÖVE runs the game it finds at the end of its executable),
    * and its DLLs.
    */
  private def windows(loveZip: File, name: String, love: Array[Byte], out: File): File = {
    val folder = folderOf(loveZip, "love.exe")
    val exe    = readEntry(loveZip, s"${folder}love.exe")
    repack(loveZip, out / s"$name-windows.zip",
      path =>
        Some(path).filter(_.startsWith(folder)).map(_.stripPrefix(folder))
          .filter(f => !f.contains("/") && (f.endsWith(".dll") || f == "license.txt")).map(f => s"$name/$f"),
      Seq(Added(s"$name/$name.exe", exe ++ love, 493 /* 0755 */)))
  }

  /** love.app, renamed, with the game in its resources, and its own name and identifier (not
    * LÖVE's, and not opening .love files).
    */
  private def macos(loveZip: File, title: String, name: String, love: Array[Byte], out: File): File = {
    val app       = s"$title.app/"
    val folder    = folderOf(loveZip, "love.app/Contents/Info.plist")
    val plistPath = s"${folder}love.app/Contents/Info.plist"
    val plist     = new String(readEntry(loveZip, plistPath), UTF_8)
    def replaced(text: String, regex: String, replacement: String, required: Boolean): String = {
      val result = text.replaceAll(regex, replacement)
      require(!required || result != text, s"$regex not found in LÖVE's Info.plist")
      result
    }
    val ownPlist = Seq(
      ("(<key>CFBundleIdentifier</key>\\s*<string>)[^<]*", s"$$1org.scalalove.${name.toLowerCase}", true),
      ("(<key>CFBundleName</key>\\s*<string>)[^<]*", s"$$1$title", true),
      // (if any) the end of its array is the first one indented like its key, entries of the dictionary
      ("(?s)\n([ \t]+)<key>UTExportedTypeDeclarations</key>\\s*\n\\1<array>.*?\n\\1</array>", "", false)
    ).foldLeft(plist) { case (text, (regex, replacement, required)) => replaced(text, regex, replacement, required) }
    repack(loveZip, out / s"$name-macos.zip",
      path =>
        Some(path).filter(p => p.startsWith(s"${folder}love.app/") && p != plistPath).map(p => app + p.stripPrefix(s"${folder}love.app/")),
      Seq(
        Added(s"${app}Contents/Info.plist", ownPlist.getBytes(UTF_8)),
        Added(s"${app}Contents/Resources/$name.love", love)
      ))
  }

  /** LÖVE's AppImage, the game, and a launcher. The AppImage runs without FUSE (which recent
    * distributions do not install by default): it then unpacks itself in /tmp.
    */
  private def linux(appImage: File, name: String, love: Array[Byte], out: File): File = {
    val launcher =
      s"""#!/bin/sh
         |HERE="$$(dirname "$$(readlink -f "$$0")")"
         |export APPIMAGE_EXTRACT_AND_RUN=1
         |exec "$$HERE/${appImage.getName}" "$$HERE/$name.love" "$$@"
         |""".stripMargin
    val files = Seq(
      Added(s"$name/${appImage.getName}", IO.readBytes(appImage), 493),
      Added(s"$name/$name.love", love),
      Added(s"$name/$name", launcher.getBytes(UTF_8), 493)
    )
    val target = out / s"$name-linux.tar.gz"
    Using.fileOutputStream()(target) { file =>
      val tar = new TarArchiveOutputStream(new GzipCompressorOutputStream(file))
      tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
      for (added <- files) {
        val entry = new TarArchiveEntry(added.path)
        entry.setSize(added.content.length.toLong)
        entry.setMode(added.mode)
        tar.putArchiveEntry(entry)
        tar.write(added.content)
        tar.closeArchiveEntry()
      }
      tar.finish()
      tar.close()
    }
    target
  }

  private def readEntry(zip: File, path: String): Array[Byte] = withZip(zip) { zipFile =>
    val entry = Option(zipFile.getEntry(path)).getOrElse(sys.error(s"$path not found in $zip"))
    val in    = zipFile.getInputStream(entry)
    try IO.readBytes(in)
    finally in.close()
  }

  /** The entries of `loveZip` that `rename` keeps, under their new path, and the added files. */
  private def repack(loveZip: File, target: File, rename: String => Option[String], added: Seq[Added]): File =
    withZip(loveZip) { zipFile =>
      IO.createDirectory(target.getParentFile)
      val zip = new ZipArchiveOutputStream(target)
      try {
        def put(path: String, unixMode: Option[Int], content: => InputStream): Unit = {
          val entry = new ZipArchiveEntry(path)
          unixMode.foreach(entry.setUnixMode)
          zip.putArchiveEntry(entry)
          val in = content
          try IO.transfer(in, zip)
          finally in.close()
          zip.closeArchiveEntry()
        }
        for (entry <- zipFile.getEntries.asScala; path <- rename(entry.getName)) {
          val unixMode = Some(entry.getUnixMode).filter(_ => entry.getPlatform == ZipArchiveEntry.PLATFORM_UNIX)
          put(path, unixMode, zipFile.getInputStream(entry))
        }
        // 0100000: a regular file
        for (file <- added) put(file.path, Some(0x8000 | file.mode), new ByteArrayInputStream(file.content))
      } finally zip.close()
      target
    }

  private def withZip[A](file: File)(f: ZipFile => A): A = {
    val zipFile = ZipFile.builder().setFile(file).get()
    try f(zipFile)
    finally zipFile.close()
  }
}
