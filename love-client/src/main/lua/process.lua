-- Runs commands on a worker thread (process_worker.lua), so that the game loop never blocks.
--
--   local id = process:run("java -version")  -- runs the command, until it ends
--   local id = process.spawn(command, log)   -- starts it in the background (it outlives us), its
--                                            -- output going to the file `log` (a path of the
--                                            -- operating system)
--   local result = process:poll()            -- nil, or { id, result, error }: result is the
--                                            -- output (stdout and stderr, together), "" for spawn
--
-- On Linux and macOS, a command is one of the shell. On Windows, it is a command line, run without
-- a shell (no cmd.exe, whose console window would show up), the program being found in the PATH.
local process = require("workers").module("process")

-- (a function, not a method: it is called on the module imported by Scala)
function process.spawn(command, log)
  return process:run(command, log)
end

return process
