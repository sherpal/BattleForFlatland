# Deploying the menu server on a VM (Oracle Cloud free tier)

The menu server needs **HTTPS** (TCP 443, the menus) and **UDP** (port 9000: the rendezvous of the
hole punching, and the lobby's pokes) on a public IPv4 address that nothing proxies in UDP. Most
platforms-as-a-service do not offer that; a plain virtual machine does, and Oracle Cloud's "Always
Free" tier gives some at no cost. (Check their current free-tier terms: they change. Any VM with a
public IPv4, on any provider, works the same way with the steps below.)

What runs there is the Docker image of the [Dockerfile](Dockerfile), with `--network host`, its HTTP
only on the loopback, behind [Caddy](https://caddyserver.com) (in a container of its own, see
[deploy/Caddyfile](deploy/Caddyfile)), which does the TLS: it gets a certificate from
[Let's Encrypt](https://letsencrypt.org), for the VM's IP address (Let's Encrypt gives some since
January 2026: they last six days, and Caddy renews them) or for a domain name that points to it, and
renews it by itself. The image only copies files that are built on your computer, which the `deploy`
task of sbt does for you.

| | |
|---|---|
| `deploy/oracle-vm-setup.sh` | runs **on the VM**, once: installs Docker, opens ports 80 and 443 (TCP) and 9000 (UDP) in its firewall |
| `sbt deploy` (`project/Deploy.scala`) | runs **on your computer**, on any system: builds, uploads the image and the Caddyfile to the VM, (re)starts the server and Caddy |

## 1. Create the VM

In the Oracle Cloud console (the labels move around, but the choices are these):

1. Create an account (a credit card is asked, for verification).
2. *Compute → Instances → Create instance*:
   - **Image**: Ubuntu (22.04 or 24.04). The default user is then `ubuntu`.
   - **Shape**: one that is marked *Always Free-eligible*. The ARM ones (`VM.Standard.A1.Flex`,
     Ampere) have the most memory: 1 OCPU and 6 GB is plenty. The x86 `VM.Standard.E2.1.Micro` has only
     1 GB, which is tight for the JVM. The ARM ones are sometimes "out of capacity": try again later,
     or in another availability domain.
   - **Networking**: put it in a public subnet, with a **public IPv4 address** assigned.
   - **SSH keys**: upload yours (or let Oracle generate one, and keep the private key).
3. Note the VM's public IP address, and its processor: `linux/arm64` for the Ampere shapes,
   `linux/amd64` for the others.

## 2. Open the port in the cloud's network

The VM's firewall is not the only one: the virtual network has its own rules, which let in only SSH.

*Networking → Virtual cloud networks →* the VM's VCN *→ Security → Security lists →* the subnet's
list *→ Add ingress rules*, three of them:

| Source CIDR | IP protocol | Destination port |
|---|---|---|
| `0.0.0.0/0` | TCP | `443` (HTTPS) |
| `0.0.0.0/0` | TCP | `80` (for Let's Encrypt to check that the address is ours) |
| `0.0.0.0/0` | UDP | `9000` |

These are **destination** ports: leave the *source* port range empty (all). Putting them as the source
port makes the rules match nothing, and connections just time out. (The TCP port 9000 of a previous
deploy can be closed: the menu server only listens on the loopback now.)

## 3. Prepare the VM

Copy the script to the VM, and run it there:

```bash
scp deploy/oracle-vm-setup.sh ubuntu@<ip>:
ssh ubuntu@<ip> sudo ./oracle-vm-setup.sh
```

(Add `-i <private key>` to `ssh` and `scp` if the key is not your default one.)

## 4. Ship the server

From the repository, on your computer (Linux, macOS or Windows: it only needs `sbt`, `docker`, `ssh`
and `scp` in the `PATH`, which Windows 10 and later have for the last two):

```
BFF_MENU_SERVER=<site> sbt "deploy ubuntu@<ip> linux/amd64 --key ~/.ssh/oracle.key"
```

- the platform is the VM's processor: `linux/amd64` (the default) or `linux/arm64` (`uname -m` on the VM
  says `x86_64` or `aarch64`);
- `--key` is the private key, if not one of your default ones (`~` and `$HOME`-like paths work, on Windows
  too: `--key C:/Users/you/.ssh/oracle.key`). Without it, ssh uses its own rules, which include the hosts
  of your `~/.ssh/config`, so `sbt "deploy oracle --site <ip>"` works with a `Host oracle` entry there.
- `--site` is what the players type, which the certificate is for: the VM's IP address (the default,
  when the target is one), or a domain name that points to it (better: the IP address of a VM may
  change; see [the README](README.md#the-domain-name) for the one of the game).
- `BFF_MENU_SERVER` is the menu server that the packaged clients connect to (they do not ask for it):
  `battle-for-flatland.eu` by default, so it is only needed for another site. It is the build's
  `love-client/packagedMenuServer` setting, which can also be changed in sbt, e.g. `set
  love-client/packagedMenuServer := "81.12.34.56"`.
- Each package has a version (a hash of the game, which the menu server gives at
  `/api/bff/client-version`): when the players' one is not the deployed one, their login screen tells them
  to download the game again, with a button to the landing page.
- LÖVE 12's builds, which the clients are packaged with, need a GitHub token the first time (see
  [the client's README](love-client/README.md#playing-with-friends)): `GITHUB_TOKEN=...`.

It runs the build (jars, Java runtimes, clients), builds the image for that processor, saves it
(compressed: about 370 MB) and uploads it with `scp`, and starts it on the VM with Caddy in front of it,
both restarting automatically after a reboot. Caddy gets the certificate within a minute (`docker logs
bff-caddy`); it keeps it in a Docker volume, which outlives the deploys.

Do it again to deploy a new version.

## 5. Check

```bash
curl https://<site>/api                       # Hello World!
curl -o /dev/null -w '%{http_code}\n' https://<site>/api/bff/client/linux   # 200
ssh ubuntu@<ip> docker logs bff                 # ... rendezvous on UDP 0.0.0.0:9000
ssh ubuntu@<ip> docker logs bff-caddy           # ... certificate obtained successfully
```

Players then download the client from the landing page, `https://<site>/` (which goes to `/en`,
`/fr`... in their language), or straight from `https://<site>/api/bff/client/<windows|macos|linux>` (see
[the client's README](love-client/README.md#playing-with-friends)), which connect to `<site>`. The hole punching is the real test: two players on different networks, in the same
game, and the logs of the VM (`docker logs bff`) and of the clients (`client.log`).

## Good to know

- **The IP address** of the VM is the one to give to the players. It may change if the VM is
  recreated: Oracle can *reserve* a public IP for you (free while it is attached to a VM), which then
  stays.
- **Memory**: the JVM takes up to 75% of what the machine has.
- **Data**: the server's data (`target/db` in the container) is lost when the container is
  removed, i.e. at each `sbt deploy`.
- **Logs and restarts**: `ssh <vm> docker logs -f bff`, `docker restart bff`.
- **TLS**: the menus are in HTTPS (with LÖVE 12's HTTPS module, which checks the certificate against
  the system's: a self-signed one would not do). The UDP datagrams are authenticated with the secrets
  of the games, not encrypted (see [game-hosting.md](game-hosting.md)). There are still no passwords:
  anyone with the address can log in with any pseudo.
- **Floods**: the menu server limits what each IP address can ask (`server/RequestLimits.scala`: a
  429 past that, mostly for the big downloads), and Caddy closes the connections that are too slow to
  send their requests. That is against one computer that floods it, not a real DDoS from many
  addresses, which only the network in front of the VM can absorb: Oracle's network has a basic
  protection of its own, and a proxy like Cloudflare would hide the VM for the website, but not for the UDP of the
  rendezvous, which needs its address.
- **Certificates**: Let's Encrypt limits how many it gives for the same address (a few a week): the
  volume `bff-caddy-data` keeps them across the deploys, do not remove it.
- **Not on Oracle**: on another provider, only the network rules of step 2 change (the provider's
  firewall or security group: TCP 80 and 443, UDP 9000). On Fly.io, see [fly.toml](fly.toml): its UDP needs a
  paid dedicated IPv4.
- **Not tested here**: these scripts were written without an Oracle VM to run them on.
