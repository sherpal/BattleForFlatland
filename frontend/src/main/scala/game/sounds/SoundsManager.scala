package game.sounds

import indigo.*

import scala.scalajs.js
import game.scenes.ingame.InGameScene.StartupData
import game.IndigoViewModel
import gamelogic.entities.Entity
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.gameactions.*
import game.events.CustomIndigoEvents
import gamelogic.abilities.Ability
import assets.sounds.SoundAsset

class SoundsManager(myId: Entity.Id) {

  def handle(
      context: FrameContext[StartupData],
      viewModel: IndigoViewModel,
      event: GlobalEvent
  ): js.Array[PlaySound] = event match {
    case CustomIndigoEvents.GameEvent.NewAction(action) =>
      handleGameAction(context, viewModel, action)
    case _ => js.Array()
  }

  private def handleGameAction(
      context: FrameContext[StartupData],
      viewModel: IndigoViewModel,
      action: GameAction
  ): js.Array[PlaySound] = action match {
    case UseAbility(_, _, caster, _, ability) if caster == myId =>
      (ability.abilityId match {
        case Ability.pentagonPentagonBullet =>
          js.Array(SoundAsset.sounds.pentagonBulletSound)
        case Ability.createPentagonZoneId =>
          js.Array(SoundAsset.sounds.pentagonZoneSound)
        case Ability.triangleEnergyKick =>
          js.Array(SoundAsset.sounds.triangleSmallHitSound)
        case Ability.triangleDirectHit =>
          js.Array(SoundAsset.sounds.triangleBigHitSound)
        case Ability.hexagonHexagonHotId =>
          js.Array(SoundAsset.sounds.hexagonHealHotSound)

        case _ => js.Array()
      }).map(_.play(volume))
    case _ => js.Array()
  }

  inline private def volume = Volume(0.2)

}
