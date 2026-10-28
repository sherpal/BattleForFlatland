-- SHA-256 of files, computed on a worker thread (digest_worker.lua), so that the game loop never
-- blocks.
--
--   local id = digest:run("/home/me/file.jar")  -- a path of the operating system
--   local result = digest:poll()  -- nil, or { id, result, error }: result is the SHA-256
--                                 -- (hexadecimal, lower case)
return require("workers").module("digest")
