package gamelogic.buffs

import gamelogic.buffs.abilities.classes.{BraceShield, Facets}
import gamelogic.entities.Entity
import gamelogic.physics.PosInt

/** The [[StackedBuff]]s, which, unlike the [[SimpleBuffs]], also need a number of stacks. */
object StackedBuffs {

  private val stackedBuffs
      : Map[Buff.StackedBuffIdentifier, (Buff.Id, Entity.Id, Entity.Id, Long, PosInt) => Buff] =
    Map(
      Buff.squareBrace    -> BraceShield.apply,
      Buff.pentagonFacets -> Facets.apply
    )

  def apply(
      identifier: Buff.StackedBuffIdentifier,
      buffId: Buff.Id,
      bearerId: Entity.Id,
      sourceId: Entity.Id,
      appearanceTime: Long,
      stacks: PosInt
  ): Buff = stackedBuffs(identifier)(buffId, bearerId, sourceId, appearanceTime, stacks)

}
