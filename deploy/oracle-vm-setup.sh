#!/usr/bin/env bash
# Prepares an Ubuntu VM (made for the Oracle Cloud free tier, see deploy.md) to run the menu server:
# installs Docker, and opens the port 9000 (TCP: HTTP and websockets, UDP: the rendezvous) in the VM's
# firewall. Run it on the VM, once (it can be run again):
#
#   sudo ./oracle-vm-setup.sh
#
# The port also has to be open in the cloud's own network rules, which this cannot do (see deploy.md).
set -euo pipefail

PORT="${PORT:-9000}"

if [ "$(id -u)" -ne 0 ]; then
  echo "Run it with sudo: sudo $0" >&2
  exit 1
fi

export DEBIAN_FRONTEND=noninteractive

echo "==> Installing Docker"
apt-get update
apt-get install -y docker.io iptables-persistent
systemctl enable --now docker
# so that the user who ran sudo can use docker (after their next login)
if [ -n "${SUDO_USER:-}" ]; then
  usermod -aG docker "$SUDO_USER"
fi

# Ubuntu images of Oracle Cloud come with iptables rules that reject what is not allowed (apart from
# SSH). The container runs on the host's network (see project/Deploy.scala), so these are the rules in play.
echo "==> Opening port $PORT (TCP and UDP) in the VM's firewall"
for protocol in tcp udp; do
  iptables -C INPUT -p "$protocol" --dport "$PORT" -j ACCEPT 2>/dev/null \
    || iptables -I INPUT 1 -p "$protocol" --dport "$PORT" -j ACCEPT
done
# survives reboots
netfilter-persistent save

echo
echo "Done. Docker $(docker --version | cut -d' ' -f3 | tr -d ,) is installed and $PORT is open (TCP and UDP)."
echo "Log out and in again before using docker without sudo, then ship the server from your computer:"
echo "  sbt \"deploy <user>@<this VM's public IP> <linux/arm64 or linux/amd64>\""
