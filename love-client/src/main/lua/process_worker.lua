-- Worker thread of process.lua: runs the commands one after the other, and collects their output (or
-- starts them in the background, when they come with a log file).
local inputsName, resultsName = ...
require("love.thread")

local inputs = love.thread.getChannel(inputsName)
local results = love.thread.getChannel(resultsName)

-- Linux and macOS: commands of the shell, with io.popen

local function quote(argument)
  return "'" .. argument:gsub("'", "'\\''") .. "'"
end

local function run(command)
  local pipe = io.popen(command .. " 2>&1", "r")
  if not pipe then return nil, "cannot run " .. command end
  local output = pipe:read("*a") or ""
  pipe:close()
  return output
end

local function spawn(command, log)
  return run("nohup " .. command .. " > " .. quote(log) .. " < /dev/null &")
end

-- Windows: io.popen runs the command with cmd.exe, which (as a console program started by a program
-- that has no console, LÖVE) shows a console window, for as long as it runs. The program is run with
-- CreateProcessW instead, with no shell, and CREATE_NO_WINDOW: a console program gets a console,
-- without a window. It is given only the handles of its input (NUL) and output (a pipe, or the log
-- file): not the ones of LÖVE (e.g. its sockets), which a game server would keep open after we quit.

if jit.os == "Windows" then
  local ffi = require("ffi")
  local C = ffi.C

  ffi.cdef([[
    typedef void *HANDLE;
    typedef struct {
      uint32_t nLength; void *lpSecurityDescriptor; int bInheritHandle;
    } SECURITY_ATTRIBUTES;
    typedef struct {
      uint32_t cb; uint16_t *lpReserved; uint16_t *lpDesktop; uint16_t *lpTitle;
      uint32_t dwX, dwY, dwXSize, dwYSize, dwXCountChars, dwYCountChars, dwFillAttribute, dwFlags;
      uint16_t wShowWindow, cbReserved2; uint8_t *lpReserved2;
      HANDLE hStdInput, hStdOutput, hStdError;
    } STARTUPINFOW;
    typedef struct { STARTUPINFOW StartupInfo; void *lpAttributeList; } STARTUPINFOEXW;
    typedef struct {
      HANDLE hProcess, hThread; uint32_t dwProcessId, dwThreadId;
    } PROCESS_INFORMATION;

    int MultiByteToWideChar(uint32_t codePage, uint32_t flags, const char *text, int size,
                            uint16_t *wide, int wideSize);
    uint32_t GetLastError(void);
    uint32_t FormatMessageA(uint32_t flags, const void *source, uint32_t id, uint32_t language,
                            char *buffer, uint32_t size, void *arguments);
    int CreatePipe(HANDLE *read, HANDLE *write, SECURITY_ATTRIBUTES *attributes, uint32_t size);
    HANDLE CreateFileW(const uint16_t *name, uint32_t access, uint32_t shareMode,
                       SECURITY_ATTRIBUTES *attributes, uint32_t disposition, uint32_t flags,
                       HANDLE template);
    int ReadFile(HANDLE file, void *buffer, uint32_t size, uint32_t *read, void *overlapped);
    int CloseHandle(HANDLE handle);
    uint32_t WaitForSingleObject(HANDLE handle, uint32_t milliseconds);
    int InitializeProcThreadAttributeList(void *list, uint32_t count, uint32_t flags, size_t *size);
    int UpdateProcThreadAttribute(void *list, uint32_t flags, size_t attribute, void *value,
                                  size_t size, void *previous, size_t *returnSize);
    void DeleteProcThreadAttributeList(void *list);
    int CreateProcessW(const uint16_t *application, uint16_t *commandLine,
                       SECURITY_ATTRIBUTES *processAttributes, SECURITY_ATTRIBUTES *threadAttributes,
                       int inheritHandles, uint32_t flags, void *environment,
                       const uint16_t *directory, STARTUPINFOW *startup,
                       PROCESS_INFORMATION *information);
  ]])

  local CP_UTF8 = 65001
  local FORMAT_MESSAGE_FROM_SYSTEM_IGNORE_INSERTS = 0x1200
  local GENERIC_READ, GENERIC_WRITE = 0x80000000, 0x40000000
  local FILE_SHARE_ALL = 7 -- read, write and delete
  local OPEN_EXISTING, CREATE_ALWAYS = 3, 2
  local FILE_ATTRIBUTE_NORMAL = 0x80
  local INVALID_HANDLE_VALUE = ffi.cast("HANDLE", -1)
  local INFINITE = 0xFFFFFFFF
  local STARTF_USESTDHANDLES = 0x100
  local PROC_THREAD_ATTRIBUTE_HANDLE_LIST = 0x20002
  local CREATE_NO_WINDOW, EXTENDED_STARTUPINFO_PRESENT = 0x08000000, 0x00080000

  -- the handles that we create are inheritable, but only the ones listed are inherited
  local inheritable = ffi.new("SECURITY_ATTRIBUTES", ffi.sizeof("SECURITY_ATTRIBUTES"), nil, 1)

  -- UTF-8 (LÖVE's paths) to UTF-16 (Windows')
  local function wide(text)
    local size = C.MultiByteToWideChar(CP_UTF8, 0, text, -1, nil, 0)
    local buffer = ffi.new("uint16_t[?]", size)
    C.MultiByteToWideChar(CP_UTF8, 0, text, -1, buffer, size)
    return buffer
  end

  local function failure(what)
    local code = C.GetLastError()
    local buffer = ffi.new("char[512]")
    local size = C.FormatMessageA(FORMAT_MESSAGE_FROM_SYSTEM_IGNORE_INSERTS, nil, code, 0, buffer, 512, nil)
    local message = size > 0 and (ffi.string(buffer, size):gsub("%s+$", "")) or "failed"
    return what .. ": " .. message .. " (error " .. code .. ")"
  end

  local function openFile(path, access, disposition)
    local file = C.CreateFileW(wide(path), access, FILE_SHARE_ALL, inheritable, disposition,
      FILE_ATTRIBUTE_NORMAL, nil)
    if file == INVALID_HANDLE_VALUE then return nil, failure("cannot open " .. path) end
    return file
  end

  -- Starts the command line (its program is found like Windows does, e.g. in the PATH, `.exe` being
  -- optional), with these handles as its input, and its output and errors. Returns its process.
  local function start(commandLine, input, output)
    local size = ffi.new("size_t[1]")
    C.InitializeProcThreadAttributeList(nil, 1, 0, size)
    local attributes = ffi.new("uint8_t[?]", tonumber(size[0]))
    if C.InitializeProcThreadAttributeList(attributes, 1, 0, size) == 0 then
      return nil, failure("cannot run " .. commandLine)
    end
    local handles = ffi.new("HANDLE[2]", input, output)
    local information = ffi.new("PROCESS_INFORMATION")
    local started = C.UpdateProcThreadAttribute(attributes, 0, PROC_THREAD_ATTRIBUTE_HANDLE_LIST,
      handles, ffi.sizeof(handles), nil, nil) ~= 0
    if started then
      local startup = ffi.new("STARTUPINFOEXW")
      startup.StartupInfo.cb = ffi.sizeof(startup)
      startup.StartupInfo.dwFlags = STARTF_USESTDHANDLES
      startup.StartupInfo.hStdInput = input
      startup.StartupInfo.hStdOutput = output
      startup.StartupInfo.hStdError = output
      startup.lpAttributeList = attributes
      started = C.CreateProcessW(nil, wide(commandLine), nil, nil, 1,
        CREATE_NO_WINDOW + EXTENDED_STARTUPINFO_PRESENT, nil, nil,
        ffi.cast("STARTUPINFOW *", startup), information) ~= 0
    end
    local err = not started and failure("cannot run " .. commandLine)
    C.DeleteProcThreadAttributeList(attributes)
    if not started then return nil, err end
    C.CloseHandle(information.hThread)
    return information.hProcess
  end

  -- Starts the command line with its input from NUL and its output to `output` (closed then).
  local function startWithOutput(commandLine, output)
    local input, err = openFile("NUL", GENERIC_READ, OPEN_EXISTING)
    local process
    if input then
      process, err = start(commandLine, input, output)
      C.CloseHandle(input)
    end
    C.CloseHandle(output)
    return process, err
  end

  run = function(commandLine)
    local read, write = ffi.new("HANDLE[1]"), ffi.new("HANDLE[1]")
    if C.CreatePipe(read, write, inheritable, 0) == 0 then
      return nil, failure("cannot run " .. commandLine)
    end
    local process, err = startWithOutput(commandLine, write[0])
    local output
    if process then
      -- until the program closes its output (when it ends)
      local chunks, buffer, count = {}, ffi.new("uint8_t[4096]"), ffi.new("uint32_t[1]")
      while C.ReadFile(read[0], buffer, 4096, count, nil) ~= 0 and count[0] > 0 do
        chunks[#chunks + 1] = ffi.string(buffer, count[0])
      end
      C.WaitForSingleObject(process, INFINITE)
      C.CloseHandle(process)
      output = table.concat(chunks)
    end
    C.CloseHandle(read[0])
    return output, err
  end

  spawn = function(commandLine, log)
    local output, err = openFile(log, GENERIC_WRITE, CREATE_ALWAYS)
    if not output then return nil, err end
    local process
    process, err = startWithOutput(commandLine, output)
    if not process then return nil, err end
    C.CloseHandle(process)
    return ""
  end
end

while true do
  local r = inputs:demand()
  local output, err
  if r.option then output, err = spawn(r.input, r.option) else output, err = run(r.input) end
  results:push({ id = r.id, result = output or "", error = err or "" })
end
