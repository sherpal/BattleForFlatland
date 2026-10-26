# The LÖVE client

The game's client is a [LÖVE](https://love2d.org) (11.5) game written in Scala: the code is compiled
by Scala.js, and its IR is linked to Lua by [`lua-linker`](../lua-linker/README.md) instead of
JavaScript. It uses the game's `shared` code as it is (game logic, models, codecs), like the former
browser frontend did, and talks to the same servers.

```bash
sbt love-client/luaLink   # writes the program in love-client/target/lua
sbt love-client/luaRun    # links it and runs it with LÖVE (`love` must be installed)
```

To play, the menu server and the game server launcher must run (see the main README). On the login
screen, type the menu server's address (`127.0.0.1` locally; `host` or `host:port`, port 9000 by
default).

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
* `ui` is a small immediate mode UI toolkit;
* `love` holds the facades of LÖVE's API (with `LoveInput`), and `lua` those of our own Lua modules
  (`src/main/lua`: `websocket.lua`, `http.lua`, `images.lua`, and `conf.lua`, LÖVE's configuration).

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
  `GameConfiguration.validate` has nothing to say. The game server credentials then arrive on the
  lobby websocket, and the game starts. The panel on the right (`DocsPanel`) documents the boss and
  the classes (life, resource, description, and each ability with its icon, cast time, cooldown and
  cost); it scrolls with the mouse wheel.

The server, pseudo, class, colour and boss are remembered in `menu.json` in LÖVE's save directory
(`~/.local/share/love/battle-for-flatland/` on Linux).

The "Controls" button (in the games' list and in the lobby) opens the controls: click a binding,
then press the key (possibly with Shift) or the mouse button to bind to it; keys bound twice are
pointed out, the mouse over can be toggled, and everything can be reset. They are saved in
`controls.json` (which also accepts what the browser frontend stored in its local storage under the
key `controls`). The client's own settings, which the game's `Controls` cannot hold, are there too
and saved in `settings.json`: the size of the HUD, from 0.5 to 2.

The UI toolkit (`ui/Ui.scala`) is immediate mode: every frame, the screens call widgets that draw
themselves and return what happened, e.g. `if ui.button("Join", rect) then join()`. It has labels,
wrapped text, panels, buttons, text fields (UTF-8 aware, with keyboard focus), check boxes,
`< value >` selectors, segmented buttons and colour swatches; screens position widgets with `Rect`s.

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
end of the game arrives like any other. Leaving goes back to the menus; the game's server is stopped
only if we are its only human player. When the game ends (or the connection is lost), an overlay
shows the outcome, with "Back to menus" and "Quit". Going back stops the game's server through the
menu server (game servers do not stop by themselves after their game, and all use port 22222) and
leaves the game (the menu server never ends games by itself, so the players would otherwise stay
"already playing").

## Testing

`tools/love-client-test.sh` (at the root of the repository) runs the menu server (from a temporary
directory, where it keeps its JSON files) and the game server launcher from their sbt classpaths,
plays a game with the client, then stops everything. It needs `sbt game-server/assembly
love-client/luaLink` first, and refuses to run if the servers' ports are taken (e.g. by servers
started by hand).

```bash
tools/love-client-test.sh            # headless, scripted menus
tools/love-client-test.sh --window   # with the menus' UI
# Through tools/screenshot: log in as alice and create a game
tools/love-client-test.sh --screenshot 50,100 "text@10:alice,return@20,click@70:445:125"
```

Headless (`BFF_HEADLESS`), the menus are scripted (`MenuSession.scala`): log in, create a game, pick
`BFF_CLASS` and get ready, choose `BFF_BOSS`, start, play; the client walks and casts a bit, logs
its status every second, and quits at the end of the game. `BFF_LAG_MS` simulates a round trip of
latency (half on each way), e.g. `BFF_LAG_MS=300 tools/love-client-test.sh --window`.
`BFF_CLIENT=<launcher>` plays with another client, e.g. the Linux package.

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

The packages are not signed: on Windows, SmartScreen asks to confirm ("More info", then "Run
anyway"); on macOS, the app must be unquarantined and signed again (the game added to LÖVE's app
breaks its signature), in a terminal, once:

```bash
xattr -cr "Battle for Flatland.app"
codesign --force --deep --sign - "Battle for Flatland.app"
```

To host the game, run the menu server and the game server launcher as usual (`server/reStart`
listens on all interfaces; not with `-DisProd=false`, which only listens on 127.0.0.1), and forward
TCP ports **9000** (menu server) and **22222** (game servers) of the router to that machine (and
open them in its firewall, if any). The launcher's port (22223) stays private. Players then type
the router's public address on the login screen, e.g. `81.12.34.56`; the game server is reached at
the same address. When the address changes, they type the new one (or use a dynamic DNS name: host
names work too). There are no passwords: anyone with the address can log in, with any pseudo.

The connection is plain HTTP and websockets (LuaSocket has no TLS).
