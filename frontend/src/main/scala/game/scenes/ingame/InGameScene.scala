package game.scenes.ingame

import indigo.*
import indigo.scenes.*
import game.IndigoModel
import game.IndigoViewModel
import game.scenes.ingame.InGameScene.StartupData
import scala.scalajs.js.JSConverters.*
import gamelogic.physics.Complex
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.*
import gamelogic.gamestate.*
import game.BackendCommWrapper

import scala.scalajs.js
import game.events.CustomIndigoEvents
import models.bff.ingame.Controls
import models.bff.ingame.Controls.*
import models.bff.ingame.Controls.KeyInputModifier.WithShift
import assets.Asset
import indigo.shared.events.MouseEvent.Click
import models.bff.ingame.InGameWSProtocol
import game.drawers.PentagonBulletsDrawer
import gamelogic.abilities.pentagon.CreatePentagonZone
import gamelogic.abilities.Ability
import assets.fonts.Fonts
import gamelogic.gamestate.gameactions.markers.UpdateMarker
import game.drawers.*
import game.handlers.*
import game.drawers.bossspecificdrawers.Boss102Drawer
import game.sounds.SoundsManager

class InGameScene(
    myId: Entity.Id,
    sendGameAction: GameAction => Unit,
    backendCommWrapper: BackendCommWrapper,
    deltaWithServer: Seconds,
    controls: Controls
) extends Scene[InGameScene.StartupData, IndigoModel, IndigoViewModel]
    with KeyboardHandler {
  type SceneModel     = InGameScene.InGameModel
  type SceneViewModel = IndigoViewModel

  val castAbilitiesHandler = CastAbilitiesHandler(myId, deltaWithServer.toMillis.toLong)
  val gameMarkersHandler   = MarkersHandler()
  val nextTargetHandler    = NextTargetHandler(myId)
  val soundsManager        = SoundsManager(myId)

  inline def serverTime: Long = System.currentTimeMillis() + deltaWithServer.toMillis.toLong

  override def subSystems: Set[SubSystem[IndigoModel]] = Set.empty

  override def present(
      context: SceneContext[StartupData],
      model: SceneModel,
      viewModel: SceneViewModel
  ): Outcome[SceneUpdateFragment] = {

    val gameState = viewModel.gameState
    val now       = serverTime

    def text(message: String, point: Point) = TextBox(message, 400, 30)
      .withFontFamily(FontFamily.cursive)
      .withColor(RGBA.White)
      .withFontSize(Pixels(16))
      .withStroke(TextStroke(RGBA.Red, Pixels(1)))
      .withPosition(point)

    val localCameraPos       = context.gameToLocal(viewModel.currentCameraPosition)
    val playerCenteredCamera = Camera.LookAt(localCameraPos)

    val bossDrawers = gameState.bosses.values.headOption.toJSArray
      .map(game.drawers.bossspecificdrawers.drawerMapping)

    Outcome(
      SceneUpdateFragment(
        Batch(
          js.Array(
            Layer(
              Shape
                .Box(
                  context.startUpData.bounds,
                  Fill.Color(RGBA.fromColorInts(200, 200, 200)),
                  Stroke(1, RGBA.Blue)
                )
                .withDepth(Depth.far)
                .withPosition(localCameraPos - context.startUpData.bounds.size.toPoint / 2),
              aztecDiamondBackground.withPosition(context.startUpData.bounds.center)
            ).withCamera(playerCenteredCamera)
          ) ++ bossDrawers.map(
            _.cloneLayer(gameState, gameState.time, context.gameToLocal).withCamera(
              playerCenteredCamera
            )
          ) ++
            js.Array(
              Layer(
                Batch.fromJSArray(
                  ObstacleDrawer.drawAll(gameState, gameState.time, context.gameToLocal) ++
                    BossDrawer.drawAll(gameState, gameState.time, context.gameToLocal) ++
                    js.Array(
                      Shape
                        .Circle(context.gameToLocal(0), 2, Fill.Color(RGBA.White))
                        .withDepth(Depth.far)
                    ) ++ PentagonBulletsDrawer.drawAll(
                      gameState,
                      viewModel.maybeChoosingAbilityPosition.collect {
                        case Ability.createPentagonZoneId =>
                          viewModel.localMousePosToWorld(context.mouse.position)
                      },
                      myId,
                      now,
                      context.gameToLocal
                    ) ++ PlayerDrawer.drawAll(
                      gameState,
                      gameState.time,
                      context.gameToLocal
                    ) ++ bossDrawers.flatMap(_.drawAll(gameState, now, context.gameToLocal)) ++
                    viewModel.maybeLaunchGameButton.getOrElse(js.Array())
                )
              ).withCamera(playerCenteredCamera),
              Layer(
                Batch(
                  viewModel.effectsManager
                    .present(context.frameContext, viewModel) ++ GameMarkersDrawer
                    .drawAll(viewModel.gameState, serverTime, context.gameToLocal)
                )
              )
                .withCamera(playerCenteredCamera),
              Layer(Batch(viewModel.uiParent.presentAll(context.frameContext, viewModel)))
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
      val gameState                    = newModel.projectedGameState
      gameState.players.get(myId) match {
        case Some(player) =>
          val playerX: Int =
            (if controls.rightKey.isActionDown(context) then 1 else 0) +
              (if controls.leftKey.isActionDown(context) then -1 else 0)

          val playerY: Int =
            (if controls.upKey.isActionDown(context) then 1 else 0) +
              (if controls.downKey.isActionDown(context) then -1 else 0)

          val maybePlayerDirection =
            if playerX == 0 && playerY == 0 then None else Some(Math.atan2(playerY, playerX))

          val rotation = (for {
            targetId <- model.maybeTarget
            if model.lockInToTarget
            target <- gameState.targetableEntityById(targetId)
          } yield (target.currentPosition(gameState.time) - player.pos).arg)
            .getOrElse(gameMousePos.arg)

          val playerMoveAction = MovingBodyMoves(
            GameAction.Id.zero,
            serverTime,
            myId,
            maybePlayerDirection.fold(player.pos)(dir =>
              player.lastValidPosition(
                player.pos + player.speed * context.gameTime.delta.toDouble * Complex
                  .rotation(dir),
                gameState.obstacles.values
              )
            ),
            maybePlayerDirection.getOrElse(0),
            rotation,
            player.speed,
            maybePlayerDirection.isDefined
          )
          val maybeAction = Option.when(
            playerMoveAction.moving || playerMoveAction.moving != player.moving || player.rotation != rotation
          )(playerMoveAction)
          maybeAction.foreach(sendGameAction(_))
          val outputModel = newModel.withActionGatherer(
            newModel.actionGatherer,
            newModel.unconfirmedActions :+ playerMoveAction
          )
          Outcome(outputModel).addGlobalEvents(triggeredActions)
        case None =>
          Outcome(
            newModel.withActionGatherer(
              newModel.actionGatherer,
              Vector(UpdateTimestamp(GameAction.Id.dummy, serverTime))
            )
          ).addGlobalEvents(triggeredActions)
      }

    case send: CustomIndigoEvents.GameEvent.SendAction =>
      sendGameAction(send.action)
      Outcome(model)

    case putMarker: CustomIndigoEvents.GameEvent.PutMarkers =>
      println(putMarker.info)
      sendGameAction(UpdateMarker(GameAction.Id.dummy, serverTime, putMarker.info))
      Outcome(model)

    case event: CustomIndigoEvents.GameEvent.TargetEvent =>
      Outcome(model.withTarget(event.maybeTargetId))
    case CustomIndigoEvents.GameEvent.ToggleTargetLockIn() =>
      Outcome(model.toggleTargetLockIn)
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
      modelBeforeUI: SceneViewModel
  ): GlobalEvent => Outcome[SceneViewModel] = globalEvent =>
    modelBeforeUI.uiParent
      .changeViewModel(
        context.frameContext,
        modelBeforeUI,
        globalEvent
      )
      .addGlobalEvents(
        Batch(soundsManager.handle(context.frameContext, modelBeforeUI, globalEvent))
      )
      .flatMap((viewModel, stopPropagation) =>
        if stopPropagation then Outcome(viewModel)
        else
          globalEvent match {
            case FrameTick =>
              Outcome(
                viewModel.effectsManager.updateViewModel(
                  context.frameContext,
                  viewModel
                    .withUpToDateGameState(model.projectedGameState)
                    .newCameraPosition(myId, context.gameTime.delta)
                    .addFPSDataPoint(context.delta)
                )
              )

            case mouse: MouseEvent.Move =>
              Outcome(viewModel.withMousePos(mouse.position))

            case CustomIndigoEvents.GameEvent.NewAction(action)
                if !action.isInstanceOf[UpdateTimestamp] =>
              // this is where we trigger animations effects
              Outcome(
                viewModel.effectsManager
                  .handleActionAndModifyViewModel(action, context.frameContext, viewModel)
                  .handleAction(action)
              )

            case kbd: KeyboardEvent.KeyUp =>
              val richKbd = kbd.enrich(context.frameContext)
              Outcome(viewModel).addGlobalEvents(
                Batch(
                  castAbilitiesHandler.handleKeyboardEvent(
                    richKbd,
                    context,
                    model,
                    viewModel,
                    serverTime
                  ) ++ gameMarkersHandler
                    .handleKeyboardEvent(
                      richKbd,
                      context.frameContext,
                      model,
                      viewModel,
                      serverTime
                    ) ++ nextTargetHandler
                    .handleKeyUpEvent(richKbd, model.projectedGameState, context.frameContext) ++
                    ToggleLockInTargetHandler.handleKeyboardEvent(richKbd)
                )
              )

            case click: Click
                if viewModel
                  .doesMouseClickLaunchButton(click.position) =>
              println("We should launch the game, woot!")

              Outcome(viewModel)
                .addGlobalEvents(CustomIndigoEvents.GameEvent.SendStartGame())

            case click: Click =>
              castAbilitiesHandler
                .handleClickEvent(
                  click,
                  viewModel,
                  serverTime
                )
                .addGlobalEvents(Batch(viewModel.targetFromMouseClick(click)))

            case CustomIndigoEvents.GameEvent.StartChoosingAbility(abilityId) =>
              Outcome(viewModel.withChoosingAbilityPosition(abilityId))

            case CustomIndigoEvents.GameEvent.SendStartGame() =>
              backendCommWrapper.sendMessageToBackend(InGameWSProtocol.LetsBegin)
              Outcome(viewModel)
            case event: CustomIndigoEvents.GameEvent.TargetEvent =>
              Outcome(viewModel.copy(maybeTargetId = event.maybeTargetId))
            case CustomIndigoEvents.GameEvent.ToggleTargetLockIn() =>
              Outcome(viewModel.copy(lockInToTarget = !viewModel.lockInToTarget))
            case _ => Outcome(viewModel)
          }
      )

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

  private val aztecDiamondBackground = {
    val background = Asset.background.anAztecDiamond
    Graphic(background.size, Material.ImageEffects(background.assetName).withAlpha(0.5))
      .withDepth(Depth.far - 1)
      .withRef(background.center)
      .withScale(background.scaleTo(background.size * 2))
  }

}

object InGameScene {
  case class StartupData(bounds: Rectangle, controls: Controls)

  class InGameModel(
      val actionGatherer: ActionGatherer,
      val unconfirmedActions: Vector[GameAction],
      val lastTimeMovementActionSent: Seconds,
      val maybeTarget: Option[Entity.Id],
      val lockInToTarget: Boolean
  ) extends js.Object {
    inline def withActionGatherer(
        newActionGatherer: ActionGatherer,
        newUnconfirmedActions: Vector[GameAction]
    ): InGameModel =
      InGameModel(
        newActionGatherer,
        newUnconfirmedActions,
        lastTimeMovementActionSent,
        maybeTarget = maybeTarget,
        lockInToTarget = lockInToTarget
      )

    inline def withTarget(newMaybeTarget: Option[Entity.Id]) =
      InGameModel(
        actionGatherer,
        unconfirmedActions,
        lastTimeMovementActionSent,
        maybeTarget = newMaybeTarget,
        lockInToTarget = lockInToTarget
      )

    inline def toggleTargetLockIn: InGameModel = InGameModel(
      actionGatherer,
      unconfirmedActions,
      lastTimeMovementActionSent,
      maybeTarget = maybeTarget,
      lockInToTarget = !lockInToTarget
    )

    inline def alsoSendAction(now: Seconds, sendAction: => Unit): InGameModel =
      sendAction
      InGameModel(
        actionGatherer,
        unconfirmedActions,
        now,
        maybeTarget = maybeTarget,
        lockInToTarget = lockInToTarget
      )

    lazy val projectedGameState: GameState =
      actionGatherer.currentGameState.applyActions(unconfirmedActions)
  }

  def initialModel(initialGameState: GameState): InGameModel = InGameModel(
    GreedyActionGatherer(initialGameState),
    Vector.empty,
    Seconds.zero,
    maybeTarget = Option.empty,
    lockInToTarget = false
  )

  val name = SceneName("in game")
}
