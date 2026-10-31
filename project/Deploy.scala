import java.io.File

import scala.sys.process.{Process, ProcessLogger}

import sbt._

/** Deploys the menu server on a VM (see deploy.md): builds its Docker image (which only copies what
  * sbt built), uploads it, and (re)starts it there.
  *
  * Only runs `docker`, `ssh` and `scp` (which Windows 10 and later have too), and no shell script
  * or pipe, so that it works the same on all the platforms. The image goes through a file, since
  * the pipes of some shells corrupt binary data.
  */
object Deploy {
  val image     = "battle-for-flatland"
  val container = "bff"

  /** @param target
    *   `<user>@<host>` of the VM (or a host of `~/.ssh/config`)
    * @param platform
    *   the VM's processor, `linux/amd64` or `linux/arm64`
    * @param key
    *   the private SSH key to use, if not the default ones (or the ones of `~/.ssh/config`)
    */
  final case class Args(target: String, platform: String, key: Option[String])

  object Args {
    val usage = "<user@host> [<linux/amd64 | linux/arm64>] [--key <ssh key file>]"

    def parse(args: Seq[String]): Args = {
      val (positional, key) = split(args.toList, Nil, None)
      positional match {
        case target :: Nil             => Args(target, "linux/amd64", key)
        case target :: platform :: Nil => Args(target, platform, key)
        case _                         => sys.error(s"Usage: deploy $usage")
      }
    }

    @scala.annotation.tailrec
    private def split(
        rest: List[String],
        positional: List[String],
        key: Option[String]
    ): (List[String], Option[String]) = rest match {
      case "--key" :: file :: tail => split(tail, positional, Some(file))
      case "--key" :: Nil          => sys.error(s"--key needs a file. Usage: deploy $usage")
      case arg :: tail             => split(tail, positional :+ arg, key)
      case Nil                     => (positional, key)
    }
  }

  /** @param root
    *   the repository, where the Dockerfile is
    * @param work
    *   where to put the image's archive, temporarily
    */
  def apply(root: File, work: File, args: Args, log: Logger): Unit = {
    val ssh = args.key.toSeq.flatMap(key => Seq("-o", "IdentitiesOnly=yes", "-i", expandHome(key)))

    log.info(s"Building the image for ${args.platform}")
    run(Seq("docker", "build", "--platform", args.platform, "-t", image, root.getAbsolutePath), root, log)

    IO.createDirectory(work)
    val archive = "bff-image.tar.gz"
    val tar     = work / "bff-image.tar"
    log.info("Saving the image")
    run(Seq("docker", "save", "-o", tar.getAbsolutePath, image), root, log)
    IO.gzip(tar, work / archive)
    IO.delete(tar)

    // from `work`, with a relative name: scp takes `C:\...` for a host
    log.info(s"Uploading the image (${(work / archive).length() / 1000000} MB) to ${args.target}")
    try run(Seq("scp") ++ ssh ++ Seq(archive, s"${args.target}:"), work, log)
    finally IO.delete(work / archive)

    // On the host's network: no docker NAT for the UDP datagrams, and the VM's firewall rules (opened by
    // oracle-vm-setup.sh) are the ones that count. --restart: the server comes back after a reboot.
    log.info("(Re)starting the server")
    run(
      Seq("ssh") ++ ssh ++ Seq(
        args.target,
        s"gunzip -c $archive | docker load && rm -f $archive && " +
          s"{ docker rm -f $container >/dev/null 2>&1 || true; } && " +
          s"docker run -d --name $container --restart unless-stopped --network host $image"
      ),
      root,
      log
    )

    log.info(s"Running. Logs: ssh ${args.target} docker logs -f $container")
    log.info(s"Check: curl http://${args.target.split('@').last}:9000/api")
  }

  /** `~` is for the shell, which sbt is not. */
  private def expandHome(path: String): String =
    if (path == "~" || path.startsWith("~/") || path.startsWith("~\\"))
      System.getProperty("user.home") + path.drop(1)
    else path

  private def run(command: Seq[String], cwd: File, log: Logger): Unit = {
    // docker, ssh and scp print their progress on stderr: not an error
    val status = Process(command, cwd) ! ProcessLogger(line => log.info(line), line => log.info(line))
    if (status != 0) sys.error(s"${command.head} failed ($status)")
  }
}
