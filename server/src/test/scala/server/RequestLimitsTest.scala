package server

import server.RequestLimits.{Limits, Rate}

final class RequestLimitsTest extends munit.FunSuite {

  private val second = 1_000_000_000L

  test("a visitor has a burst of requests, and then a number per second") {
    val limits = Limits(requests = Rate(burst = 3, perSecond = 2))
    for _ <- 1 to 3 do assertEquals(limits.admit("a", download = false, 0), None)
    assertEquals(limits.admit("a", download = false, 0), Some(1L))
    assertEquals(limits.admit("b", download = false, 0), None, "the others are not slowed down")
    assertEquals(limits.admit("a", download = false, second / 2), None)
    assertEquals(limits.admit("a", download = false, second / 2), Some(1L))
  }

  test("downloads have their own limits, and only so many run at the same time") {
    val limits = Limits(downloads = Rate(burst = 3, perSecond = 1.0 / 60), concurrentDownloads = 2)
    assertEquals(limits.admit("a", download = true, 0), None)
    assertEquals(limits.admit("a", download = true, 0), None)
    assertEquals(limits.admit("a", download = true, 0), Some(30L), "two are running")
    assertEquals(limits.admit("a", download = false, 0), None, "other requests still go")
    limits.downloadDone("a")
    assertEquals(limits.admit("a", download = true, 0), None)
    limits.downloadDone("a")
    assertEquals(limits.admit("a", download = true, 0), Some(60L), "none left until a minute")
    assertEquals(limits.admit("a", download = true, 60 * second), None)
  }

  test("the visitors who have all they can have are forgotten, but not while they download") {
    val limits = Limits(requests = Rate(burst = 10, perSecond = 10))
    limits.admit("a", download = false, 0)
    limits.admit("b", download = true, 0)
    assertEquals(limits.size, 2)
    limits.admit("c", download = false, 61 * second)
    assertEquals(limits.size, 2, "a is forgotten, b downloads still")
    limits.downloadDone("b")
    assertEquals(limits.admit("a", download = false, 61 * second), None)
  }

  test("the big files are the downloads") {
    assert(RequestLimits.isDownload("/api/bff/client/linux"))
    assert(RequestLimits.isDownload("/api/bff/game-server/jar"))
    assert(RequestLimits.isDownload("/api/bff/game-server/runtime"))
    assert(!RequestLimits.isDownload("/api/bff/game-server/runtime/version"))
    assert(!RequestLimits.isDownload("/en"))
  }
}
