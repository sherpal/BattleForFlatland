-- Runs shell commands on a worker thread (process_worker.lua), so that the game loop never blocks.
--
--   local id = process:run("java -version")  -- runs the command (in the system's shell)
--   local result = process:poll()            -- nil, or { id, result, error }: result is the
--                                            -- output (stdout and stderr, together)
--
-- To start a program in the background, the command itself must detach it (e.g. `cmd &`, or
-- `start "" /B cmd` on Windows), and redirect its output.
return require("workers").module("process")
