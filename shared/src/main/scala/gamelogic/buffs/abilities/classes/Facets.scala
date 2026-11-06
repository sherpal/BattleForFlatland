package gamelogic.buffs.abilities.classes

import gamelogic.buffs.{Buff, PassiveBuff, StackedBuff}
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.{PutStackedBuff, RemoveBuff}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.PosInt
import gamelogic.utils.{IdGeneratorContainer, IdsProducer}

/** The facets of the Pentagon: each [[gamelogic.entities.movingstuff.PentagonBullet]] hitting an
  * enemy lights one, up to `Facets.maxStacks`, and [[gamelogic.abilities.pentagon.Pentagram]]
  * spends them. A bullet hitting while they are all lit is wasted (it only resets their time).
  *
  * They do nothing by themselves. After `Facets.duration` without being lit, one goes out (see
  * [[gamelogic.buffs.StackedBuff]]).
  */
final case class Facets(
    buffId: Buff.Id,
    bearerId: Entity.Id,
    sourceId: Entity.Id,
    appearanceTime: Long,
    stacks: PosInt
) extends PassiveBuff
    with StackedBuff {

  def actionTransformer(gameAction: GameAction): Vector[GameAction] = Vector(gameAction)

  def duration: Long = Facets.duration

  def resourceIdentifier: Buff.StackedBuffIdentifier = Buff.pentagonFacets
}

object Facets {

  /** Time (in millis) after which a facet goes out. */
  inline def duration: Long = 15000L

  /** Maximum number of facets (one per side of the pentagon). */
  inline def maxStacks: PosInt = PosInt.of(5)

  /** The facets that `entityId` currently has. There should be at most one. */
  def on(gameState: GameState, entityId: Entity.Id): Vector[Facets] =
    gameState.allBuffsOfEntity(entityId).collect { case facets: Facets => facets }.toVector

  /** The number of facets that `entityId` currently has, if any. */
  def stacksOn(gameState: GameState, entityId: Entity.Id): Option[PosInt] =
    on(gameState, entityId).map(_.stacks).maxOption

  /** Actions replacing the facets of `entityId` (if any) by `stacks` facets (if any), starting at
    * `time`. The number of facets can't go above `maxStacks`.
    */
  def setStacks(gameState: GameState, entityId: Entity.Id, time: Long, stacks: Option[PosInt])(using
      IdGeneratorContainer
  ): Vector[GameAction] =
    on(gameState, entityId).map(facets =>
      RemoveBuff(IdsProducer.genActionId(), time, entityId, facets.buffId)
    ) ++ stacks.map(newStacks =>
      PutStackedBuff(
        IdsProducer.genActionId(),
        time,
        IdsProducer.genBuffId(),
        entityId,
        entityId,
        time,
        Buff.pentagonFacets,
        newStacks.min(maxStacks)
      )
    )

}
