package application.ai.goodais.bosses.sirofvice

import application.ai.goodais.classes.PentagonAIController
import gamelogic.buffs.boss.sirofvice.{Inflamed, Punished}
import gamelogic.entities.Entity
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.boss.firstraid.SirOfVice
import gamelogic.entities.classes.Pentagon
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex

/** Ranged damage dealers. They are also in charge of dispelling [[Inflamed]].
  *
  * The composition has two Pentagons: `index` gives each of them its own spot, and delays the
  * reaction of the second one so that they don't both dispel the same debuff.
  *
  * They don't dispel instantly, but after a reaction time similar to what a human would need to
  * notice the debuff (see [[reactionTime]]).
  */
final case class PentagonForSirOfVice(index: Int, entityId: Entity.Id)
    extends PentagonAIController(index)
    with SirOfViceBot[Pentagon] {

  def isMelee: Boolean = false

  private val direction: Angle =
    Angle.Pi * Angle.fromDouble(if index % 2 == 0 then 2.0 / 3 else 4.0 / 3)

  def restingPosition(
      gameState: GameState,
      me: Pentagon,
      maybeBoss: Option[SirOfVice],
      time: Long
  ): (Complex, Double) = rangedSpot(direction, maybeBoss, time) -> 10.0

  def actionsInPlace(
      gameState: GameState,
      me: Pentagon,
      boss: SirOfVice,
      time: Long
  ): Vector[GameAction] =
    if isSilenced(gameState, me) then Vector.empty
    else {
      // a punished player is better off taking the first tick, which frees them
      def maybeDispel = gameState
        .allTBuffs[Inflamed]
        .filter(buff => time - buff.appearanceTime > reactionTime(buff))
        .filterNot(buff => gameState.hasBuffOfType[Punished](buff.bearerId))
        .toVector
        .sortBy(_.appearanceTime)
        .iterator
        .flatMap(buff => maybeDispelUsage(gameState, time, buff.bearerId, me))
        .nextOption()

      def maybeBullet = Option
        .when(gameState.areTheyInSight(me.id, boss.id, time).getOrElse(false))(
          maybePentagonBulletUsage(
            gameState,
            time,
            me,
            (boss.currentPosition(time) - me.currentPosition(time)).arg
          )
        )
        .flatten

      maybeDispel.orElse(maybeBullet).toVector
    }

  /** How long it takes to react to the given debuff: somewhere between
    * [[PentagonForSirOfVice.minimumReactionTime]] and that plus
    * [[PentagonForSirOfVice.reactionTimeSpread]]. It is drawn once per debuff (the buff id is the
    * seed), otherwise re-drawing at each frame would always end up with the shortest one. The
    * second Pentagon reacts a bit later, so that they don't both go for the same debuff.
    */
  private def reactionTime(buff: Inflamed): Long =
    PentagonForSirOfVice.minimumReactionTime +
      scala.util
        .Random(buff.buffId.hashCode.toLong)
        .nextLong(PentagonForSirOfVice.reactionTimeSpread) +
      index * PentagonForSirOfVice.secondPentagonExtraDelay

}

object PentagonForSirOfVice {

  inline def minimumReactionTime: Long      = 1500L
  inline def reactionTimeSpread: Long       = 1000L
  inline def secondPentagonExtraDelay: Long = 700L

}
