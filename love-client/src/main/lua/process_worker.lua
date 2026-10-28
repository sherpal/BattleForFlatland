-- Worker thread of process.lua: runs the commands one after the other, and collects their output.
local inputsName, resultsName = ...
require("love.thread")

local inputs = love.thread.getChannel(inputsName)
local results = love.thread.getChannel(resultsName)

while true do
  local r = inputs:demand()
  local pipe = io.popen(r.input .. " 2>&1", "r")
  if pipe then
    local output = pipe:read("*a") or ""
    pipe:close()
    results:push({ id = r.id, result = output, error = "" })
  else
    results:push({ id = r.id, result = "", error = "cannot run " .. r.input })
  end
end
