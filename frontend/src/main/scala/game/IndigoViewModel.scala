package game

import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import gamelogic.entities.Entity
import gamelogic.entities.boss.Boss101
import indigo.*
import scala.scalajs.js
import gamelogic.abilities.Ability.AbilityId
import game.scenes.ingame.InGameScene
import indigo.shared.events.MouseEvent.MouseDown
import indigo.shared.events.MouseEvent.Click
import game.ui.*
import game.ui.components.grid.GridContainer
import scala.scalajs.js.JSConverters.*
import game.scenes.ingame.InGameScene.StartupData
import models.bff.outofgame.PlayerClasses
import game.viewmodelmisc.Telemetry
import game.drawers.effects.EffectsManager

case class IndigoViewModel(
    startupData: InGameScene.StartupData,
    gameState: GameState,
    currentCameraPosition: Complex,
    initialBossPosition: Complex,
    maybeTarget: Option[Entity],
    maybeChoosingAbilityPosition: Option[AbilityId],
    localMousePos: Point,
    uiParent: UIParent[InGameScene.StartupData, IndigoViewModel],
    cachedComponents: Component.CachedComponentsInfo,
    effectsManager: EffectsManager,
    telemetry: Telemetry
) {

  def withMousePos(pos: Point): IndigoViewModel = copy(
    localMousePos = pos
  )

  def withUpToDateGameState(newGameState: GameState): IndigoViewModel = copy(
    gameState = newGameState
  )

  def withChoosingAbilityPosition(abilityId: AbilityId): IndigoViewModel = copy(
    maybeChoosingAbilityPosition = Some(abilityId)
  )

  def stopChoosingAbilityPosition: IndigoViewModel = copy(
    maybeChoosingAbilityPosition = Option.empty
  )

  inline def gameToLocal(z: Complex): Point =
    game.gameutils.gameToLocal(z)(startupData.bounds)

  inline def localToGame(p: Point): Complex =
    game.gameutils.localToGame(p)(startupData.bounds)

  def localMousePosToWorld(pos: Point): Complex = localToGame(pos) + currentCameraPosition

  lazy val worldMousePosition: Complex = localMousePosToWorld(localMousePos)

  def targetFromMouseClick(event: Click): IndigoViewModel = {
    val mousePos = localMousePosToWorld(event.position)
    val maybeNextTarget = gameState.allTargetableEntities.toVector
      .sortBy(_.shape.radius)
      .find(entity => entity.shape.contains(mousePos, entity.pos, entity.rotation))

    copy(maybeTarget = maybeNextTarget)
  }
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

  def maybeLaunchGameButtonPosition =
    Option.unless(gameState.started) {
      val pos = gameToLocal(initialBossPosition - Complex(30, -10))
      Rectangle(pos.x, pos.y, 100, 20)
    }

  def doesMouseClickLaunchButton(
      mousePosition: Point
  ): Boolean =
    maybeLaunchGameButtonPosition.exists(_.isPointWithin(gameToLocal(worldMousePosition)))

  def maybeLaunchGameButton =
    maybeLaunchGameButtonPosition.map { rect =>
      js.Array(
        Shape
          .Box(rect, Fill.Color(RGBA.Red)),
        TextBox("Start game", rect.width, rect.height)
          .withFontFamily(FontFamily.cursive)
          .withColor(RGBA.White)
          .withFontSize(Pixels(16))
          .withStroke(TextStroke(RGBA.Black, Pixels(1)))
          .withPosition(rect.position)
      )
    }

  def addFPSDataPoint(delta: Seconds): IndigoViewModel = copy(
    telemetry = telemetry.addFPSDataPoint(delta.toMillis.toLong)
  )

  private val cameraSpeed: Double = 300.0

}

object IndigoViewModel {

  def initial(
      gameState: GameState,
      initialBossPosition: Complex,
      startupData: InGameScene.StartupData,
      myId: Entity.Id
  ): IndigoViewModel =
    IndigoViewModel(
      startupData,
      gameState,
      Complex.zero,
      initialBossPosition,
      maybeTarget = Option.empty,
      maybeChoosingAbilityPosition = Option.empty,
      localMousePos = Point.zero,
      uiParent = UIParent[InGameScene.StartupData, IndigoViewModel](
        { (context, viewModel) =>
          given FrameContext[StartupData] = context
          given IndigoViewModel           = viewModel
          js.Array[Component](
            game.ui.components.PlayerFrameContainer(Point(0, 150)),
            game.ui.components.actionbar.ActionBar(myId),
            game.ui.components.TargetFrame(),
            game.ui.components.PlayerFrame(
              myId,
              viewModel.gameState.players.get(myId).fold(PlayerClasses.Hexagon)(_.cls),
              Anchor(AnchorPoint.TopRight, AnchorPoint.BottomCenter, Point(-10, -120))
            ),
            game.ui.components.FPSDisplay(Anchor.bottomLeft),
            game.ui.components.PlayerCastBar(myId),
            game.ui.components.BossCooldownsContainer(),
            game.ui.components.BossThreadMeter(),
            game.ui.components.BossFrame()
          )
        },
        startupData.bounds.width,
        startupData.bounds.height
      ),
      cachedComponents = Component.CachedComponentsInfo.empty,
      telemetry = Telemetry.empty,
      effectsManager = EffectsManager.empty(myId)
    )

}