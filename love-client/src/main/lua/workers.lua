-- Worker threads: their code (e.g. http_worker.lua), to give to love.thread.newThread, and the
-- modules whose inputs a worker handles one after the other (e.g. process.lua), so that the game
-- loop never blocks.
local M = {}

function M.code(name)
  -- From the game directory, or from package.path when main.lua is run from another directory
  local code = love.filesystem.getInfo(name .. ".lua") and love.filesystem.read(name .. ".lua")
  if not code then
    local path = assert(package.searchpath(name, package.path), name .. ".lua not found")
    local file = assert(io.open(path, "r"))
    code = file:read("*a")
    file:close()
  end
  return code
end

-- A module whose inputs are handled by `<name>_worker.lua`, started with the names of the channels
-- of the inputs, { id, input, option } (the option, a string, may be nil), and of the results,
-- { id, result, error } (error is "" when it worked). Its functions are methods (they are called on
-- a JS object from Scala):
--
--   local id = module:run(input, option)
--   local result = module:poll()  -- nil, or { id, result, error }
function M.module(name)
  local inputsName = "scalalove_" .. name .. "_inputs"
  local resultsName = "scalalove_" .. name .. "_results"
  local inputs = love.thread.getChannel(inputsName)
  local results = love.thread.getChannel(resultsName)
  local worker
  local lastId = 0
  local module = {}

  function module:run(input, option)
    if not worker then
      worker = love.thread.newThread(M.code(name .. "_worker"))
      worker:start(inputsName, resultsName)
    end
    lastId = lastId + 1
    inputs:push({ id = lastId, input = input, option = option })
    return lastId
  end

  function module:poll()
    if worker then
      local err = worker:getError()
      if err then error(name .. " worker failed: " .. err) end
    end
    return results:pop()
  end

  return module
end

return M
