package utils.customheader

import akka.http.scaladsl.model.headers._

import scala.util.Try

final class GameServerSecretHeader(secret: String) extends ModeledCustomHeader[GameServerSecretHeader] {
  def companion: ModeledCustomHeaderCompanion[GameServerSecretHeader] = GameServerSecretHeader

  def value: String = secret

  def renderInRequests: Boolean = true

  def renderInResponses: Boolean = true
}

object GameServerSecretHeader extends ModeledCustomHeaderCompanion[GameServerSecretHeader] {
  def name: String = "game-secret"

  def parse(value: String): Try[GameServerSecretHeader] = scala.util.Success(new GameServerSecretHeader(value))
}
