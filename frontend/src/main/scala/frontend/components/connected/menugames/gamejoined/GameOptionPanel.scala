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
import utils.laminarzio.onMountZIO
import frontend.components.utils.ToggleButton

final class GameOptionPanel private (initialGameInfo: MenuGameWithPlayers, socketOutWriter: Observer[WebSocketProtocol])
    extends Component[html.Element] {

  val bossNameStorageKey = "lastBossName"

  def selectFirstBoss(
      maybeInitialBoss: Option[String],
      selectElement: html.Select
  ): ZIO[LocalStorage, Exception, String] =
    (for {
      _                       <- failIfWith(maybeInitialBoss.isDefined, maybeInitialBoss.get)
      maybePreviouslySelected <- retrieveFrom[String](bossNameStorageKey)
      nextSelected = maybePreviouslySelected.getOrElse(BossEntity.allBossesNames.head)
      _ <- storeAt(bossNameStorageKey, nextSelected)
      _ <- ZIO.effect(socketOutWriter.onNext(WebSocketProtocol.UpdateBossName(nextSelected))).orDie
    } yield nextSelected)
      .catchSome { case bossName: String => UIO(bossName) }
      .mapError(ser => new Exception(s"This is weird: $ser"))
      .tap(
        bossName =>
          ZIO.effectTotal {
            selectElement.value = bossName
          }
      )

  def selectNewBoss(bossName: String) =
    (for {
      _ <- ZIO.effect(socketOutWriter.onNext(WebSocketProtocol.UpdateBossName(bossName)))
      _ <- storeAt(bossNameStorageKey, bossName)
    } yield ()).orDie

  val nextBossNameBus: EventBus[String] = new EventBus

  val chooseAis = div(
    "Watch AIs ",
    ToggleButton(
      socketOutWriter.contramap[Boolean](if (_) WebSocketProtocol.ChooseAIs else WebSocketProtocol.ChooseHumans)
    )
  )

  val element: ReactiveHtmlElement[html.Element] = section(
    h2(
      className := "text-2xl",
      className := s"text-$primaryColour-$primaryColourDark",
      "Game Options"
    ),
    select(
      BossEntity.allBossesNames.map(name => option(value := name, name)),
      onMountCallback { ctx =>
        utils.runtime
          .unsafeRunToFuture(selectFirstBoss(initialGameInfo.game.gameConfiguration.maybeBossName, ctx.thisNode.ref))
        nextBossNameBus.events
          .foreach(bossName => utils.runtime.unsafeRunToFuture(selectNewBoss(bossName)))(ctx.owner)
      },
      inContext(elem => onChange.mapTo(elem.ref.value) --> nextBossNameBus)
    ),
    chooseAis
  )

}

object GameOptionPanel {
  def apply(initialGameInfo: MenuGameWithPlayers, socketOutWriter: Observer[WebSocketProtocol]): GameOptionPanel =
    new GameOptionPanel(initialGameInfo, socketOutWriter)
}
