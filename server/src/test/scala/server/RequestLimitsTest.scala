package server

import server.RequestLimits.{Kind, Limits, Rate}

final class RequestLimitsTest extends munit.FunSuite {

  private val second = 1_000_000_000L

  test("a visitor has a burst of requests, and then a number per second") {
    val limits = Limits(requests = Rate(burst = 3, perSecond = 2))
    for _ <- 1 to 3 do assertEquals(limits.admit("a", Kind.Other, 0), None)
    assertEquals(limits.admit("a", Kind.Other, 0).map(_.retryAfter), Some(1L))
    assertEquals(limits.admit("b", Kind.Other, 0), None, "the others are not slowed down")
    assertEquals(limits.admit("a", Kind.Other, second / 2), None)
    assertEquals(limits.admit("a", Kind.Other, second / 2).map(_.retryAfter), Some(1L))
  }

  test("downloads have their own limits, and only so many run at the same time") {
    val limits = Limits(downloads = Rate(burst = 3, perSecond = 1.0 / 60), concurrentDownloads = 2)
    assertEquals(limits.admit("a", Kind.Download, 0), None)
    assertEquals(limits.admit("a", Kind.Download, 0), None)
    assertEquals(
      limits.admit("a", Kind.Download, 0).map(_.retryAfter),
      Some(30L),
      "two are running"
    )
    assertEquals(limits.admit("a", Kind.Other, 0), None, "other requests still go")
    limits.downloadDone("a")
    assertEquals(limits.admit("a", Kind.Download, 0), None)
    limits.downloadDone("a")
    assertEquals(
      limits.admit("a", Kind.Download, 0).map(_.retryAfter),
      Some(60L),
      "none left until a minute"
    )
    assertEquals(limits.admit("a", Kind.Download, 60 * second), None)
  }

  test("the menus' calls have far more, apart from the other requests") {
    val limits = Limits(requests = Rate(burst = 2, perSecond = 1), menus = Rate(10, 5))
    for _ <- 1 to 10 do assertEquals(limits.admit("a", Kind.Menu, 0), None)
    assertEquals(limits.admit("a", Kind.Menu, 0).map(_.retryAfter), Some(1L))
    assertEquals(limits.admit("a", Kind.Other, 0), None, "the website still goes")
  }

  test("the requests refused are logged once a minute at most, for each visitor") {
    val limits = Limits(requests = Rate(burst = 1, perSecond = 1))
    limits.admit("a", Kind.Other, 0)
    assertEquals(limits.admit("a", Kind.Other, 0).map(_.log), Some(true))
    assertEquals(limits.admit("a", Kind.Other, 0).map(_.log), Some(false))
    assertEquals(limits.admit("a", Kind.Other, second), None)
    assertEquals(limits.admit("a", Kind.Other, second).map(_.log), Some(false))
    limits.admit("a", Kind.Other, 60 * second)
    assertEquals(limits.admit("a", Kind.Other, 60 * second).map(_.log), Some(true))
  }

  test("the visitors who have all they can have are forgotten, but not while they download") {
    val limits = Limits(requests = Rate(burst = 10, perSecond = 10))
    limits.admit("a", Kind.Other, 0)
    limits.admit("b", Kind.Download, 0)
    assertEquals(limits.size, 2)
    limits.admit("c", Kind.Other, 61 * second)
    assertEquals(limits.size, 2, "a is forgotten, b downloads still")
    limits.downloadDone("b")
    assertEquals(limits.admit("a", Kind.Other, 61 * second), None)
  }

  test("by default, four players behind the same address download all they need, together") {
    val limits = Limits()
    // the client, the game server and its Java runtime, each, at the same time but for one
    for round <- 1 to 3 do {
      for _ <- 1 to 4 do assertEquals(limits.admit("family", Kind.Download, 0), None, round)
      for _ <- 1 to 4 do limits.downloadDone("family")
    }
    // and again after a deploy, a few minutes later
    for _ <- 1 to 4 do assertEquals(limits.admit("family", Kind.Download, 300 * second), None)
  }

  test("the big files are the downloads") {
    assert(RequestLimits.isDownload("/api/bff/client/linux"))
    assert(RequestLimits.isDownload("/api/bff/game-server/jar"))
    assert(RequestLimits.isDownload("/api/bff/game-server/runtime"))
    assert(!RequestLimits.isDownload("/api/bff/game-server/runtime/version"))
    assert(!RequestLimits.isDownload("/en"))
  }
}
