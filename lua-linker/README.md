# lua-linker

A Scala.js IR → Lua linker targeting [LÖVE](https://love2d.org/) (LuaJIT), with which the game's
client ([`love-client`](../love-client/README.md)) is compiled. It started as the ScalaLove proof
of concept.

The projects compiled to Lua use the settings of `project/LuaSettings.scala`:

| Task         | What it does                                                                         |
|--------------|--------------------------------------------------------------------------------------|
| `luaLink`    | Links the program to Lua (`target/lua/main.lua`), with the Lua modules of `src/main/lua` |
| `luaRun`     | Links the program and runs it with LÖVE                                              |
| `luaPackage` | Packages it with LÖVE's builds for Windows, macOS and Linux (`target/dist`)          |
| `luaProbe`   | Prints the linked classes and IR node statistics (debugging)                         |

The linker is run by sbt (`lua-linker/runMain scalalove.linker.Main <main class> <output dir>
<classpath>`), with the classpath of the project's compiled Scala.js IR.

## How it works

```
Scala sources ──scalac + Scala.js plugin──▶ .sjsir (Scala.js IR)
                                              │
                       Scala.js linker frontend (unchanged): loading,
                       reachability analysis, dead code elimination
                                              │
                                              ▼
                          lua-linker : Lua emitter ──▶ main.lua
```

* `lua-linker` is a plain JVM project depending on `scalajs-linker`. It runs the
  standard Scala.js frontend (optimizer disabled) and replaces the JavaScript
  backend with a Lua emitter:
  * `LuaEmitter.scala`: whole program layout (class tables, inheritance, runtime hooks, main).
  * `FunctionEmitter.scala`: IR trees → Lua statements/expressions.
  * `Naming.scala`: IR names → Lua identifiers.
  * `resources/scalalove/runtime.lua`: the runtime library, prepended to the output.
* Only reachable code is emitted: the LÖVE client links about 3200 classes (~196k lines of Lua).

### Value representation

| Scala                       | Lua                                                            |
|-----------------------------|----------------------------------------------------------------|
| `Int`, `Short`, `Byte`      | number, wrapped to 32 bits with `bit.tobit`                    |
| `Double`, `Float`           | number (`Float` rounded through the FFI)                       |
| `Long`                      | LuaJIT `int64_t` cdata (native 64-bit wraparound)              |
| `Char`                      | number (UTF-16 code unit); boxed as `{c = ...}` when upcast    |
| `Boolean`                   | boolean                                                        |
| `String`                    | Lua string (**byte** semantics for now)                        |
| `null`, `()`                | `nil`                                                          |
| objects                     | tables whose metatable is the class table (methods copied down)|
| arrays, `js.Array`          | 0-based tables with an explicit `length` field                 |

Control flow: labeled blocks and early returns use `goto` (LuaJIT supports it),
`try`/`catch`/`finally` use `pcall`.

Performance notes (LuaJIT):

* Scala lambdas are lifted to top-level functions; a lambda value is a table
  `{F.fn, captures...}`. LuaJIT cannot compile closure creation (`FNEW`), which
  otherwise aborts most traces in collection-heavy code.
* The runtime raises LuaJIT's machine-code and trace limits (`jit.opt.start`):
  with the defaults, big programs make LuaJIT flush and recompile its traces
  endlessly (10× slower on the game's logic).
* Lua allows 200 locals per function: huge methods (e.g. inlined Magnolia
  derivations) store their locals in a table instead.

Futures and timers: the runtime emulates the JS event loop (`setTimeout`,
`setInterval`, `queueMicrotask`), which Scala.js' global `ExecutionContext` uses.
In LÖVE, the pending tasks run at the start of every frame (`love.update` is
wrapped), so `Future`s and `js.timers` work as in the browser; elsewhere, call
`scalalove.runTimers()`.

Lua modules: `@JSImport("mod", ...)` facades are compiled to `require("mod")`,
and the `.lua` files of a project's `src/main/lua` are copied next to `main.lua`
by `luaLink`. The runtime also exposes a `scalalove` global with
`stringToBytes`/`bytesToString` to convert between Lua (binary) strings and
Scala `Array[Byte]`.

### Calling LÖVE

LÖVE is called through ordinary Scala.js facades (see the client's
`love-client/src/main/scala/bffclient/love/Love.scala`):

* A `@JSGlobal("love.graphics") object Graphics extends js.Object` is a Lua module:
  its methods are called with a dot, `love.graphics.rectangle(...)`.
* Other JS-typed values (e.g. a `Font` returned by `newFont`) are objects: their
  methods are called with a colon, `font:getWidth(...)`.
* Lua sequences (1-based tables, e.g. the lines returned by `Font:getWrap`) are
  not `js.Array`s (0-based, with a `length`): `scalalove.fromLuaSequence` and
  `toLuaSequence` convert them.
* Scala functions assigned to `js.Function` fields become plain Lua functions,
  so `Love.update = dt => ...` is `love.update = function(dt) ... end`.
* Lua functions returning several values are declared with a Scala tuple
  (preferably a named tuple) as result type. All the returned values are packed
  into the tuple (missing ones are `null`, i.e. `0` for numeric elements):

  ```scala
  @js.native @JSGlobal("love.graphics")
  object Graphics extends js.Object {
    def getDimensions(): (width: Int, height: Int) = js.native
  }

  val screen = Graphics.getDimensions()
  screen.width                                   // by name
  val (width, height) = Graphics.getDimensions() // or by position
  ```

  This works with any Lua function (e.g. `string.find`) and tuples up to `Tuple22`.

The linker frontend is configured for ECMAScript 5.1, which makes the Scala.js
library pick its pure Scala fallbacks over newer JS built-ins (e.g. heap
`ByteBuffer`s instead of typed arrays).

The Scala.js standard library itself calls a few JavaScript built-ins
(`Math.max`, `Object.prototype.hasOwnProperty`, JS arrays, string methods,
`JSON.parse`/`JSON.stringify`, `instanceof`, `RegExp`, ...); the runtime emulates those in
`R.js`. Regular expressions use a small backtracking engine for the JS syntax that the Scala.js
regex compiler produces (`java.util.regex`, `String.format`, `split`, `replaceAll`, ...).

## Known limitations

* **Strings are byte strings**: `length`/`charAt` count bytes, not UTF-16 units.
  ASCII text behaves exactly like on the JVM; string literals are encoded in UTF-8.
* **Regular expressions** work on bytes: classes like `\w` or `[a-z]` are about
  ASCII, and characters outside Latin-1 never match.
* Runtime checks are disabled (like Scala.js `fullLink`): no `ClassCastException`,
  `ArrayIndexOutOfBoundsException`, ... but `ArithmeticException` on `/ by zero` works.
* Not supported yet: returning multiple values *from* Scala callbacks to Lua, JS classes defined in Scala,
  dynamic imports, reflection beyond `getClass.getName`.
* LuaJIT only (LÖVE). Plain Lua 5.1 (World of Warcraft) needs `goto`-free control
  flow, a `Long` emulation (Scala.js' `RuntimeLong`) and a namespace table instead
  of globals.
