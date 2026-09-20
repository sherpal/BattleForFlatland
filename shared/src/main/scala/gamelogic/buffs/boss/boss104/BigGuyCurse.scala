package gamelogic.buffs.boss.boss104

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, PassiveBuff}
import gamelogic.entities.Entity
import gamelogic.entities.Entity.Id
import gamelogic.entities.boss.boss104.{BigGuy, BigGuyDeathMark}
import gamelogic.gamestate.gameactions.boss104.{AddBigGuyDeathMark, AddDeathMarkTriangle}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** Invisible, never-ending buff carried by every [[BigGuy]], whose sole purpose is to leave a
  * [[BigGuyDeathMark]] behind when its bearer dies -- and, when that mark is the third one, to draw
  * the [[gamelogic.entities.boss.boss104.DeathMarkTriangle]] that Boss104's finisher needs.
  *
  * Overriding `bearerDiedAction` (instead of `endingAction`, which it defaults to) is the idiomatic
  * way to implement "when this thing dies, something happens" mechanics.
  */
final case class BigGuyCurse(
    buffId: Buff.Id,
    bearerId: Entity.Id,
    sourceId: Entity.Id,
    appearanceTime: Long
) extends PassiveBuff {
  override def actionTransformer(gameAction: GameAction): Vector[GameAction] = Vector(gameAction)

  override def duration: Long = -1

  override def resourceIdentifier: ResourceIdentifier = Buff.boss104BigGuyCurse

  override def endingAction(gameState: GameState, time: Long, maybeDispelledBy: Option[Id])(using
      IdGeneratorContainer
  ): Vector[GameAction] = Vector.empty

  override def bearerDiedAction(gameState: GameState, time: Long)(using
      IdGeneratorContainer
  ): Vector[GameAction] =
    gameState
      .entityByIdAs[BigGuy](bearerId)
      .toVector
      .flatMap {
        bigGuy => // this function is called on a game state where the action to remove it did not occur yet
          val existingMarks = gameState.allTEntities[BigGuyDeathMark].values.map(_.pos).toVector

          // This big guy is the third one to fall: its mark closes the triangle.
          val maybeTriangle = Option.when(existingMarks.sizeIs == 2)(
            AddDeathMarkTriangle(genActionId(), time, genEntityId(), existingMarks :+ bigGuy.pos)
          )

          val addMark = AddBigGuyDeathMark(genActionId(), time, genEntityId(), bigGuy.pos)

          addMark +: maybeTriangle.toVector
      }
}
