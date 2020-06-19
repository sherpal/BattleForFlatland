package game.ui.gui.components

import gamelogic.entities.Entity
import gamelogic.entities.classes.PlayerClass
import gamelogic.gamestate.GameState
import typings.pixiJs.PIXI.Texture
import typings.pixiJs.anon.Align
import typings.pixiJs.mod.{Text, TextStyle}
import utils.misc.RGBColour

import scala.Ordering.Double.TotalOrdering

final class BossThreatMeter(bossId: Entity.Id, barTexture: Texture) extends GUIComponent {

  val barWidth: Double  = 200
  val barHeight: Double = 15

  /**
    * Class representing the information to display for each threat bar.
    * Since the information for displaying the bars is global (each bar has its length as percentage of the
    * threat of the current target), we gather all information in one place and we display bars using it.

    * @param threatPercentage between 0 and 1
    */
  private case class PlayerThreatInfo(playerId: Entity.Id, bar: StatusBar, threatPercentage: Double, text: Text)

  private var bars: List[PlayerThreatInfo] = Nil

  private def setBarPercentages(gameState: GameState): Unit =
    gameState.bosses.get(bossId) match {
      case Some(boss) =>
        container.visible = true
        val allThreats = boss.damageThreats
        allThreats.values.maxOption.foreach { maxThreatAmount =>
          val barsBefore = bars.length
          allThreats.keys.filterNot(bars.map(_.playerId).contains).flatMap(gameState.players.get).foreach(addBar)
          val barsAfter = bars.length

          if (barsAfter != barsBefore) {
            container.y = container.y - barHeight * (barsAfter - barsBefore)
          }

          bars = bars
            .map {
              case PlayerThreatInfo(playerId, bar, _, text) =>
                val threat = allThreats.getOrElse(playerId, 0.0)
                text.text = threat.toString
                PlayerThreatInfo(playerId, bar, threat / maxThreatAmount, text)
            }
            .sortBy(_.threatPercentage)

          for {
            (barInfo, idx) <- bars.zipWithIndex
            y = idx * barHeight
          } {
            barInfo.bar.container.y = y
          }
        }

      case None =>
        container.visible = false
    }

  /**
    * Add a bar to the bars map for tracking threat of the given player.
    *
    * You should only call this *once* per player.
    */
  private def addBar(entity: PlayerClass): Unit = {
    val rgb = RGBColour.fromIntColour(entity.colour)
    val bar = new StatusBar(
      { (_, _) =>
        bars.find(_.playerId == entity.id).map(_.threatPercentage).getOrElse(0.0)
      }, { (_, _) =>
        rgb
      }, { (gameState, _) =>
        gameState.entityById(entity.id).isDefined
      },
      barTexture
    )

    bar.setSize(barWidth, barHeight)

    val text = new Text(
      "",
      new TextStyle(
        Align(
          fontSize = 13.0
        )
      )
    )

    bar.container.addChild(text)
    container.addChild(bar.container)

    bars = PlayerThreatInfo(entity.id, bar, 0.0, text) +: bars
  }

  def update(gameState: GameState, currentTime: Long): Unit = {
    setBarPercentages(gameState)
    bars.foreach(_.bar.update(gameState, currentTime))
  }

}
