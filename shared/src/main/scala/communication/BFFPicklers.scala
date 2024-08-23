package communication

import boopickle.CompositePickler
import boopickle.Default.*
import gamelogic.abilities.*
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.gameactions.*
import models.bff.ingame.InGameWSProtocol
import models.bff.ingame.InGameWSProtocol.*

object BFFPicklers {

  implicit val abilityPickler: Pickler[Ability] = compositePickler[Ability]
    .addConcreteType[boss.boss101.BigDot]
    .addConcreteType[boss.boss101.BigHit]
    .addConcreteType[boss.boss101.SmallHit]
    .addConcreteType[boss.boss102.PutDamageZones]
    .addConcreteType[boss.boss102.PutLivingDamageZoneOnTarget]
    .addConcreteType[boss.boss102.SpawnHound]
    .addConcreteType[boss.boss103.CleansingNova]
    .addConcreteType[boss.boss103.Punishment]
    .addConcreteType[boss.boss103.SacredGround]
    .addConcreteType[boss.boss103.HolyFlame]
    .addConcreteType[boss.boss110.SpawnBigGuies]
    .addConcreteType[boss.boss110.PlaceBombPods]
    .addConcreteType[boss.boss110.ExplodeBombs]
    .addConcreteType[boss.boss110.SpawnSmallGuies]
    .addConcreteType[boss.boss110.addsabilities.PutBrokenArmor]
    .addConcreteType[boss.boss110.addsabilities.CreepingShadowTick]
    .addConcreteType[hexagon.FlashHeal]
    .addConcreteType[hexagon.HexagonHot]
    .addConcreteType[pentagon.CreatePentagonBullet]
    .addConcreteType[pentagon.CreatePentagonZone]
    .addConcreteType[pentagon.PentaDispel]
    .addConcreteType[square.Cleave]
    .addConcreteType[square.Enrage]
    .addConcreteType[square.HammerHit]
    .addConcreteType[square.Taunt]
    .addConcreteType[triangle.DirectHit]
    .addConcreteType[triangle.EnergyKick]
    .addConcreteType[triangle.UpgradeDirectHit]
    .addConcreteType[triangle.Stun]
    .addConcreteType[AutoAttack]
    .addConcreteType[SimpleBullet]

  implicit val gameActionPickler: Pickler[GameAction] = compositePickler[GameAction]
    .addConcreteType[boss102.AddBossHound]
    .addConcreteType[boss102.PutDamageZone]
    .addConcreteType[boss102.PutLivingDamageZone]
    .addConcreteType[boss103.PutPunishedDebuff]
    .addConcreteType[boss103.PutPurifiedDebuff]
    .addConcreteType[boss103.PutInflamedDebuff]
    .addConcreteType[boss110.AddBigGuies]
    .addConcreteType[boss110.AddBombPods]
    .addConcreteType[boss110.AddSmallGuy]
    .addConcreteType[boss110.AddCreepingShadow]
    .addConcreteType[classes.pentagon.PutPentagonZone]
    .addConcreteType[markers.RemoveMarker]
    .addConcreteType[markers.UpdateMarker]
    .addConcreteType[AddDummyMob]
    .addConcreteType[AddPlayer]
    .addConcreteType[AddPlayerByClass]
    .addConcreteType[ChangeTarget]
    .addConcreteType[CreateObstacle]
    .addConcreteType[DummyEntityMoves]
    .addConcreteType[EndGame]
    .addConcreteType[EntityCastingInterrupted]
    .addConcreteType[EntityGetsHealed]
    .addConcreteType[EntityRadiusChange]
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

  implicit val ingameWebsocketPickler: Pickler[InGameWSProtocol] = compositePickler[InGameWSProtocol]
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
