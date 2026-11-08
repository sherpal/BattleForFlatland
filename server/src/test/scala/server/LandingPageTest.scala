package server

import services.i18n.Language

final class LandingPageTest extends munit.FunSuite {

  private def preferred(header: String): Language =
    LandingRoutes.preferredLanguage(Some(Seq(header)))

  test("the root goes to the visitor's favourite language among ours") {
    assertEquals(preferred("fr-BE,fr;q=0.9,en;q=0.8"), Language.French)
    assertEquals(preferred("ja,de;q=0.5,en;q=0.7"), Language.English)
    assertEquals(preferred("nl;q=0.2,sq"), Language.Albanian)
    assertEquals(preferred("de;q=0,fr;q=0.1"), Language.French)
    assertEquals(preferred("ja"), Language.English)
    assertEquals(LandingRoutes.preferredLanguage(None), Language.English)
  }

  test("the page exists in each language, in it, with the downloads and the other languages") {
    for language <- Language.values do {
      val page = LandingPage(language)
      assert(page.startsWith("<!DOCTYPE html><html lang=\"" + language.code + "\""), language)
      assert(page.contains(language.i18n.landingTagline), language)
      assert(page.contains(language.i18n.story.heading), language)
      assert(page.contains(language.i18n.landingFaqTitle), language)
      for (title, _) <- language.i18n.landingOpenSource do assert(page.contains(title), title)
      for system <- Seq("windows", "macos", "linux", "love") do
        assert(page.contains(s"/api/bff/client/$system"), (language, system))
      for other <- Language.values do assert(page.contains(s"href=\"/${other.code}\""), other)
    }
  }

  test("the pictures are linked at the hash of their content") {
    for asset <- LandingAssets("fight.png") do {
      assertEquals(asset.url, s"/landing/${LandingAssets.hash(asset.bytes)}/fight.png")
      assert(LandingPage(Language.English).contains(asset.url))
    }
  }

  test("an ETag is matched among those that the browser has") {
    assert(LandingRoutes.matches("\"abc\"", "\"abc\""))
    assert(LandingRoutes.matches("\"x\", W/\"abc\"", "\"abc\""))
    assert(LandingRoutes.matches("*", "\"abc\""))
    assert(!LandingRoutes.matches("\"abcd\"", "\"abc\""))
  }

  test("the ranges of bytes that a browser asks are the ones it gets") {
    import LandingRoutes.range
    assertEquals(range("bytes=0-99", 1000), Some((0, 99)))
    assertEquals(range("bytes=0-1", 2), Some((0, 1)))
    assertEquals(range("bytes=500-", 1000), Some((500, 999)))
    assertEquals(range("bytes=990-2000", 1000), Some((990, 999)))
    assertEquals(range("bytes=-100", 1000), Some((900, 999)))
    assertEquals(range("bytes=-5000", 1000), Some((0, 999)))
    assertEquals(range("bytes=1000-", 1000), None)
    assertEquals(range("bytes=5-2", 1000), None)
    assertEquals(range("bytes=0-1,5-6", 1000), None)
    assertEquals(range("lines=0-1", 1000), None)
  }

  test("the shapes' points are written with dots, whatever the server's locale") {
    val default = java.util.Locale.getDefault
    java.util.Locale.setDefault(java.util.Locale.FRANCE)
    try {
      val points = """points="([^"]*)"""".r
        .findAllMatchIn(LandingPage(Language.German))
        .map(_.group(1))
        .toList
      assert(points.nonEmpty)
      for point <- points.flatMap(_.split(' ')) do
        assert(point.matches("""-?\d+\.\d,-?\d+\.\d"""), point)
    } finally java.util.Locale.setDefault(default)
  }
}
