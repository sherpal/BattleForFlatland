-- ScalaLove runtime (LuaJIT). Prepended to every generated file.
--
-- Value representation:
--   Int/Short/Byte/Char/Double/Float -> Lua number
--   Long                             -> LuaJIT int64_t cdata
--   Boolean                          -> Lua boolean
--   String                           -> Lua string (byte semantics)
--   null / undefined (Unit)          -> nil
--   boxed Char                       -> { c = code } with metatable R.CharBox
--   Scala objects                    -> tables whose metatable is their class table
--   Arrays                           -> tables { length = n, [0] = ..., [n-1] = ... }
local R = {}

-- JIT tuning for large generated programs. With the defaults (512 KB of machine
-- code, 1000 traces), LuaJIT fills its machine-code area, flushes every trace and
-- recompiles them over and over, spending most of its time in the JIT compiler.
if jit and jit.opt then
  jit.opt.start("maxmcode=65536", "maxtrace=10000", "loopunroll=60")
end

local bit = require("bit")
local ffi = require("ffi")
local tobit, band, bor, bxor, bnot = bit.tobit, bit.band, bit.bor, bit.bxor, bit.bnot
local lshift, rshift, arshift = bit.lshift, bit.rshift, bit.arshift
local floor, ceil, huge, fmod = math.floor, math.ceil, math.huge, math.fmod
local type, getmetatable, setmetatable, rawget = type, getmetatable, setmetatable, rawget
local strbyte, strchar, strsub, strformat = string.byte, string.char, string.sub, string.format

R.bit, R.ffi = bit, ffi

local C, MOD, LOAD = {}, {}, {}
R.C, R.MOD, R.LOAD = C, MOD, LOAD

-- ---------------------------------------------------------------------------
-- Classes

local classesByName = {}

-- Declares a class table. `ancestors` lists all ancestor names (incl. itself).
function R.declare(name, kind, ancestors)
  local P = { __name = name, __kind = kind, __anc = {} }
  P.__index = P
  for i = 1, #ancestors do P.__anc[ancestors[i]] = true end
  classesByName[name] = P
  return P
end

-- Copies down inherited members (superclass first, then interfaces from the
-- most specific to the least specific) into P.
function R.inherit(P, super, ifaces)
  local function copy(from)
    for k, v in pairs(from) do
      if P[k] == nil and type(k) == "string" and strsub(k, 1, 2) ~= "__" then P[k] = v end
    end
  end
  if super then copy(super) end
  if ifaces then for i = 1, #ifaces do copy(ifaces[i]) end end
end

-- ---------------------------------------------------------------------------
-- Errors / exceptions

-- Set by the generated code: wraps a caught Lua error into a Throwable
-- (JavaScriptException for non-Scala values).
R.wrapJSException = nil

function R.wrapT(e)
  if type(e) == "table" then
    local mt = getmetatable(e)
    if mt and mt.__anc and mt.__anc["java.lang.Throwable"] then return e end
  end
  return R.wrapJSException(e)
end

function R.unwrapT(e)
  if R.unwrapJSException then return R.unwrapJSException(e) end
  return e
end

R.arithmeticException = function(msg) error("ArithmeticException: " .. msg, 2) end

-- Unique marker returned from pcall'ed try-blocks to jump to an outer label.
R.JMP = setmetatable({}, { __tostring = function() return "<jump>" end })

-- ---------------------------------------------------------------------------
-- Int arithmetic (Java semantics on 32-bit integers stored in doubles)

function R.imul(a, b)
  local al, ah = band(a, 0xffff), rshift(a, 16)
  return tobit(al * b + lshift(ah * b, 16))
end

function R.idiv(a, b)
  if b == 0 then R.arithmeticException("/ by zero") end
  local q = a / b
  if q >= 0 then q = floor(q) else q = ceil(q) end
  return tobit(q)
end

function R.irem(a, b)
  if b == 0 then R.arithmeticException("/ by zero") end
  return tobit(fmod(a, b))
end

local function toUnsigned(a) if a < 0 then return a + 4294967296 else return a end end
R.toUnsigned = toUnsigned

function R.iudiv(a, b)
  if b == 0 then R.arithmeticException("/ by zero") end
  return tobit(floor(toUnsigned(a) / toUnsigned(b)))
end

function R.iurem(a, b)
  if b == 0 then R.arithmeticException("/ by zero") end
  return tobit(fmod(toUnsigned(a), toUnsigned(b)))
end

function R.d2i(x)
  if x ~= x then return 0 end
  if x >= 2147483647 then return 2147483647 end
  if x <= -2147483648 then return -2147483648 end
  if x >= 0 then return floor(x) else return ceil(x) end
end

-- JS ToInt32
function R.toInt32(x)
  if x ~= x or x == huge or x == -huge then return 0 end
  if x >= 0 then x = floor(x) else x = ceil(x) end
  return tobit(fmod(x, 4294967296))
end

function R.clz(x)
  if x == 0 then return 32 end
  local n = 0
  if band(x, 0xffff0000) == 0 then n = n + 16; x = lshift(x, 16) end
  if band(x, 0xff000000) == 0 then n = n + 8; x = lshift(x, 8) end
  if band(x, 0xf0000000) == 0 then n = n + 4; x = lshift(x, 4) end
  if band(x, 0xc0000000) == 0 then n = n + 2; x = lshift(x, 2) end
  if band(x, 0x80000000) == 0 then n = n + 1 end
  return n
end

-- ---------------------------------------------------------------------------
-- Long arithmetic (int64_t cdata)

local int64 = ffi.typeof("int64_t")
local uint64 = ffi.typeof("uint64_t")
R.int64 = int64

local LMIN = -9223372036854775807LL - 1LL
local LMAX = 9223372036854775807LL

function R.ldiv(a, b)
  if b == 0LL then R.arithmeticException("/ by zero") end
  if a == LMIN and b == -1LL then return LMIN end
  return a / b
end

function R.lrem(a, b)
  if b == 0LL then R.arithmeticException("/ by zero") end
  if b == -1LL then return 0LL end
  return a % b
end

function R.ludiv(a, b)
  if b == 0LL then R.arithmeticException("/ by zero") end
  return int64(ffi.cast(uint64, a) / ffi.cast(uint64, b))
end

function R.lurem(a, b)
  if b == 0LL then R.arithmeticException("/ by zero") end
  return int64(ffi.cast(uint64, a) % ffi.cast(uint64, b))
end

function R.lucmp(a, b)
  local ua, ub = ffi.cast(uint64, a), ffi.cast(uint64, b)
  if ua < ub then return -1 elseif ua > ub then return 1 else return 0 end
end

function R.d2l(x)
  if x ~= x then return 0LL end
  if x >= 9223372036854775807 then return LMAX end
  if x <= -9223372036854775808 then return LMIN end
  return int64(x)
end

function R.l2i(x)
  return tobit(tonumber(band(x, 0xffffffffLL)))
end

function R.lclz(x)
  if x == 0LL then return 64 end
  local hi = tobit(tonumber(rshift(x, 32)))
  if hi ~= 0 then return R.clz(hi) end
  return 32 + R.clz(R.l2i(x))
end

function R.ltos(x)
  local s = tostring(x)
  return (s:gsub("LL$", ""))
end

-- ---------------------------------------------------------------------------
-- Floating point

local fbox = ffi.new("float[1]")
function R.fround(x) fbox[0] = x; return tonumber(fbox[0]) end

local bits = ffi.new("union { double d; int64_t l; float f; int32_t i; }")
function R.doubleToBits(x) bits.d = x; return bits.l end
function R.doubleFromBits(l) bits.l = l; return bits.d end
function R.floatToBits(x) bits.f = x; return tonumber(bits.i) end
function R.floatFromBits(i) bits.i = i; return tonumber(bits.f) end

-- JS-like Number.prototype.toString()
function R.numToStr(x)
  if x ~= x then return "NaN" end
  if x == huge then return "Infinity" elseif x == -huge then return "-Infinity" end
  if x == 0 then return "0" end
  if x == floor(x) and x < 1e21 and x > -1e21 then return strformat("%.0f", x) end
  local s
  for p = 15, 17 do
    s = strformat("%." .. p .. "g", x)
    if tonumber(s) == x then break end
  end
  -- JS style exponent: 1e+21, 1.5e-7
  s = s:gsub("e([+-])0*(%d)", "e%1%2")
  return s
end

-- ---------------------------------------------------------------------------
-- Strings

local function utf8char(c)
  if c < 0x80 then return strchar(c)
  elseif c < 0x800 then return strchar(0xC0 + rshift(c, 6), 0x80 + band(c, 0x3F))
  elseif c < 0x10000 then
    return strchar(0xE0 + rshift(c, 12), 0x80 + band(rshift(c, 6), 0x3F), 0x80 + band(c, 0x3F))
  else
    return strchar(0xF0 + rshift(c, 18), 0x80 + band(rshift(c, 12), 0x3F),
      0x80 + band(rshift(c, 6), 0x3F), 0x80 + band(c, 0x3F))
  end
end
R.utf8char = utf8char

-- Char -> String. Strings are byte strings: chars < 256 map to one byte.
function R.chr(c)
  if c < 256 then return strchar(c) end
  return utf8char(c)
end

local TOSTRING -- method ident of toString(), set by the generated code
function R.setToStringName(n) TOSTRING = n end

-- Converts any value to a string, as `"" + x` would in Scala.js
function R.str(x)
  local t = type(x)
  if t == "string" then return x
  elseif t == "number" then return R.numToStr(x)
  elseif t == "boolean" then return x and "true" or "false"
  elseif x == nil then return "null"
  elseif t == "cdata" then return R.ltos(x)
  elseif t == "table" then
    local mt = getmetatable(x)
    if mt == R.CharBox then return R.chr(x.c) end
    if mt and mt[TOSTRING] then return x[TOSTRING](x) end
  end
  return tostring(x)
end

-- ---------------------------------------------------------------------------
-- Boxing / dispatch on hijacked classes

local CharBox = { __name = "java.lang.Character" }
R.CharBox = CharBox
function R.boxC(c) return setmetatable({ c = c }, CharBox) end
function R.unboxC(x) if x == nil then return 0 end return x.c end

-- Hijacked class tables, filled in by the generated code
local H = {}
R.H = H

-- Calls method `m` on `x`, which may be a primitive (hijacked) value.
function R.dp(x, m, ...)
  local t = type(x)
  if t == "table" then
    if getmetatable(x) == CharBox then return H.char[m](x.c, ...) end
    return x[m](x, ...)
  elseif t == "string" then return H.string[m](x, ...)
  elseif t == "number" then return H.number[m](x, ...)
  elseif t == "boolean" then return H.boolean[m](x, ...)
  elseif t == "cdata" then return H.long[m](x, ...)
  elseif x == nil then return H.unit[m](x, ...)
  end
  return x[m](x, ...)
end

-- ---------------------------------------------------------------------------
-- Type tests

function R.isInt(x) return type(x) == "number" and tobit(x) == x and (x ~= 0 or 1 / x > 0) end
function R.isByte(x) return R.isInt(x) and x >= -128 and x <= 127 end
function R.isShort(x) return R.isInt(x) and x >= -32768 and x <= 32767 end
function R.isFloat(x) return type(x) == "number" and (x ~= x or R.fround(x) == x) end
function R.isLong(x) return type(x) == "cdata" and ffi.istype(int64, x) end
function R.isChar(x) return type(x) == "table" and getmetatable(x) == CharBox end

-- Ancestors of the hijacked classes, filled in by the generated code
R.hijackedAncestors = { string = {}, number = {}, boolean = {}, long = {}, char = {}, unit = {} }

-- x instanceof the class/interface with the given name
function R.isInstance(x, name)
  local t = type(x)
  if t == "table" then
    local mt = getmetatable(x)
    if mt == CharBox then return R.hijackedAncestors.char[name] == true end
    if mt ~= nil and mt.__arrBase then
      return name == "java.lang.Object" or name == "java.lang.Cloneable" or name == "java.io.Serializable"
    end
    return mt ~= nil and mt.__anc ~= nil and mt.__anc[name] == true
  elseif t == "string" then return R.hijackedAncestors.string[name] == true
  elseif t == "number" then return R.hijackedAncestors.number[name] == true
  elseif t == "boolean" then return R.hijackedAncestors.boolean[name] == true
  elseif t == "cdata" then return R.hijackedAncestors.long[name] == true
  end
  return false
end

-- ---------------------------------------------------------------------------
-- Arrays

local arrayTypes = {}

-- Returns the metatable for arrays with the given base type descriptor
-- ("I", "Z", ..., or a class name) and number of dimensions.
function R.arrayType(base, dims, zero)
  local key = base .. "/" .. dims
  local T = arrayTypes[key]
  if not T then
    local name = string.rep("[", dims) .. (#base == 1 and base or ("L" .. base .. ";"))
    T = { __name = name, __arrBase = base, __arrDims = dims, __zero = zero }
    T.__index = function(_, k) return C.java_lang_Object and C.java_lang_Object[k] end
    arrayTypes[key] = T
  end
  return T
end

function R.newArray(T, n)
  local a = setmetatable({ length = n }, T)
  local z = T.__zero
  if z ~= nil then for i = 0, n - 1 do a[i] = z end end
  return a
end

function R.arrayOf(T, a, n)
  a.length = n
  return setmetatable(a, T)
end

function R.isArrayOf(x, base, dims)
  if type(x) ~= "table" then return false end
  local mt = getmetatable(x)
  if not mt or not mt.__arrBase then return false end
  if mt.__arrDims == dims then
    if mt.__arrBase == base then return true end
    if #base == 1 or #mt.__arrBase == 1 then return false end
    local P = classesByName[mt.__arrBase]
    return base == "java.lang.Object" or (P ~= nil and P.__anc[base] == true)
  elseif mt.__arrDims > dims then
    return base == "java.lang.Object" or base == "java.io.Serializable" or base == "java.lang.Cloneable"
  end
  return false
end

function R.clone(x)
  local mt = getmetatable(x)
  local y = {}
  for k, v in pairs(x) do y[k] = v end
  return setmetatable(y, mt)
end

function R.arraycopy(src, srcPos, dest, destPos, len)
  if src == dest and srcPos < destPos then
    for i = len - 1, 0, -1 do dest[destPos + i] = src[srcPos + i] end
  else
    for i = 0, len - 1 do dest[destPos + i] = src[srcPos + i] end
  end
end

-- ---------------------------------------------------------------------------
-- Identity hash codes

local idHashes = setmetatable({}, { __mode = "k" })
local lastIdHash = 0
function R.ihc(x)
  local t = type(x)
  if t == "table" and getmetatable(x) ~= CharBox then
    local h = idHashes[x]
    if not h then
      lastIdHash = tobit(lastIdHash + 0x61c88647)
      h = lastIdHash
      idHashes[x] = h
    end
    return h
  elseif x == nil then
    return 0
  end
  return R.dp(x, R.hashCodeName)
end

-- ---------------------------------------------------------------------------
-- java.lang.Class

local classObjects = {}
local primNames = {
  V = "void", Z = "boolean", C = "char", B = "byte", S = "short", I = "int",
  J = "long", F = "float", D = "double",
}

-- name: a class name, a primitive char code, or an array name like "[I"
function R.classOf(name)
  local c = classObjects[name]
  if not c then
    local P = classesByName[name]
    c = {
      name = primNames[name] or name,
      isPrimitive = primNames[name] ~= nil,
      isInterface = P ~= nil and P.__kind == "interface",
      isArrayClass = strsub(name, 1, 1) == "[",
      __desc = name,
    }
    setmetatable(c, C.java_lang_Class)
    classObjects[name] = c
  end
  return c
end

function R.getClass(x)
  local t = type(x)
  if t == "string" then return R.classOf("java.lang.String")
  elseif t == "number" then
    if R.isInt(x) then return R.classOf("java.lang.Integer") end
    return R.classOf("java.lang.Double")
  elseif t == "boolean" then return R.classOf("java.lang.Boolean")
  elseif t == "cdata" then return R.classOf("java.lang.Long")
  elseif x == nil then return R.classOf("java.lang.Void")
  end
  local mt = getmetatable(x)
  if mt == CharBox then return R.classOf("java.lang.Character") end
  if mt and mt.__name then return R.classOf(mt.__name) end
  return nil
end

-- ---------------------------------------------------------------------------
-- JavaScript interop emulation
--
-- The Scala.js standard library (javalib) calls a few JavaScript built-ins.
-- We emulate the ones it needs. Convention: JS methods receive `this` as
-- their first parameter, like Lua methods called with `:`.

local js = {}
R.js = js
setmetatable(js, { __index = function(_, k) error("JavaScript global '" .. tostring(k) .. "' is not available in Lua", 2) end })

js.undefined = nil
js.console = {
  log = function(...) print(...) end,
  info = function(...) print(...) end,
  warn = function(...) io.stderr:write(table.concat({ ... }, " "), "\n") end,
  error = function(...) io.stderr:write(table.concat({ ... }, " "), "\n") end,
}
js.NaN = 0 / 0
js.Infinity = huge

local function jsTypeof(x)
  local t = type(x)
  if t == "nil" then return "undefined"
  elseif t == "number" then return "number"
  elseif t == "string" then return "string"
  elseif t == "boolean" then return "boolean"
  elseif t == "function" then return "function"
  elseif t == "cdata" then return "bigint"
  end
  return "object"
end
R.typeof = jsTypeof

local function trunc(x) if x >= 0 then return floor(x) else return ceil(x) end end

js.Math = {
  PI = math.pi, E = math.exp(1),
  abs = math.abs, floor = floor, ceil = ceil, sqrt = math.sqrt,
  sin = math.sin, cos = math.cos, tan = math.tan, asin = math.asin, acos = math.acos,
  atan = math.atan, atan2 = math.atan2, exp = math.exp, log = math.log,
  log10 = math.log10, pow = math.pow,
  max = function(a, b) if a ~= a or b ~= b then return 0 / 0 end if a > b then return a end return b end,
  min = function(a, b) if a ~= a or b ~= b then return 0 / 0 end if a < b then return a end return b end,
  random = math.random,
  round = function(x) return floor(x + 0.5) end,
  trunc = trunc,
  sign = function(x) if x > 0 then return 1 elseif x < 0 then return -1 end return x end,
  fround = R.fround, imul = R.imul, clz32 = R.clz,
  hypot = function(a, b) return math.sqrt(a * a + b * b) end,
  cbrt = function(x) if x < 0 then return -((-x) ^ (1 / 3)) end return x ^ (1 / 3) end,
  expm1 = function(x) return math.exp(x) - 1 end,
  log1p = function(x) return math.log(1 + x) end,
  sinh = math.sinh, cosh = math.cosh, tanh = math.tanh,
  log2 = function(x) return math.log(x) / math.log(2) end,
}

js.Object = {
  prototype = {
    hasOwnProperty = function(this, k) return type(this) == "table" and rawget(this, k) ~= nil end,
    toString = function(this) return "[object Object]" end,
  },
  isSealed = function(_) return false end,
  is = function(a, b)
    if a ~= a and b ~= b then return true end
    if a == 0 and b == 0 then return 1 / a == 1 / b end
    return a == b
  end,
  keys = function(o) return R.objectKeys(o) end,
}

js.Error = {
  new = function(msg)
    return { message = msg, stack = debug.traceback(msg and tostring(msg) or "", 3) }
  end,
}

-- Wall clock: LuaSocket's (bundled with LÖVE) when available
local hasSocket, luasocket = pcall(require, "socket")
local wallClock = hasSocket and luasocket.gettime or os.time
js.performance = { now = function() return wallClock() * 1000 end }
js.Date = { now = function() return math.floor(wallClock() * 1000) end }

js.Number = {
  isNaN = function(x) return x ~= x end,
  isFinite = function(x) return x == x and x ~= huge and x ~= -huge end,
  isInteger = function(x) return x == floor(x) and x ~= huge and x ~= -huge end,
  MAX_SAFE_INTEGER = 9007199254740991,
}
js.isNaN = js.Number.isNaN
js.isFinite = js.Number.isFinite
js.parseFloat = function(s) return tonumber(s) or 0 / 0 end
js.parseInt = function(s, radix) return tonumber(s, radix or 10) or 0 / 0 end

js.String = {
  fromCharCode = function(...)
    local n = select("#", ...)
    local parts = {}
    for i = 1, n do parts[i] = R.chr((select(i, ...))) end
    return table.concat(parts)
  end,
}

-- JS arrays: like Scala arrays, 0-based tables with an explicit `length`.
local JSArray = { __name = "js.Array" }
JSArray.__index = JSArray
JSArray.__newindex = function(t, k, v)
  if type(k) == "number" and k >= t.length then rawset(t, "length", k + 1) end
  rawset(t, k, v)
end
R.JSArray = JSArray

function R.jarr(t, n)
  t.length = n
  return setmetatable(t, JSArray)
end

-- Packs Lua varargs into a JS array
function R.restArgs(...)
  local n = select("#", ...)
  local a = { length = n }
  for i = 1, n do a[i - 1] = (select(i, ...)) end
  return setmetatable(a, JSArray)
end

-- Spreads a 0-based array as Lua varargs
function R.unpackArr(a)
  return unpack(a, 0, a.length - 1)
end

function JSArray.push(a, ...)
  for i = 1, select("#", ...) do
    local n = a.length
    rawset(a, n, (select(i, ...)))
    a.length = n + 1
  end
  return a.length
end
function JSArray.pop(a)
  local n = a.length
  if n == 0 then return nil end
  local v = a[n - 1]
  rawset(a, n - 1, nil)
  a.length = n - 1
  return v
end
function JSArray.shift(a)
  local n = a.length
  if n == 0 then return nil end
  local v = a[0]
  for i = 1, n - 1 do rawset(a, i - 1, a[i]) end
  rawset(a, n - 1, nil)
  a.length = n - 1
  return v
end
function JSArray.unshift(a, ...)
  local k = select("#", ...)
  local n = a.length
  for i = n - 1, 0, -1 do rawset(a, i + k, a[i]) end
  for i = 1, k do rawset(a, i - 1, (select(i, ...))) end
  a.length = n + k
  return a.length
end
local function relIdx(i, len, default)
  if i == nil then return default end
  if i < 0 then i = len + i; if i < 0 then i = 0 end end
  if i > len then i = len end
  return i
end
function JSArray.slice(a, s, e)
  local len = a.length
  s = relIdx(s, len, 0)
  e = relIdx(e, len, len)
  local r = { length = 0 }
  for i = s, e - 1 do r[i - s] = a[i] end
  r.length = math.max(e - s, 0)
  return setmetatable(r, JSArray)
end
function JSArray.splice(a, start, deleteCount, ...)
  local len = a.length
  start = relIdx(start, len, 0)
  if deleteCount == nil then deleteCount = len - start end
  deleteCount = math.max(0, math.min(deleteCount, len - start))
  local removed = { length = deleteCount }
  for i = 0, deleteCount - 1 do removed[i] = a[start + i] end
  local items = select("#", ...)
  local tail = {}
  for i = start + deleteCount, len - 1 do tail[#tail + 1] = a[i] end
  for i = start, len - 1 do rawset(a, i, nil) end
  local pos = start
  for i = 1, items do rawset(a, pos, (select(i, ...))); pos = pos + 1 end
  for i = 1, #tail do rawset(a, pos, tail[i]); pos = pos + 1 end
  -- holes (nil elements) in `tail` are handled by the explicit length
  a.length = len - deleteCount + items
  return setmetatable(removed, JSArray)
end
function JSArray.concat(a, ...)
  local r = a:slice()
  for i = 1, select("#", ...) do
    local b = (select(i, ...))
    if type(b) == "table" and getmetatable(b) == JSArray then
      for j = 0, b.length - 1 do r:push(b[j]) end
    else
      r:push(b)
    end
  end
  return r
end
function JSArray.join(a, sep)
  local parts = {}
  for i = 0, a.length - 1 do
    local v = a[i]
    parts[i + 1] = v == nil and "" or R.str(v)
  end
  return table.concat(parts, sep or ",")
end
function JSArray.indexOf(a, v)
  for i = 0, a.length - 1 do if a[i] == v then return i end end
  return -1
end
function JSArray.reverse(a)
  local n = a.length
  for i = 0, math.floor(n / 2) - 1 do
    a[i], a[n - 1 - i] = a[n - 1 - i], a[i]
  end
  return a
end
function JSArray.fill(a, v, s, e)
  for i = relIdx(s, a.length, 0), relIdx(e, a.length, a.length) - 1 do rawset(a, i, v) end
  return a
end
function JSArray.forEach(a, f) for i = 0, a.length - 1 do f(a[i], i) end end
function JSArray.map(a, f)
  local r = { length = a.length }
  for i = 0, a.length - 1 do r[i] = f(a[i], i) end
  return setmetatable(r, JSArray)
end
function JSArray.sort(a, cmp)
  local t = {}
  for i = 0, a.length - 1 do t[i + 1] = a[i] end
  table.sort(t, cmp and function(x, y) return cmp(x, y) < 0 end or function(x, y) return R.str(x) < R.str(y) end)
  for i = 1, #t do rawset(a, i - 1, t[i]) end
  return a
end

js.Array = {
  isArray = function(x) return type(x) == "table" and getmetatable(x) == JSArray end,
  new = function(n)
    return setmetatable({ length = n or 0 }, JSArray)
  end,
}

-- ---------------------------------------------------------------------------
-- JSON
--
-- JS objects are Lua tables, which cannot hold nil values and do not keep their keys in order.
-- Objects created by JSON.parse remember their keys (in order, including the ones whose value is
-- null) in `objectKeyOrder` (a list, plus a set in `objectKeySet`), which Object.keys and
-- hasOwnProperty use.

local objectKeyOrder = setmetatable({}, { __mode = "k" })
local objectKeySet = setmetatable({}, { __mode = "k" })

js.Object.prototype.hasOwnProperty = function(this, k)
  if type(this) ~= "table" then return false end
  if rawget(this, k) ~= nil then return true end
  local set = objectKeySet[this]
  return set ~= nil and set[k] == true
end

function R.objectKeys(o)
  local keys = objectKeyOrder[o]
  local result = { length = 0 }
  if keys then
    for i = 1, #keys do result[i - 1] = keys[i] end
    result.length = #keys
  elseif type(o) == "table" then
    local sorted = {}
    for k in pairs(o) do
      if type(k) == "string" and not (getmetatable(o) == JSArray and k == "length") then sorted[#sorted + 1] = k end
    end
    table.sort(sorted)
    for i = 1, #sorted do result[i - 1] = sorted[i] end
    result.length = #sorted
  end
  return setmetatable(result, JSArray)
end

local jsonEscapes = { ['"'] = '"', ["\\"] = "\\", ["/"] = "/", b = "\b", f = "\f", n = "\n", r = "\r", t = "\t" }

local function parseJSON(s)
  local pos = 1
  local function fail(message)
    error("SyntaxError: JSON.parse: " .. message .. " at position " .. (pos - 1), 0)
  end
  local function skip()
    pos = string.find(s, "[^ \t\r\n]", pos) or (#s + 1)
  end
  local parseValue

  local function parseString()
    pos = pos + 1 -- opening quote
    local parts = {}
    while true do
      local j = string.find(s, '["\\]', pos)
      if not j then fail("unterminated string") end
      if j > pos then parts[#parts + 1] = strsub(s, pos, j - 1) end
      pos = j
      if strsub(s, j, j) == '"' then
        pos = j + 1
        return table.concat(parts)
      end
      local e = strsub(s, j + 1, j + 1)
      if e == "u" then
        local cp = tonumber(strsub(s, j + 2, j + 5), 16)
        if not cp then fail("bad unicode escape") end
        pos = j + 6
        if cp >= 0xD800 and cp <= 0xDBFF and strsub(s, pos, pos + 1) == "\\u" then
          local low = tonumber(strsub(s, pos + 2, pos + 5), 16)
          if low and low >= 0xDC00 and low <= 0xDFFF then
            cp = 0x10000 + (cp - 0xD800) * 0x400 + (low - 0xDC00)
            pos = pos + 6
          end
        end
        parts[#parts + 1] = utf8char(cp)
      else
        local c = jsonEscapes[e]
        if not c then fail("bad escape") end
        parts[#parts + 1] = c
        pos = j + 2
      end
    end
  end

  local function parseNumber()
    local text = string.match(s, "^-?%d+%.?%d*[eE]?[-+]?%d*", pos)
    local n = text and tonumber(text)
    if not n then fail("bad number") end
    pos = pos + #text
    return n
  end

  function parseValue()
    skip()
    local c = strsub(s, pos, pos)
    if c == "{" then
      pos = pos + 1
      local o, keys, set = {}, {}, {}
      skip()
      if strsub(s, pos, pos) == "}" then
        pos = pos + 1
      else
        while true do
          skip()
          if strsub(s, pos, pos) ~= '"' then fail("expected a string key") end
          local k = parseString()
          skip()
          if strsub(s, pos, pos) ~= ":" then fail("expected ':'") end
          pos = pos + 1
          local v = parseValue()
          if not set[k] then
            set[k] = true
            keys[#keys + 1] = k
          end
          o[k] = v
          skip()
          local d = strsub(s, pos, pos)
          pos = pos + 1
          if d == "}" then break elseif d ~= "," then fail("expected ',' or '}'") end
        end
      end
      objectKeyOrder[o] = keys
      objectKeySet[o] = set
      return o
    elseif c == "[" then
      pos = pos + 1
      local a = { length = 0 }
      skip()
      if strsub(s, pos, pos) == "]" then
        pos = pos + 1
      else
        local n = 0
        while true do
          a[n] = parseValue()
          n = n + 1
          skip()
          local d = strsub(s, pos, pos)
          pos = pos + 1
          if d == "]" then break elseif d ~= "," then fail("expected ',' or ']'") end
        end
        a.length = n
      end
      return setmetatable(a, JSArray)
    elseif c == '"' then
      return parseString()
    elseif c == "-" or string.find(c, "%d") then
      return parseNumber()
    elseif strsub(s, pos, pos + 3) == "true" then
      pos = pos + 4
      return true
    elseif strsub(s, pos, pos + 4) == "false" then
      pos = pos + 5
      return false
    elseif strsub(s, pos, pos + 3) == "null" then
      pos = pos + 4
      return nil
    end
    fail("unexpected " .. (c == "" and "end of input" or ("'" .. c .. "'")))
  end

  local value = parseValue()
  skip()
  if pos <= #s then fail("unexpected trailing characters") end
  return value
end

local function quoteJSON(str)
  return '"' .. str:gsub('[%c"\\]', function(c)
    if c == '"' then return '\\"' elseif c == "\\" then return "\\\\"
    elseif c == "\n" then return "\\n" elseif c == "\r" then return "\\r" elseif c == "\t" then return "\\t"
    else return strformat("\\u%04x", strbyte(c)) end
  end) .. '"'
end

local function stringifyJSON(v)
  local t = type(v)
  if v == nil then return "null"
  elseif t == "boolean" then return v and "true" or "false"
  elseif t == "number" then
    if v ~= v or v == huge or v == -huge then return "null" end
    return R.numToStr(v)
  elseif t == "string" then return quoteJSON(v)
  elseif t == "table" then
    if getmetatable(v) == JSArray then
      local parts = {}
      for i = 0, v.length - 1 do parts[#parts + 1] = stringifyJSON(v[i]) end
      return "[" .. table.concat(parts, ",") .. "]"
    end
    local keys = R.objectKeys(v)
    local parts = {}
    for i = 0, keys.length - 1 do
      local k = keys[i]
      if v[k] ~= nil then parts[#parts + 1] = quoteJSON(k) .. ":" .. stringifyJSON(v[k]) end
    end
    return "{" .. table.concat(parts, ",") .. "}"
  end
  return "null"
end

js.JSON = {
  parse = function(text) return parseJSON(text) end,
  stringify = function(value) return stringifyJSON(value) end,
}

-- JS `instanceof`
function R.jinstanceof(x, ctor)
  if ctor == js.Array then return type(x) == "table" and getmetatable(x) == JSArray end
  if ctor == js.Object then
    local t = type(x)
    return t == "table" or t == "userdata" or t == "function" or t == "cdata"
  end
  if type(ctor) == "table" and ctor.isInstance then return ctor.isInstance(x) end
  return false
end

-- ---------------------------------------------------------------------------
-- Regular expressions: a backtracking engine for the JS RegExp syntax that the Scala.js
-- library (java.util.regex.PatternCompiler) produces. Strings are byte strings, so classes
-- and escapes are about bytes (code points above 255 never match).

local regex = {}

local function newSet() return {} end
local function addRange(set, a, b) for c = math.max(a, 0), math.min(b, 255) do set[c] = true end end
local function addSet(set, other) for c = 0, 255 do if other[c] then set[c] = true end end end
local function negateSet(set) local n = {} for c = 0, 255 do n[c] = not set[c] end return n end

local DIGIT, WORD, SPACE = newSet(), newSet(), newSet()
addRange(DIGIT, 48, 57)
addRange(WORD, 48, 57); addRange(WORD, 65, 90); addRange(WORD, 97, 122); WORD[95] = true
for _, c in ipairs({ 9, 10, 11, 12, 13, 32, 160 }) do SPACE[c] = true end

local function caseFold(set)
  for c = 65, 90 do if set[c] or set[c + 32] then set[c] = true; set[c + 32] = true end end
  return set
end

-- Parser: pattern string -> AST
local function parseRegex(src, flags)
  local pos, n = 1, #src
  local groupCount, names = 0, {}
  local icase = flags.i

  local function peek(k) return strsub(src, pos + (k or 0), pos + (k or 0)) end
  local function fail(msg) error("SyntaxError: Invalid regular expression: /" .. src .. "/: " .. msg, 0) end

  local function hexAt(at, len)
    local h = strsub(src, at, at + len - 1)
    if #h == len and h:match("^%x+$") then return tonumber(h, 16) end
    return nil
  end

  -- An escape after '\' (pos on the escaped char). Returns a set (classes) or a code.
  local function parseEscape(inClass)
    local c = peek()
    pos = pos + 1
    if c == "d" then return DIGIT elseif c == "D" then return negateSet(DIGIT)
    elseif c == "w" then return WORD elseif c == "W" then return negateSet(WORD)
    elseif c == "s" then return SPACE elseif c == "S" then return negateSet(SPACE)
    elseif c == "n" then return 10 elseif c == "r" then return 13 elseif c == "t" then return 9
    elseif c == "f" then return 12 elseif c == "v" then return 11 elseif c == "0" then return 0
    elseif c == "b" and inClass then return 8
    elseif c == "x" then local v = hexAt(pos, 2); if v then pos = pos + 2; return v end; return 120
    elseif c == "u" then
      if peek() == "{" then
        local close = string.find(src, "}", pos, true)
        local v = close and tonumber(strsub(src, pos + 1, close - 1), 16)
        if v then pos = close + 1; return v end
      end
      local v = hexAt(pos, 4); if v then pos = pos + 4; return v end; return 117
    elseif c == "c" then local l = strbyte(src, pos); pos = pos + 1; return band(l or 0, 31)
    end
    return strbyte(c)
  end

  local function parseClass()
    -- pos after '['
    local negated = false
    if peek() == "^" then negated = true; pos = pos + 1 end
    local set = newSet()
    local first = true
    while true do
      local c = peek()
      if c == "" then fail("unterminated character class") end
      if c == "]" and not (first and false) then pos = pos + 1; break end
      first = false
      local lo
      if c == "\\" then
        pos = pos + 1
        local e = parseEscape(true)
        if type(e) == "table" then addSet(set, e) else lo = e end
      else
        lo = strbyte(c); pos = pos + 1
      end
      if lo then
        if peek() == "-" and peek(1) ~= "]" and peek(1) ~= "" then
          pos = pos + 1
          local hc = peek()
          local hi
          if hc == "\\" then
            pos = pos + 1
            local e = parseEscape(true)
            if type(e) == "table" then addSet(set, e); set[lo] = true; set[45] = true; hi = nil else hi = e end
          else
            hi = strbyte(hc); pos = pos + 1
          end
          if hi then
            if hi < lo then fail("range out of order") end
            addRange(set, lo, hi)
          end
        else
          if lo <= 255 then set[lo] = true end
        end
      end
    end
    if icase then caseFold(set) end
    return { type = "set", set = negated and negateSet(set) or set }
  end

  local parseAlternation

  local function parseAtom()
    local c = peek()
    if c == "(" then
      pos = pos + 1
      local node
      if peek() == "?" then
        local k = peek(1)
        if k == ":" then pos = pos + 2; node = { type = "group", node = parseAlternation() }
        elseif k == "=" or k == "!" then
          pos = pos + 2; node = { type = "look", behind = false, negative = (k == "!"), node = parseAlternation() }
        elseif k == "<" and (peek(2) == "=" or peek(2) == "!") then
          local negative = peek(2) == "!"
          pos = pos + 3; node = { type = "look", behind = true, negative = negative, node = parseAlternation() }
        elseif k == "<" then
          local close = string.find(src, ">", pos, true) or fail("bad group name")
          local name = strsub(src, pos + 2, close - 1)
          pos = close + 1
          groupCount = groupCount + 1
          local index = groupCount
          names[name] = index
          node = { type = "capture", index = index, name = name, node = parseAlternation() }
        else fail("invalid group") end
      else
        groupCount = groupCount + 1
        local index = groupCount
        node = { type = "capture", index = index, node = parseAlternation() }
      end
      if peek() ~= ")" then fail("unterminated group") end
      pos = pos + 1
      return node
    elseif c == "[" then pos = pos + 1; return parseClass()
    elseif c == "." then pos = pos + 1; return { type = "any" }
    elseif c == "^" then pos = pos + 1; return { type = "bol" }
    elseif c == "$" then pos = pos + 1; return { type = "eol" }
    elseif c == "\\" then
      pos = pos + 1
      local e = peek()
      if e == "b" then pos = pos + 1; return { type = "wordb", negative = false } end
      if e == "B" then pos = pos + 1; return { type = "wordb", negative = true } end
      if e:match("%d") and e ~= "0" then
        local digits = string.match(src, "^%d+", pos)
        pos = pos + #digits
        return { type = "backref", index = tonumber(digits) }
      end
      if e == "k" and peek(1) == "<" then
        local close = string.find(src, ">", pos, true) or fail("bad group name")
        local name = strsub(src, pos + 2, close - 1)
        pos = close + 1
        return { type = "backref", name = name }
      end
      local v = parseEscape(false)
      if type(v) == "table" then return { type = "set", set = v } end
      if icase and ((v >= 65 and v <= 90) or (v >= 97 and v <= 122)) then
        local set = newSet(); set[v] = true; return { type = "set", set = caseFold(set) }
      end
      return { type = "char", code = v }
    end
    pos = pos + 1
    local code = strbyte(c)
    if icase and ((code >= 65 and code <= 90) or (code >= 97 and code <= 122)) then
      local set = newSet(); set[code] = true; return { type = "set", set = caseFold(set) }
    end
    return { type = "char", code = code }
  end

  local function parseQuantified()
    local atom = parseAtom()
    while true do
      local c = peek()
      local min, max
      if c == "*" then min, max = 0, huge; pos = pos + 1
      elseif c == "+" then min, max = 1, huge; pos = pos + 1
      elseif c == "?" then min, max = 0, 1; pos = pos + 1
      elseif c == "{" then
        local a, b, close = string.match(src, "^{(%d+)(,?%d*)}()", pos)
        if not a then break end
        min = tonumber(a)
        if b == "" then max = min elseif b == "," then max = huge else max = tonumber(strsub(b, 2)) end
        pos = close
      else break end
      local greedy = true
      if peek() == "?" then greedy = false; pos = pos + 1 end
      atom = { type = "repeat", node = atom, min = min, max = max, greedy = greedy }
    end
    return atom
  end

  local function parseSequence()
    local items = {}
    while true do
      local c = peek()
      if c == "" or c == "|" or c == ")" then break end
      items[#items + 1] = parseQuantified()
    end
    return { type = "seq", items = items }
  end

  function parseAlternation()
    local alternatives = { parseSequence() }
    while peek() == "|" do
      pos = pos + 1
      alternatives[#alternatives + 1] = parseSequence()
    end
    if #alternatives == 1 then return alternatives[1] end
    return { type = "alt", alternatives = alternatives }
  end

  local ast = parseAlternation()
  if pos <= n then fail("unmatched ')'") end
  return ast, groupCount, names
end

-- Compiler: AST -> matcher function(i, k) over the state `st` (str, len, caps).
-- `k(j)` is the continuation; matchers return the final end index, or nil.
local function compileRegex(ast, flags, st, names)
  local multiline, dotAll, icase = flags.m, flags.s, flags.i

  local function isWordAt(i) local b = strbyte(st.str, i); return b ~= nil and WORD[b] == true end

  local compile

  local function singleByte(node)
    -- Returns a predicate on bytes if the node always matches exactly one byte
    if node.type == "char" then local c = node.code; return function(b) return b == c end
    elseif node.type == "set" then local set = node.set; return function(b) return set[b] == true end
    elseif node.type == "any" then
      if dotAll then return function(b) return true end end
      return function(b) return b ~= 10 and b ~= 13 end
    end
    return nil
  end

  function compile(node)
    local t = node.type
    if t == "char" then
      local c = node.code
      return function(i, k) if strbyte(st.str, i) == c then return k(i + 1) end return nil end
    elseif t == "set" or t == "any" then
      local p = singleByte(node)
      return function(i, k) local b = strbyte(st.str, i); if b and p(b) then return k(i + 1) end return nil end
    elseif t == "seq" then
      local fs = {}
      for idx, item in ipairs(node.items) do fs[idx] = compile(item) end
      local count = #fs
      if count == 0 then return function(i, k) return k(i) end end
      if count == 1 then return fs[1] end
      local function run(idx, i, k)
        if idx > count then return k(i) end
        local f = fs[idx]
        return f(i, function(j) return run(idx + 1, j, k) end)
      end
      return function(i, k) return run(1, i, k) end
    elseif t == "alt" then
      local fs = {}
      for idx, a in ipairs(node.alternatives) do fs[idx] = compile(a) end
      return function(i, k)
        for idx = 1, #fs do
          local r = fs[idx](i, k)
          if r then return r end
        end
        return nil
      end
    elseif t == "group" then
      return compile(node.node)
    elseif t == "capture" then
      local inner, s, e = compile(node.node), 2 * node.index - 1, 2 * node.index
      return function(i, k)
        return inner(i, function(j)
          local caps = st.caps
          local ps, pe = caps[s], caps[e]
          caps[s], caps[e] = i, j
          local r = k(j)
          if not r then caps[s], caps[e] = ps, pe end
          return r
        end)
      end
    elseif t == "backref" then
      return function(i, k)
        local index = node.index or names[node.name]
        local caps = st.caps
        local s, e = caps[2 * index - 1], caps[2 * index]
        if not s then return k(i) end
        local sub = strsub(st.str, s, e - 1)
        local here = strsub(st.str, i, i + #sub - 1)
        if here == sub or (icase and here:lower() == sub:lower()) then return k(i + #sub) end
        return nil
      end
    elseif t == "look" then
      local inner = compile(node.node)
      if not node.behind then
        return function(i, k)
          local r = inner(i, function(j) return j end)
          if node.negative then if r then return nil end return k(i) end
          if r then return k(i) end
          return nil
        end
      end
      return function(i, k)
        local found = false
        for j = i, 1, -1 do
          if inner(j, function(e) if e == i then return e end return nil end) then found = true; break end
        end
        if found ~= node.negative then return k(i) end
        return nil
      end
    elseif t == "bol" then
      return function(i, k)
        if i == 1 or (multiline and (strbyte(st.str, i - 1) == 10 or strbyte(st.str, i - 1) == 13)) then return k(i) end
        return nil
      end
    elseif t == "eol" then
      return function(i, k)
        if i == st.len + 1 or (multiline and (strbyte(st.str, i) == 10 or strbyte(st.str, i) == 13)) then return k(i) end
        return nil
      end
    elseif t == "wordb" then
      return function(i, k)
        local atBoundary = isWordAt(i - 1) ~= isWordAt(i)
        if atBoundary ~= node.negative then return k(i) end
        return nil
      end
    elseif t == "repeat" then
      local min, max, greedy = node.min, node.max, node.greedy
      local p = singleByte(node.node)
      if p then
        -- Fast path for repeated single bytes: no recursion per iteration
        return function(i, k)
          local str, j, count = st.str, i, 0
          while count < max do
            local b = strbyte(str, j)
            if not b or not p(b) then break end
            j = j + 1; count = count + 1
          end
          if count < min then return nil end
          if greedy then
            for c = count, min, -1 do
              local r = k(i + c)
              if r then return r end
            end
          else
            for c = min, count do
              local r = k(i + c)
              if r then return r end
            end
          end
          return nil
        end
      end
      local inner = compile(node.node)
      local function rep(i, k, count)
        if count < min then
          return inner(i, function(j) return rep(j, k, count + 1) end)
        end
        if greedy then
          if count < max then
            local r = inner(i, function(j) if j == i then return nil end return rep(j, k, count + 1) end)
            if r then return r end
          end
          return k(i)
        end
        local r = k(i)
        if r then return r end
        if count < max then
          return inner(i, function(j) if j == i then return nil end return rep(j, k, count + 1) end)
        end
        return nil
      end
      return function(i, k) return rep(i, k, 0) end
    end
    error("regex: unknown node " .. tostring(t))
  end

  return compile(ast)
end

local RegExp = { __name = "RegExp" }
RegExp.__index = RegExp
R.RegExp = RegExp

local supportedFlags = { g = "global", i = "ignoreCase", m = "multiline", s = "dotAll", y = "sticky" }

local function newRegExp(source, flags)
  if type(source) == "table" and getmetatable(source) == RegExp then
    if flags == nil then flags = source.flags end
    source = source.source
  end
  source = source == nil and "(?:)" or R.str(source)
  flags = flags or ""
  local f = {}
  for c in flags:gmatch(".") do
    if not supportedFlags[c] then error("SyntaxError: Invalid regular expression flags '" .. flags .. "'", 0) end
    f[c] = true
  end
  local ast, groupCount, names = parseRegex(source, f)
  local st = { str = "", len = 0, caps = {} }
  local re = setmetatable({
    source = source, flags = flags, lastIndex = 0,
    global = f.g == true, ignoreCase = f.i == true, multiline = f.m == true,
    dotAll = f.s == true, sticky = f.y == true, unicode = false,
  }, RegExp)
  rawset(re, "_matcher", compileRegex(ast, f, st, names))
  rawset(re, "_state", st)
  rawset(re, "_groupCount", groupCount)
  rawset(re, "_names", names)
  return re
end

-- Finds a match from the 0-based index `from`; returns 1-based start, end (exclusive) and captures
local function regexFind(re, str, from, stickyOnly)
  local st, matcher = re._state, re._matcher
  st.str, st.len = str, #str
  local function done(j) return j end
  local last = stickyOnly and from + 1 or #str + 1
  for start = from + 1, last do
    st.caps = {}
    local e = matcher(start, done)
    if e then return start, e, st.caps end
  end
  return nil
end

function RegExp.exec(re, str)
  str = R.str(str)
  local useLast = re.global or re.sticky
  local from = useLast and re.lastIndex or 0
  if from > #str then
    re.lastIndex = 0
    return nil
  end
  local s, e, caps = regexFind(re, str, from, re.sticky)
  if not s then
    if useLast then re.lastIndex = 0 end
    return nil
  end
  if useLast then re.lastIndex = e - 1 end
  local result = { length = re._groupCount + 1 }
  result[0] = strsub(str, s, e - 1)
  for g = 1, re._groupCount do
    local cs, ce = caps[2 * g - 1], caps[2 * g]
    if cs then result[g] = strsub(str, cs, ce - 1) end
  end
  result.index = s - 1
  result.input = str
  if next(re._names) then
    local groups = {}
    for name, g in pairs(re._names) do groups[name] = result[g] end
    result.groups = groups
  end
  return setmetatable(result, JSArray)
end

function RegExp.test(re, str) return RegExp.exec(re, str) ~= nil end
function RegExp.toString(re) return "/" .. re.source .. "/" .. re.flags end

js.RegExp = {
  new = newRegExp,
  isInstance = function(x) return type(x) == "table" and getmetatable(x) == RegExp end,
}

-- The replacement of a match, with JS's $ patterns or a function
local function substitution(replacement, match)
  if type(replacement) == "function" then
    local args = {}
    for g = 0, match.length - 1 do args[#args + 1] = match[g] end
    return R.str(replacement(unpack(args, 1, match.length)))
  end
  replacement = R.str(replacement)
  return (replacement:gsub("%$([%$&`'0-9<])([0-9]?)", function(c, d)
    if c == "$" then return "$" .. d
    elseif c == "&" then return match[0] .. d
    elseif c == "`" then return strsub(match.input, 1, match.index) .. d
    elseif c == "'" then return strsub(match.input, match.index + #match[0] + 1) .. d
    elseif c == "<" then return "$<" .. d
    end
    local two = tonumber(c .. d)
    if d ~= "" and two and two < match.length then return match[two] or "" end
    local one = tonumber(c)
    if one and one > 0 and one < match.length then return (match[one] or "") .. d end
    return "$" .. c .. d
  end))
end

local function regexReplace(s, re, replacement)
  local parts, last = {}, 1
  local global = re.global
  local saved = re.lastIndex
  re.lastIndex = 0
  local from = 0
  while from <= #s do
    local ms, me, caps = regexFind(re, s, from, re.sticky)
    if not ms then break end
    local match = { length = re._groupCount + 1, index = ms - 1, input = s }
    match[0] = strsub(s, ms, me - 1)
    for g = 1, re._groupCount do
      if caps[2 * g - 1] then match[g] = strsub(s, caps[2 * g - 1], caps[2 * g] - 1) end
    end
    parts[#parts + 1] = strsub(s, last, ms - 1)
    parts[#parts + 1] = substitution(replacement, match)
    last = me
    if not global then break end
    from = (me > ms) and (me - 1) or me
  end
  parts[#parts + 1] = strsub(s, last)
  re.lastIndex = global and 0 or saved
  return table.concat(parts)
end

-- ---------------------------------------------------------------------------
-- Event loop: setTimeout/setInterval/queueMicrotask. Scala.js' global ExecutionContext (and so
-- Futures) schedules its tasks with setTimeout(task, 0).
--
-- scalalove.runTimers() runs the tasks that are due. In LÖVE, it runs automatically at the start
-- of every frame: `love.update` is wrapped (see the end of this file).

local immediate = {}   -- FIFO of tasks with no delay (zero-delay timeouts, microtasks)
local delayed = {}     -- timers with a due time (in seconds)
local cancelled = {}
local lastTimerId = 0

local function newTimerId() lastTimerId = lastTimerId + 1; return lastTimerId end

local function schedule(fn, delay, args, interval)
  local id = newTimerId()
  delay = tonumber(delay) or 0
  if delay <= 0 and not interval then
    immediate[#immediate + 1] = { id = id, fn = fn, args = args }
  else
    delayed[#delayed + 1] = { id = id, fn = fn, args = args, due = wallClock() + math.max(delay, 1) / 1000,
      interval = interval and math.max(delay, 1) / 1000 or nil }
  end
  return id
end

js.setTimeout = function(fn, delay, ...) return schedule(fn, delay, { ... }, false) end
js.setInterval = function(fn, delay, ...) return schedule(fn, delay, { ... }, true) end
js.clearTimeout = function(id) if id ~= nil then cancelled[id] = true end end
js.clearInterval = js.clearTimeout
js.queueMicrotask = function(fn) immediate[#immediate + 1] = { id = newTimerId(), fn = fn, args = {} } end

local function runTask(task)
  if cancelled[task.id] then return end
  local ok, err = xpcall(function() task.fn(unpack(task.args)) end, debug.traceback)
  if not ok then io.stderr:write("Uncaught error in a timer: ", tostring(err), "\n") end
end

scalalove = scalalove or {}

function scalalove.pendingTimers()
  local n = #immediate
  for i = 1, #delayed do if not cancelled[delayed[i].id] then n = n + 1 end end
  return n
end

function scalalove.runTimers()
  local deadline = wallClock()
  local budget = 100000
  local function runImmediate()
    while #immediate > 0 and budget > 0 do
      local queue = immediate
      immediate = {}
      for i = 1, #queue do runTask(queue[i]); budget = budget - 1 end
    end
  end
  runImmediate()
  -- Timers due at the start of this run, in due order (intervals run at most once per run)
  local due = {}
  local remaining = {}
  for i = 1, #delayed do
    local t = delayed[i]
    if cancelled[t.id] then
      -- dropped
    elseif t.due <= deadline then due[#due + 1] = t
    else remaining[#remaining + 1] = t end
  end
  delayed = remaining
  table.sort(due, function(a, b) if a.due ~= b.due then return a.due < b.due end return a.id < b.id end)
  for i = 1, #due do
    local t = due[i]
    if not cancelled[t.id] then
      runTask(t)
      if t.interval and not cancelled[t.id] then
        t.due = math.max(t.due + t.interval, deadline + 0.0001)
        delayed[#delayed + 1] = t
      end
    end
    runImmediate()
  end
end

-- Methods of JS strings, numbers and functions (when called with a receiver)
local JSString = {}
local JSNumber = {}
local JSFunction = {}

local function clampIdx(i, len)
  if i == nil then return 0 end
  if i ~= i then return 0 end
  if i < 0 then return 0 elseif i > len then return len end
  return trunc(i)
end

function JSString.substring(s, a, b)
  local len = #s
  a = clampIdx(a, len)
  b = b == nil and len or clampIdx(b, len)
  if a > b then a, b = b, a end
  return strsub(s, a + 1, b)
end
function JSString.slice(s, a, b)
  local len = #s
  a = a or 0
  if a < 0 then a = math.max(len + a, 0) end
  b = b == nil and len or b
  if b < 0 then b = math.max(len + b, 0) end
  if a >= b then return "" end
  return strsub(s, a + 1, b)
end
function JSString.charCodeAt(s, i) return strbyte(s, i + 1) or 0 / 0 end
JSString.codePointAt = JSString.charCodeAt
function JSString.charAt(s, i) return strsub(s, i + 1, i + 1) end
function JSString.indexOf(s, sub, from)
  local r = string.find(s, sub, (from or 0) + 1, true)
  return r and (r - 1) or -1
end
function JSString.lastIndexOf(s, sub, from)
  local last = -1
  local start = 1
  local limit = from and (from + 1) or #s + 1
  while true do
    local r = string.find(s, sub, start, true)
    if not r or r > limit then break end
    last = r - 1
    start = r + 1
  end
  return last
end
function JSString.toUpperCase(s) return string.upper(s) end
function JSString.toLowerCase(s) return string.lower(s) end
JSString.toLocaleUpperCase = JSString.toUpperCase
JSString.toLocaleLowerCase = JSString.toLowerCase
function JSString.trim(s) return (s:gsub("^%s+", ""):gsub("%s+$", "")) end
function JSString.startsWith(s, p, pos) pos = pos or 0; return strsub(s, pos + 1, pos + #p) == p end
function JSString.endsWith(s, p) return p == "" or strsub(s, -#p) == p end
function JSString.includes(s, p) return string.find(s, p, 1, true) ~= nil end
function JSString.concat(s, ...) return s .. table.concat({ ... }) end
function JSString.replace(s, pattern, replacement)
  if type(pattern) == "table" and getmetatable(pattern) == RegExp then return regexReplace(s, pattern, replacement) end
  pattern = R.str(pattern)
  local i, j = string.find(s, pattern, 1, true)
  if not i then return s end
  local match = { [0] = pattern, length = 1, index = i - 1, input = s }
  return strsub(s, 1, i - 1) .. substitution(replacement, match) .. strsub(s, j + 1)
end
function JSString.split(s, sep, limit)
  local parts = { length = 0 }
  local function add(p) if limit == nil or parts.length < limit then parts[parts.length] = p; parts.length = parts.length + 1 end end
  if sep == nil then add(s)
  elseif type(sep) == "table" and getmetatable(sep) == RegExp then
    if #s == 0 then
      if not regexFind(sep, s, 0, false) then add(s) end
    else
      local last, from = 1, 1
      while from <= #s do
        local ms, me = regexFind(sep, s, from - 1, false)
        if not ms or ms > #s then break end
        if me == ms then
          -- Empty match: split between bytes, but not at the start of the current piece
          if ms > last then add(strsub(s, last, ms - 1)); last = ms end
          from = ms + 1
        else
          add(strsub(s, last, ms - 1)); last = me; from = me
        end
      end
      add(strsub(s, last))
    end
  else
    sep = R.str(sep)
    if sep == "" then for i = 1, #s do add(strsub(s, i, i)) end
    else
      local start = 1
      while true do
        local i, j = string.find(s, sep, start, true)
        if not i then break end
        add(strsub(s, start, i - 1)); start = j + 1
      end
      add(strsub(s, start))
    end
  end
  return setmetatable(parts, JSArray)
end
function JSString.match(s, re)
  if not (type(re) == "table" and getmetatable(re) == RegExp) then re = newRegExp(re) end
  if not re.global then return RegExp.exec(re, s) end
  local all = { length = 0 }
  re.lastIndex = 0
  while true do
    local m = RegExp.exec(re, s)
    if not m then break end
    all[all.length] = m[0]; all.length = all.length + 1
    if #m[0] == 0 then re.lastIndex = re.lastIndex + 1 end
  end
  if all.length == 0 then return nil end
  return setmetatable(all, JSArray)
end
function JSString.search(s, re)
  if not (type(re) == "table" and getmetatable(re) == RegExp) then re = newRegExp(re) end
  local ms = regexFind(re, s, 0, false)
  return ms and (ms - 1) or -1
end
function JSString.toString(s) return s end
function JSString.valueOf(s) return s end
function JSString.localeCompare(a, b) if a < b then return -1 elseif a > b then return 1 end return 0 end
JSString["repeat"] = function(s, n) return string.rep(s, n) end

function JSNumber.toString(x, base)
  if base == nil or base == 10 then return R.numToStr(x) end
  if x ~= floor(x) then error("Number.toString(radix) only supports integers here") end
  local digits = "0123456789abcdefghijklmnopqrstuvwxyz"
  local neg = x < 0
  if neg then x = -x end
  if x == 0 then return "0" end
  local out = {}
  while x > 0 do
    local d = x % base
    out[#out + 1] = strsub(digits, d + 1, d + 1)
    x = floor(x / base)
  end
  local s = string.reverse(table.concat(out))
  if neg then s = "-" .. s end
  return s
end
function JSNumber.toFixed(x, n) return strformat("%." .. (n or 0) .. "f", x) end
function JSNumber.valueOf(x) return x end

function JSFunction.call(f, thisArg, ...) return f(thisArg, ...) end

-- o.m(args) for a JS value o (string, number, Lua object, userdata, ...)
function R.jcall(o, m, ...)
  local t = type(o)
  local f
  if t == "string" then f = JSString[m]
  elseif t == "number" then f = JSNumber[m]
  elseif t == "function" then f = JSFunction[m]
  else return o[m](o, ...) end
  if f == nil then error("JS method '" .. tostring(m) .. "' is not available on a " .. t, 2) end
  return f(o, ...)
end

-- o[k] for a JS value o
function R.jget(o, k)
  local t = type(o)
  if t == "table" or t == "userdata" then
    local v = o[k]
    -- In JS, a["0"] is a[0]
    if v == nil and type(k) == "string" and t == "table" and getmetatable(o) == JSArray then
      local i = tonumber(k)
      if i then return o[i] end
    end
    return v
  end
  if t == "string" then
    if k == "length" then return #o end
    return JSString[k]
  end
  if t == "number" then return JSNumber[k] end
  if t == "function" then return JSFunction[k] end
  error("cannot read property '" .. tostring(k) .. "' of " .. tostring(o), 2)
end

function R.jnew(ctor, ...)
  if type(ctor) == "table" and ctor.new then return ctor.new(...) end
  error("cannot instantiate JS class " .. tostring(ctor), 2)
end

-- JS `+`
function R.jadd(a, b)
  if type(a) == "string" or type(b) == "string" then return R.str(a) .. R.str(b) end
  return a + b
end

function R.jin(k, o) return type(o) == "table" and o[k] ~= nil end

-- JS truthiness
function R.truthy(x)
  return not (x == nil or x == false or x == 0 or x == "" or x ~= x)
end

-- Packs the values returned by a Lua function into a Scala tuple.
-- `P` is the TupleN class table and `ctor` its constructor taking N values.
function R.tuple(P, ctor, ...)
  local first = ...
  -- A function that already returns a Scala tuple
  if select("#", ...) == 1 and type(first) == "table" and getmetatable(first) == P then return first end
  return P[ctor](P.__new(), ...)
end

-- Utilities available to Lua code and facades as the `scalalove` global
scalalove = scalalove or {}

-- Lua (binary) string -> Scala Array[Byte]
function scalalove.stringToBytes(s)
  local n = #s
  local a = { length = n }
  local i = 1
  while i <= n do
    local j = math.min(i + 4095, n)
    local chunk = { strbyte(s, i, j) }
    for k = 1, #chunk do
      local b = chunk[k]
      if b > 127 then b = b - 256 end
      a[i + k - 2] = b
    end
    i = j + 1
  end
  return setmetatable(a, R.arrayType("B", 1, 0))
end

-- Scala Array[Byte] -> Lua (binary) string
function scalalove.bytesToString(a)
  local n = a.length
  local parts = {}
  local i = 0
  while i < n do
    local j = math.min(i + 4095, n - 1)
    local chunk = {}
    for k = i, j do chunk[k - i + 1] = band(a[k], 255) end
    parts[#parts + 1] = strchar(unpack(chunk))
    i = j + 1
  end
  return table.concat(parts)
end

-- Lua sequence (1-based, as returned by Lua APIs) -> js.Array, and back
function scalalove.fromLuaSequence(t)
  local n = #t
  local a = { length = n }
  for i = 1, n do a[i - 1] = t[i] end
  return setmetatable(a, JSArray)
end

function scalalove.toLuaSequence(a)
  local t = {}
  for i = 0, a.length - 1 do t[i + 1] = a[i] end
  return t
end

function R.zor(a, b) return a or b end
function R.zand(a, b) return a and b end
function R.void(_) return nil end
function R.unsupported(what) error("ScalaLove: unsupported operation: " .. tostring(what), 2) end
-- Array type descriptors ("[I", "[Lfoo.Bar;") -> (base, dims)
local function parseArrayDesc(desc)
  local dims = 0
  while strsub(desc, dims + 1, dims + 1) == "[" do dims = dims + 1 end
  local rest = strsub(desc, dims + 1)
  if strsub(rest, 1, 1) == "L" then rest = strsub(rest, 2, -2) end
  return rest, dims
end
local zeros = { Z = false, J = 0LL, C = 0, B = 0, S = 0, I = 0, F = 0, D = 0 }

function R.classComponentType(c)
  local d = c.__desc
  if strsub(d, 1, 1) ~= "[" then return nil end
  local inner = strsub(d, 2)
  if strsub(inner, 1, 1) == "L" then inner = strsub(inner, 2, -2) end
  return R.classOf(inner)
end

-- java.lang.reflect.Array.newInstance(componentType, n)
function R.classNewArray(c, n)
  local d = c.__desc
  local base, dims
  if strsub(d, 1, 1) == "[" then
    base, dims = parseArrayDesc(d)
    dims = dims + 1
  else
    base, dims = d, 1
  end
  local zero = nil
  if dims == 1 then zero = zeros[base] end
  return R.newArray(R.arrayType(base, dims, zero), n)
end

function R.classIsInstance(c, x)
  local d = c.__desc
  if d == "java.lang.Object" then return x ~= nil end
  return R.isInstance(x, d)
end
function R.classIsAssignableFrom(a, b)
  if a == b or a.__desc == "java.lang.Object" then return true end
  local P = classesByName[b.__desc]
  return P ~= nil and P.__anc[a.__desc] == true
end

-- In LÖVE, run the timers at the start of every frame, whatever the game sets as love.update
if type(love) == "table" and getmetatable(love) == nil then
  local userUpdate = rawget(love, "update")
  rawset(love, "update", nil)
  setmetatable(love, {
    __index = function(_, k)
      if k == "update" then
        local f = userUpdate
        return function(...)
          scalalove.runTimers()
          if f then return f(...) end
        end
      end
      return nil
    end,
    __newindex = function(t, k, v)
      if k == "update" then userUpdate = v else rawset(t, k, v) end
    end,
  })
end

return R
