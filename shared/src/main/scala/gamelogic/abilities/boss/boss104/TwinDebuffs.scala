package gamelogic.abilities.boss.boss104

import gamelogic.abilities.Ability
import gamelogic.entities.Entity
import gamelogic.utils.IdGeneratorContainer
import gamelogic.entities.Resource.ResourceAmount
import gamelogic.abilities.Ability.AbilityId
import gamelogic.abilities.Ability.UseId
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.GameState
import gamelogic.entities.Resource
import utils.misc.RGBAColour
import gamelogic.physics.Complex
import utils.misc.RGBColour
import scala.util.Random
import gamelogic.gamestate.gameactions.boss104.PutTwinDebuff
import gamelogic.entities.classes.PlayerClass
import gamelogic.entities.boss.dawnoftime.Boss104

/** Curses two random players with a [[gamelogic.buffs.boss.boss104.TwinDebuff]] of two different
  * colours, and drops a [[gamelogic.entities.boss.boss104.DebuffCircle]] of each colour somewhere
  * in the room. A Pentagon must dispel each cursed player from inside the circle of the matching
  * colour, or take a big chunk of damage.
  */
final case class TwinDebuffs(
    useId: Ability.UseId,
    time: Long,
    casterId: Entity.Id
) extends Ability {

  override def castingTime: Long = TwinDebuffs.castingTime

  override def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability =
    copy(time = newTime, useId = newId)

  override def abilityId: AbilityId = Ability.boss104TwinDebuffs

  /** Boss abilities delegate legality to their controller (which checks cooldown, silence, etc. via
    * `canUseAbilityBoolean`), so there is nothing else to veto here.
    */
  override def canBeCast(gameState: GameState, time: Long): None.type = None

  /** This method is called exactly once, by the game master, when the cast completes; its resulting
    * actions are then broadcast as-is to every client and AI. It is therefore the *only* place
    * where randomness is allowed in the whole game logic.
    *
    * Every decision taken here (which players, which colours, where the circles land, which ids) is
    * frozen into the [[PutTwinDebuff]] actions, because their state transformers have to be pure
    * and replayable on all clients.
    */
  override def createActions(
      gameState: GameState
  )(using IdGeneratorContainer): Vector[GameAction] = {
    val colours = Random.shuffle(TwinDebuffs.possibleColours).take(2)
    val colour1 = colours(0)
    val colour2 = colours(1)

    val chosenPlayers = Random.shuffle(gameState.players.values.toVector).take(2)
    def makeTwinDebuff(player: PlayerClass, colour: RGBColour): PutTwinDebuff = {
      val size     = Boss104.size * 0.8
      val position = Complex(Random.between(-size, size), Random.between(-size, size))

      PutTwinDebuff(
        genActionId(),
        time,
        genBuffId(),
        player.id,
        casterId,
        colour,
        genEntityId(),
        position
      )
    }
    Vector(
      chosenPlayers.headOption.map(makeTwinDebuff(_, colour1)),
      chosenPlayers.lastOption.map(makeTwinDebuff(_, colour2))
    ).flatten
  }

  override def cost: ResourceAmount = ResourceAmount(0, Resource.NoResource)

  override def cooldown: Long = TwinDebuffs.cooldown

}

object TwinDebuffs {

  inline def cooldown: Long = 20000

  inline def castingTime: Long = 1000

  inline def timeToFirstUse: Long = 10000

  val possibleColours = Vector(
    RGBColour.blue,
    RGBColour.green,
    RGBColour.orange,
    RGBColour.fuchsia
  )

}
