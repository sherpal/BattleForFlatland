-- Non-blocking HTTP for LÖVE: requests are run by worker threads (http_worker.lua, LuaSocket),
-- responses are polled from the main thread.
--
--   local id = http.request("POST", "http://localhost:9000/api/x", body, cookie)
--   local response = http.poll()  -- nil, or { id, status, body, setCookie, error }
--
-- With a file (a path of the operating system), the body of the response is written to it instead
-- (only if the status is 200; it is written to `file .. ".part"` meanwhile), and the progress of the
-- download is polled too, as { id, received } (the bytes received so far). The requests are run in
-- parallel by a pool of workers sharing their channel (so their responses may come in any order), and
-- the downloads (with a file) one after the other, on another worker: a long download does not delay
-- the requests.
local M = {}

local workers = require("workers")

local responsesName = "scalalove_http_responses"
local responses = love.thread.getChannel(responsesName)
-- requests and downloads: { channel name, number of workers, workers (once started) }
local queues = {
  requests = { name = "scalalove_http_requests", size = 4 },
  downloads = { name = "scalalove_http_downloads", size = 1 },
}
local lastId = 0

function M.request(method, url, body, cookie, file)
  local queue = (file and file ~= "") and queues.downloads or queues.requests
  if not queue.workers then
    queue.workers = {}
    for i = 1, queue.size do
      local worker = love.thread.newThread(workers.code("http_worker"))
      worker:start(queue.name, responsesName)
      queue.workers[i] = worker
    end
  end
  lastId = lastId + 1
  love.thread.getChannel(queue.name):push({
    id = lastId, method = method, url = url, body = body or "", cookie = cookie or "", file = file or "",
  })
  return lastId
end

function M.poll()
  for _, queue in pairs(queues) do
    for _, worker in ipairs(queue.workers or {}) do
      local err = worker:getError()
      if err then error("HTTP worker failed: " .. err) end
    end
  end
  return responses:pop()
end

return M
