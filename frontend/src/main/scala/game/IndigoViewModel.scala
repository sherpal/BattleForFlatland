package game

import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import gamelogic.entities.Entity
import gamelogic.entities.boss.Boss101
import indigo.*

case class IndigoViewModel(
    gameState: GameState,
    currentCameraPosition: Complex
) {

  def withUpToDateGameState(newGameState: GameState): IndigoViewModel = copy(
    gameState = newGameState
  )

  def newCameraPosition(myId: Entity.Id, deltaTime: Seconds): IndigoViewModel = copy(
    currentCameraPosition = gameState.players
      .get(myId)
      .map(_.pos)
      .orElse {
        gameState.bosses.headOption.map(_._2).map { boss =>
          val targetCameraPosition = boss.pos
          val distance             = targetCameraPosition.distanceTo(currentCameraPosition)
          val cameraMovementSize   = cameraSpeed * 0.5 * deltaTime.toDouble
          if distance < cameraMovementSize then targetCameraPosition
          else
            currentCameraPosition + (targetCameraPosition - currentCameraPosition).safeNormalized * cameraMovementSize
        }
      }
      .getOrElse(Complex.zero)
  )

  private val cameraSpeed: Double = 300.0

}
