package services.hostelections

import zio.*
import menus.data.{HostCandidacy, User}
import errors.ErrorADT

def open(
    gameId: String,
    gameSecret: String,
    gameCreator: String,
    players: Set[String]
): ZIO[HostElections, Nothing, Promise[Nothing, Unit]] =
  ZIO.serviceWithZIO[HostElections](_.open(gameId, gameSecret, gameCreator, players))

def candidacy(
    user: User,
    candidacy: HostCandidacy
): ZIO[HostElections, Nothing, Either[ErrorADT, Unit]] =
  ZIO.serviceWithZIO[HostElections](_.candidacy(user, candidacy))

def close(gameId: String): ZIO[HostElections, Nothing, Unit] =
  ZIO.serviceWithZIO[HostElections](_.close(gameId))

def nextHost(gameId: String): ZIO[HostElections, Nothing, Option[(String, String)]] =
  ZIO.serviceWithZIO[HostElections](_.nextHost(gameId))

def isHost(gameId: String, userName: String): ZIO[HostElections, Nothing, Boolean] =
  ZIO.serviceWithZIO[HostElections](_.isHost(gameId, userName))

def over(gameId: String): ZIO[HostElections, Nothing, Option[String]] =
  ZIO.serviceWithZIO[HostElections](_.over(gameId))
