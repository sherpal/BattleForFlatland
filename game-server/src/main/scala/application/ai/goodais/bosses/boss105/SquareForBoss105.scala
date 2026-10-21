package application.ai.goodais.bosses.boss105

import application.ai.goodais.classes.SquareAIController
import gamelogic.entities.boss.boss105.Shade
import gamelogic.entities.boss.dawnoftime.Boss105
import gamelogic.entities.classes.Square
import gamelogic.entities.{Entity, MovingBody, WithTarget}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex

/** The two tanks of Boss105 each hold one of the twins, far from the other one.
  *
  * Which twin is mine? When the other Square is a bot, the bots simply split the work by `index`
  * (the first one takes the boss, the second one the Shade). When it is a human, I take the twin
  * that they don't seem to be holding: the one that is attacking them, or else the one that is
  * closer to them.
  */
final case class SquareForBoss105(index: Int, entityId: Entity.Id)
    extends SquareAIController(index)
    with Boss105Bot[Square] {

  override def isTankOfATwin(gameState: GameState, me: Square): Boolean = true

  /** The corner where I hold the Shade, remembered while it is unclear. */
  private var shadeCorner: Complex = Boss105.shadeStartingPosition

  /** Whether I am the one who holds the Shade (rather than the boss). */
  def holdsTheShade(gameState: GameState, me: Square, time: Long): Boolean =
    gameState.players.values
      .collectFirst { case other: Square if other.id != me.id => other }
      .fold(false) { other =>
        if Boss105Bot.playerClassIsBot(other) then index == 1
        else {
          val otherPosition = other.currentPosition(time)
          val shadePosition =
            Boss105Bot.theShade(gameState).fold(shadeCorner)(_.currentPosition(time))
          val bossPosition = Boss105Bot
            .theBoss(gameState)
            .fold(Boss105.startingPosition)(
              _.currentPosition(time)
            )
          val theyHoldTheBoss =
            Boss105Bot.theBoss(gameState).exists(_.targetId == other.id) ||
              (!Boss105Bot.theShade(gameState).exists(_.targetId == other.id) &&
                otherPosition.distanceTo(bossPosition) < otherPosition.distanceTo(shadePosition))
          theyHoldTheBoss
        }
      }

  def restingPosition(gameState: GameState, me: Square, time: Long): (Complex, Double) =
    if holdsTheShade(gameState, me, time) then {
      shadeCorner = Boss105Bot.maybeShadeCorner(gameState, time).getOrElse(shadeCorner)
      Boss105Bot.shadeTankPosition(shadeCorner) -> 10.0
    } else Boss105Bot.bossTankPosition -> 10.0

  /** The twin that I hold, if it is there. */
  private def myTwin(
      gameState: GameState,
      me: Square,
      time: Long
  ): Option[MovingBody & WithTarget] =
    if holdsTheShade(gameState, me, time) then Boss105Bot.theShade(gameState)
    else Boss105Bot.theBoss(gameState)

  override def instantActions(gameState: GameState, me: Square, time: Long): Vector[GameAction] =
    myTwin(gameState, me, time).fold(Vector.empty[GameAction]) { twin =>
      val shouldIHammer = twin match {
        case shade: Shade => isMyThreatTowardsTargetEnough(shade, me)
        case boss: Boss105 => isMyThreatTowardsTargetEnough(boss, me)
        case _ => true
      }

      val maybeTaunt =
        Option.unless(shouldIHammer)(maybeTauntUsage(gameState, time, me, twin)).flatten
      val maybeEnrage = maybeEnrageUsage(
        gameState,
        time,
        me,
        me.resourceAmount.amount < 5 && me.life > 100 && !alreadyEnraged(gameState, me)
      )
      // hitting a Shade held by a stun would break it
      val maybeHammerHit =
        Option
          .when(
            shouldIHammer && !(twin
              .isInstanceOf[Shade] && Boss105Bot.holdFireOnShade(gameState, time))
          )(
            maybeHammerHitUsage(gameState, time, me, twin)
          )
          .flatten

      maybeHammerHit.orElse(maybeTaunt).orElse(maybeEnrage).toVector
    }

  def actionsInPlace(gameState: GameState, me: Square, time: Long): Vector[GameAction] =
    Vector.empty

}
