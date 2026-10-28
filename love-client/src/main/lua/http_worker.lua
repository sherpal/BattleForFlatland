-- Worker thread of http.lua: runs blocking LuaSocket requests one after the other, taken from the
-- channel named by its first argument, their responses going to the one named by its second.
local requestsName, responsesName = ...
require("love.thread")
local socket = require("socket")
local http = require("socket.http")
local ltn12 = require("ltn12")

-- Not LuaSocket's 60 s: a wrong server address should not hang the menus for a minute
http.TIMEOUT = 10

-- Seconds between the progress updates of a download (LuaSocket reads 2 KB at a time)
local PROGRESS_INTERVAL = 0.1

local requests = love.thread.getChannel(requestsName)
local responses = love.thread.getChannel(responsesName)

while true do
  local r = requests:demand()
  local chunks = {}
  local sink = ltn12.sink.table(chunks)
  local part = r.file ~= "" and (r.file .. ".part")
  local partFile
  if part then
    partFile = io.open(part, "wb")
    -- (closed by the sink at the end)
    if partFile then
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
  end
  local headers = { ["content-type"] = "application/json", ["content-length"] = tostring(#r.body) }
  if r.cookie ~= "" then headers.cookie = r.cookie end
  local ok, status, responseHeaders = http.request({
    url = r.url,
    method = r.method,
    headers = headers,
    source = ltn12.source.string(r.body),
    sink = sink,
  })
  local downloadError
  if part then
    if not partFile then
      downloadError = "cannot write " .. part
    elseif ok and status == 200 then
      os.remove(r.file)
      local renamed, renameError = os.rename(part, r.file)
      if not renamed then downloadError = tostring(renameError) end
    else
      os.remove(part)
    end
  end
  responses:push({
    id = r.id,
    status = (ok and not downloadError) and status or 0,
    body = table.concat(chunks),
    setCookie = (ok and responseHeaders and responseHeaders["set-cookie"]) or "",
    error = downloadError or (ok and "" or tostring(status)),
  })
end
