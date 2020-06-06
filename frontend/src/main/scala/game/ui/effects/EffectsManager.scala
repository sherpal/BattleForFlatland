package game.ui.effects

import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.ownership.Owner
import game.Camera
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.{EntityGetsHealed, EntityTakesDamage}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import typings.pixiJs.mod.{Application, Container}
import utils.misc.RGBColour

import scala.collection.mutable

final class EffectsManager(
    playerId: Entity.Id,
    $actionsAndStates: EventStream[(GameAction, GameState)],
    camera: Camera,
    application: Application
)(implicit owner: Owner) {

  private val container: Container = new Container
  application.stage.addChild(container)

  private val simpleTextEffects: mutable.Set[SimpleTextEffect] = mutable.Set.empty

  $actionsAndStates.foreach {
    case (action, gameState) =>
      gameState
        .applyActionChangers(action)
        .flatMap {
          case EntityTakesDamage(_, time, entityId, amount, sourceId) if entityId == playerId && sourceId != playerId =>
            Some(
              new SimpleTextEffect(
                amount.toString,
                RGBColour.red,
                time,
                Path.goDown(2000, 40) + gameState.players.get(playerId).fold(Complex.zero)(_.pos),
                camera
              )
            )
          case EntityTakesDamage(_, time, entityId, amount, sourceId) if sourceId == playerId =>
            Some(
              new SimpleTextEffect(
                amount.toString,
                RGBColour.white,
                time,
                Path.goUp(2000, 40) + gameState
                  .movingBodyEntityById(entityId)
                  .fold(Complex.zero)(entity => entity.pos + entity.shape.radius * Complex.i),
                camera
              )
            )
          case EntityGetsHealed(_, time, entityId, amount, sourceId) if sourceId == playerId =>
            Some(
              new SimpleTextEffect(
                amount.toString,
                RGBColour.green,
                time,
                Path.goUp(2000, 40) + gameState.movingBodyEntityById(entityId).fold(Complex.zero)(_.pos),
                camera
              )
            )
          case _ =>
            Option.empty[SimpleTextEffect]
        }
        .foreach { newTextEffect =>
          println("new effect!")
          container.addChild(newTextEffect.pixiText)
          simpleTextEffects += newTextEffect
        }

  }

  def update(currentTime: Long): Unit =
    simpleTextEffects.foreach { textEffect =>
      if (textEffect.isOver(currentTime)) {
        simpleTextEffects -= textEffect
        textEffect.destroy()
      } else {
        textEffect.update(currentTime)
      }
    }

}
