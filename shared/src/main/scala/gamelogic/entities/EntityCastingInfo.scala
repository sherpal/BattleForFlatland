package gamelogic.entities

import gamelogic.abilities.Ability
import gamelogic.physics.Complex

/**
  * An [[EntityCastingInfo]] gathers everything there is to know about an entity currently casting a spell.
  * Namely, its id, the ability currently casting, the time it started casting it, the position it was in when it
  * started to cast the ability.
  *
  * This will be used for at least (off the top of my head) two things:
  * - allowing the server to know whether the caster moved, hence breaking the casting
  * - allowing the server to know whether the spell finished casting
  * - allowing the UI to display the entity casting its stuff.
  */
final case class EntityCastingInfo(
    casterId: Entity.Id,
    positionWhenStarted: Complex,
    startedTime: Long,
    castingTime: Long, // casting time can be changed at particular time the ability is cast
    ability: Ability
)
