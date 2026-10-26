-- Loads an image from the game directory, or nil if it does not exist.
-- When the game is run from elsewhere (e.g. by tools/screenshot), love.filesystem does not see the
-- game directory: the image is then read next to this module.
local M = {}

local here = debug.getinfo(1, "S").source:match("^@(.*[/\\])") or ""

function M.load(path)
  if love.filesystem.getInfo(path) then return love.graphics.newImage(path) end
  local file = io.open(here .. path, "rb")
  if not file then return nil end
  local data = file:read("*a")
  file:close()
  return love.graphics.newImage(love.filesystem.newFileData(data, path))
end

return M
