-- Copies what the client prints (`print`, and `io.stderr:write`: the uncaught errors) to client.log, in
-- LÖVE's save directory, each line with the time: on Windows, a LÖVE game has no console to read it in.
-- The file starts anew at each launch. Loaded by conf.lua, before anything prints.
--
-- (The worker threads have Lua states of their own: what they print is not copied.)

local file -- opened on the first write: the save directory is only known after love.conf

local function append(text)
  if file == nil then
    file = false
    if love.filesystem and love.filesystem.getIdentity() ~= "" then
      local ok, opened = pcall(love.filesystem.newFile, "client.log", "w")
      if ok and opened then file = opened end
    end
  end
  if file then
    file:write(text)
    file:flush()
  end
end

local function line(text)
  append(os.date("%H:%M:%S ") .. text .. (text:sub(-1) == "\n" and "" or "\n"))
end

local originalPrint = print
print = function(...)
  originalPrint(...)
  local parts = {}
  for i = 1, select("#", ...) do parts[i] = tostring((select(i, ...))) end
  line(table.concat(parts, "\t"))
end

local stderr = io.stderr
io.stderr = setmetatable({
  write = function(self, ...)
    stderr:write(...)
    local parts = {}
    for i = 1, select("#", ...) do parts[i] = tostring((select(i, ...))) end
    line(table.concat(parts))
    return self
  end,
}, {
  -- the other methods of the file
  __index = function(_, name)
    return function(_, ...) return stderr[name](stderr, ...) end
  end,
})
