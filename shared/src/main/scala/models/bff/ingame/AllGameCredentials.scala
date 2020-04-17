package models.bff.ingame

final case class AllGameCredentials(
    gameCredentials: GameCredentials,
    allGameUserCredentials: List[GameUserCredentials]
)
