# The LÖVE client

The game's client is a [LÖVE](https://love2d.org) (11.5) game written in Scala: the code is compiled
by Scala.js, and its IR is linked to Lua by [`lua-linker`](../lua-linker/README.md) instead of
JavaScript. It uses the game's `shared` code as it is (game logic, models, codecs), like the former
browser frontend did, and talks to the same servers.

```bash
sbt love-client/luaLink   # writes the program in love-client/target/lua
sbt love-client/luaRun    # links it and runs it with LÖVE (`love` must be installed)
```

To play, the menu server must run (see the main README). On the login screen, type the menu
server's address (`127.0.0.1` locally; `host` or `host:port`, port 9000 by default).

## Code organisation

The code (`src/main/scala/bffclient`) is organised by package:

* `Main` starts the menus or the game;
* `game` is the client side of a game (`GameClient`, `AbilityCaster`);
* `render` draws it (`Renderer`, `Painter`, `Sprites`, `Effects`, `Smoothing`, `FrameRate`), with
  the bosses' visuals in `render.bosses` (one `BossVisuals` per boss);
* `hud` draws the HUD (`Hud`, `ActionBar`, in the style of `HudStyle`) and the overlays (game menu,
  end of game);
* `controls` loads, saves and edits the controls and the client's settings;
* `menu` holds the menus' screens and the menu server's API;
* `hosting` runs the game server on this computer, when the menu server chooses us to;
* `ui` is a small immediate mode UI toolkit;
* `love` holds the facades of LÖVE's API (with `LoveInput`), and `lua` those of our own Lua modules
  (`src/main/lua`: `websocket.lua`, `http.lua`, `udp.lua`, `process.lua`, `images.lua`, and `conf.lua`, LÖVE's configuration).

The images (`src/main/lua/assets`) come from the former browser frontend; `luaLink` copies them
next to `main.lua` with the Lua modules. Without an image, the client draws shapes instead.

## Menus

The menus go through the menu server, over HTTP and the lobby websocket, with the game's own
request/response types and circe codecs (`MenuApi.scala`). HTTP requests run on a LÖVE thread
(`http.lua`, `http_worker.lua`, LuaSocket), so the game loop never blocks; `HttpClient` returns
`Future`s. The websocket is a small RFC 6455 client in Lua on top of LuaSocket (bundled with LÖVE),
`websocket.lua`.

The screens (package `menu`, one file per screen) are:

* **login**: type the menu server's address and choose a pseudo (validated with
  `LoginUser.validator`);
* **games**: the games that did not start, refreshed live from the lobby websocket; create one, or
  join one (or join back the one you are in);
* **game lobby**: the players with their class, colour and readiness; choose your class and colour,
  get ready; the creator chooses the boss, kicks players, adds AIs and launches the game once
  `GameConfiguration.validate` has nothing to say. One of the players' clients then runs the game
  server (see "Hosting the game server" below), its addresses and our credentials arrive on the
  lobby websocket, and the game starts. The panel on the right (`DocsPanel`) documents the boss and
  the classes (life, resource, description, and each ability with its icon, cast time, cooldown and
  cost); it scrolls with the mouse wheel.

The server, pseudo, class, colour and boss are remembered in `menu.json` in LÖVE's save directory
(`~/.local/share/love/battle-for-flatland/` on Linux).

The "Settings" button (in the games' list and in the lobby) opens the settings, in two tabs. In
"Controls": click a binding, then press the key (possibly with Shift) or the mouse button to bind to
it; keys bound twice are pointed out, the mouse over can be toggled, and everything can be reset.
They are saved in `controls.json` (which also accepts what the browser frontend stored in its local
storage under the key `controls`). In "General", the client's own settings, which the game's
`Controls` cannot hold, saved in `settings.json`: the size of the HUD, from 0.5 to 2, and whether to
host game servers (on by default; see below).

The UI toolkit (`ui/Ui.scala`) is immediate mode: every frame, the screens call widgets that draw
themselves and return what happened, e.g. `if ui.button("Join", rect) then join()`. It has labels,
wrapped text, panels, buttons, text fields (UTF-8 aware, with keyboard focus), check boxes,
`< value >` selectors, segmented buttons and colour swatches; screens position widgets with `Rect`s.

## UDP

`udp.lua` gives non-blocking UDP sockets on top of LuaSocket, and `LuaUDPSocket` (package `lua`) is the
game's `UDPSocket` (`shared`) on top of it, so that `communication.udp.Connection` and `Envelope` can be
used as they are, like the game server does. There are no threads: the datagrams received are
collected at the start of every frame (with the timers, see `lua-linker`) and given to the message
callback, so a message is seen up to a frame late. A socket that never `bind`s gets a port from the
system when it first sends. Sending to a host name resolves it first, which blocks, once per name.
`tools/love-udp-test.sh` checks all of that in LÖVE, on the loopback interface, `Connection` included.

The game is played over UDP (`game/GameLink.scala`): the client punches every address of the game
server it was given, and registers at the menu server's rendezvous meanwhile, so that the game server
punches back through its NAT (`Rendezvous`, see [game-hosting.md](../game-hosting.md)). It says hello
to the first address that answers, with its credentials, until the server answers (`Envelope`), then synchronizes its
clock with the server's (`Connection.computeLinkTime`), and only then starts the game, whose messages
all go through a `Connection`: the ones that have to arrive do (`OrderedReliable`: the handshake, the
actions the server sends, our casts), our position is sent as often as before but is allowed to get
lost (`Ordered`: only the latest matters), except when starting or stopping (`Reliable`). The
connection is deemed lost when the server does not talk for 20 to 30 seconds. The websocket of the game
server is not used by the client anymore.

## The game

The client plays the start-of-game handshake with the game server (`Ready`, then `ReadyToStart`
once it received its entity id and the boss's position from the game master), and keeps a
client-side game state with the game's own `GreedyActionGatherer.slaveAddAndRemoveActions`.

Once every player is ready, they can move and place themselves before the boss comes: a "Start game"
button is drawn where the boss spawns. Clicking it starts a 5 seconds countdown, which every player
sees. The game server has no countdown (it spawns the boss when a player sends `LetsBegin`), so its
start is shared as a Star marker fixed exactly on the boss's spawn point (which nobody can place by
hand), the time of its action being the start of the countdown. At its end, every client sends
`LetsBegin` (the first one spawns the boss, the others are ignored), and the marker is removed.

**Inputs.** The game's `Controls` are used as they are: LÖVE scancodes are converted to the DOM key
codes they use (`LoveInput.scala`), then to `UserInput`s. Abilities are cast with the game's legality
checks (`AbilityCaster`), a click selects a target or places a zone, markers are sent as
`UpdateMarker`. With "mouse over" enabled in the controls, the abilities with a target fall back to
the entity whose frame is under the mouse when there is no target or the ability cannot be cast on
it (e.g. heal the team while keeping the boss as target).

**Prediction.** Our own movement is predicted: every frame, the client moves its player itself
(stopping at obstacles), sends the new position, and shows the server's game state with that
movement applied on top, instead of waiting one round trip for the server's echo. Positions come
from the frame's duration, so the speed does not depend on the frame rate (the game's
`lastValidPosition` ignores moves under 2 units, which happen above ~75 fps: small steps are checked
against the obstacles directly). Movements are sent when starting, stopping, turning or changing
direction, and about 60 times per second while moving.

**Smoothing.** The other entities are extrapolated from the last movement received, so they jump
when a turn or a stop arrives (one round trip late). Those jumps are smoothed when drawing
(`Smoothing.scala`): a jump becomes an offset from the extrapolated position, which decays in about
100 ms (jumps over 100 units, teleports, are shown as they are). Rotations turn towards the actual
one by the shortest way, in about 60 ms. The game logic keeps using the actual positions.

**Rendering.** The world (`Renderer`, `Painter`) is drawn around our player: the background, the
obstacles, the players with their class sprites tinted with their colour (with sparks while they
cast), the boss, zones and bullets in their owner's colour, life and cast bars, buff icons, markers.
The abilities have effects (`Effects.scala`): damage and heal numbers, the square's cleave, the
swing of the triangle's direct hit. What is specific to each boss is drawn by its `BossVisuals`
(package `render.bosses`), chosen by boss name: Boss 102's hounds and damage zones; Boss 104's big
guys, death marks, debuff circles and the finisher's triangle; Boss 105's Shade, the twins' auras and
bond, the Skewer's beam, the volatile charges, the reformation points, Siphon and Reunion; Sir of
Vice's Sacred Ground, Cleansing Nova and Holy Wrath. A `BossVisuals` can also tint the players'
frames (e.g. Boss 104's twin debuffs) and add a line to the HUD (Boss 102's hounds).

**HUD.** The frames of the team on the left (faded when out of healing range), our frame and our
target's above the action bar, our cast bar, the boss's frame on top, the cooldowns of the boss's
abilities on the top right and the threat meter on the bottom right. Clicking a frame targets its
entity. The action bar (`hud/ActionBar.scala`) shows the abilities' icons, with their key, their
cooldown, and a tooltip with what they do (the game's `AbilityDoc`).

**Window.** The window can be resized, and F11 (or the game menu) makes it fullscreen. The world is
scaled to show as much of it as in a 1200 x 800 window (more in the longer direction), while the
menus keep their size and the HUD has its own scale (the "HUD scale" setting): it is drawn as in a
window as much smaller or bigger, so that it stays at the window's edges, and its texts are drawn
with fonts of the scaled size, so that they stay sharp.

**Game menu and end of the game.** Escape opens a menu with "Resume", "Surrender", "Fullscreen",
"Leave game" and "Quit"; the game goes on meanwhile. Surrendering (confirmed with a second click)
ends the fight in a defeat for everyone: the client sends the game's own `EndGame` action, and the
end of the game arrives like any other. Leaving goes back to the menus. When the game ends (or the
connection is lost), an overlay shows the outcome, with "Back to menus" and "Quit". Going back leaves
the game (the menu server never ends games by itself, so the players would otherwise stay "already
playing"). The game server stops by itself a minute after everyone left it.

## Hosting the game server

When the game is launched, the menu server asks every client for its candidacy to run the game
server (`hosting/HostingAgent.scala`): whether the menu server has a Java runtime for its platform
(`linux-x64`, `macos-arm64`, ... from `love.system.getOS()` and LuaJIT's `jit.arch`), the version of
the `java` of the computer, if any (BFF_JAVA, otherwise the one of the PATH), whether it has everything
already, the number of processors, and how long a request to the menu server takes. It chooses one of
them, whose client runs the game server's jar in the background, its output going to
`game-servers/game-server.log` in LÖVE's save directory (`hosting/GameServerHost.scala`).

The clients download the jar and the Java runtime (a tar.gz, unpacked with `tar`) from the menu
server as soon as they enter the game's lobby (once per version, into `game-servers/` there, on an
HTTP worker of its own so that the menus' requests are not delayed), so that they are most likely
there when the game is launched. The lobby shows the progress of the download (the menu server gives
their size with their version). Their version is their SHA-256 (for the runtime, the one of its
manifest, `bff-runtime.sha256`, which has the SHA-256 of each of its files): the client checks them
against it, after the download and on its next runs (with LÖVE's `love.data.hash`, on a thread of its
own, `digest.lua`), and downloads them again when they do not match. Without a runtime for its
platform, the client runs the game server with the `java` of the computer, if it is 17 or later.
With "Host game servers" turned off in the settings, the client downloads nothing, and its candidacy
says that it does not host (turning it back on in the lobby starts the downloads).
BFF_GAME_SERVER_JAR runs that jar instead, and BFF_JAVA that `java` (without the runtime). Shell
commands run on a LÖVE thread (`process.lua`), so the menus never block. The game server outlives the
client that started it (the others may still be playing).

## Testing

`tools/love-client-test.sh` (at the root of the repository) runs the menu server (from a temporary
directory, where it keeps its JSON files) from its sbt classpath, plays a game with the client (which
hosts it: it downloads the game server from the menu server and runs it), then stops everything. It
needs `sbt game-server/assembly love-client/luaLink` first (and `game-server/gameServerRuntimes` for the
client to run the game server with the Java runtime, rather than with `java`), and refuses to run if
the menu server's port is taken (e.g. by a server started by hand).

```bash
tools/love-client-test.sh            # headless, scripted menus
tools/love-client-test.sh --window   # with the menus' UI
# Through tools/screenshot: log in as alice and create a game
tools/love-client-test.sh --screenshot 50,100 "text@10:alice,return@20,click@70:445:125"
```

Headless (`BFF_HEADLESS`), the menus are scripted (`MenuSession.scala`): log in, create a game, pick
`BFF_CLASS` and get ready, choose `BFF_BOSS`, start, play; the client walks and casts a bit, logs
its status every second, and quits at the end of the game. `BFF_LAG_MS` simulates a round trip of
latency (half on each way), e.g. `BFF_LAG_MS=300 tools/love-client-test.sh --window`, and
`BFF_LOSS_PERCENT` the loss of that percentage of the datagrams, in both directions (they are applied
to the game's UDP socket, `DelayedUDPSocket`).
`BFF_CLIENT=<launcher>` plays with another client, e.g. the Linux package.

`tools/love-udp-test.sh` links `bffclient.lua.UdpSelfTest` instead of the client, and runs it headless:
it checks the UDP sockets (see above), with no server involved.

`tools/screenshot` runs a LÖVE game with scripted inputs (keys, clicks, mouse moves, text, wheel)
and takes screenshots at given frames (see its `main.lua`).

## Playing with friends

`sbt love-client/luaPackage` packages the client with LÖVE's own release builds, so that players do
not need to install LÖVE (see [Game Distribution](https://love2d.org/wiki/Game_Distribution)). It
links the client, zips it into `love-client/target/dist/BattleForFlatland.love`, and writes next to
it:

* `BattleForFlatland-windows.zip`: `BattleForFlatland.exe` (`love.exe` with the game appended, which
  LÖVE runs) and LÖVE's DLLs;
* `BattleForFlatland-macos.zip`: `Battle for Flatland.app` (LÖVE's `love.app` with the game in its
  resources, and its own name and identifier);
* `BattleForFlatland-linux.tar.gz`: LÖVE's AppImage, the game, and a `BattleForFlatland` launcher
  (which runs the AppImage without FUSE, often not installed nowadays).

LÖVE's builds (11.5) are downloaded once into `target/love-11.5`, and their archives are copied
entry by entry (`project/LovePackage.scala`, with commons-compress), keeping the Unix modes and the
symbolic links of the macOS app.

What the client prints (and its uncaught errors) also goes to `client.log` in LÖVE's save directory
(`src/main/lua/console_log.lua`), started anew at each launch: on Windows, a LÖVE game has no console.
The save directory is `%APPDATA%\battle-for-flatland` with the Windows package,
`~/Library/Application Support/battle-for-flatland` with the macOS one, and
`~/.local/share/love/battle-for-flatland` with the Linux one (which runs the game with `love`).

The packages are not signed: on Windows, SmartScreen asks to confirm ("More info", then "Run
anyway"); on macOS, the app must be unquarantined and signed again (the game added to LÖVE's app
breaks its signature), in a terminal, once:

```bash
xattr -cr "Battle for Flatland.app"
codesign --force --deep --sign - "Battle for Flatland.app"
```

To host the menu server, run it as usual (`server/reStart` listens on all interfaces; not with
`-DisProd=false`, which only listens on 127.0.0.1), and forward the port **9000**, in **TCP** (menu
server) and in **UDP** (rendezvous), of the router to that machine (and open them in its firewall, if
any). Players then type the router's public address on the login screen, e.g. `81.12.34.56`. When the
address changes, they type the new one (or use a dynamic DNS name: host names work too). There are no
passwords: anyone with the address can log in, with any pseudo.

The menu server also serves the packages that `sbt love-client/luaPackage` made:
`api/bff/client/windows`, `macos`, `linux` (the archives) and `love` (the game alone), from
`love-client/target/dist` (or the `clientPackages` property). On Windows, in PowerShell (with the
menu server's address, and the game closed, since its files are replaced):

```powershell
$ProgressPreference = 'SilentlyContinue'; $zip = "$env:USERPROFILE\Downloads\BattleForFlatland-windows.zip"; Invoke-WebRequest http://81.12.34.56:9000/api/bff/client/windows -OutFile $zip; Expand-Archive -Force $zip "$env:USERPROFILE\Downloads"; & "$env:USERPROFILE\Downloads\BattleForFlatland\BattleForFlatland.exe"
```

The game servers run on the players' computers, and need no port forwarding: the players reach them
through UDP hole punching (see [game-hosting.md](../game-hosting.md)). The host's firewall may ask
whether to let `java` receive connections (it must). The menu server can run on the computer of one
of the players: their client and game server then find out their public address with a STUN server.

The connections are plain HTTP, websockets and UDP (LuaSocket has no TLS).
