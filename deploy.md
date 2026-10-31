# Deploying the menu server on a VM (Oracle Cloud free tier)

The menu server needs **TCP** (HTTP, websockets) and **UDP** (the rendezvous of the hole punching)
on the same port, 9000, on a public IPv4 address that nothing proxies. Most platforms-as-a-service
do not offer that; a plain virtual machine does, and Oracle Cloud's "Always Free" tier gives some at
no cost. (Check their current free-tier terms: they change. Any VM with a public IPv4, on any
provider, works the same way with the steps below.)

What runs there is the Docker image of the [Dockerfile](Dockerfile), with `--network host`. The image
only copies files that are built on your computer, which the `deploy` task of sbt does for you.

| | |
|---|---|
| `deploy/oracle-vm-setup.sh` | runs **on the VM**, once: installs Docker, opens port 9000 (TCP and UDP) in its firewall |
| `sbt deploy` (`project/Deploy.scala`) | runs **on your computer**, on any system: builds, uploads the image to the VM, (re)starts the server |

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
list *→ Add ingress rules*, two of them:

| Source CIDR | IP protocol | Destination port |
|---|---|---|
| `0.0.0.0/0` | TCP | `9000` |
| `0.0.0.0/0` | UDP | `9000` |

The **destination** port is 9000: leave the *source* port range empty (all). Putting 9000 as the source
port makes the rule match nothing, and connections just time out.

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
sbt "deploy ubuntu@<ip> linux/amd64 --key ~/.ssh/oracle.key"
```

- the platform is the VM's processor: `linux/amd64` (the default) or `linux/arm64` (`uname -m` on the VM
  says `x86_64` or `aarch64`);
- `--key` is the private key, if not one of your default ones (`~` and `$HOME`-like paths work, on Windows
  too: `--key C:/Users/you/.ssh/oracle.key`). Without it, ssh uses its own rules, which include the hosts
  of your `~/.ssh/config`, so `sbt "deploy oracle"` works with a `Host oracle` entry there.

It runs the build (jars, Java runtimes, clients), builds the image for that processor, saves it
(compressed: about 370 MB) and uploads it with `scp`, and starts it on the VM, restarting automatically after
a reboot.

Do it again to deploy a new version.

## 5. Check

```bash
curl http://<ip>:9000/api                       # Hello World!
curl -o /dev/null -w '%{http_code}\n' http://<ip>:9000/api/bff/client/linux   # 200
ssh ubuntu@<ip> docker logs bff                 # ... rendezvous on UDP 0.0.0.0:9000
```

Players then download the client from `http://<ip>:9000/api/bff/client/<windows|macos|linux>` (see
[the client's README](love-client/README.md#playing-with-friends)), or use theirs, and type `<ip>` on
the login screen. The hole punching is the real test: two players on different networks, in the same
game, and the logs of the VM (`docker logs bff`) and of the clients (`client.log`).

## Good to know

- **The IP address** of the VM is the one to give to the players. It may change if the VM is
  recreated: Oracle can *reserve* a public IP for you (free while it is attached to a VM), which then
  stays.
- **Memory**: the JVM takes up to 75% of what the machine has.
- **Data**: the server's data (`target/db` in the container) is lost when the container is
  removed, i.e. at each `sbt deploy`.
- **Logs and restarts**: `ssh <vm> docker logs -f bff`, `docker restart bff`.
- **No TLS**: the connections are plain HTTP and UDP (LuaSocket has no TLS), and there are no
  passwords: anyone with the address can log in with any pseudo. It is fine for tests, not more.
- **Not on Oracle**: on another provider, only the network rules of step 2 change (the provider's
  firewall or security group: TCP and UDP 9000). On Fly.io, see [fly.toml](fly.toml): its UDP needs a
  paid dedicated IPv4.
- **Not tested here**: these scripts were written without an Oracle VM to run them on.
