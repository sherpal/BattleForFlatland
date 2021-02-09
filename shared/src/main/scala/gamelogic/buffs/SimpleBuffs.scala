package gamelogic.buffs

import gamelogic.buffs.abilities.classes.UpgradeDirectHit
import gamelogic.buffs.ai.{DamageThreatAware, HealingThreatAware}
import gamelogic.buffs.resourcebuffs.{EnergyFiller, ManaFiller, RageFiller}
import gamelogic.entities.Entity
import gamelogic.buffs.abilities.classes.TriangleStunDebuff
import gamelogic.buffs.boss.boss110.BrokenArmor

/**
  * A lot of buffs (especially the "never ending" ones) simply require
  * - a bearerId [[gamelogic.entities.Entity.Id]]
  * - an appearance time (Long)
  *
  * All these buffs will be grouped into a single [[gamelogic.gamestate.GameAction]] so that we drastically
  * reduce the number of game actions needed to be created.
  */
object SimpleBuffs {

  final val simpleBuffs: Map[Buff.ResourceIdentifier, (Buff.Id, Entity.Id, Entity.Id, Long) => Buff] = Map(
    Buff.rageFiller -> RageFiller.apply,
    Buff.squareDefaultShield -> BasicShield.apply,
    Buff.healingThreatAware -> HealingThreatAware.apply,
    Buff.damageThreatAware -> DamageThreatAware.apply,
    Buff.energyFiller -> { (buffId, entityId, sourceId, appearanceTime) =>
      EnergyFiller(buffId, entityId, appearanceTime, appearanceTime)
    },
    Buff.triangleUpgradeDirectHit -> UpgradeDirectHit.apply,
    Buff.manaFiller -> { (buffId, entityId, sourceId, appearanceTime) =>
      ManaFiller(buffId, entityId, appearanceTime, appearanceTime)
    },
    Buff.triangleStun -> TriangleStunDebuff.apply,
    Buff.boss110BrokenArmor -> BrokenArmor.apply
  )

  def apply(
      identifier: Buff.ResourceIdentifier,
      buffId: Buff.Id,
      bearerId: Entity.Id,
      sourceId: Entity.Id,
      appearanceTime: Long
  ): Option[Buff] =
    simpleBuffs.get(identifier).map(_(buffId, bearerId, sourceId, appearanceTime))

}
