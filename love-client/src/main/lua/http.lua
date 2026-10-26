-- Non-blocking HTTP for LÖVE: requests are run by a worker thread (http_worker.lua, LuaSocket),
-- responses are polled from the main thread.
--
--   local id = http.request("POST", "http://localhost:9000/api/x", body, cookie)
--   local response = http.poll()  -- nil, or { id, status, body, setCookie, error }
local M = {}

local requests = love.thread.getChannel("scalalove_http_requests")
local responses = love.thread.getChannel("scalalove_http_responses")
local worker
local lastId = 0

function M.request(method, url, body, cookie)
  if not worker then
    -- From the game directory, or from package.path when main.lua is run from another directory
    local code = love.filesystem.getInfo("http_worker.lua") and love.filesystem.read("http_worker.lua")
    if not code then
      local path = assert(package.searchpath("http_worker", package.path), "http_worker.lua not found")
      local file = assert(io.open(path, "r"))
      code = file:read("*a")
      file:close()
    end
    worker = love.thread.newThread(code)
    worker:start()
  end
  lastId = lastId + 1
  requests:push({ id = lastId, method = method, url = url, body = body or "", cookie = cookie or "" })
  return lastId
end

function M.poll()
  if worker then
    local err = worker:getError()
    if err then error("HTTP worker failed: " .. err) end
  end
  return responses:pop()
end

return M
