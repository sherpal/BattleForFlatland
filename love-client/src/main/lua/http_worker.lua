-- Worker thread of http.lua: runs blocking LuaSocket requests one after the other.
require("love.thread")
local http = require("socket.http")
local ltn12 = require("ltn12")

-- Not LuaSocket's 60 s: a wrong server address should not hang the menus for a minute
http.TIMEOUT = 10

local requests = love.thread.getChannel("scalalove_http_requests")
local responses = love.thread.getChannel("scalalove_http_responses")

while true do
  local r = requests:demand()
  local chunks = {}
  local headers = { ["content-type"] = "application/json", ["content-length"] = tostring(#r.body) }
  if r.cookie ~= "" then headers.cookie = r.cookie end
  local ok, status, responseHeaders = http.request({
    url = r.url,
    method = r.method,
    headers = headers,
    source = ltn12.source.string(r.body),
    sink = ltn12.sink.table(chunks),
  })
  responses:push({
    id = r.id,
    status = ok and status or 0,
    body = table.concat(chunks),
    setCookie = (ok and responseHeaders and responseHeaders["set-cookie"]) or "",
    error = ok and "" or tostring(status),
  })
end
