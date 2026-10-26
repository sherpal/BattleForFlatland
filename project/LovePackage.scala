import java.io.{ByteArrayInputStream, InputStream}
import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8

import scala.collection.JavaConverters._

import org.apache.commons.compress.archivers.tar.{TarArchiveEntry, TarArchiveOutputStream}
import org.apache.commons.compress.archivers.zip.{ZipArchiveEntry, ZipArchiveOutputStream, ZipFile}
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import sbt._
import sbt.io.Using

/** Packages a LÖVE game with LÖVE's own release builds, for players who do not have LÖVE
  * (https://love2d.org/wiki/Game_Distribution).
  *
  * LÖVE's archives are not unpacked: their entries are copied to the packages, keeping their Unix
  * modes (and so the symbolic links of the macOS app's frameworks).
  */
object LovePackage {
  val loveVersion = "11.5"

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
      windows(download(s"love-$loveVersion-win64.zip", cache, log), name, love, out),
      macos(download(s"love-$loveVersion-macos.zip", cache, log), title, name, love, out),
      linux(download(s"love-$loveVersion-x86_64.AppImage", cache, log), name, love, out)
    )
    (loveFile +: packages).foreach(file => log.info(s"Packaged $file"))
    loveFile +: packages
  }

  private def download(file: String, cache: File, log: Logger): File = {
    val target = cache / file
    if (!target.exists()) {
      val url = s"https://github.com/love2d/love/releases/download/$loveVersion/$file"
      log.info(s"Downloading $url")
      val part = cache / s"$file.part"
      IO.createDirectory(cache)
      Using.urlInputStream(new URI(url).toURL)(in => IO.transfer(in, part))
      IO.move(part, target)
    }
    target
  }

  /** love.exe with the game appended (LÖVE runs the game it finds at the end of its executable),
    * and its DLLs.
    */
  private def windows(loveZip: File, name: String, love: Array[Byte], out: File): File = {
    val folder = s"love-$loveVersion-win64/"
    val exe    = readEntry(loveZip, s"${folder}love.exe")
    repack(loveZip, out / s"$name-windows.zip",
      path => Some(path.stripPrefix(folder)).filter(f => f.endsWith(".dll") || f == "license.txt").map(f => s"$name/$f"),
      Seq(Added(s"$name/$name.exe", exe ++ love, 493 /* 0755 */)))
  }

  /** love.app, renamed, with the game in its resources, and its own name and identifier (not
    * LÖVE's, and not opening .love files).
    */
  private def macos(loveZip: File, title: String, name: String, love: Array[Byte], out: File): File = {
    val app       = s"$title.app/"
    val plistPath = "love.app/Contents/Info.plist"
    val plist     = new String(readEntry(loveZip, plistPath), UTF_8)
    def replaced(text: String, regex: String, replacement: String): String = {
      val result = text.replaceAll(regex, replacement)
      require(result != text, s"$regex not found in LÖVE's Info.plist")
      result
    }
    val ownPlist = Seq(
      ("(<key>CFBundleIdentifier</key>\\s*<string>)[^<]*", s"$$1org.scalalove.${name.toLowerCase}"),
      ("(<key>CFBundleName</key>\\s*<string>)[^<]*", s"$$1$title"),
      // The dictionary's entries are indented by one tab, so this is the end of its array
      ("(?s)\t<key>UTExportedTypeDeclarations</key>\n\t<array>.*?\n\t</array>\n", "")
    ).foldLeft(plist) { case (text, (regex, replacement)) => replaced(text, regex, replacement) }
    repack(loveZip, out / s"$name-macos.zip",
      path => Some(path).filter(p => p.startsWith("love.app/") && p != plistPath).map(p => app + p.stripPrefix("love.app/")),
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
