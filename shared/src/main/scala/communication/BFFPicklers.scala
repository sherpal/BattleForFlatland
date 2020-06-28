package communication

import boopickle.CompositePickler
import boopickle.Default._
import gamelogic.abilities._
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.gameactions._
import models.bff.ingame.InGameWSProtocol
import models.bff.ingame.InGameWSProtocol._

object BFFPicklers {

  implicit val abilityPickler: Pickler[Ability] = CompositePickler[Ability]
    .addConcreteType[boss.boss101.BigDot]
    .addConcreteType[boss.boss101.BigHit]
    .addConcreteType[boss.boss101.SmallHit]
    .addConcreteType[boss.boss102.PutDamageZones]
    .addConcreteType[boss.boss102.PutLivingDamageZoneOnTarget]
    .addConcreteType[boss.boss102.SpawnHound]
    .addConcreteType[hexagon.FlashHeal]
    .addConcreteType[hexagon.HexagonHot]
    .addConcreteType[pentagon.CreatePentagonBullet]
    .addConcreteType[pentagon.CreatePentagonZone]
    .addConcreteType[square.Cleave]
    .addConcreteType[square.Enrage]
    .addConcreteType[square.HammerHit]
    .addConcreteType[square.Taunt]
    .addConcreteType[triangle.DirectHit]
    .addConcreteType[triangle.UpgradeDirectHit]
    .addConcreteType[AutoAttack]
    .addConcreteType[SimpleBullet]

  implicit val gameActionPickler: Pickler[GameAction] = CompositePickler[GameAction]
    .addConcreteType[boss102.AddBossHound]
    .addConcreteType[boss102.PutDamageZone]
    .addConcreteType[boss102.PutLivingDamageZone]
    .addConcreteType[classes.pentagon.PutPentagonZone]
    .addConcreteType[AddDummyMob]
    .addConcreteType[AddPlayer]
    .addConcreteType[AddPlayerByClass]
    .addConcreteType[ChangeTarget]
    .addConcreteType[CreateObstacle]
    .addConcreteType[DummyEntityMoves]
    .addConcreteType[EndGame]
    .addConcreteType[EntityCastingInterrupted]
    .addConcreteType[EntityGetsHealed]
    .addConcreteType[EntityResourceChanges]
    .addConcreteType[EntityStartsCasting]
    .addConcreteType[EntityTakesDamage]
    .addConcreteType[GameStart]
    .addConcreteType[MovingBodyMoves]
    .addConcreteType[NewPentagonBullet]
    .addConcreteType[NewSimpleBullet]
    .addConcreteType[PutConstantDot]
    .addConcreteType[PutSimpleBuff]
    .addConcreteType[RemoveBuff]
    .addConcreteType[RemoveEntity]
    .addConcreteType[SpawnBoss]
    .addConcreteType[ThreatToEntityChange]
    .addConcreteType[TickerBuffTicks]
    .addConcreteType[UpdateConstantHot]
    .addConcreteType[UpdateTimestamp]
    .addConcreteType[UseAbility]

  implicit val ingameWebsocketPickler: Pickler[InGameWSProtocol] = CompositePickler[InGameWSProtocol]
    .addConcreteType[HeartBeat.type]
    .addConcreteType[Ping]
    .addConcreteType[Pong]
    .addConcreteType[Ready]
    .addConcreteType[ReadyToStart]
    .addConcreteType[LetsBegin.type]
    .addConcreteType[GameActionWrapper]
    .addConcreteType[RemoveActions]
    .addConcreteType[AddAndRemoveActions]
    .addConcreteType[YourEntityIdIs]
    .addConcreteType[StartingBossPosition]

}