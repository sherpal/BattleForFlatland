package gamelogic.docs

import models.bff.outofgame.PlayerClasses
import gamelogic.entities.boss.BossFactory
import models.bff.outofgame.gameconfig.PlayerInfo
import utils.misc.RGBColour
import models.bff.outofgame.gameconfig.PlayerStatus
import models.bff.outofgame.gameconfig.PlayerType
import models.bff.outofgame.gameconfig.PlayerName

/** The [[BossMetadata]] describe the common metadata that boss entities must provide.
  */
trait BossMetadata {

  /** Name of the boss (in English).
    *
    * This is a val so that we can pattern match on it.
    */
  val name: String

  def maxLife: Double

  /** Number of players this boss is intended to be fought against. */
  def intendedFor: Int

  /** Composition of a team form of all AIs. None if the boss does not support AIs. */
  def maybeAIComposition: Option[List[PlayerClasses]]

  final def nextAvailableAI(currentClasses: Vector[PlayerClasses]): Option[PlayerClasses] =
    maybeAIComposition.flatMap { aiConfiguration =>
      val aiClassesCounts = aiConfiguration.groupMapReduce(identity)(_ => 1)(_ + _).toVector
      aiClassesCounts.collectFirst {
        case (cls, count) if currentClasses.count(_ == cls) < count => cls
      }
    }

  final def fillWithAI(currentClasses: Vector[PlayerClasses]): Option[Vector[PlayerClasses]] =
    maybeAIComposition.map(_.toVector).map { _ =>
      def addNext(accumulatedClasses: Vector[PlayerClasses]): Vector[PlayerClasses] =
        nextAvailableAI(currentClasses ++ accumulatedClasses) match {
          case None            => accumulatedClasses
          case Some(nextClass) => addNext(accumulatedClasses :+ nextClass)
        }

      addNext(Vector.empty)
    }

  /** Returns maybe the composition where the left element in the tuples is the name given to the
    * player. Names follow a simple pattern of appending a by-class index to the name of the class.
    *
    * For example, Hexagon0, Hexagon1, Square0, Triangle0
    */
  final def maybeAICompositionWithNames: Option[List[(String, PlayerInfo)]] =
    for {
      ais <- maybeAIComposition
      aisByClasses = ais.groupBy(identity).toList
      aisByClassesWithName = aisByClasses
        .flatMap { case (cls, elems) =>
          elems.indices.map(PlayerName.AIPlayerName(cls, _))
        }
        .zip(RGBColour.repeatedColours)
        .map { case (name, colour) =>
          (
            name.name,
            PlayerInfo(
              name,
              Some(name.cls),
              Some(colour),
              PlayerStatus.Ready,
              PlayerType.ArtificialIntelligence
            )
          )
        }
    } yield aisByClassesWithName

}

object BossMetadata {

  /** Returns the [[BossMetadata]] with the given name, or [[scala.None]] if it does not exist (or
    * if that boss' name is not a [[BossMetadata]]).
    */
  def maybeMetadataByName(bossName: String): Option[BossMetadata] =
    BossFactory.factoriesByBossName.get(bossName).collect { case metadata: BossMetadata =>
      metadata
    }

  def firstBossName = allBossNames.head

  inline def allBossNames = BossFactory.factoriesByBossName.keys.toVector.sorted

}
