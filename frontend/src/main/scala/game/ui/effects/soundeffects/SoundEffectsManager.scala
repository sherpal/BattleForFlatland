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

/**
  * This class is responsible for emitting sounds related to the [[GameAction]] occuring
  * during the game.
  * Abilities are usually only applied for the caster and if they are the target of such spells.
  *
  * Bosses will likely say some "threatening" lines during the game.
  *
  * @param playerId id of the player for this client
  * @param actionsAndStatesEvents all actions occuring during the game, with the state at that point.
  * @param soundsResources function giving [[Audio]] instances from the [[SoundAsset]].
  */
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
          case _: EndGame =>
            val assetToPlay = if (state.playersWon) SoundAsset.bossDefeated else SoundAsset.gameOverSoundAsset
            liftedResources(assetToPlay)
          case _ =>
            Option.empty[Audio]
        }
    }
    .collect {
      case Some(audio) => audio
    }
    .foreach(_.play())

}
