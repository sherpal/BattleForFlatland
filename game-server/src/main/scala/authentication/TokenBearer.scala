package authentication

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import models.bff.ingame.GameUserCredentials
import models.users.User

import scala.collection.immutable.Queue

object TokenBearer {

  sealed trait Message

  case class CredentialsWrapper(users: List[User], userCredentials: List[GameUserCredentials]) extends Message

  /** A user asked the token. We return it if that user exists and the provided credentials are correct.  */
  case class TokenForUser(userCredentials: GameUserCredentials, replyTo: ActorRef[Option[String]]) extends Message

  /** Used for testing purposes. returns the first non-requested token. */
  case class TokenForTest(replyTo: ActorRef[Option[String]]) extends Message

  /** A user connected, we check that their token matches. Why on earth am I doing in two steps? */
  case class UserConnects(userId: String, token: String, replyTo: ActorRef[Boolean]) extends Message

  def apply(): Behavior[Message] = waitingForCredentials(Queue())

  private def waitingForCredentials(queuedMessages: Queue[Message]): Behavior[Message] = Behaviors.receive {
    (context, message) =>
      message match {
        case CredentialsWrapper(users, userCredentials) =>
          println(s"Received credentials. I had ${queuedMessages.size} elements in the queue.")
          queuedMessages.foreach(context.self ! _)
          receiver(
            for {
              (user, idx) <- users.zipWithIndex
              credentials <- userCredentials
              if user.userId == credentials.userId
            } yield UserInfo(user, credentials.userSecret, s"$idx-${credentials.userSecret}", tokenRequested = false),
            userCredentials.headOption.map(_.gameId).getOrElse("")
          )
        case other =>
          waitingForCredentials(queuedMessages.enqueue(other))
      }
  }

  private def receiver(playerTokens: List[UserInfo], gameId: String): Behavior[Message] = Behaviors.receive {
    (context, message) =>
      message match {
        case TokenForUser(userCredentials, replyTo) if userCredentials.gameId != gameId =>
          replyTo ! None
          Behaviors.same
        case TokenForUser(userCredentials, replyTo) =>
          replyTo ! playerTokens
            .find(
              userInfo =>
                userInfo.user.userId == userCredentials.userId && userInfo.userSecret == userCredentials.userSecret
            )
            .map(_.token)
          receiver(
            playerTokens.map(
              info =>
                info.copy(
                  tokenRequested = info.tokenRequested || (
                    info.user.userId == userCredentials.userId && info.userSecret == userCredentials.userSecret
                  )
                )
            ),
            gameId
          )
        case TokenForTest(replyTo) =>
          playerTokens.find(!_.tokenRequested) match {
            case Some(info) =>
              context.self ! TokenForUser(
                GameUserCredentials(
                  info.user.userId,
                  "",
                  info.userSecret
                ),
                replyTo
              )
            case None => replyTo ! None
          }
          Behaviors.same
        case UserConnects(userId, token, replyTo) =>
          replyTo ! playerTokens.exists(info => info.user.userId == userId && info.token == token)
          Behaviors.same
        case _: CredentialsWrapper => Behaviors.unhandled
      }
  }

  /**
    * Token simply is the user secret prepended by a unique number from 0 until the number of players.
    * This ensures that the token is unique (even though in practice the user secrets will always be) and it
    * will be used to identify the newcomer when they connect.
    */
  private case class UserInfo(user: User, userSecret: String, token: String, tokenRequested: Boolean)

}
