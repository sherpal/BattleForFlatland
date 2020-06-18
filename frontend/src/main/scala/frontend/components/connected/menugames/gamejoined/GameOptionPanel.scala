package frontend.components.connected.menugames.gamejoined

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontend.components.Component
import frontend.components.utils.tailwind.{primaryColour, primaryColourDark}
import gamelogic.entities.boss.BossEntity
import models.bff.gameantichamber.WebSocketProtocol
import models.bff.outofgame.MenuGameWithPlayers
import org.scalajs.dom.html
import services.localstorage._
import utils.ziohelpers.failIfWith
import zio.{Task, UIO, ZIO, ZLayer}

final class GameOptionPanel private (initialGameInfo: MenuGameWithPlayers, socketOutWriter: Observer[WebSocketProtocol])
    extends Component[html.Element] {

  val localStorage: ZLayer[Any, Nothing, LocalStorage] = zio.clock.Clock.live >>> FLocalStorage.live

  val bossNameStorageKey = "lastBossName"

  def selectFirstBoss(maybeInitialBoss: Option[String], selectElement: html.Select): Task[String] =
    (for {
      _ <- failIfWith(maybeInitialBoss.isDefined, maybeInitialBoss.get)
      maybePreviouslySelected <- retrieveFrom[String](bossNameStorageKey)
      nextSelected = maybePreviouslySelected.getOrElse(BossEntity.allBossesNames.head)
      _ <- storeAt(bossNameStorageKey, nextSelected)
      _ <- ZIO.effect(socketOutWriter.onNext(WebSocketProtocol.UpdateBossName(nextSelected))).orDie
    } yield nextSelected)
      .catchSome { case bossName: String => UIO(bossName) }
      .mapError(ser => new Exception(s"This is weird: $ser"))
      .provideLayer(localStorage)
      .tap(
        bossName =>
          ZIO.effectTotal {
            selectElement.value = bossName
          }
      )

  def selectNewBoss(bossName: String): UIO[Unit] =
    (for {
      _ <- ZIO.effect(socketOutWriter.onNext(WebSocketProtocol.UpdateBossName(bossName)))
      _ <- storeAt(bossNameStorageKey, bossName)
    } yield ()).orDie.provideLayer(localStorage)

  val nextBossNameBus: EventBus[String] = new EventBus

  val element: ReactiveHtmlElement[html.Element] = section(
    h2(
      className := "text-2xl",
      className := s"text-$primaryColour-$primaryColourDark",
      "Game Options"
    ),
    select(
      BossEntity.allBossesNames.map(name => option(value := name, name)),
      onMountCallback { ctx =>
        zio.Runtime.default
          .unsafeRunToFuture(selectFirstBoss(initialGameInfo.game.gameConfiguration.maybeBossName, ctx.thisNode.ref))
        nextBossNameBus.events
          .foreach(bossName => zio.Runtime.default.unsafeRunToFuture(selectNewBoss(bossName)))(ctx.owner)
      },
      inContext(elem => onChange.mapTo(elem.ref.value) --> nextBossNameBus)
    )
  )

}

object GameOptionPanel {
  def apply(initialGameInfo: MenuGameWithPlayers, socketOutWriter: Observer[WebSocketProtocol]): GameOptionPanel =
    new GameOptionPanel(initialGameInfo, socketOutWriter)
}
