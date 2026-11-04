#!/usr/bin/env bash
# Prepares an Ubuntu VM (made for the Oracle Cloud free tier, see deploy.md) to run the menu server:
# installs Docker, and opens in the VM's firewall the TCP ports 443 (HTTPS, to Caddy, in front of the
# menu server) and 80 (for Let's Encrypt to check that the address is ours), and the UDP port 9000 (the
# rendezvous and the lobby). Run it on the VM, once (it can be run again):
#
#   sudo ./oracle-vm-setup.sh
#
# The ports also have to be open in the cloud's own network rules, which this cannot do (see deploy.md).
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
echo "==> Opening the ports 80 and 443 (TCP) and $PORT (UDP) in the VM's firewall"
for rule in "tcp 80" "tcp 443" "udp $PORT"; do
  set -- $rule
  iptables -C INPUT -p "$1" --dport "$2" -j ACCEPT 2>/dev/null \
    || iptables -I INPUT 1 -p "$1" --dport "$2" -j ACCEPT
done
# survives reboots
netfilter-persistent save

echo
echo "Done. Docker $(docker --version | cut -d' ' -f3 | tr -d ,) is installed, 80 and 443 (TCP) and $PORT (UDP) are open."
echo "Log out and in again before using docker without sudo, then ship the server from your computer:"
echo "  sbt \"deploy <user>@<this VM's public IP> <linux/arm64 or linux/amd64>\""
