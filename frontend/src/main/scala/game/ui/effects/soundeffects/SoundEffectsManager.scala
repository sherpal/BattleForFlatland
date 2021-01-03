package game.ui.effects.soundeffects

import assets.sounds.SoundAsset
import typings.std.global.Audio
import gamelogic.entities.Entity
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.ownership.Owner
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.GameState
import gamelogic.gamestate.gameactions.EndGame
import gamelogic.gamestate.gameactions.UseAbility
import gamelogic.abilities.triangle.DirectHit

final class SoundEffectsManager(
    playerId: Entity.Id,
    actionsAndStatesEvents: EventStream[(GameAction, GameState)],
    soundsResources: PartialFunction[SoundAsset[_], Audio]
)(implicit owner: Owner) {

  val liftedResources = soundsResources.lift

  actionsAndStatesEvents
    .map {
      case (action, state) =>
        action match {
          case useAbility: UseAbility if useAbility.casterId == playerId =>
            for {
              soundAsset <- SoundAsset.abilitySounds.get(useAbility.ability.abilityId)
              audio      <- liftedResources(soundAsset)
            } yield audio
          case _: UseAbility =>
            // no need to go further the match so we artificially stop here.
            Option.empty
          case _: EndGame if !state.playersWon =>
            liftedResources(SoundAsset.gameOverSoundAsset)
          case _ =>
            Option.empty[Audio]
        }
    }
    .collect {
      case Some(audio) => audio
    }
    .foreach(_.play())

}
