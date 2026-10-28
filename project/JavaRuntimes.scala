import java.io.{BufferedInputStream, InputStream}
import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{Files, Path => JPath}
import java.nio.file.attribute.PosixFilePermission
import java.security.MessageDigest

import scala.collection.JavaConverters._
import scala.sys.process.Process

import org.apache.commons.compress.archivers.tar.{TarArchiveEntry, TarArchiveInputStream, TarArchiveOutputStream}
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.compress.compressors.gzip.{GzipCompressorInputStream, GzipCompressorOutputStream}
import sbt._
import sbt.io.Using

/** The Java runtimes that the players run the game server with, so that they do not need to install
  * Java: one per platform, made by jlink with only the modules the game server's jar needs (jdeps).
  *
  * jlink links the runtimes of all the platforms from the build's computer, with the jmods of each
  * platform's JDK (Temurin's builds, downloaded once, and checked against their SHA-256). jlink must
  * be the very same version as these jmods: it is the one of the JDK of the build's platform.
  *
  * Each runtime is a `<platform>.tar.gz` (its files at its root, the symbolic links resolved), with
  * its manifest, `bff-runtime.sha256`: the SHA-256 of each of its files (the format of `sha256sum`),
  * also written next to it as `<platform>.sha256`. The SHA-256 of the manifest is the runtime's
  * version, with which the players check what they unpacked.
  */
object JavaRuntimes {
  val release = "jdk-21.0.12+8"

  /** Name of the manifest, in the runtimes. */
  val manifestName = "bff-runtime.sha256"

  /** A platform's JDK: the platform as the players' client names it, its archive and SHA-256. */
  final case class Jdk(platform: String, file: String, sha256: String)

  val jdks: Seq[Jdk] = Seq(
    Jdk(
      "linux-x64",
      "OpenJDK21U-jdk_x64_linux_hotspot_21.0.12_8.tar.gz",
      "e4446ff06a276155697597cc0f1b15da004ff083f4964a35271ecee567177370"
    ),
    Jdk(
      "macos-x64",
      "OpenJDK21U-jdk_x64_mac_hotspot_21.0.12_8.tar.gz",
      "6b85c260eea574a995eacd0b3ee23c8042aa93b23a08e6478edafca0a0333d7f"
    ),
    Jdk(
      "macos-arm64",
      "OpenJDK21U-jdk_aarch64_mac_hotspot_21.0.12_8.tar.gz",
      "021d629349ebc12a409faa517b837ec80ceee8f58a5ac85c788ecad07ca6881c"
    ),
    Jdk(
      "windows-x64",
      "OpenJDK21U-jdk_x64_windows_hotspot_21.0.12_8.zip",
      "9ba963ee2371874a74185d18bc7bb2ab9407df7683300855ed7606e0662321d0"
    )
  )

  /** Not seen by jdeps: loaded as services (the elliptic curves of TLS). */
  private val extraModules = Seq("jdk.crypto.ec")

  /** The runtimes for the game server's `jar`, in `out` (the JDKs being downloaded once in
    * `cache`).
    */
  def apply(jar: File, cache: File, out: File, log: Logger): Seq[File] = {
    val host      = hostJdk(cache, log)
    val modules   = moduleDependencies(host, jar, log)
    val work      = cache / "jlink"
    IO.delete(out)
    IO.createDirectory(out)
    jdks.map { jdk =>
      val jmods = this.jmods(jdk, cache, log)
      val image = work / jdk.platform
      IO.delete(image)
      log.info(s"Linking the Java runtime of ${jdk.platform}")
      run(
        Seq(
          (host / "bin" / executable("jlink")).getAbsolutePath,
          "--module-path", jmods.getAbsolutePath,
          "--add-modules", modules.mkString(","),
          // not --strip-debug: on Linux, it strips the native symbols too, with objcopy (Linux binaries only)
          "--strip-java-debug-attributes", "--no-man-pages", "--no-header-files",
          "--compress=zip-6",
          "--output", image.getAbsolutePath
        ),
        log
      )
      val archive = out / s"${jdk.platform}.tar.gz"
      val manifest = pack(image, archive)
      IO.write(out / s"${jdk.platform}.sha256", manifest)
      log.info(s"Java runtime of ${jdk.platform}: $archive (${archive.length() / 1000000} MB)")
      archive
    }
  }

  /** The modules that the jar needs (jdeps, of the JDK `jdk`). */
  private def moduleDependencies(jdk: File, jar: File, log: Logger): Seq[String] = {
    val printed = Process(
      Seq(
        (jdk / "bin" / executable("jdeps")).getAbsolutePath,
        "--ignore-missing-deps", "--print-module-deps", "--multi-release", "21",
        jar.getAbsolutePath
      )
    ).!!.trim
    val modules = (printed.split(",").map(_.trim).filter(_.nonEmpty) ++ extraModules).distinct.sorted
    log.info(s"Modules of the Java runtimes: ${modules.mkString(", ")}")
    modules.toSeq
  }

  private def run(command: Seq[String], log: Logger): Unit = {
    val status = Process(command) ! log
    if (status != 0) sys.error(s"${command.head} failed ($status)")
  }

  private def windowsHost = System.getProperty("os.name").toLowerCase.contains("windows")

  private def executable(name: String) = if (windowsHost) s"$name.exe" else name

  /** The whole JDK of the build's platform (to run jlink and jdeps), unpacked in the cache. */
  private def hostJdk(cache: File, log: Logger): File = {
    val os = System.getProperty("os.name").toLowerCase
    val arch = System.getProperty("os.arch") match {
      case "amd64" | "x86_64" => "x64"
      case "aarch64" | "arm64" => "arm64"
      case other => other
    }
    val platform =
      if (os.contains("windows")) s"windows-$arch"
      else if (os.contains("mac")) s"macos-$arch"
      else s"linux-$arch"
    val jdk = jdks.find(_.platform == platform).getOrElse(
      sys.error(s"No JDK for $platform, the platform of the build: add it to JavaRuntimes.jdks")
    )
    val home = cache / release / platform / "jdk"
    if (!(home / "bin" / executable("jlink")).exists()) {
      IO.delete(home)
      // the JDK's own directory at the root of the archive, and Contents/Home in the macOS ones
      val root = s"$release/" + (if (platform.startsWith("macos")) "Contents/Home/" else "")
      unpack(download(jdk, cache, log), path => Some(path).filter(_.startsWith(root)).map(_.stripPrefix(root)), home)
    }
    home
  }

  /** The jmods of a platform's JDK, unpacked in the cache. */
  private def jmods(jdk: Jdk, cache: File, log: Logger): File = {
    val jmods = cache / release / jdk.platform / "jmods"
    if (!(jmods / "java.base.jmod").exists()) {
      IO.delete(jmods)
      unpack(
        download(jdk, cache, log),
        path => Some(path).filter(p => p.contains("/jmods/") && p.endsWith(".jmod")).map(_.split('/').last),
        jmods
      )
    }
    jmods
  }

  private def download(jdk: Jdk, cache: File, log: Logger): File = {
    val target = cache / release / jdk.file
    if (!target.exists()) {
      val url =
        s"https://github.com/adoptium/temurin21-binaries/releases/download/${release.replace("+", "%2B")}/${jdk.file}"
      log.info(s"Downloading $url")
      val part = cache / release / s"${jdk.file}.part"
      IO.createDirectory(part.getParentFile)
      Using.urlInputStream(new URI(url).toURL)(in => IO.transfer(in, part))
      val sha256 = Using.fileInputStream(part)(sha256Hex)
      if (sha256 != jdk.sha256) {
        IO.delete(part)
        sys.error(s"${jdk.file}: SHA-256 $sha256, expected ${jdk.sha256}")
      }
      IO.move(part, target)
    }
    target
  }

  /** The entries of the JDK's archive that `rename` keeps, under their new path, keeping their Unix
    * modes and symbolic links (tar.gz) — a zip for Windows.
    */
  private def unpack(archive: File, rename: String => Option[String], out: File): Unit = {
    IO.createDirectory(out)
    if (archive.getName.endsWith(".zip")) {
      val zipFile = ZipFile.builder().setFile(archive).get()
      try
        for (entry <- zipFile.getEntries.asScala if !entry.isDirectory; path <- rename(entry.getName)) {
          val target = out / path
          IO.createDirectory(target.getParentFile)
          val in = zipFile.getInputStream(entry)
          try IO.transfer(in, target)
          finally in.close()
        }
      finally zipFile.close()
    } else
      Using.fileInputStream(archive) { file =>
        val tar = new TarArchiveInputStream(new GzipCompressorInputStream(new BufferedInputStream(file)))
        var entry = tar.getNextEntry
        while (entry != null) {
          for (path <- rename(entry.getName) if path.nonEmpty && !entry.isDirectory) {
            val target = out / path
            IO.createDirectory(target.getParentFile)
            if (entry.isSymbolicLink) Files.createSymbolicLink(target.toPath, JPath.of(entry.getLinkName))
            else {
              IO.transfer(tar, target)
              if ((entry.getMode & 0x49 /* 0111 */) != 0) target.setExecutable(true)
            }
          }
          entry = tar.getNextEntry
        }
      }
  }

  /** The files of `image` in a tar.gz, the symbolic links resolved (they are not all supported on
    * Windows), with its manifest; returns the manifest.
    */
  private def pack(image: File, archive: File): String = {
    val files = Path
      .allSubpaths(image)
      .filter(_._1.isFile) // follows the symbolic links
      .map { case (file, path) => (file, path.replace('\\', '/')) }
      .toVector
      .sortBy(_._2)
    val manifest = files
      .map { case (file, path) => s"${Using.fileInputStream(file)(sha256Hex)}  $path\n" }
      .mkString
    Using.fileOutputStream()(archive) { out =>
      val tar = new TarArchiveOutputStream(new GzipCompressorOutputStream(out))
      tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
      def put(path: String, mode: Int, size: Long)(write: => Unit): Unit = {
        val entry = new TarArchiveEntry(path)
        entry.setSize(size)
        entry.setMode(mode)
        entry.setModTime(0L)
        tar.putArchiveEntry(entry)
        write
        tar.closeArchiveEntry()
      }
      for ((file, path) <- files)
        put(path, mode(file.toPath), file.length())(Using.fileInputStream(file)(in => IO.transfer(in, tar)))
      val manifestBytes = manifest.getBytes(UTF_8)
      put(manifestName, 420 /* 0644 */, manifestBytes.length.toLong)(tar.write(manifestBytes))
      tar.finish()
      tar.close()
    }
    manifest
  }

  /** 0755 or 0644, as the file is executable or not (jlink makes the programs of a runtime
    * executable, whatever its platform).
    */
  private def mode(file: JPath): Int = {
    val executable = scala.util
      .Try(Files.getPosixFilePermissions(file).contains(PosixFilePermission.OWNER_EXECUTE))
      .getOrElse(Files.isExecutable(file))
    if (executable) 493 else 420
  }

  def sha256Hex(in: InputStream): String = {
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = new Array[Byte](1 << 16)
    var read   = in.read(buffer)
    while (read >= 0) {
      digest.update(buffer, 0, read)
      read = in.read(buffer)
    }
    digest.digest().map("%02x".format(_)).mkString
  }
}
