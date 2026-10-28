package menus.data

final class HostCandidacyTest extends munit.FunSuite {

  private def candidacy(
      java: Option[Int],
      processors: Int,
      roundTrip: Int,
      bundledJava: Boolean = false,
      ready: Boolean = true,
      willing: Boolean = true
  ) =
    HostCandidacy("game", willing, java, bundledJava, ready, processors, "Linux", roundTrip)

  test("only the players with a recent enough java, or the menu server's one, can host") {
    val ranking = HostCandidacy.ranking(
      Map(
        "no java"      -> candidacy(None, 16, 10),
        "old java"     -> candidacy(Some(11), 16, 10),
        "java 17"      -> candidacy(Some(17), 2, 400),
        "bundled java" -> candidacy(None, 2, 500, bundledJava = true)
      ),
      "no java"
    )
    assertEquals(ranking, Vector("java 17", "bundled java"))
  }

  test("the players who do not host game servers are not chosen") {
    val ranking = HostCandidacy.ranking(
      Map(
        "willing"     -> candidacy(Some(17), 2, 400),
        "mobile"      -> candidacy(Some(21), 16, 5, bundledJava = true, willing = false),
        "mobile, too" -> candidacy(None, 8, 5, willing = false)
      ),
      "mobile"
    )
    assertEquals(ranking, Vector("willing"))
  }

  test("decent computer, then ready, then network, then the creator") {
    val ranking = HostCandidacy.ranking(
      Map(
        "slow computer" -> candidacy(Some(21), 2, 5),
        "far"           -> candidacy(Some(21), 8, 300),
        "near"          -> candidacy(Some(21), 4, 30),
        "creator"       -> candidacy(Some(17), 4, 40),
        "downloading"   -> candidacy(None, 8, 5, bundledJava = true, ready = false)
      ),
      "creator"
    )
    assertEquals(ranking, Vector("creator", "near", "far", "downloading", "slow computer"))
  }
}
