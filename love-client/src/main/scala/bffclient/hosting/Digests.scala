package bffclient.hosting

import bffclient.lua.DigestModule

/** SHA-256 of files, computed on a LÖVE thread (`digest.lua`): `run(path)` (a path of the operating
  * system) gives it in hexadecimal, lower case, the same as the JVM's `MessageDigest` (the menu
  * server's) would.
  */
object Digests extends WorkerResults(DigestModule)
