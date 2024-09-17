package game.scenes.ingame

import indigo.*
import indigo.scenes.*
import game.IndigoModel
import game.IndigoViewModel
import game.scenes.ingame.InGameScene.StartupData
import gamelogic.gamestate.GameState
import gamelogic.gamestate.ActionGatherer
import scala.scalajs.js.JSConverters.*
import gamelogic.physics.Complex
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.*
import gamelogic.gamestate.GameAction
import game.BackendCommWrapper
import gamelogic.gamestate.GreedyActionGatherer

import scala.scalajs.js
import game.events.CustomIndigoEvents

/** Next steps:
  *
  *   - use the camera to center drawing on the player
  *   - use controls defined by user (UI is missing!)
  *   - button to launch the game
  *   - handle when player is dead
  *   - send game actions other than moving (using abilities, mostly)
  *   - draw the UI (player frame, all player frames, target frames, boss frame, damage threat)
  *   - draw the effects
  */
class InGameScene(
    myId: Entity.Id,
    sendGameAction: GameAction => Unit,
    backendCommWrapper: BackendCommWrapper,
    deltaWithServer: Seconds
) extends Scene[InGameScene.StartupData, IndigoModel, IndigoViewModel] {
  type SceneModel     = InGameScene.InGameModel
  type SceneViewModel = Unit

  override def subSystems: Set[SubSystem[IndigoModel]] = Set.empty

  override def present(
      context: SceneContext[StartupData],
      model: SceneModel,
      viewModel: SceneViewModel
  ): Outcome[SceneUpdateFragment] = {
    val bounds = context.startUpData.bounds
    def gameToLocal(z: Complex): Point = {
      val x = z.re + bounds.center.x
      val y = bounds.center.y - z.im
      Point(x.toInt, y.toInt)
    }

    def localToGame(p: Point): Complex = {
      val x = p.x - bounds.center.x
      val y = bounds.center.y - p.y
      Complex(x, y)
    }

    val gameState = model.actionGatherer.currentGameState.applyActions(model.unconfirmedActions)

    Outcome(
      SceneUpdateFragment(
        Batch.fromJSArray(
          Shape
            .Box(
              bounds,
              Fill.Color(RGBA.Black),
              Stroke(4, RGBA.Blue)
            )
            .withDepth(Depth.far) +:
            TextBox("We are in the game, woot!", 400, 30)
              .withFontFamily(FontFamily.cursive)
              .withColor(RGBA.White)
              .withFontSize(Pixels(16))
              .withStroke(TextStroke(RGBA.Red, Pixels(1)))
              .withPosition(Point(10, 100)) +:
            gameState.players.values.toJSArray.map { player =>
              Shape.Circle(
                center = gameToLocal(player.pos),
                radius = player.shape.radius.toInt,
                fill = Fill.Color(RGBA.fromColorInts(player.rgb._1, player.rgb._2, player.rgb._3)),
                stroke = Stroke.apply(2, RGBA.White)
              )
            }
        )
      )
    )
  }

  override def updateModel(
      context: SceneContext[StartupData],
      model: SceneModel
  ): GlobalEvent => Outcome[SceneModel] = {
    case FrameTick =>
      val (newModel, triggeredActions) = backendCommWrapper.transform(model)
      val nowGameTime                  = context.gameTime.running
      newModel.actionGatherer.currentGameState.players.get(myId) match {
        case Some(player) =>
          inline def keyIsDown(key: Key): Boolean = context.keyboard.keysDown.contains[Key](key)

          val playerX: Int =
            (if keyIsDown(Key.RIGHT_ARROW) then 1 else 0) +
              (if keyIsDown(Key.LEFT_ARROW) then -1 else 0)

          val playerY: Int =
            (if keyIsDown(Key.UP_ARROW) then 1 else 0) +
              (if keyIsDown(Key.DOWN_ARROW) then -1 else 0)

          val maybePlayerDirection =
            if playerX == 0 && playerY == 0 then None else Some(Math.atan2(playerY, playerX))

          val playerMoveAction = MovingBodyMoves(
            GameAction.Id.zero,
            System.currentTimeMillis() + deltaWithServer.toMillis.toLong,
            myId,
            maybePlayerDirection.fold(player.pos)(dir =>
              player.pos + player.speed * context.gameTime.delta.toDouble * Complex
                .rotation(dir)
            ),
            maybePlayerDirection.getOrElse(0),
            0,
            player.speed,
            maybePlayerDirection.isDefined
          )
          if playerMoveAction.moving || playerMoveAction.moving != player.moving then
            sendGameAction(playerMoveAction)
          Outcome(
            newModel.withActionGatherer(
              newModel.actionGatherer,
              newModel.unconfirmedActions :+ playerMoveAction
            )
          ).addGlobalEvents(triggeredActions)
        case None =>
          Outcome(newModel).addGlobalEvents(triggeredActions)
      }
    case CustomIndigoEvents.GameEvent.NewAction(action) if !action.isInstanceOf[UpdateTimestamp] =>
      // this is where we trigger animations effects
      println(action)
      Outcome(model)
    case _ => Outcome(model)
  }

  override def eventFilters: EventFilters = EventFilters.AllowAll

  override def viewModelLens: Lens[IndigoViewModel, SceneViewModel] = Lens(_ => (), (a, _) => a)

  override def name: SceneName = InGameScene.name

  override def modelLens: Lens[IndigoModel, SceneModel] =
    Lens(_.inGameState, _.withInGameState(_))

  override def updateViewModel(
      context: SceneContext[StartupData],
      model: SceneModel,
      viewModel: SceneViewModel
  ): GlobalEvent => Outcome[SceneViewModel] = _ => Outcome(viewModel)

}

object InGameScene {
  case class StartupData(bounds: Rectangle)

  class InGameModel(
      val actionGatherer: ActionGatherer,
      val unconfirmedActions: Vector[GameAction]
  ) extends js.Object {
    inline def withActionGatherer(
        newActionGatherer: ActionGatherer,
        newUnconfirmedActions: Vector[GameAction]
    ): InGameModel =
      InGameModel(newActionGatherer, newUnconfirmedActions)
  }

  def initialModel(initialGameState: GameState): InGameModel = InGameModel(
    GreedyActionGatherer(initialGameState),
    Vector.empty
  )

  val name = SceneName("in game")
}
