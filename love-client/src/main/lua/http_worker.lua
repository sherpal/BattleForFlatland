-- Worker thread of http.lua: runs blocking requests one after the other, taken from the channel
-- named by its first argument (which other workers may share), their responses going to the one
-- named by its second.
--
-- With LÖVE 12's HTTPS module (lua-https, which does TLS with the system's libraries, and checks the
-- certificates against the system's), http:// and https:// URLs both work. Without it (LÖVE 11),
-- LuaSocket only does http://.
local requestsName, responsesName = ...
require("love.thread")
local socket = require("socket")
local hasHttps, https = pcall(require, "https")

-- Seconds between the progress updates of a download (LuaSocket reads 2 KB at a time)
local PROGRESS_INTERVAL = 0.1

-- lua-https gives nothing before the end of a response: downloads are made of pieces this big
-- (asked with Range), after each of which the progress is told
local PIECE = 4 * 1024 * 1024

local requests = love.thread.getChannel(requestsName)
local responses = love.thread.getChannel(responsesName)

local function headersOf(r)
  local headers = { ["content-type"] = "application/json" }
  if r.cookie ~= "" then headers.cookie = r.cookie end
  return headers
end

-- The value of a header of a response (the names of the headers are as the system gives them)
local function header(headers, name)
  for key, value in pairs(headers or {}) do
    -- (some backends keep the spaces after the colon)
    if key:lower() == name then return (value:gsub("^%s+", ""):gsub("%s+$", "")) end
  end
  return nil
end

-- { status, body, setCookie, error } of a request with lua-https
local function httpsRequest(r)
  local options = { method = r.method, headers = headersOf(r) }
  if r.method ~= "GET" and r.method ~= "HEAD" then options.data = r.body end
  local ok, code, body, headers = pcall(https.request, r.url, options)
  if not ok or not code or code == 0 then
    -- (lua-https tells nothing more when the connection fails, e.g. on a certificate it refuses)
    local message = not ok and tostring(code)
      or tostring(body ~= "" and body or "no answer (no server there, or its certificate is refused)")
    return { status = 0, body = "", setCookie = "", error = message }
  end
  return { status = code, body = body or "", setCookie = header(headers, "set-cookie") or "", error = "" }
end

-- Downloads with lua-https, a piece at a time, into `part`; returns { status, body, error }
local function httpsDownload(r, part)
  local file = io.open(part, "wb")
  if not file then return { status = 0, body = "", error = "cannot write " .. part } end
  local received, total = 0, nil
  local headers = headersOf(r)
  while total == nil or received < total do
    headers.range = "bytes=" .. received .. "-" .. (received + PIECE - 1)
    local ok, code, body, responseHeaders = pcall(https.request, r.url, { method = "GET", headers = headers })
    if not ok or not code or code == 0 then
      file:close()
      return { status = 0, body = "", error = not ok and tostring(code) or tostring(body) }
    elseif code == 200 then
      -- the whole file at once (the server does not do ranges)
      file:write(body)
      received, total = #body, #body
    elseif code == 206 then
      file:write(body)
      received = received + #body
      total = tonumber((header(responseHeaders, "content-range") or ""):match("/(%d+)$"))
      if not total or #body == 0 then
        file:close()
        return { status = 0, body = "", error = "unexpected Content-Range" }
      end
    else
      file:close()
      return { status = code, body = body or "", error = "" }
    end
    responses:push({ id = r.id, received = received })
  end
  file:close()
  return { status = 200, body = "", error = "" }
end

-- { status, body, setCookie, error } of a request with LuaSocket (http:// only), its body going to
-- `part` (a file) if given
local function luaSocketRequest(r, part)
  if r.url:match("^https://") then
    return { status = 0, body = "", setCookie = "", error = "HTTPS needs LÖVE 12 (this is " ..
      love._version .. ")" }
  end
  local http = require("socket.http")
  local ltn12 = require("ltn12")
  -- Not LuaSocket's 60 s: a wrong server address should not hang the menus for a minute
  http.TIMEOUT = 10
  local chunks = {}
  local sink = ltn12.sink.table(chunks)
  if part then
    local partFile = io.open(part, "wb")
    if not partFile then return { status = 0, body = "", setCookie = "", error = "cannot write " .. part } end
    -- (closed by the sink at the end)
    -- the bytes received so far are told every PROGRESS_INTERVAL, as { id, received }
    local fileSink = ltn12.sink.file(partFile)
    local received, toldAt = 0, socket.gettime()
    sink = function(chunk, err)
      if chunk then
        received = received + #chunk
        local now = socket.gettime()
        if now - toldAt >= PROGRESS_INTERVAL then
          toldAt = now
          responses:push({ id = r.id, received = received })
        end
      end
      return fileSink(chunk, err)
    end
  end
  local headers = headersOf(r)
  headers["content-length"] = tostring(#r.body)
  local ok, status, responseHeaders = http.request({
    url = r.url,
    method = r.method,
    headers = headers,
    source = ltn12.source.string(r.body),
    sink = sink,
  })
  return {
    status = ok and status or 0,
    body = table.concat(chunks),
    setCookie = (ok and responseHeaders and responseHeaders["set-cookie"]) or "",
    error = ok and "" or tostring(status),
  }
end

while true do
  local r = requests:demand()
  local part = r.file ~= "" and (r.file .. ".part") or nil
  local response
  if hasHttps then
    response = part and httpsDownload(r, part) or httpsRequest(r)
  else
    response = luaSocketRequest(r, part)
  end
  if part then
    if response.status == 200 and response.error == "" then
      os.remove(r.file)
      local renamed, renameError = os.rename(part, r.file)
      if not renamed then response.status, response.error = 0, tostring(renameError) end
    else
      os.remove(part)
    end
  end
  responses:push({
    id = r.id,
    status = response.status,
    body = response.body,
    setCookie = response.setCookie or "",
    error = response.error,
  })
end
