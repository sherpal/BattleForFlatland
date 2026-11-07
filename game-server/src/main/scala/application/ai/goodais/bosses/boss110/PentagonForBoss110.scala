package application.ai.goodais.bosses.boss110

import application.ai.goodais.classes.PentagonAIController
import gamelogic.entities.{Entity, MovingBody}
import gamelogic.entities.boss.boss110.SmallGuy
import gamelogic.entities.classes.Pentagon
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex

/** The four Pentagons of Boss110 cover the bombs: they wait around the area where the bombs fall,
  * and each of them runs to its own bomb when they are down (see [[Boss110Bot.assignedBomb]]).
  *
  * Between two bombs, the first two thin out the small guys (with a zone when they are many), and
  * the other two shoot the big guy that the Triangles kill, or the boss.
  */
final case class PentagonForBoss110(index: Int, entityId: Entity.Id)
    extends PentagonAIController(index)
    with Boss110Bot[Pentagon] {

  private def onSmallGuys: Boolean = index < 2

  def restingPosition(gameState: GameState, me: Pentagon, time: Long): (Complex, Double) =
    Boss110Bot
      .assignedBomb(gameState, index)
      .fold(Boss110Bot.bombWaitingPosition(index) -> 10.0)(_.pos -> 3.0)

  def actionsInPlace(gameState: GameState, me: Pentagon, time: Long): Vector[GameAction] = {
    val smallGuys = gameState.allTEntities[SmallGuy].values.toVector

    val maybeZone = Option
      .when(onSmallGuys && smallGuys.length >= 3 && !isMyZoneThere(gameState, me))(
        smallGuys.map(_.currentPosition(time)).sum / smallGuys.length
      )
      .flatMap(maybePentagonZoneUsage(gameState, time, me, _))

    val foe: Option[MovingBody] =
      if onSmallGuys && smallGuys.nonEmpty then smallGuys.minByOption(_.life)
      else
        Boss110Bot
          .bigGuyToKill(gameState)
          .orElse(gameState.bosses.values.headOption.filter(notTooMuchThreat(_, me)))

    maybeZone
      .orElse(foe.flatMap(maybeDamageUsage(gameState, time, me, _)))
      .toVector
  }

  /** Taking the boss from the tank would be a disaster. */
  private def notTooMuchThreat(boss: gamelogic.entities.boss.BossEntity, me: Pentagon): Boolean =
    (for {
      myThreat   <- boss.damageThreats.get(me.id)
      tankThreat <- boss.damageThreats.values.maxOption
    } yield myThreat.value < tankThreat.value * 0.9).getOrElse(true)

}
