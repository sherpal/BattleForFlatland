# Hosting the game servers

A game's server (`game-server`) lives for the duration of the game. It runs on the computer of one of
the game's players, the **host**, chosen by the menu server (`server`) when the game is launched. The
players reach it in UDP, through their NATs, thanks to the menu server, which acts as the
**rendezvous**.

## Launching a game

```
everyone, on entering the lobby: downloads the Java runtime of their platform and the jar, if needed
                    (GET api/bff/game-server/runtime/version, .../runtime, .../version, .../jar),
                    unless they turned off "Host game servers" in their settings
creator ── POST start-game ──────────────▶ menu server        game marked as started, credentials created
everyone ◀── HostCandidacyRequested ─────  (lobby websocket)
everyone ── POST host-candidacy ─────────▶                    willing, java, ready, processors, round trip
           (everyone answered, or 5 s)                        HostCandidacy.ranking
everyone ◀── HostElected(host) ──────────
host     ◀── SpawnGameServer(id, secret, rendezvous port) ─
host: runs the jar with the runtime's java (waiting for the downloads to finish, if needed)
game server ── GET get-all-credentials ──▶                    the game, the players' credentials
game server ── ServerRegister (UDP) ─────▶ rendezvous         seen from its public address
game server ── GET game-server-ready ────▶                    its UDP port, its LAN addresses
everyone ◀── HereAreYourCredentials(addresses, rendezvous port)
```

* **The election** (`programs/launchGame.scala`, `services/hostelections`): the candidates are the
  players who did not turn off "Host game servers" in the settings of their client (e.g. on a mobile
  network: their client then downloads nothing), and for whose platform the menu server has a Java
  runtime, or whose `java` is 17 or later; the
  best one has at least 4 processors, then has everything already (nothing to download), then the
  fastest round trip to the menu server (in steps of 50 ms), then is the game creator
  (`HostCandidacy.ranking`, in `shared`).
* **When the host fails** (it says so, `POST host-failed`, e.g. the download failed; or the game
  server is not ready within 3 minutes), the next candidate takes over. Without any candidate left,
  everyone receives `GameLaunchFailed`, and the game can be launched again.
* **The game server** takes its configuration from its arguments,
  `<menu server host> <menu server port> <rendezvous port> <game id> <game secret> [<UDP port>]`, and
  asks the menu server for the rest. It listens to UDP on a port chosen by the system, its HTTP server
  only on the loopback. It stops by itself when nobody came within 3 minutes, or when everyone left
  for a minute (`Watchdog`).
* **The addresses** given to the players are the game server's public address (as the rendezvous sees
  it), its addresses on its local network, and, for the host only, the loopback.

## What runs the game server: the jar and a Java runtime

The players run the game server's fat jar (`sbt game-server/assembly`) with a Java runtime made for
it (`sbt game-server/gameServerRuntimes`, `project/JavaRuntimes.scala`): jlink links, from Temurin's
JDK of each platform, only the modules that the jar needs (jdeps), about 35 MB compressed. The menu
server serves both (`-DgameServerJar`, `-DgameServerRuntimes`), the runtimes by platform
(`linux-x64`, `macos-x64`, `macos-arm64`, `windows-x64`).

Their versions are fingerprints: the jar's is its SHA-256; a runtime's, the SHA-256 of its manifest
(`bff-runtime.sha256`, in the runtime, the SHA-256 of each of its files). The version routes give
them with the size of what is downloaded (`menus.data.GameServerFile`), so that the clients show the
progress of their downloads in the lobby (the HTTP worker tells the bytes received so far, every
tenth of a second). The clients keep them in
their save directory, under names made of their version, and check them, after the download and on
their next runs: what does not match is downloaded again (the jar), or downloaded and unpacked again
(the runtime). SHA-256 being a standard, the client computes it with LÖVE (`love.data.hash`, native
code, on a thread of its own), which gives the very same as the JVM's `MessageDigest`.

Without a runtime for their platform (e.g. Linux on ARM, or when the runtimes were not made), the
players need `java` 17 or later on their computer to host.

## Reaching the game server: UDP hole punching

A NAT only lets in the datagrams from an address that a socket behind it sent a datagram to. The
rendezvous (`server/RendezvousServer.scala`, UDP, on the menu server's port by default:
`-DrendezvousPort`) sees the addresses the NATs give to the game's sockets, and tells the game server
about the players' ones. The protocol is `communication.udp.Rendezvous` (in `shared`). Its datagrams
travel on the game's sockets, next to the `Envelope` ones (their first byte tells them apart).

```
game server ── ServerRegister ─────▶ rendezvous ── Players([(name, address)]) ──▶ game server  (every second)
player ─────── PlayerRegister ─────▶ rendezvous ── Registered(address) ────────▶ player       (until answered)
game server ── Punch ──▶ every player without a session      (opens the game server's NAT to them)
player ─────── Punch ──▶ every address of the game server    (opens the player's NAT to it)
game server ── Punch ──▶ player, in answer to theirs
player ─────── Hello ──▶ the first address that answered, then the game as before (Envelope, Connection)
```

The players punch before saying hello because a hello creates a session for the address it comes
from: saying hello to several addresses would make the game server believe that the player keeps
reconnecting from another one.

* **Players on the host's local network** reach it at its local address (their NAT is not involved),
  the host itself at the loopback.
* **Other players** reach it at its public address, once both NATs are open.

## Limitations, and what could come next

* **Symmetric NATs** (some corporate and mobile networks) give a different public address for every
  destination: punching does not work through them. A relay through the menu server (TURN-like)
  would be the fallback.
* **Spoofing**: like the rest of the UDP protocol, the addresses of the datagrams are not
  authenticated (only the credentials in them are checked).
* **The host leaving** the game does not stop it (the game server outlives its client), but if their
  computer or network goes away, the game is over: there is no migration of the game server.
* **Windows and macOS** are not tested: the client runs the game server with `start "" /B` on
  Windows, and the firewalls ask whether to let `java` in. The runtimes are unpacked with `tar` (in
  Windows since Windows 10).
* **The first game** of a player downloads about 80 MB (the runtime and the jar), while in the lobby.
* The election's criteria are rough (the round trip to the menu server says little about the upload
  bandwidth, which is what a host needs most).
