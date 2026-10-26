-- Minimal WebSocket client (RFC 6455) on top of LuaSocket, for LÖVE.
--
--   local ws, err = websocket.connect("ws://localhost:8080/path?query"[, timeout[, cookie]])
--   ws:send(binaryString)      -- binary frame
--   ws:sendText(text)          -- text frame
--   local message = ws:poll()  -- next received message (string) or nil; never blocks
--   ws:isOpen()
--   ws:close()
local socket = require("socket")
local bit = require("bit")
local band, bor, bxor, rshift = bit.band, bit.bor, bit.bxor, bit.rshift
local strchar, strbyte, strsub, concat = string.char, string.byte, string.sub, table.concat

local M = {}

local WebSocket = {}
WebSocket.__index = WebSocket

local b64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
local function base64(s)
  local out = {}
  for i = 1, #s, 3 do
    local a, b, c = strbyte(s, i, i + 2)
    local n = a * 65536 + (b or 0) * 256 + (c or 0)
    local c1, c2 = rshift(n, 18), band(rshift(n, 12), 63)
    local c3, c4 = band(rshift(n, 6), 63), band(n, 63)
    out[#out + 1] = b64:sub(c1 + 1, c1 + 1) .. b64:sub(c2 + 1, c2 + 1) ..
      (b and b64:sub(c3 + 1, c3 + 1) or "=") .. (c and b64:sub(c4 + 1, c4 + 1) or "=")
  end
  return concat(out)
end

local function randomBytes(n)
  local t = {}
  for i = 1, n do t[i] = math.random(0, 255) end
  return strchar(unpack(t))
end

-- Builds a (masked, as required for clients) frame
local function frame(opcode, payload)
  local n = #payload
  local header
  if n < 126 then
    header = strchar(0x80 + opcode, 0x80 + n)
  elseif n < 65536 then
    header = strchar(0x80 + opcode, 0x80 + 126, rshift(n, 8), band(n, 255))
  else
    header = strchar(0x80 + opcode, 0x80 + 127, 0, 0, 0, 0,
      band(rshift(n, 24), 255), band(rshift(n, 16), 255), band(rshift(n, 8), 255), band(n, 255))
  end
  local mask = { math.random(0, 255), math.random(0, 255), math.random(0, 255), math.random(0, 255) }
  local parts = { header, strchar(mask[1], mask[2], mask[3], mask[4]) }
  local i = 1
  while i <= n do
    local j = math.min(i + 4095, n)
    local bytes = { strbyte(payload, i, j) }
    for k = 1, #bytes do
      bytes[k] = bxor(bytes[k], mask[(i + k - 2) % 4 + 1])
    end
    parts[#parts + 1] = strchar(unpack(bytes))
    i = j + 1
  end
  return concat(parts)
end

function M.connect(url, timeout, cookie)
  local host, port, path = url:match("^ws://([^:/]+):?(%d*)(.*)$")
  if not host then return nil, "invalid websocket url: " .. tostring(url) end
  port = tonumber(port) or 80
  if path == "" then path = "/" end

  local tcp = socket.tcp()
  tcp:settimeout(timeout or 5)
  local ok, err = tcp:connect(host, port)
  if not ok then return nil, "cannot connect to " .. host .. ":" .. port .. ": " .. tostring(err) end
  tcp:setoption("tcp-nodelay", true)

  tcp:send(concat({
    "GET " .. path .. " HTTP/1.1",
    "Host: " .. host .. ":" .. port,
    "Upgrade: websocket",
    "Connection: Upgrade",
    "Sec-WebSocket-Key: " .. base64(randomBytes(16)),
    "Sec-WebSocket-Version: 13",
    (cookie and cookie ~= "") and ("Cookie: " .. cookie) or nil,
  }, "\r\n") .. "\r\n\r\n")

  local status = tcp:receive("*l")
  if not status or not status:match("^HTTP/1%.1 101") then
    -- Not upgraded: read the response body (e.g. an error message) for diagnostics
    local body = tcp:receive("*a") or ""
    tcp:close()
    return nil, tostring(status) .. " " .. body:gsub("^.-\r\n\r\n", "")
  end
  repeat
    local line = tcp:receive("*l")
  until line == nil or line == ""

  tcp:settimeout(0)
  return setmetatable({ tcp = tcp, buffer = "", messages = {}, fragments = nil, open = true }, WebSocket)
end

function WebSocket:isOpen() return self.open end

function WebSocket:send(data)
  if self.open then self.tcp:send(frame(0x2, data)) end
end

function WebSocket:sendText(text)
  if self.open then self.tcp:send(frame(0x1, text)) end
end

function WebSocket:close()
  if self.open then
    self.tcp:send(frame(0x8, ""))
    self.open = false
  end
  self.tcp:close()
end

-- Reads what is available and decodes complete frames
function WebSocket:readFrames()
  while true do
    local data, err, partial = self.tcp:receive(65536)
    local chunk = data or partial
    if chunk and #chunk > 0 then self.buffer = self.buffer .. chunk end
    if err == "closed" then self.open = false end
    if not data then break end
  end

  local buf = self.buffer
  while true do
    if #buf < 2 then break end
    local b1, b2 = strbyte(buf, 1, 2)
    local fin, opcode = band(b1, 0x80) ~= 0, band(b1, 0x0f)
    local masked, len = band(b2, 0x80) ~= 0, band(b2, 0x7f)
    local pos = 3
    if len == 126 then
      if #buf < 4 then break end
      local x, y = strbyte(buf, 3, 4)
      len, pos = x * 256 + y, 5
    elseif len == 127 then
      if #buf < 10 then break end
      len = 0
      for k = 3, 10 do len = len * 256 + strbyte(buf, k) end
      pos = 11
    end
    local mask
    if masked then
      if #buf < pos + 3 then break end
      mask = { strbyte(buf, pos, pos + 3) }
      pos = pos + 4
    end
    if #buf < pos + len - 1 then break end
    local payload = strsub(buf, pos, pos + len - 1)
    buf = strsub(buf, pos + len)
    if mask then
      local bytes = { strbyte(payload, 1, -1) }
      for k = 1, #bytes do bytes[k] = bxor(bytes[k], mask[(k - 1) % 4 + 1]) end
      payload = strchar(unpack(bytes))
    end

    if opcode == 0x0 then
      self.fragments[#self.fragments + 1] = payload
      if fin then
        self.messages[#self.messages + 1] = concat(self.fragments)
        self.fragments = nil
      end
    elseif opcode == 0x1 or opcode == 0x2 then
      if fin then self.messages[#self.messages + 1] = payload
      else self.fragments = { payload } end
    elseif opcode == 0x8 then
      if self.open then self.tcp:send(frame(0x8, "")) end
      self.open = false
    elseif opcode == 0x9 then
      self.tcp:send(frame(0xA, payload))
    end
  end
  self.buffer = buf
end

function WebSocket:poll()
  if #self.messages == 0 and (self.open or #self.buffer > 0) then self:readFrames() end
  return table.remove(self.messages, 1)
end

return M
