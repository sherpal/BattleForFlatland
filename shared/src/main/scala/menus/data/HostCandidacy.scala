package menus.data

import io.circe.Codec

/** What a player's client says about its ability to run the game server, when the game starts (the
  * game server runs on the computer of one of the players, see [[HostCandidacy.ranking]]).
  *
  * @param willing
  *   whether the player lets their computer run game servers (a setting of the client, e.g. off on
  *   a mobile network): if not, the rest says nothing
  * @param javaVersion
  *   the major version of the `java` of the player's computer, if any (the game server is a jar)
  * @param bundledJava
  *   whether the menu server has a Java runtime for the player's platform, which the client runs the
  *   game server with (downloading it first, if needed), rather than the `java` of the computer
  * @param ready
  *   whether the client has all it needs to run the game server already (nothing to download)
  * @param processorCount
  *   number of logical processors
  * @param os
  *   as LÖVE says it (e.g. "Linux", "Windows", "OS X")
  * @param menuServerRoundTripMs
  *   how long a request to the menu server took, a rough idea of how good the network is
  */
final case class HostCandidacy(
    gameId: String,
    willing: Boolean,
    javaVersion: Option[Int],
    bundledJava: Boolean,
    ready: Boolean,
    processorCount: Int,
    os: String,
    menuServerRoundTripMs: Int
) {
  def canHost: Boolean =
    willing && (bundledJava || javaVersion.exists(_ >= HostCandidacy.minimumJavaVersion))

  /** At least 4 processors: the game server runs the game and the AIs, next to a client. */
  def isDecentComputer: Boolean = processorCount >= 4
}

object HostCandidacy {
  given Codec[HostCandidacy] = io.circe.generic.semiauto.deriveCodec

  /** The game server's jar is compiled for Java 17. */
  val minimumJavaVersion = 17

  /** The players who can (and want to) host, best first: a decent computer first, then the ones who are ready
    * (no download before the game starts), then the best network (the fastest round trip to the
    * menu server, in steps of 50 ms so that noise does not count), and the game creator among
    * equals.
    */
  def ranking(candidacies: Map[String, HostCandidacy], gameCreator: String): Vector[String] =
    candidacies.toVector
      .filter(_._2.canHost)
      .sortBy((name, candidacy) =>
        (
          !candidacy.isDecentComputer,
          !candidacy.ready,
          candidacy.menuServerRoundTripMs / 50,
          name != gameCreator,
          name
        )
      )
      .map(_._1)
}
