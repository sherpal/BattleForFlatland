-- Non-blocking HTTP for LÖVE: requests are run by worker threads (http_worker.lua, LuaSocket),
-- responses are polled from the main thread.
--
--   local id = http.request("POST", "http://localhost:9000/api/x", body, cookie)
--   local response = http.poll()  -- nil, or { id, status, body, setCookie, error }
--
-- With a file (a path of the operating system), the body of the response is written to it instead
-- (only if the status is 200; it is written to `file .. ".part"` meanwhile), and the progress of the
-- download is polled too, as { id, received } (the bytes received so far). The requests are run one
-- after the other, and so are the downloads (with a file), on another worker: a long download does not
-- delay the requests.
local M = {}

local workers = require("workers")

local responsesName = "scalalove_http_responses"
local responses = love.thread.getChannel(responsesName)
-- requests and downloads: { channel name, worker (once started) }
local queues = {
  requests = { name = "scalalove_http_requests" },
  downloads = { name = "scalalove_http_downloads" },
}
local lastId = 0

function M.request(method, url, body, cookie, file)
  local queue = (file and file ~= "") and queues.downloads or queues.requests
  if not queue.worker then
    queue.worker = love.thread.newThread(workers.code("http_worker"))
    queue.worker:start(queue.name, responsesName)
  end
  lastId = lastId + 1
  love.thread.getChannel(queue.name):push({
    id = lastId, method = method, url = url, body = body or "", cookie = cookie or "", file = file or "",
  })
  return lastId
end

function M.poll()
  for _, queue in pairs(queues) do
    if queue.worker then
      local err = queue.worker:getError()
      if err then error("HTTP worker failed: " .. err) end
    end
  end
  return responses:pop()
end

return M
