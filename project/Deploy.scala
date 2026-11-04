import java.io.File

import scala.sys.process.{Process, ProcessLogger}

import sbt._

/** Deploys the menu server on a VM (see deploy.md): builds its Docker image (which only copies what
  * sbt built), uploads it, and (re)starts it there, behind Caddy, which does the TLS (HTTPS, with a
  * certificate of Let's Encrypt, see deploy/Caddyfile).
  *
  * Only runs `docker`, `ssh` and `scp` (which Windows 10 and later have too), and no shell script
  * or pipe, so that it works the same on all the platforms. The image goes through a file, since
  * the pipes of some shells corrupt binary data.
  */
object Deploy {
  val image     = "battle-for-flatland"
  val container = "bff"

  /** Caddy's image (2.10 at least, for the certificates of IP addresses), and its container. */
  val caddyImage     = "caddy:2.10.2"
  val caddyContainer = "bff-caddy"

  /** @param target
    *   `<user>@<host>` of the VM (or a host of `~/.ssh/config`)
    * @param platform
    *   the VM's processor, `linux/amd64` or `linux/arm64`
    * @param key
    *   the private SSH key to use, if not the default ones (or the ones of `~/.ssh/config`)
    * @param site
    *   what the players type to reach the menu server, which its certificate is for: the VM's
    *   public IP address, or a domain name that points to it
    */
  final case class Args(target: String, platform: String, key: Option[String], site: String)

  object Args {
    val usage = "<user@host> [<linux/amd64 | linux/arm64>] [--key <ssh key file>] [--site <IP address or domain>]"

    private val IpAddress = """\d{1,3}(\.\d{1,3}){3}"""

    def parse(args: Seq[String]): Args = {
      val (positional, options) = split(args.toList, Nil, Map.empty)
      val (target, platform) = positional match {
        case target :: Nil             => (target, "linux/amd64")
        case target :: platform :: Nil => (target, platform)
        case _                         => sys.error(s"Usage: deploy $usage")
      }
      // the host of the target, when it is an IP address (not a host of ~/.ssh/config)
      val site = options.get("--site").orElse(Some(target.split('@').last).filter(_.matches(IpAddress))).getOrElse(
        sys.error(s"--site is needed (the VM's public IP address, or its domain name). Usage: deploy $usage")
      )
      Args(target, platform, options.get("--key"), site)
    }

    @scala.annotation.tailrec
    private def split(
        rest: List[String],
        positional: List[String],
        options: Map[String, String]
    ): (List[String], Map[String, String]) = rest match {
      case (option @ ("--key" | "--site")) :: value :: tail => split(tail, positional, options + (option -> value))
      case (option @ ("--key" | "--site")) :: Nil => sys.error(s"$option needs a value. Usage: deploy $usage")
      case arg :: tail                             => split(tail, positional :+ arg, options)
      case Nil                                     => (positional, options)
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

    // from `work`, with relative names: scp takes `C:\...` for a host
    IO.copyFile(root / "deploy" / "Caddyfile", work / "Caddyfile")
    log.info(s"Uploading the image (${(work / archive).length() / 1000000} MB) to ${args.target}")
    try run(Seq("scp") ++ ssh ++ Seq(archive, "Caddyfile", s"${args.target}:"), work, log)
    finally IO.delete(work / archive)

    // On the host's network: no docker NAT for the UDP datagrams, and the VM's firewall rules (opened by
    // oracle-vm-setup.sh) are the ones that count. --restart: the server comes back after a reboot.
    // The menu server's HTTP only listens on the loopback, for Caddy, which keeps its certificates in a
    // volume (they outlive the deploys: Let's Encrypt limits how many it gives).
    log.info(s"(Re)starting the server, behind Caddy for ${args.site}")
    run(
      Seq("ssh") ++ ssh ++ Seq(
        args.target,
        s"gunzip -c $archive | docker load && rm -f $archive && " +
          s"{ docker rm -f $container $caddyContainer >/dev/null 2>&1 || true; } && " +
          s"docker run -d --name $container --restart unless-stopped --network host -e HTTP_ADDRESS=127.0.0.1 $image && " +
          s"docker run -d --name $caddyContainer --restart unless-stopped --network host -e BFF_SITE='${args.site}' " +
          "-v \"$HOME/Caddyfile:/etc/caddy/Caddyfile:ro\" -v bff-caddy-data:/data -v bff-caddy-config:/config " +
          caddyImage
      ),
      root,
      log
    )

    log.info(s"Running. Logs: ssh ${args.target} docker logs -f $container (and $caddyContainer, for the certificate)")
    log.info(s"Check: curl https://${args.site}/api")
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
