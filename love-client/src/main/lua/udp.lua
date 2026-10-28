-- Non-blocking UDP sockets for LÖVE, on top of LuaSocket (bundled with LÖVE).
--
--   local socket = udp.new()
--   local ok, err = socket:bind(port, address)   -- port 0: the system chooses; address "*" or nil: all interfaces
--   local ip, port = socket:getsockname()        -- local address (after bind, or after the first send)
--   local ok, err = socket:send(data, host, port)  -- data: a (binary) string; host: IP address or name
--   local datagram = socket:poll()               -- next received datagram, {data, ip, port} (the sender's), or nil; never blocks
--   socket:close()
--
-- Everything is best effort, like UDP itself: `send` only fails if the destination cannot be resolved or
-- the socket is closed, not if the datagram is lost. A socket that is not bound when it sends is bound to
-- any address and a port chosen by the system.
local socket = require("socket")

local M = {}

local UdpSocket = {}
UdpSocket.__index = UdpSocket

-- LuaSocket's `sendto` only takes IP addresses. Host names are resolved (which blocks, but only once per
-- name: the result is kept, and a failure too, for a while, so that a bad name does not freeze every send).
-- IP addresses are taken as they are: LuaSocket's `toip` would look up their name (a reverse lookup), for
-- nothing.
local resolved = {}
local FAILURE_TTL = 5 -- seconds

local function resolve(host)
  if host:match("^%d+%.%d+%.%d+%.%d+$") then return host end
  local cached = resolved[host]
  if cached then
    if cached.ip then return cached.ip end
    if socket.gettime() - cached.at < FAILURE_TTL then return nil, cached.err end
  end
  local ip, err = socket.dns.toip(host)
  resolved[host] = { ip = ip, err = err, at = socket.gettime() }
  return ip, err
end

-- IPv4 sockets: with `socket.udp()`, the family is the one of the first address bound, and "*" can be IPv6 on
-- Windows, whose sockets then cannot send to IPv4 addresses ("No such host is known"), which all ours are.
function M.new()
  local udp = assert(socket.udp4())
  udp:settimeout(0)
  return setmetatable({ udp = udp, open = true }, UdpSocket)
end

function UdpSocket:bind(port, address)
  local ok, err = self.udp:setsockname(address or "*", port or 0)
  if not ok then return false, err end
  self.bound = true
  return true
end

-- (LuaSocket may give ports as strings)
function UdpSocket:getsockname()
  local ip, port = self.udp:getsockname()
  return ip, tonumber(port)
end

function UdpSocket:send(data, host, port)
  if not self.open then return false, "closed" end
  -- LuaSocket (3.0-rc1) cannot send from a socket that was not bound: "refused"
  if not self.bound then
    local ok, err = self:bind(0, "*")
    if not ok then return false, err end
  end
  local ip, err = resolve(host)
  if not ip then return false, "cannot resolve " .. tostring(host) .. ": " .. tostring(err) end
  -- Not being able to send (e.g. no buffer space, network unreachable) is like a lost datagram
  local ok, sendErr = self.udp:sendto(data, ip, port)
  if not ok then return false, sendErr end
  return true
end

-- On Windows, a datagram sent to a port where nothing listens (e.g. a punch to an address of the game
-- server that is not the right one) makes a later receive fail ("connection refused") instead of giving the
-- next datagram: those failures are skipped, a few at most per poll.
local SKIPPED_FAILURES = 16

function UdpSocket:poll()
  if not self.open then return nil end
  for _ = 1, SKIPPED_FAILURES do
    local data, ip, port = self.udp:receivefrom()
    if data ~= nil then return { data = data, ip = ip, port = tonumber(port) } end
    -- On failure, the second result is the error ("timeout" when there is nothing to receive)
    if ip == "timeout" then return nil end
  end
  return nil
end

function UdpSocket:close()
  if self.open then
    self.open = false
    self.udp:close()
  end
end

return M
