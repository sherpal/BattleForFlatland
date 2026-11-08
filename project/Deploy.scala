import java.io.{File, IOException}
import java.net.{HttpURLConnection, InetAddress, ServerSocket, URI}

import scala.sys.process.{Process, ProcessLogger}

import sbt._

/** Deploys the menu server on a VM (see deploy.md): builds its Docker image (which only copies what
  * sbt built), pushes it to a registry on the VM (through an SSH tunnel: it only listens on the VM's
  * loopback), and (re)starts it there, behind Caddy, which does the TLS (HTTPS, with a certificate of
  * Let's Encrypt, see deploy/Caddyfile). The registry keeps the layers of the previous deploy, so
  * that only the changed ones are uploaded.
  *
  * Only runs `docker`, `ssh` and `scp` (which Windows 10 and later have too), and no shell script
  * or pipe, so that it works the same on all the platforms.
  */
object Deploy {
  val image     = "battle-for-flatland"
  val container = "bff"

  /** Caddy's image (2.10 at least, for the certificates of IP addresses), and its container. */
  val caddyImage     = "caddy:2.10.2"
  val caddyContainer = "bff-caddy"

  /** The registry on the VM, on its loopback, which the tunnel reaches on the same port of this computer
    * (docker accepts registries without TLS on localhost).
    */
  val registryImage     = "registry:3.0.0"
  val registryContainer = "bff-registry"
  val registryPort      = 5050
  val imageInRegistry   = s"localhost:$registryPort/$image"

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
    *   where to put the Caddyfile, temporarily
    */
  def apply(root: File, work: File, args: Args, log: Logger): Unit = {
    val ssh = args.key.toSeq.flatMap(key => Seq("-o", "IdentitiesOnly=yes", "-i", expandHome(key)))

    log.info(s"Building the image for ${args.platform}")
    run(
      Seq("docker", "build", "--platform", args.platform, "-t", image, "-t", imageInRegistry, root.getAbsolutePath),
      root,
      log
    )

    // from `work`, with a relative name: scp takes `C:\...` for a host
    IO.createDirectory(work)
    IO.copyFile(root / "deploy" / "Caddyfile", work / "Caddyfile")
    log.info(s"Uploading the Caddyfile, and starting the registry on ${args.target}")
    run(Seq("scp") ++ ssh ++ Seq("Caddyfile", s"${args.target}:"), work, log)
    run(
      Seq("ssh") ++ ssh ++ Seq(
        args.target,
        s"docker start $registryContainer >/dev/null 2>&1 || " +
          s"docker run -d --name $registryContainer --restart unless-stopped --network host " +
          s"-e REGISTRY_HTTP_ADDR=127.0.0.1:$registryPort -v bff-registry:/var/lib/registry $registryImage"
      ),
      root,
      log
    )

    // Only the layers that the registry does not have yet are uploaded (the Dockerfile puts the ones that
    // change the least first).
    log.info(s"Pushing the image to the registry of ${args.target}, through an SSH tunnel")
    // otherwise what answers there would be something else of this computer, the tunnel failing to take it
    try new ServerSocket(registryPort, 0, InetAddress.getLoopbackAddress).close()
    catch {
      case _: IOException => sys.error(s"The port $registryPort is taken on this computer: the tunnel needs it")
    }
    val tunnel = Process(
      Seq("ssh") ++ ssh ++ Seq(
        "-N",
        "-o",
        "ExitOnForwardFailure=yes",
        "-L",
        s"$registryPort:127.0.0.1:$registryPort",
        args.target
      ),
      root
    ).run(ProcessLogger(line => log.info(line), line => log.info(line)))
    try {
      waitForRegistry(tunnel)
      run(Seq("docker", "push", imageInRegistry), root, log)
    } finally tunnel.destroy()

    // On the host's network: no docker NAT for the UDP datagrams, and the VM's firewall rules (opened by
    // oracle-vm-setup.sh) are the ones that count. --restart: the server comes back after a reboot.
    // The menu server's HTTP only listens on the loopback, for Caddy, which keeps its certificates in a
    // volume (they outlive the deploys: Let's Encrypt limits how many it gives).
    // Then the previous image goes, from docker and from the registry (whose garbage collection removes the
    // layers that the new image does not have; the restart empties its cache of the layers it has).
    log.info(s"(Re)starting the server, behind Caddy for ${args.site}")
    run(
      Seq("ssh") ++ ssh ++ Seq(
        args.target,
        s"docker pull $imageInRegistry && " +
          s"{ docker rm -f $container $caddyContainer >/dev/null 2>&1 || true; } && " +
          s"docker run -d --name $container --restart unless-stopped --network host -e HTTP_ADDRESS=127.0.0.1 $imageInRegistry && " +
          s"docker run -d --name $caddyContainer --restart unless-stopped --network host -e BFF_SITE='${args.site}' " +
          "-v \"$HOME/Caddyfile:/etc/caddy/Caddyfile:ro\" -v bff-caddy-data:/data -v bff-caddy-config:/config " +
          s"$caddyImage && " +
          "docker image prune -f >/dev/null && " +
          s"{ docker exec $registryContainer registry garbage-collect --delete-untagged /etc/distribution/config.yml >/dev/null 2>&1 && " +
          s"docker restart $registryContainer >/dev/null || echo 'The garbage collection of the registry failed' >&2; }"
      ),
      root,
      log
    )

    log.info(s"Running. Logs: ssh ${args.target} docker logs -f $container (and $caddyContainer, for the certificate)")
    log.info(s"Check: curl https://${args.site}/api")
  }

  /** Waits until the registry answers through the tunnel. */
  private def waitForRegistry(tunnel: Process): Unit = {
    val deadline = System.currentTimeMillis() + 30000
    def answers =
      try {
        val connection =
          new URI(s"http://localhost:$registryPort/v2/").toURL.openConnection().asInstanceOf[HttpURLConnection]
        connection.setConnectTimeout(1000)
        connection.setReadTimeout(5000)
        try connection.getHeaderField("Docker-Distribution-Api-Version") != null
        finally connection.disconnect()
      } catch { case _: IOException => false }
    while (!answers) {
      if (!tunnel.isAlive()) sys.error("The SSH tunnel to the registry closed")
      if (System.currentTimeMillis() > deadline) sys.error(s"The registry did not answer on localhost:$registryPort")
      Thread.sleep(500)
    }
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
