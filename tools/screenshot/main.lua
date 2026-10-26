-- Runs a generated game in LÖVE, takes screenshots at the given frames, then quits.
-- Usage: love tools/screenshot <game dir> [frame,frame,...] [event,event,...]
--   e.g. love tools/screenshot love-client/target/lua 60,120 w:30-90,1@100
-- Events:
--   key:from-to       the key is held between the given frames (love.keyboard.isDown and isScancodeDown)
--   key@frame         a key press and release (love.keypressed then love.keyreleased)
--   click@frame:x:y   a left click (love.mousepressed then love.mousereleased) at (x, y)
--   mouse@frame:x:y   moves the mouse to (x, y) (love.mouse.getPosition; clicks move it too)
--   button@frame:n    presses and releases the mouse button n (2 right, 3 middle, ...) where the mouse is
--   wheel@frame:dy    turns the mouse wheel (love.wheelmoved; positive upwards)
--   text@frame:text   typed text (love.textinput), which cannot contain commas
-- Keys are given as LÖVE scancodes. The window has the size given by the game's conf.lua, if any.
-- Screenshots are written to LÖVE's save directory (scalalove-screenshot/screenshot-<frame>.png),
-- which is also the game's. SCREENSHOT_IDENTITY changes it (e.g. to run several games at once).
love.filesystem.setIdentity(os.getenv("SCREENSHOT_IDENTITY") or "scalalove-screenshot")
local gameDir = arg[2]
local frames = {}
for f in (arg[3] or "90"):gmatch("%d+") do frames[#frames + 1] = tonumber(f) end
local lastFrame = frames[#frames]
local heldKeys, pressedKeys, clicks, texts, moves, buttons, wheels = {}, {}, {}, {}, {}, {}, {}
for event in (arg[4] or ""):gmatch("[^,]+") do
  local moveAt, moveX, moveY = event:match("^mouse@(%d+):(%d+):(%d+)$")
  local buttonAt, button = event:match("^button@(%d+):(%d+)$")
  local wheelAt, wheel = event:match("^wheel@(%d+):(-?%d+)$")
  local at, x, y = event:match("^click@(%d+):(%d+):(%d+)$")
  local textAt, text = event:match("^text@(%d+):(.*)$")
  local key, from, to = event:match("^(%w+):(%d+)-(%d+)$")
  local pressed, pressedAt = event:match("^(%w+)@(%d+)$")
  if moveAt then moves[#moves + 1] = { at = tonumber(moveAt), x = tonumber(moveX), y = tonumber(moveY) }
  elseif buttonAt then buttons[#buttons + 1] = { at = tonumber(buttonAt), button = tonumber(button) }
  elseif wheelAt then wheels[#wheels + 1] = { at = tonumber(wheelAt), dy = tonumber(wheel) }
  elseif at then clicks[#clicks + 1] = { at = tonumber(at), x = tonumber(x), y = tonumber(y) }
  elseif textAt then texts[#texts + 1] = { at = tonumber(textAt), text = text }
  elseif key then heldKeys[#heldKeys + 1] = { key = key, from = tonumber(from), to = tonumber(to) }
  elseif pressed then pressedKeys[#pressedKeys + 1] = { key = pressed, at = tonumber(pressedAt) }
  else error("Unknown event " .. event) end
end

local conf = loadfile(gameDir .. "/conf.lua")
if conf then
  conf()
  local t = { window = {}, modules = {} }
  love.conf(t)
  if t.window and t.window.width then love.window.setMode(t.window.width, t.window.height) end
  love.conf = nil
end

package.path = gameDir .. "/?.lua;" .. package.path
dofile(gameDir .. "/main.lua")

local frame = 0
local function simulate(original)
  return function(key, ...)
    for _, k in ipairs(heldKeys) do
      if k.key == key and frame >= k.from and frame <= k.to then return true end
    end
    return original(key, ...)
  end
end
love.keyboard.isDown = simulate(love.keyboard.isDown)
love.keyboard.isScancodeDown = simulate(love.keyboard.isScancodeDown)

-- The simulated mouse position, once an event set it
local mouseX, mouseY
local getPosition = love.mouse.getPosition
love.mouse.getPosition = function()
  if mouseX then return mouseX, mouseY end
  return getPosition()
end

-- Frames are counted in love.timer.step (called once per frame by love.run), which keeps working
-- when the game replaces its callbacks (e.g. love.update when going from the menus to the game).
local step = love.timer.step
love.timer.step = function(...)
  local dt = step(...)
  frame = frame + 1
  for _, k in ipairs(pressedKeys) do
    if k.at == frame then
      if love.keypressed then love.keypressed(k.key, k.key, false) end
      if love.keyreleased then love.keyreleased(k.key, k.key) end
    end
  end
  for _, m in ipairs(moves) do
    if m.at == frame then mouseX, mouseY = m.x, m.y end
  end
  for _, c in ipairs(clicks) do
    if c.at == frame then
      mouseX, mouseY = c.x, c.y
      if love.mousepressed then love.mousepressed(c.x, c.y, 1, false, 1) end
      if love.mousereleased then love.mousereleased(c.x, c.y, 1, false, 1) end
    end
  end
  for _, b in ipairs(buttons) do
    if b.at == frame then
      local x, y = love.mouse.getPosition()
      if love.mousepressed then love.mousepressed(x, y, b.button, false, 1) end
      if love.mousereleased then love.mousereleased(x, y, b.button, false, 1) end
    end
  end
  for _, w in ipairs(wheels) do
    if w.at == frame and love.wheelmoved then love.wheelmoved(0, w.dy) end
  end
  for _, t in ipairs(texts) do
    if t.at == frame and love.textinput then love.textinput(t.text) end
  end
  for _, f in ipairs(frames) do
    if frame == f then
      love.graphics.captureScreenshot(function(imageData)
        local name = "screenshot-" .. f .. ".png"
        imageData:encode("png", name)
        print("screenshot: " .. love.filesystem.getSaveDirectory() .. "/" .. name)
        if f == lastFrame then love.event.quit() end
      end)
    end
  end
  return dt
end
