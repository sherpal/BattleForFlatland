package bffclient.hosting

import bffclient.lua.ProcessModule

/** Shell commands, run on a LÖVE thread (`process.lua`): `run(command)` gives their output (stdout
  * and stderr, together).
  */
object Processes extends WorkerResults(ProcessModule)
