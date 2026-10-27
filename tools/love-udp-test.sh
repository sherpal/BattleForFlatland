#!/usr/bin/env bash
# Checks that the UDP socket of the LÖVE client (love-client/src/main/lua/udp.lua and LuaUDPSocket) works, by linking
# bffclient.lua.UdpSelfTest to Lua and running it, headless, with LÖVE (`love` must be installed). It only uses the
# loopback interface.
set -euo pipefail

HERE="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT

cd "$HERE"
# `luaLink` links the client's main class: link the self test instead, with the same classpath
sbt -batch -error love-client/compile
CP="$(sbt -batch -error "export love-client/Compile/fullClasspath" | tail -1)"
sbt -batch -error "lua-linker/runMain scalalove.linker.Main bffclient.lua.UdpSelfTest $OUT $CP" > /dev/null
cp -r love-client/src/main/lua/. "$OUT"/
BFF_HEADLESS=1 timeout 60 love "$OUT"
