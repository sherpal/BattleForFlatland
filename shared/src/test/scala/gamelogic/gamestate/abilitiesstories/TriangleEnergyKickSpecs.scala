package gamelogic.gamestate.abilitiesstories

import gamelogic.abilities.triangle.EnergyKick
import gamelogic.gamestate.gameactions.EntityResourceChanges
import gamelogic.entities.Resource.Energy
import testutils.ActionComposer
import gamelogic.gamestate.GameState
import gamelogic.entities.classes.Triangle

final class TriangleEnergyKickSpecs extends StoryTeller {

  test("Energy should not go above maximum with Energy Kick") {
    val player = addPlayer(2)
    val hound  = addHound(2)

    val ability       = EnergyKick(idGenerator.abilityUseIdGenerator(), 3, player.id, hound.id)
    val useEnergyKick = useAbilityFromAbility(ability)

    val composer = ActionComposer.empty >>
      start >>
      player >> hound >>>
      (gs => ability.createActions(gs)) >> useEnergyKick >>>> { (gs: GameState) =>
      for {
        p           <- gs.players.get(player.id)
        pAsTriangle <- Some(p).collect { case p: Triangle => p }
        energyAmount = pAsTriangle.resourceAmount.amount
      } yield assertEqualsDouble(energyAmount, Triangle.initialResourceAmount.amount, 1e-2)
    }
  }

  test("Energy should move up and target should take damage with Energy Kick") {
    val player = addPlayer(2)
    val hound  = addHound(2)

    val someEnergyLoss = EntityResourceChanges(idGenerator.gameActionIdGenerator(), 3, player.id, -20, Energy)

    val ability       = EnergyKick(idGenerator.abilityUseIdGenerator(), 3, player.id, hound.id)
    val useEnergyKick = useAbilityFromAbility(ability)

    val composer = ActionComposer.empty >>
      start >>
      player >> hound >>
      someEnergyLoss >>>> { (gs: GameState) =>
      for {
        p           <- gs.players.get(player.id)
        pAsTriangle <- Some(p).collect { case p: Triangle => p }
        energyAmount = pAsTriangle.resourceAmount.amount
      } yield assertEqualsDouble(energyAmount, Triangle.initialResourceAmount.amount + someEnergyLoss.amount, 1e-2)
    } >>> (gs => ability.createActions(gs)) >> useEnergyKick >>>> { (gs: GameState) =>
      (for {
        p           <- gs.players.get(player.id)
        pAsTriangle <- Some(p).collect { case p: Triangle => p }
        energyAmount = pAsTriangle.resourceAmount.amount
      } yield energyAmount) match {
        case Some(energyAmount) =>
          assertEqualsDouble(
            energyAmount,
            Triangle.initialResourceAmount.amount + someEnergyLoss.amount + EnergyKick.energyGain,
            1e-2
          )
        case None => fail("Energy value of player should exist!")
      }

      (for {
        h <- gs.livingEntityAndMovingBodyById(hound.id)
        life    = h.life
        maxLife = h.maxLife
      } yield (life, maxLife)) match {
        case Some((life, maxLife)) =>
          assertEqualsDouble(life, maxLife - EnergyKick.damage, 1e-2)
        case None =>
          fail("Life value of the hound should exist.")
      }
    }
  }

}
