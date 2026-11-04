#!/usr/bin/env bash
# Checks that the UDP socket of the LÖVE client (love-client/src/main/lua/udp.lua and LuaUDPSocket) works, by linking
# bffclient.lua.UdpSelfTest to Lua and running it, headless, with LÖVE (`love` must be installed). It only uses the
# loopback interface.
set -euo pipefail

HERE="$(cd "$(dirname "$0")/.." && pwd)"
# LÖVE 12: LOVE, or the AppImage that `sbt love-client/luaRun` (or luaPackage) downloads, or `love`
LOVE="${LOVE:-$(ls "$HERE"/target/love-12.0/love.AppImage 2>/dev/null || echo love)}"
export APPIMAGE_EXTRACT_AND_RUN=1
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT

cd "$HERE"
# `luaLink` links the client's main class: link the self test instead, with the same classpath
sbt -batch -error love-client/compile
# The classpath is the line with jars: sbt may print other lines after it (e.g. an escape sequence when CI is set)
CP="$(sbt -batch -error "export love-client/Compile/fullClasspath" | grep -E '\.jar' | tail -1)"
sbt -batch -error "lua-linker/runMain scalalove.linker.Main bffclient.lua.UdpSelfTest $OUT $CP" > /dev/null
cp -r love-client/src/main/lua/. "$OUT"/
BFF_HEADLESS=1 timeout 60 "$LOVE" "$OUT"
