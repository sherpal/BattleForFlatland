package utils.customheader

import akka.http.scaladsl.model.headers.{ModeledCustomHeader, ModeledCustomHeaderCompanion}

import scala.util.Try

final class GameServerIdHeader(gameId: String) extends ModeledCustomHeader[GameServerIdHeader] {
  def companion: ModeledCustomHeaderCompanion[GameServerIdHeader] = GameServerIdHeader

  def value: String = gameId

  def renderInRequests: Boolean = true

  def renderInResponses: Boolean = true
}

object GameServerIdHeader extends ModeledCustomHeaderCompanion[GameServerIdHeader] {
  def name: String = "game-id"

  def parse(value: String): Try[GameServerIdHeader] = scala.util.Success(new GameServerIdHeader(value))
}
