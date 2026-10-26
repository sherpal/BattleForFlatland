#!/usr/bin/env bash
# Plays a game through the menu server, like a player: the LÖVE client logs in, creates a game, gets ready, starts it, receives the game server
# credentials on the lobby websocket, then plays. Headless by default (with scripted menus).
#   --window                    plays in a window, with the menus' UI
#   --screenshot FRAMES EVENTS  runs the window through tools/screenshot (see there), e.g.
#                               --screenshot 30,90 "text@10:alice,return@20"
# BFF_CLIENT runs another client than love-client/target/lua (headless or --window), e.g. the Linux
# package of `sbt love-client/luaPackage`: BFF_CLIENT=path/to/BattleForFlatland/BattleForFlatland
#
# Runs the menu server (in a temporary working directory, where it keeps its JSON "database"),
# the game server launcher, and stops them (and the game server) at the end.
#
# Prerequisites: `sbt game-server/assembly love-client/luaLink`
set -euo pipefail

HERE="$(cd "$(dirname "$0")/.." && pwd)"
# Our servers need the ports of the menu server (9000) and the launcher (22223). If they are taken (e.g.
# by servers started by hand), stop before doing anything: we would talk to those servers, and stop
# their game servers.
if ss -ltn | grep -qE ':(9000|22223)\b'; then
  echo "Ports 9000 or 22223 are already in use (the servers running?): stop them first." >&2
  exit 1
fi

WORK="$(mktemp -d)"

stop_game_servers() {
  for pid in $(ps -eo pid,args | awk '$2 == "java" && /game-server\.jar/ {print $1}'); do kill "$pid" 2>/dev/null || true; done
}
pids=()
cleanup() {
  for pid in "${pids[@]}"; do kill "$pid" 2>/dev/null || true; done
  # Wait for them to exit, so that the next run does not talk to the dying ones
  for pid in "${pids[@]}"; do wait "$pid" 2>/dev/null || true; done
  stop_game_servers
  rm -rf "$WORK"
}
trap cleanup EXIT

# Classpaths of the menu server and the launcher (`server/assembly` would also build the frontend)
(cd "$HERE" && sbt -batch "export server/Runtime/fullClasspath" "export game-server-launcher/Runtime/fullClasspath") \
  | grep -E '\.jar' > "$WORK/classpaths"
SERVER_CP="$(sed -n 1p "$WORK/classpaths")"
LAUNCHER_CP="$(sed -n 2p "$WORK/classpaths")"

# BFF's game servers keep running after their game, and always use port 22222
stop_game_servers

(cd "$WORK" && exec java -DisProd=false -cp "$SERVER_CP" server.Server) > "$WORK/menu-server.log" 2>&1 &
pids+=($!)
(cd "$HERE" && exec java -cp "$LAUNCHER_CP" server.Server) > "$WORK/launcher.log" 2>&1 &
pids+=($!)
for _ in $(seq 1 60); do
  curl -s -o /dev/null http://127.0.0.1:9000/api/users/me && ss -ltn | grep -q ":22223" && break
  sleep 0.5
done
curl -s -o /dev/null http://127.0.0.1:9000/api/users/me || { tail "$WORK/menu-server.log"; exit 1; }

export BFF_MENU_URL=http://127.0.0.1:9000 BFF_USER="${BFF_USER:-alice}" BFF_CLASS="${BFF_CLASS:-Triangle}"
client=(love "$HERE/love-client/target/lua")
[[ -z "${BFF_CLIENT:-}" ]] || client=("$BFF_CLIENT")
if [[ "${1:-}" == "--window" ]]; then
  "${client[@]}"
elif [[ "${1:-}" == "--screenshot" ]]; then
  timeout 180 love "$HERE/tools/screenshot" "$HERE/love-client/target/lua" "$2" "${3:-}"
else
  BFF_HEADLESS=1 timeout 120 "${client[@]}"
fi
