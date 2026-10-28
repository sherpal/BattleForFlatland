-- Worker thread of digest.lua: computes the SHA-256 of the files one after the other (with LÖVE's
-- love.data.hash, the standard SHA-256: the same as the JVM's).
local inputsName, resultsName = ...
require("love.thread")
require("love.data")

local inputs = love.thread.getChannel(inputsName)
local results = love.thread.getChannel(resultsName)

while true do
  local r = inputs:demand()
  local file, openError = io.open(r.input, "rb")
  if file then
    local contents = file:read("*a")
    file:close()
    local sha256 = love.data.encode("string", "hex", love.data.hash("sha256", contents)):lower()
    -- the files are big (tens of MB): not to keep them until the next collection
    contents = nil
    collectgarbage()
    results:push({ id = r.id, result = sha256, error = "" })
  else
    results:push({ id = r.id, result = "", error = "cannot read the file: " .. tostring(openError) })
  end
end
