package game.ai.goodais.boss.goodaiscreators

import gamelogic.docs.BossMetadata
import game.ai.goodais.boss.GoodAICreator
import models.bff.outofgame.gameconfig.PlayerName
import gamelogic.entities.boss.dawnoftime.Boss110
import gamelogic.entities.boss.Boss101

/**
  * Dispatchers are responsible for mapping the name of the ai to the corresponding
  * [[GoddAICreator]].
  *
  * They are required to implement a partial function doing exactly that. Then, the maybeCreator
  * method can be used to access the creator for the given name.
  */
trait GoodAICreatorDispatcher[BossType <: BossMetadata] {

  def metadata: BossType

  val maybeCreatorPartial: PartialFunction[PlayerName.AIPlayerName, GoodAICreator[BossType]]

  final def maybeCreator(name: PlayerName.AIPlayerName): Option[GoodAICreator[BossType]] =
    maybeCreatorPartial.lift(name)

}

object GoodAICreatorDispatcher {

  private val dispatcher: PartialFunction[BossMetadata, GoodAICreatorDispatcher[_ <: BossMetadata]] = {
    case metadata: Boss110.type => new Boss110CreatorsDispatcher(metadata)
    case metadata: Boss101.type => new Boss101CreatorsDispatcher(metadata)
  }

  def maybeDispatcherByMetadata(metadata: BossMetadata): Option[GoodAICreatorDispatcher[_ <: BossMetadata]] =
    dispatcher.lift(metadata)

}
