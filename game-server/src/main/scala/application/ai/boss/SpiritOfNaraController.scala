package application.ai.boss

import application.ai.AIController
import application.ai.utils.*
import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.UseId
import gamelogic.abilities.boss.spiritofnara.*
import gamelogic.buffs.boss.spiritofnara.{Chained, SchoolLockout}
import gamelogic.entities.Entity
import gamelogic.entities.Entity.Id
import gamelogic.entities.boss.firstraid.SpiritOfNara
import gamelogic.entities.classes.PlayerClass
import gamelogic.gamestate.gameactions.{EntityStartsCasting, SpawnBoss}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.physics.pathfinding.Graph

import scala.util.Random

/** The brain of the [[SpiritOfNara]].
  *
  * It never moves. It does nothing while it casts or drinks, and otherwise uses its abilities by
  * decreasing order of priority:
  *   - what must follow what it just did: the [[Pyroblast]] after drinking, the [[ArcaneExplosion]]
  *     after the [[MagneticPull]];
  *   - the Water Elementals (a wave under each threshold), and drinking, as soon as it needs them;
  *   - a special ability every [[SpiritOfNara.specialInterval]], chosen at random among those which
  *     are not on cooldown (their cooldown is twice the interval, so it is never the last one);
  *   - [[Counterspell]] if someone is close enough, [[ChainsOfIce]];
  *   - a spell of a random school it is not locked out of, at a random player.
  */
object SpiritOfNaraController extends AIController[SpiritOfNara, SpawnBoss] {

  override protected def takeActions(
      currentGameState: GameState,
      me: SpiritOfNara,
      currentPosition: Complex,
      startTime: Long,
      timeSinceLastFrame: Long,
      maybeTarget: Option[PlayerClass],
      obstacleGraph: Graph
  ): Vector[GameAction] =
    Option
      .unless(currentGameState.entityIsCasting(me.id) || me.isDrinking(currentGameState))(
        maybeTarget
      )
      .flatten
      .map { target =>
        val maybeChangeTarget = changeTarget(me, target.id, startTime)

        def maybeCast(ability: Ability): Option[EntityStartsCasting] =
          Some(ability)
            .filter(me.canUseAbilityBoolean(_, startTime, currentGameState))
            .map(_.toStartCasting(startTime))

        def maybeCastWhen(condition: Boolean)(ability: => Ability): Option[EntityStartsCasting] =
          Option.when(condition)(ability).flatMap(maybeCast)

        val players = currentGameState.players.values.toVector

        def maybeSpecial = Option
          .when(startTime - me.lastSpecial >= SpiritOfNara.specialInterval)(
            Random
              .shuffle(
                Vector[Ability](
                  MagneticPull(UseId.dummy, startTime, me.id),
                  FlameWreath(UseId.dummy, startTime, me.id),
                  Blizzard(UseId.dummy, startTime, me.id)
                )
              )
              .iterator
              .flatMap(maybeCast)
              .nextOption()
          )
          .flatten

        // anyone but its target, and not twice the same
        def maybeChainsOfIce = {
          val chained    = currentGameState.allTBuffs[Chained].map(_.bearerId).toSet
          val candidates = players.filterNot(p => p.id == target.id || chained.contains(p.id))
          Random
            .shuffle(candidates)
            .headOption
            .flatMap(victim => maybeCast(ChainsOfIce(UseId.dummy, startTime, me.id, victim.id)))
        }

        def maybeNuke = {
          val schools =
            MagicSchool.all.filterNot(SchoolLockout.isLocked(currentGameState, me.id, _))
          Random
            .shuffle(schools)
            .iterator
            .flatMap(school =>
              Random
                .shuffle(players)
                .headOption
                .flatMap(victim => maybeCast(nuke(school, startTime, me.id, victim.id)))
            )
            .nextOption()
        }

        useAbility(
          Vector(
            maybeCast(ShadowPyroblast(UseId.dummy, startTime, me.id)),
            maybeCastWhen(me.pyroblastPending(currentGameState))(
              Pyroblast(UseId.dummy, startTime, me.id)
            ),
            maybeCastWhen(me.arcaneExplosionPending)(
              ArcaneExplosion(UseId.dummy, startTime, me.id)
            ),
            me.dueElementalWave.flatMap(wave =>
              maybeCast(SummonWaterElementals(UseId.dummy, startTime, me.id, wave))
            ),
            maybeCastWhen(me.needsToDrink)(MassPolymorph(UseId.dummy, startTime, me.id)),
            maybeSpecial,
            maybeCastWhen(Counterspell.victims(currentGameState, me.id, startTime).nonEmpty)(
              Counterspell(UseId.dummy, startTime, me.id)
            ),
            maybeChainsOfIce,
            maybeNuke,
            me.maybeAutoAttack(startTime, currentGameState).map(_.toStartCasting(startTime))
          ),
          maybeChangeTarget,
          None
        )
      }
      .getOrElse(Vector.empty)

  private def nuke(school: Int, time: Long, casterId: Entity.Id, targetId: Entity.Id): NaraNuke =
    school match {
      case MagicSchool.fire => Fireball(UseId.dummy, time, casterId, targetId)
      case MagicSchool.frost => Frostbolt(UseId.dummy, time, casterId, targetId)
      case _ => ArcaneMissiles(UseId.dummy, time, casterId, targetId)
    }

  override protected def getMe(gameState: GameState, entityId: Id): Option[SpiritOfNara] =
    gameState.bosses.get(entityId).collect { case boss: SpiritOfNara => boss }

}
