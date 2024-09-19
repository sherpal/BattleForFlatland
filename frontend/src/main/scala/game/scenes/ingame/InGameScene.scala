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
import models.bff.ingame.Controls
import models.bff.ingame.Controls.InputCode
import models.bff.ingame.Controls.KeyCode
import models.bff.ingame.Controls.ModifiedKeyCode
import models.bff.ingame.Controls.MouseCode
import models.bff.ingame.Controls.KeyInputModifier.WithShift
import assets.Asset
import indigo.shared.events.MouseEvent.Click
import models.bff.ingame.InGameWSProtocol
import game.handlers.CastAbilitiesHandler

/** Next steps:
  *
  *   - [x] use the camera to center drawing on the player
  *   - [x] use controls defined by user (UI is missing!)
  *   - [x] button to launch the game
  *   - [x] handle when player is dead
  *   - [x] send game actions other than moving (using abilities, mostly)
  *   - [ ] draw the UI (player frame, all player frames, target frames, boss frame, damage threat)
  *   - [ ] draw the effects
  *   - [ ] scale camera to best fit
  */
class InGameScene(
    myId: Entity.Id,
    sendGameAction: GameAction => Unit,
    backendCommWrapper: BackendCommWrapper,
    deltaWithServer: Seconds,
    controls: Controls
) extends Scene[InGameScene.StartupData, IndigoModel, IndigoViewModel] {
  type SceneModel     = InGameScene.InGameModel
  type SceneViewModel = IndigoViewModel

  val castAbilitiesHandler = CastAbilitiesHandler(myId, controls, deltaWithServer.toMillis.toLong)

  override def subSystems: Set[SubSystem[IndigoModel]] = Set.empty

  override def present(
      context: SceneContext[StartupData],
      model: SceneModel,
      viewModel: SceneViewModel
  ): Outcome[SceneUpdateFragment] = {

    val gameState        = viewModel.gameState
    val gameBoundsCenter = context.localToGame(context.startUpData.bounds.center)

    def text(message: String, point: Point) = TextBox(message, 400, 30)
      .withFontFamily(FontFamily.cursive)
      .withColor(RGBA.White)
      .withFontSize(Pixels(16))
      .withStroke(TextStroke(RGBA.Red, Pixels(1)))
      .withPosition(point)

    Outcome(
      SceneUpdateFragment(
        Layer(
          Shape
            .Box(context.startUpData.bounds, Fill.Color(RGBA.Black), Stroke(4, RGBA.Blue))
            .withDepth(Depth.far)
        ),
        Layer(
          Batch.fromJSArray(
            gameState.bosses.values.headOption.toJSArray.map(boss =>
              Shape
                .Circle(
                  context.gameToLocal(boss.currentPosition(System.currentTimeMillis())),
                  boss.shape.radius.toInt,
                  Fill.Color(RGBA.White)
                )
                .withDepth(Depth(4))
            ) ++
              js.Array(
                Shape
                  .Circle(context.gameToLocal(0), 2, Fill.Color(RGBA.White))
                  .withDepth(Depth.far)
              ) ++
              gameState.players.values.toJSArray.map { player =>
                val asset = Asset.playerClassAssetMap(player.cls)
                Graphic(
                  Rectangle(Size(50)),
                  2,
                  Material
                    .ImageEffects(asset.assetName)
                    .withTint(
                      RGBA.fromColorInts(player.rgb._1, player.rgb._2, player.rgb._3)
                    )
                ).withPosition(
                  context.gameToLocal(
                    player.pos - player.shape.radius * math.sqrt(2) * Complex.rotation(
                      player.rotation - math.Pi / 4
                    )
                  )
                ).withRotation(Radians(-player.rotation))
                  .withScale(Vector2(player.shape.radius / 25))
              } ++
              viewModel.maybeLaunchGameButton.getOrElse(js.Array())
          )
        ).withCamera(Camera.LookAt(context.gameToLocal(viewModel.currentCameraPosition))),
        Layer(
          text("We are in game, woot!", Point(10, 100)),
          text(
            s"Down keys are ${context.keyboard.keysDown.map(_.code).distinct.mkString(", ")}",
            Point(10, 130)
          ),
          text(
            s"Mouse position: ${viewModel.localMousePos} --- ${viewModel.worldMousePosition.toIntComplex}",
            Point(10, 160)
          ),
          text(
            s"Player pos: ${gameState.players.get(myId).map(_.pos.toIntComplex)}",
            Point(10, 190)
          ),
          text(
            s"Game bounds center: ${context.startUpData.bounds.center} --- $gameBoundsCenter",
            Point(10, 220)
          ),
          text(
            gameState.bosses.values.toVector
              .map(boss => s"${boss.name}: ${boss.life.toInt}/${boss.maxLife.toInt}")
              .mkString,
            Point(10, context.startUpData.bounds.height - 60)
          ),
          text(
            gameState.players.values.toVector
              .map(player => s"${player.name}: ${player.life.toInt}/${player.maxLife.toInt}")
              .mkString(" | "),
            Point(10, context.startUpData.bounds.height - 30)
          )
        )
      )
    )
  }

  override def updateModel(
      context: SceneContext[StartupData],
      model: SceneModel
  ): GlobalEvent => Outcome[SceneModel] = {
    case FrameTick =>
      val gameMousePos                 = context.localToGame(context.mouse.position)
      val (newModel, triggeredActions) = backendCommWrapper.transform(model)
      val nowGameTime                  = context.gameTime.running
      newModel.projectedGameState.players.get(myId) match {
        case Some(player) =>
          val playerX: Int =
            (if controls.rightKey.isActionDown(context) then 1 else 0) +
              (if controls.leftKey.isActionDown(context) then -1 else 0)

          val playerY: Int =
            (if controls.upKey.isActionDown(context) then 1 else 0) +
              (if controls.downKey.isActionDown(context) then -1 else 0)

          val maybePlayerDirection =
            if playerX == 0 && playerY == 0 then None else Some(Math.atan2(playerY, playerX))

          val rotation = gameMousePos.arg

          val playerMoveAction = MovingBodyMoves(
            GameAction.Id.zero,
            System.currentTimeMillis() + deltaWithServer.toMillis.toLong,
            myId,
            maybePlayerDirection.fold(player.pos)(dir =>
              player.pos + player.speed * context.gameTime.delta.toDouble * Complex
                .rotation(dir)
            ),
            maybePlayerDirection.getOrElse(0),
            rotation,
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

    case send: CustomIndigoEvents.GameEvent.SendAction =>
      sendGameAction(send.action)
      Outcome(model)

    case CustomIndigoEvents.GameEvent.NewAction(action) if !action.isInstanceOf[UpdateTimestamp] =>
      // this is where we trigger animations effects
      println(action)
      Outcome(model)
    case _ => Outcome(model)
  }

  override def eventFilters: EventFilters = EventFilters.AllowAll

  override def viewModelLens: Lens[IndigoViewModel, SceneViewModel] = Lens(identity, (_, a) => a)

  override def name: SceneName = InGameScene.name

  override def modelLens: Lens[IndigoModel, SceneModel] =
    Lens(_.inGameState, _.withInGameState(_))

  override def updateViewModel(
      context: SceneContext[StartupData],
      model: SceneModel,
      viewModel: SceneViewModel
  ): GlobalEvent => Outcome[SceneViewModel] = {
    case FrameTick =>
      Outcome(
        viewModel
          .withUpToDateGameState(model.projectedGameState)
          .newCameraPosition(myId, context.gameTime.delta)
      )

    case mouse: MouseEvent.Move =>
      Outcome(viewModel.withMousePos(mouse.position))

    case kbd: KeyboardEvent =>
      Outcome(viewModel).addGlobalEvents(
        Batch(
          castAbilitiesHandler.handleKeyboardEvent(
            kbd,
            context,
            model,
            viewModel,
            System.currentTimeMillis()
          )
        )
      )

    case click: Click
        if viewModel
          .doesMouseClickLaunchButton(click.position) =>
      println("We should launch the game, woot!")

      Outcome(viewModel)
        .addGlobalEvents(CustomIndigoEvents.GameEvent.SendStartGame())

    case click: Click =>
      castAbilitiesHandler.handleClickEvent(
        click,
        viewModel.targetFromMouseClick(click),
        System.currentTimeMillis()
      )

    case CustomIndigoEvents.GameEvent.StartChoosingAbility(abilityId) =>
      Outcome(viewModel.withChoosingAbilityPosition(abilityId))

    case CustomIndigoEvents.GameEvent.SendStartGame() =>
      backendCommWrapper.sendMessageToBackend(InGameWSProtocol.LetsBegin)
      Outcome(viewModel)
    case _ => Outcome(viewModel)
  }

  extension (input: InputCode) {
    inline def isActionDown(context: SceneContext[StartupData]): Boolean = input match
      case kc: KeyCode => context.keyboard.keysDown.exists(_.code == kc.keyCode)
      case mkc: ModifiedKeyCode =>
        val isModifierDown = mkc.modifier match
          case WithShift => context.keyboard.keysDown.contains(Key.SHIFT)

        isModifierDown && context.keyboard.keysDown.exists(_.code == mkc.keyCode)
      case MouseCode(code) =>
        MouseButton.fromOrdinalOpt(code).exists(context.mouse.buttonsDown.contains)
  }

  extension (context: SceneContext[StartupData]) {
    inline def gameToLocal(z: Complex): Point =
      game.gameutils.gameToLocal(z)(context.startUpData.bounds)

    inline def localToGame(p: Point): Complex =
      game.gameutils.localToGame(p)(context.startUpData.bounds)
  }

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

    lazy val projectedGameState: GameState =
      actionGatherer.currentGameState.applyActions(unconfirmedActions)
  }

  def initialModel(initialGameState: GameState): InGameModel = InGameModel(
    GreedyActionGatherer(initialGameState),
    Vector.empty
  )

  val name = SceneName("in game")
}
