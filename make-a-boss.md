# How to implement a new Boss in Battle for Flatland

> **Audience**: an AI coding agent (or a developer) implementing a brand-new boss encounter.
> **Prerequisite**: read nothing else first. This file is self-contained; it points at the exact
> files you need. The narrative "diary" version of this guide lives in [README.md](README.md)
> (section *Create a new Boss*) and may contain extra colour, but **this file is the normative one**.
>
> **Reference implementation**: `Boss104`. Every template below is extracted from real, compiling
> Boss104 code. When in doubt, open the corresponding Boss104 file listed in
> [§9 Boss104 file index](#9-boss104-file-index) and mirror it.

---

## 1. The engine in 60 seconds

You cannot write a boss correctly without these six facts.

1. **The game is a deterministic fold.** `GameState` is immutable. The whole game is
   `actions.foldLeft(initialGameState)(_ apply _)`. Given the same ordered list of `GameAction`s,
   every client and the server compute the same state. **Nothing else may influence the state.**

2. **`GameAction` → `GameStateTransformer` must be pure.**
   [`GameAction.createGameStateTransformer`](shared/src/main/scala/gamelogic/gamestate/GameAction.scala)
   is replayed on every client. No randomness, no clock, no `gameState.players.head` tie-breaks that
   could differ. Everything the transformer needs (which player, which colour, which position, which
   fresh id) must be **stored as a field of the action**.

3. **`Ability.createActions` is the *only* place randomness is allowed.** It runs exactly once, on
   the game master (server), inside
   [`ManageUsedAbilities`](shared/src/main/scala/gamelogic/gamestate/serveractions/ManageUsedAbilities.scala).
   Its output actions are broadcast verbatim. So: *roll the dice in the ability, freeze the result
   into the action.* This is the single most important design rule of the codebase.

4. **Ability lifecycle.**
   `AIController.takeActions` emits `EntityStartsCasting(…, ability)` →
   the server tracks it in `gameState.castingEntityInfo` →
   when `now - startedTime >= castingTime`, `ManageUsedAbilities` emits `UseAbility(…)` **plus**
   `ability.createActions(gameState)` →
   `UseAbility` records the usage in the caster's `relevantUsedAbilities`, which is what drives
   cooldowns via `WithAbilities.abilityOnCooldown`.

5. **Three separate AI systems, three separate places.**
   | System | Manager | Registered by |
   |---|---|---|
   | The boss & its adds (hostile) | [`AIManager`](game-server/src/main/scala/application/ai/AIManager.scala) | a `case action: XxxCreatorAction =>` clause |
   | Friendly player-bots | [`GoodAIManager`](game-server/src/main/scala/application/ai/GoodAIManager.scala) | `bossAIContainers` map |
   | Nothing else | — | — |
   Both live in `game-server` **only**. Never put AI code in `shared`.

6. **The client is a pure function of `GameState`.** A boss' visuals read the game state and draw
   it with `love.graphics`. They never decide anything.

---

## 2. Where things live

| Sub-project | Platform | Contains |
|---|---|---|
| `shared` | JS + JVM | **All game logic**: entities, abilities, buffs, actions, game state. Both the client and the server run this identical code. |
| `game-server` | JVM | The game master loop and **all AI** (boss AI + friendly bots). |
| `love-client` | Scala.js, linked to Lua by `lua-linker`, run by LÖVE | Rendering, HUD, menus, assets (see [its README](love-client/README.md)). |
| `server` / `shared-backend` / `game-server-launcher` / `lua-linker` | JVM | Lobby, launching, linking the client. **You will not touch these.** |

Canonical package layout for a boss (using `Boss104` / theme package `dawnoftime` as the model):

```
shared/src/main/scala/gamelogic/
  entities/boss/dawnoftime/Boss104.scala          ← the boss entity + its BossFactory companion
  entities/boss/boss104/                          ← entities the boss creates (adds, zones, markers)
  abilities/boss/boss104/                         ← the boss' and its adds' abilities
  buffs/boss/boss104/                             ← buffs/debuffs the boss applies
  gamestate/gameactions/boss104/                  ← the actions those abilities produce

game-server/src/main/scala/application/ai/
  boss/Boss104Controller.scala                    ← the boss' brain
  boss/boss104units/BigGuyController.scala        ← each add's brain
  goodais/bosses/boss104/                         ← friendly bots tuned for this encounter

love-client/src/main/scala/bffclient/
  render/bosses/Boss104Visuals.scala              ← what is drawn for the boss, its HUD tweaks
  render/Sprites.scala                            ← buff icons etc.
love-client/src/main/lua/assets/boss/dawn-of-time/boss104/   ← 32×32 PNGs
```

> **Naming**: the *theme* package (`dawnoftime`) groups bosses by flavour and holds the boss class.
> The *per-boss* packages (`boss104`) hold everything that boss owns. Pick a new theme package only
> if you are starting a new group of bosses. The `firstraid` theme (bosses of the first raid) names
> its bosses instead of numbering them: `firstraid/SirOfVice.scala`, with per-boss packages
> `sirofvice`, ability ids `Ability.sirOfViceXxx` and assets under `assets/boss/first-raid/sir-of-vice/`.

---

## 3. The registry checklist ⚠️

This is the part that an agent gets wrong. Creating a class is never enough — almost everything
must be **registered** somewhere. A missing registration typically produces a *runtime* crash or a
silently inert boss, not a compile error.

| You created… | You MUST also… | File | Symptom if you forget |
|---|---|---|---|
| a new **`Ability`** | add `val bossXxxYyy: AbilityId = nextAbilityId()` | [`gamelogic/abilities/Ability.scala`](shared/src/main/scala/gamelogic/abilities/Ability.scala) | can't reference the id |
| " | `.addConcreteType[boss.bossXxx.Yyy]` on `abilityPickler` | [`communication/BFFPicklers.scala`](shared/src/main/scala/communication/BFFPicklers.scala) | **game crashes** the first time the ability is sent over the websocket |
| " | add the id to the caster's `abilities` **and** `abilityNames` | the entity class | `canUseAbility` returns `"You don't have that ability"`; ability silently never fires |
| " | make an AI actually emit `EntityStartsCasting` for it | the relevant `AIController` | ability is legal but never used |
| " | add an `AbilityDoc` for it to the boss' `abilitiesDoc` (with `.castBy("Add name")` if an add casts it) | your boss' companion | players don't see it in the lobby; `GameDocsSpecs` fails for the boss' own abilities |
| a new **`GameAction`** | `.addConcreteType[bossXxx.YourAction]` on `gameActionPickler` | [`BFFPicklers.scala`](shared/src/main/scala/communication/BFFPicklers.scala) | **game crashes** on broadcast |
| a new **`Buff`** | `val bossXxxYyy = nextId()` in `Buff`'s companion | [`gamelogic/buffs/Buff.scala`](shared/src/main/scala/gamelogic/buffs/Buff.scala) | no `resourceIdentifier` |
| " | a 32×32 PNG in `love-client/src/main/lua/assets/` + an entry in `buffIcons` | [`render/Sprites.scala`](love-client/src/main/scala/bffclient/render/Sprites.scala) | buff has no icon in player frames |
| a new **boss** | add the factory to `factoriesByBossName` | [`entities/boss/BossFactory.scala`](shared/src/main/scala/gamelogic/entities/boss/BossFactory.scala) | boss not selectable in the lobby |
| " | `case action: SpawnBoss if action.bossName == BossXxx.name =>` | [`AIManager.scala`](game-server/src/main/scala/application/ai/AIManager.scala) | boss spawns and stands still forever |
| **visuals** (optional, in practice needed) | an `object BossXxxVisuals extends BossVisuals` + add it to `byBossName` | [`render/bosses/BossVisuals.scala`](love-client/src/main/scala/bffclient/render/bosses/BossVisuals.scala) | the boss' zones, markers and adds are drawn as plain grey outlines; no frame tint or HUD line |
| " | list every entity it draws in its `draws` | your `BossXxxVisuals` | the entity is drawn twice (yours, and a grey outline) |
| an **add** (mob with its own AI) | `case action: AddYourAdd => aiControllers.addOne(action.entityId -> …)` | [`AIManager.scala`](game-server/src/main/scala/application/ai/AIManager.scala) | add spawns and stands still |
| " | its `shape.radius` in the `GraphManager(Vector(...))` radii list | [`AIManager.scala`](game-server/src/main/scala/application/ai/AIManager.scala) | console warn `There is no graph for me`, add never moves |
| **friendly bots** | a `BossAIContainer` + an entry in `bossAIContainers` | [`GoodAIManager.scala`](game-server/src/main/scala/application/ai/GoodAIManager.scala) | `I don't handle boss …` printed; bots inert |
| " | implement `maybeAIComposition` | your boss' companion | "Add AI" buttons stay disabled in the lobby |
| " | implement `description` and `abilitiesDoc` (build the texts from your constants, see Boss104) | your boss' companion | does not compile |

> **Note on circe**: `GameAction` and `Ability` also carry hand-written circe `Encoder`/`Decoder`
> instances. These are **legacy and not used for in-game traffic** (boopickle is). Sir of Vice/Boss104/Boss110
> actions are absent from them and the game works fine. Do not bother extending them unless you have
> a specific reason.

---

## 4. Procedure

Work in this order. Each phase ends with an **acceptance check** — an observable thing. Do not skip
ahead; an unverified phase costs far more to debug later.

### Phase 1 — Skeleton that compiles

1. Create `shared/src/main/scala/gamelogic/entities/boss/<theme>/BossXxx.scala`.
2. `final case class BossXxx(...) extends BossEntity` — **case class is mandatory** (you need `copy`
   for the `MovingBody`/`WithThreat`/`WithAbilities` patch methods, and `Pointed[BossXxx].unit`
   derivation for `initialBoss`).
3. `object BossXxx extends BossFactory[BossXxx] with BossMetadata`.
4. Let the compiler list the abstract members and fill them. Most are mechanical — copy from
   [`Boss104.scala`](shared/src/main/scala/gamelogic/entities/boss/dawnoftime/Boss104.scala).
   **Narrow the return types to your boss type** (`override def changeTarget(…): BossXxx = …`)
   rather than leaving the super-trait type; it makes later code much easier.
5. Temporarily: `abilities = Set.empty`, `abilityNames = Map.empty`,
   `initialBossActions = healAndDamageAwareActions(entityId, time)`,
   `whenBossDiesActions = Vector.empty`, `playersStartingPosition = 0`.
6. `initialBoss` = `Pointed[BossXxx].unit.copy(id = entityId, time = time, maxLife = maxLife,
   life = maxLife, speed = …)`. **Set `life`/`maxLife`**, otherwise the boss dies instantly.

**Check**: `sbt sharedJVM/compile` succeeds.

### Phase 2 — The room (`stagingBossActions`)

`stagingBossActions(time)` returns the actions applied at the origin of time, before anyone spawns.
Its job is the **topology of the arena**: obstacles.

For a plain bounded square arena, reuse the helper:

```scala
val size = 500.0

def gameBoundariesActions(time: Long)(using IdGeneratorContainer): Vector[CreateObstacle] =
  Obstacle.squareGameArea(time, size)

def stagingBossActions(time: Long)(using IdGeneratorContainer): Vector[GameAction] =
  gameBoundariesActions(time)
```

See [`Obstacle.squareGameArea`](shared/src/main/scala/gamelogic/entities/staticstuff/Obstacle.scala)
for how walls are built from segments, and `Boss102` for a more elaborate room.

### Phase 3 — Make it appear

1. Register the factory in `BossFactory.factoriesByBossName`. (The client needs nothing yet: a
   boss without visuals is drawn with the generic ones.)

**Check**: launch the game (see [§8](#8-building-and-running)). The boss is selectable, the arena
walls are drawn, the boss spawns on "start game" — and stands perfectly still while you kill it.

### Phase 4 — Movement

Create `game-server/src/main/scala/application/ai/boss/BossXxxController.scala`:

```scala
object BossXxxController extends AIController[BossXxx, SpawnBoss] {

  override protected def getMe(gameState: GameState, entityId: Id): Option[BossXxx] =
    gameState.bosses.get(entityId).collect { case boss: BossXxx => boss }

  override protected def takeActions(
      currentGameState: GameState, me: BossXxx, currentPosition: Complex,
      startTime: Long, timeSinceLastFrame: Long,
      maybeTarget: Option[PlayerClass], obstacleGraph: Graph
  ): Vector[GameAction] =
    Option
      .unless(currentGameState.entityIsCasting(me.id))(maybeTarget)   // busy casting ⇒ do nothing
      .flatten                                                        // no target ⇒ everyone's dead
      .map { target =>
        val maybeChangeTarget = changeTarget(me, target.id, startTime)
        val maybeMove = aiMovementToTargetWithGraph(
          me.id, startTime, timeSinceLastFrame, currentPosition, me.shape.radius,
          target.currentPosition(startTime + 100), BossXxx.meleeRange,
          BossXxx.fullSpeed, BossXxx.fullSpeed / 4, me.speed, me.moving, me.rotation,
          obstacleGraph
        )
        useAbility(Vector.empty, maybeChangeTarget, maybeMove)
      }
      .getOrElse(Vector.empty)
}
```

Then register it in `AIManager.handleNewActions`:

```scala
case action: SpawnBoss if action.bossName == BossXxx.name =>
  aiControllers.addOne(action.entityId -> boss.BossXxxController)
```

> `aiMovementToTargetWithGraph` uses the pathfinding graph (a
> [`VisibilityGraph`](shared/src/main/scala/gamelogic/physics/pathfinding/VisibilityGraph.scala),
> cheap enough to be queried at every frame) to walk around obstacles, and only considers the
> target reached when it is in range **and** in line of sight. `aiMovementToTarget` (no graph) walks
> in a straight line — fine for a convex arena or an add that doesn't need to route around walls.
> Both are in [`application/ai/utils/globals.scala`](game-server/src/main/scala/application/ai/utils/globals.scala).
> Note `AIManager`'s `GraphManager` is constructed with a **fixed list of collision radii**; an
> entity whose radius is not in that list gets no graph.

**Check**: the boss walks towards you and stops at melee range.

### Phase 5 — Auto-attack

Nearly free, because `AutoAttack` already exists.

1. Add `Ability.autoAttackId` to `abilities` and `Ability.autoAttackId -> "Auto Attack"` to
   `abilityNames`.
2. Add a helper on the boss class:

```scala
def maybeAutoAttack(time: Long, gameState: GameState): Option[AutoAttack] =
  Some(AutoAttack(
    Ability.UseId.zero, time, id, targetId,
    BossXxx.autoAttackDamage, BossXxx.autoAttackTickRate, NoResource, BossXxx.meleeRange
  )).filter(_.canBeCast(gameState, time).isEmpty)
    .filter(canUseAbilityBoolean(_, time, gameState))
```

3. In the controller, feed it to `useAbility`:

```scala
useAbility(
  Vector(
    me.maybeAutoAttack(startTime, currentGameState).map(_.toStartCasting(startTime))
  ),
  maybeChangeTarget,
  maybeMove
)
```

> **How `useAbility` works** (see
> [`AIController`](game-server/src/main/scala/application/ai/AIController.scala)): it takes an
> *ordered* vector of candidate casts and picks **the first defined one**. If one fires, the entity
> also stops moving. Otherwise it just moves/retargets. **Order = priority**: put long-cooldown
> signature abilities first, auto-attack last.

**Check**: the boss damages you in melee; its cooldown bar renders.

### Phase 6 — One signature ability (repeat per ability)

This is the core loop. For each ability, do all eight steps. See
[§5 Recipes](#5-recipes) for the code templates.

1. **Design what it creates.** Debuffs? Entities on the ground? Damage? List them.
2. **Create the entities** it spawns (Recipe C / D).
3. **Create the buffs** it applies (Recipe B).
4. **Create the `GameAction`** that materialises 2+3 (Recipe A, step *action*). Remember: the action
   carries *all* the frozen decisions.
5. **Create the `Ability`** (Recipe A). Its `createActions` does the rolling and returns the actions.
6. **Register**: ability id, boopickle for both the ability and the action(s), boss `abilities` +
   `abilityNames`. (§3)
7. **Schedule first use** in `initialBoss` via `relevantUsedAbilities` (see Recipe A).
8. **Teach the controller** to cast it, at the right priority (Recipe A, step *controller*).

**Check**: the ability fires at the expected time. It will be *invisible* until Phase 7 — verify via
its effects (damage taken, life bars) or a temporary `println` in the controller.

### Phase 7 — Make it visible

1. **Buff icons**: drop a 32×32 PNG in
   `love-client/src/main/lua/assets/boss/dawn-of-time/bossXxx/`, and map
   `Buff.bossXxxYyy -> "boss/dawn-of-time/bossXxx/yyy"` in `buffIcons`, in
   [`Sprites.scala`](love-client/src/main/scala/bffclient/render/Sprites.scala).
2. **World rendering**: write
   `love-client/src/main/scala/bffclient/render/bosses/BossXxxVisuals.scala`, an
   `object BossXxxVisuals extends BossVisuals`, and add it to `byBossName` in the `BossVisuals`
   companion (Recipe E).
3. **Boss-specific HUD** (optional): `hudText` in your visuals adds a line to the HUD (Boss102
   counts its hounds); `playerFrameTint` tints the frames of the players, typically for a debuff
   whose nature must be readable from them (Recipe F).

**Check**: you can *see* the mechanic and play around it.

### Phase 8 — Friendly AIs (recommended, not optional in practice)

You cannot test a 5-player encounter alone. Player-bots make it possible.

1. Implement `maybeAIComposition` on your boss companion — this both enables the lobby buttons and
   declares the intended composition:

```scala
override def intendedFor: Int = 5
override def maybeAIComposition: Option[List[PlayerClasses]] = Some(List(
  PlayerClasses.Square, PlayerClasses.Pentagon, PlayerClasses.Pentagon,
  PlayerClasses.Triangle, PlayerClasses.Hexagon
))
```

2. Create `game-server/src/main/scala/application/ai/goodais/bosses/bossXxx/` with one class per
   player class, extending `SquareAIController` / `PentagonAIController` / `TriangleAIController` /
   `HexagonAIController`. Each takes `(index: Int, entityId: Entity.Id)`.
   - `index` disambiguates duplicate classes (with 5 players, at least one class repeats — pigeonhole).
     Use it to split roles: "pentagon 0 goes left, pentagon 1 goes right", or to jitter timings so
     two bots don't converge on the same target (see `PentagonForBoss104.handleTwinCircle`).
3. Create a `BossXxxContainer extends BossAIContainer` wiring the four, and add
   `BossXxx -> BossXxxContainer()` to `GoodAIManager.bossAIContainers`.

> **Movement**: `preGameMovement` walks in a straight line. In a room with pillars or walls, use
> `maybeMoveTowards(time, me, currentPosition, target, obstacleGraph)` instead: it routes around
> obstacles, and returns `None` once arrived and stopped — which is exactly when it is safe to start
> casting (moving interrupts casts). `SirOfViceBot` is built around it.
>
> **State**: `takeActions` should ideally be pure, but each controller instance is created once per
> entity at game start, lives until the end, and is driven single-threaded — so mutable `var`s in
> the class are safe when you genuinely need memory.
>
> **Caveat**: bots react instantly and never misplay. They answer *"is this even possible?"*, not
> *"is this fun and fairly tuned?"* Never tune difficulty against bots alone.

**Check**: "Fill with AIs" in the lobby, launch, and the encounter plays itself.

### Phase 9 — Tune

`maxLife`, damage numbers, `cooldown`, `castingTime`, `timeToFirstUse`, arena `size`. These are
plain constants in the companion objects — changing them is cheap, so iterate. Consider
`whenBossDiesActions` to clean up lingering adds/debuffs so players don't die after winning.

---

## 5. Recipes

### Recipe A — A boss ability

**A.1 — the id** in [`Ability.scala`](shared/src/main/scala/gamelogic/abilities/Ability.scala)'s companion:

```scala
val bossXxxTwinDebuffs: AbilityId = nextAbilityId()
```

**A.2 — the ability** in `shared/src/main/scala/gamelogic/abilities/boss/bossXxx/`:

```scala
final case class TwinDebuffs(useId: Ability.UseId, time: Long, casterId: Entity.Id) extends Ability {

  override def abilityId: AbilityId = Ability.bossXxxTwinDebuffs
  override def cooldown: Long       = TwinDebuffs.cooldown
  override def castingTime: Long    = TwinDebuffs.castingTime
  override def cost: ResourceAmount = ResourceAmount(0, Resource.NoResource)

  override def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability =
    copy(time = newTime, useId = newId)

  // Legality is enforced by the controller for boss abilities ⇒ None is fine.
  override def canBeCast(gameState: GameState, time: Long): None.type = None

  // ⚠️ THE ONLY PLACE RANDOMNESS IS ALLOWED. Runs once, on the game master.
  override def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] = {
    val colours       = Random.shuffle(TwinDebuffs.possibleColours).take(2)
    val chosenPlayers = Random.shuffle(gameState.players.values.toVector).take(2)
    …
    Vector(/* PutTwinDebuff(genActionId(), time, genBuffId(), …, genEntityId(), position) */)
  }
}

object TwinDebuffs {
  inline def cooldown: Long       = 20000
  inline def castingTime: Long    = 1000
  inline def timeToFirstUse: Long = 10000   // convention, used by initialBoss below
}
```

Notes:
- `useId`, `time`, `casterId` are **always** constructor args. `time` is filled by the game master
  with the moment casting *completed*.
- Constants go in the companion, **unless** they depend on game state at cast time — then make them
  constructor args.
- Ids inside `createActions` come from `IdsProducer`: `genActionId()`, `genEntityId()`, `genBuffId()`.
- Set `canBeCut = true` if a Triangle should be able to interrupt it (see `BigGuyKick`).
- If the ability has a target, extend `WithTargetAbility` instead and implement `range` +
  `canBeCast = isInRangeAndInSight(gameState, time)`.

**A.3 — the action** in `shared/src/main/scala/gamelogic/gamestate/gameactions/bossXxx/`:

```scala
final case class PutTwinDebuff(
    id: GameAction.Id, time: Long,
    buffId: Buff.Id, bearerId: Entity.Id, sourceId: Entity.Id,
    colour: RGBColour,                         // ← frozen decision
    circleId: Entity.Id, circlePosition: Complex   // ← frozen decisions
) extends GameAction {

  override def changeId(newId: Id): GameAction = copy(id = newId)
  override def isLegal(gameState: GameState): Option[String] = None

  // PURE. Compose building blocks with ++.
  override def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    WithBuff(TwinDebuff(buffId, bearerId, sourceId, time, time, colour)) ++
      WithEntity(DebuffCircle(circleId, time, circlePosition, colour), time)
}
```

Available transformers are in
[`gamelogic/gamestate/statetransformers/`](shared/src/main/scala/gamelogic/gamestate/statetransformers/):
`WithEntity`, `WithBuff`, `RemoveEntityTransformer`, `RemoveBuffTransformer`, `CasterUsesAbility`, …
If the action creates an entity, also mix in `GameAction.EntityCreatorAction` (an `entityId` field) —
that is what lets `AIManager` hook an AI onto the new entity.

**A.4 — register** (§3): `BFFPicklers.abilityPickler` **and** `BFFPicklers.gameActionPickler`.

**A.5 — give it to the boss**:

```scala
override def abilities: Set[AbilityId] = Set(Ability.autoAttackId, Ability.bossXxxTwinDebuffs)
override def abilityNames: Map[AbilityId, String] = Map(
  Ability.autoAttackId          -> "Auto Attack",
  Ability.bossXxxTwinDebuffs    -> "Twin Debuffs"
)
```

**A.6 — schedule the first use.** Cooldowns are computed from `relevantUsedAbilities`, so you
pre-seed a fake past usage in `initialBoss`. The idiom:

```scala
relevantUsedAbilities = Map(
  Ability.bossXxxTwinDebuffs -> Pointed[TwinDebuffs].unit.copy(
    time = time - TwinDebuffs.cooldown + TwinDebuffs.timeToFirstUse
  )
)
```

Read it as: "it came off cooldown `timeToFirstUse` ms after the game starts." Giving players a
warm-up before the first big mechanic is the house style.

**A.7 — cast it** in the controller:

```scala
val maybeUseTwinDebuffs =
  Some(TwinDebuffs(UseId.dummy, startTime, me.id))
    .filter(me.canUseAbilityBoolean(_, startTime, currentGameState))
    .map(ability => EntityStartsCasting(GameAction.Id.dummy, startTime, ability.castingTime, ability))

useAbility(
  Vector(maybeUseTwinDebuffs, /* … */ me.maybeAutoAttack(…).map(_.toStartCasting(startTime))),
  maybeChangeTarget,
  maybeMove
)
```

`canUseAbilityBoolean` already checks ownership, cooldown, resource cost and silence — you rarely
need more. `UseId.dummy` / `GameAction.Id.dummy` are placeholders; the game master assigns real ids.

### Recipe B — A buff / debuff

`shared/src/main/scala/gamelogic/buffs/boss/bossXxx/`. Pick a base:

| Base trait | Use for |
|---|---|
| `TickerBuff` | periodic effect (damage over time, heal over time). Implement `tickRate`, `tickEffect`, `changeLastTickTime`. |
| `PassiveBuff` | a permanent marker or an action-rewriter. Implement `actionTransformer`. |
| `SilencingBuff` / `ActionPreventerBuff` | disabling effects. |

Every buff needs:
- `resourceIdentifier` — a `Buff.nextId()` value from
  [`Buff.scala`](shared/src/main/scala/gamelogic/buffs/Buff.scala)'s companion. **This is what picks
  the icon in the client** (`Sprites.buffIcons`).
- `duration` — ms, or `-1` for "never expires on its own".
- `endingAction(gameState, time, maybeDispelledBy)` — what happens when it falls off.
  `maybeDispelledBy` is `Some(playerId)` when a player dispelled it, `None` on natural expiry. This
  is where dispel-mechanics live:

```scala
override def endingAction(gameState: GameState, time: Long, maybeDispelledBy: Option[Entity.Id])(
    using IdGeneratorContainer
): Vector[GameAction] =
  val circles = gameState.allTEntities[DebuffCircle].values.filter(_.colour == colour)
  (for {
    dispellerId <- maybeDispelledBy
    dispeller   <- gameState.players.get(dispellerId)
    if !circles.exists(circle => dispeller.collides(circle, time))   // dispelled from the wrong place
  } yield EntityTakesDamage(genActionId(), time, dispellerId, 90.0, bossId)).toVector ++
    circles.map(circle => RemoveEntity(genActionId(), time, circle.id))
```

- `canBeDispelled = true` if a Pentagon's `PentaDispel` should be able to remove it.
- `bearerDiedAction` — defaults to `endingAction`; **override it** for death-triggered mechanics.
  `BigGuyCurse` uses exactly this to drop a `BigGuyDeathMark` where the add died. It is invoked by
  [`ManageDeadAIs`](shared/src/main/scala/gamelogic/gamestate/serveractions/ManageDeadAIs.scala) on a
  game state where the bearer is *still present* — so you can read its position.

Then: `Buff.nextId()` entry + icon + `Sprites.buffIcons` (§3).

### Recipe C — A passive entity (ground marker, zone, circle)

The simplest possible entity: extend `Body`.

```scala
final case class DebuffCircle(id: Entity.Id, time: Long, pos: Complex, colour: RGBColour)
    extends Body {
  override def rotation: Angle = 0
  override def shape: Shape    = DebuffCircle.shape
  override def teamId: TeamId  = Entity.teams.mobTeam
}

object DebuffCircle {
  inline def radius: Double = Constants.playerRadius * 3
  val shape = Circle(radius)
}
```

Needs **no** pickler registration (entities travel as part of actions, never on their own). Without
visuals, the client draws it as a plain grey outline: give it its look in your `BossVisuals`
(Recipe E). Add it to the world with `WithEntity(...)` in an action; remove it
with a `RemoveEntity` action.

### Recipe C bis — An entity as the state of a multi-stage mechanic

When a mechanic happens in several beats (a zone that becomes safe, a debuff that flips, an attack
that resolves twice), do **not** try to remember the beat in the AI controller. Put it in an entity
and let everyone read it. The controller stays stateless, the client can draw the current beat,
the friendly bots can react to it, and a client that joins mid-fight sees the truth.

`DeathMarkTriangle` is the worked example: it *is* the zone (a `Body`, so `containsPoint` does the
geometry) **and** it is the state machine of Boss104's two-stage finisher.

```scala
final case class DeathMarkTriangle(id, time, pos, relativeVertices: Vector[Complex], stage: Int)
    extends Body {
  override val shape: ConvexPolygon = ConvexPolygon(relativeVertices)
  def insideIsDeadly: Boolean       = stage == DeathMarkTriangle.firstStage
  def nextStage: DeathMarkTriangle  = copy(stage = stage + 1)
}
```

Advancing the state is a tiny action. Because the entity is already identical on every client, the
transformer may read it from the game state and still be pure:

```scala
override def createGameStateTransformer(gameState: GameState): GameStateTransformer =
  gameState.entityByIdAs[DeathMarkTriangle](triangleId)
    .fold(GameStateTransformer.identityTransformer)(t => WithEntity(t.nextStage, time))
```

`WithEntity` overwrites by id, so this is how you update *any* entity in place.

Three things to watch:
- **`ConvexPolygon` expects positively oriented vertices** and stores them relative to the entity's
  `pos` (the convention every `Body` follows). If you build one from positions gathered at runtime,
  re-centre on the centroid and `sortBy(_.arg)` — otherwise `contains` silently answers nonsense.
  Put that in a companion builder so the invariant cannot be broken.
- **Keep the fields picklable.** Anything reaching an action's constructor must have a boopickle
  `Pickler`. `Complex`, `Vector[Complex]`, `RGBColour`, `Int`, `Long` all do; a bespoke enum
  probably doesn't. A plain `Int` plus named constants is worth the small ugliness.
- **Gate the ability on the entity instead of on a cooldown**: `canBeCast` returning
  `Option.unless(gameState.allTEntities[TheEntity].nonEmpty)("...")` makes the ability exist only
  while the mechanic does, and its cooldown then means "delay between beats".

### Recipe D — An add (a mob with a brain)

Mix in exactly the capabilities it needs:

| Trait | Grants |
|---|---|
| `MovingBody` | position + movement (`move`) |
| `LivingEntity` | life, damage, death (`maxLife`, `patchLifeTotal`) |
| `WithThreat` | threat table — drives who it attacks |
| `WithTarget` | a current target |
| `WithAbilities` | can cast (`abilities`, `useAbility`, `relevantUsedAbilities`) |

`BigGuy` mixes all five; start from
[`BigGuy.scala`](shared/src/main/scala/gamelogic/entities/boss/boss104/BigGuy.scala) (or
`boss102/BossHound.scala`) and change only the three real decisions: **`maxLife`**, **`shape`**
(adds are traditionally triangles — the lowest rank in Flatland's lore — with a per-add radius), and
**`abilities`**.

Its **spawn action** wires everything at once:

```scala
override def createGameStateTransformer(gameState: GameState): GameStateTransformer =
  WithEntity(BigGuy(entityId, time, position, …, relevantUsedAbilities = Map(
    Ability.bossXxxBigGuyKick -> Pointed[BigGuyKick].unit.copy(
      time = time - BigGuyKick.cooldown + BigGuyKick.timeToFirstUse
    )
  )), time) ++
    WithBuff(DamageThreatAware(threatAwareBuffId, entityId, entityId, time)) ++  // ← needs this to
    WithBuff(BigGuyCurse(curseBuffId, entityId, entityId, time))                 //   build a threat table
```

> `DamageThreatAware` is what makes threat accumulate. Without it the add never picks a sensible
> target. For the boss itself, `BossFactory.healAndDamageAwareActions` does the equivalent.

Its **controller** goes in `game-server/.../ai/boss/bossXxxunits/`, extends
`AIController[BigGuy, AddBigGuy]`, and is registered in `AIManager` by matching on the spawn action:

```scala
case action: AddBigGuy =>
  aiControllers.addOne(action.entityId -> boss.bossXxxunits.BigGuyController())
```

**Do not forget** to add `math.round(BigGuy.shape.radius).toInt` to `AIManager`'s `GraphManager`
radii list.

### Recipe E — The visuals

A boss' visuals implement [`BossVisuals`](love-client/src/main/scala/bffclient/render/bosses/BossVisuals.scala):
`ground` draws under the bodies (zones, auras, markers), `adds` draws the adds, and `draws` lists the
entities it draws (the renderer draws the other ones as grey outlines). They draw in world
coordinates through the [`Painter`](love-client/src/main/scala/bffclient/render/Painter.scala)
(`circle`, `polygon`, `line`, `shape`, and `position(entity)`, the position as shown on screen).
From [`Boss104Visuals`](love-client/src/main/scala/bffclient/render/bosses/Boss104Visuals.scala):

```scala
object BossXxxVisuals extends BossVisuals {
  val bossName: String = BossXxx.name

  def draws(entity: Entity): Boolean = entity match {
    case _: BigGuy | _: DebuffCircle => true
    case _                           => false
  }

  def ground(p: Painter): Unit = {
    import p.*
    for debuffCircle <- gameState.allTEntities[DebuffCircle].values do
      circle(debuffCircle.pos, debuffCircle.shape.radius, Some(paint(debuffCircle.colour, 0.5)))
  }

  def adds(p: Painter): Unit =
    for bigGuy <- p.gameState.allTEntities[BigGuy].values do {
      p.shape(bigGuy, Some(addColour.withAlpha(0.3)), Some((2, addColour)))
      withBars(p, bigGuy) // life bar, cast bar and buff icons
    }
}
```

Then add it to `byBossName` in the `BossVisuals` companion. The visuals are chosen by boss name
(and kept once the boss died, for its adds).

The querying vocabulary you will use constantly, from
[`GameState`](shared/src/main/scala/gamelogic/gamestate/GameState.scala):

| Call | Returns |
|---|---|
| `gameState.allTEntities[T]` | `Map[Entity.Id, T]` of all entities of that type |
| `gameState.allTBuffs[T]` | all buffs of that type, anywhere |
| `gameState.entityByIdAs[T](id)` | `Option[T]` |
| `gameState.allBuffsOfEntity(id)` | every buff on one entity |
| `gameState.hasBuffOfType[T](id)` | boolean |
| `gameState.players` / `.bosses` / `.obstacles` | the obvious maps |
| `gameState.castingEntityInfo.get(id)` | cast bar info (`isCasting`, `remainingCastingTime`) |
| `entity.currentPosition(time)` | **dead-reckoned** position — always prefer this to `entity.pos` |

`withBars(p, add)` gives adds a life bar, a cast bar and their buff icons for free — use it, players
need to see a cast bar to know when to interrupt. `casting(p, id)` and `progress(info, now)` help
drawing what an entity casts (e.g. Boss105's Skewer beam).

### Recipe F — Tinting the player frames, and a HUD line

Two optional hooks of `BossVisuals`, both pure functions of the game state:

```scala
  /** The twin debuffs must be dispelled from inside the circle of their colour. */
  override def playerFrameTint(gameState: GameState, playerId: Entity.Id): Option[(Paint, Double)] =
    gameState.allBuffsOfEntity(playerId).collectFirst { case debuff: TwinDebuff =>
      (paint(debuff.colour), 0.45) // the colour of the contour, and the opacity of the background
    }

  override def hudText(gameState: GameState): Option[String] =
    Some(s"Hounds: ${gameState.allTEntities[BossHound].size}")
```

If a mechanic needs to alter a part of the HUD that has no hook yet, add one to `BossVisuals`
(doing nothing by default) and call it from [`Hud`](love-client/src/main/scala/bffclient/hud/Hud.scala).

---

## 6. Hard rules

1. **Never** put randomness, `System.currentTimeMillis()`, or map-iteration-order dependence in
   `createGameStateTransformer` or `isLegal`. Freeze decisions into action fields.
2. **Never** put AI code in `shared`. It must not reach the client.
3. Positions are `gamelogic.physics.Complex`. Real = horizontal, imaginary = vertical. `Complex.polar(r, θ)`
   and `Complex.i` are your friends; `z.distanceTo(w)`, `z.arg`, `Complex.rotation(θ)` cover most needs.
4. Times are `Long` **milliseconds**. `-1` duration means "infinite".
5. Every new ability *and* every new action needs a boopickle line, or the game crashes at runtime.
6. `scalacOptions` includes **`-Werror`** — warnings fail the build. Unused imports and
   non-exhaustive matches will stop you.
7. Prefer `inline def` for constants in companions, matching the surrounding style.
8. An ability with no cooldown should still get a small one (the GCD, `Ability.gcd = 200L`).

---

## 7. Common failure modes

| Symptom | Cause |
|---|---|
| Game crashes the instant the ability/action first happens | missing `.addConcreteType[…]` in `BFFPicklers` |
| The boss' zones or adds are grey outlines | no `BossVisuals` for the boss (or not in `byBossName`), or the entity is not in its `draws` |
| Boss spawns and never moves | not registered in `AIManager` |
| Add spawns and never moves | not registered in `AIManager`, **or** its radius missing from the `GraphManager` list (look for `[warn] There is no graph for me` in the game-server output) |
| Ability never fires | not in the entity's `abilities` set, or the controller never emits `EntityStartsCasting`, or an earlier entry in the `useAbility` vector always wins |
| Ability fires immediately at pull | no `relevantUsedAbilities` seed in `initialBoss` |
| Boss dies instantly | `life`/`maxLife` left at the `Pointed` default of 0 |
| Add never attacks anybody sensible | missing `DamageThreatAware` buff at spawn |
| Buff has no icon | missing `Sprites.buffIcons` entry (or the PNG isn't in `love-client/src/main/lua/assets/...`) |
| `I don't handle boss …` printed, bots inert | missing `GoodAIManager.bossAIContainers` entry |
| Bots exist but "Add AI" is greyed out | `maybeAIComposition` still returns `None` |
| Clients desync / rubber-band | impure `createGameStateTransformer` |
| A polygon zone thinks everyone is outside (or inside) | `ConvexPolygon` built from unsorted vertices — re-centre on the centroid and `sortBy(_.arg)` |

---

## 8. Building and running

```bash
sbt sharedJVM/compile game-server/compile love-client/compile
```

Tests (game logic is deterministic and immutable, so it tests well):

```bash
sbt sharedJVM/test
```

Two test patterns are worth writing for every new boss, because they cover the two failure modes
that otherwise only show up in a live game:

1. **Story tests** — [`testutils/ActionComposer`](shared/src/test/scala/testutils/ActionComposer.scala)
   pipelines actions (`>>`), computes actions from the current state (`>>>`) and lets you assert at
   any point (`>>>>`). Extend
   [`StoryTeller`](shared/src/test/scala/gamelogic/gamestate/abilitiesstories/StoryTeller.scala) for
   the id plumbing. [`Boss104Specs`](shared/src/test/scala/gamelogic/gamestate/abilitiesstories/bosses/dawnoftime/Boss104Specs.scala)
   plays the whole death-mark-triangle mechanic this way, including a hand-rolled `killBigGuy` that
   mimics what `ManageDeadAIs` does — you can drive `bearerDiedAction` and `createActions` directly
   rather than standing up a server.
2. **Pickler round trips** — [`BFFPicklersSpecs`](shared/src/test/scala/communication/BFFPicklersSpecs.scala)
   pickles and unpickles each new ability and action. Deleting one `addConcreteType` line turns it
   from green to `IllegalArgumentException: This CompositePickler doesn't know class ...`, which is
   exactly the runtime crash it exists to prevent. Add your boss' types to it.

3. **Headless simulation** — [`SirOfViceSimulation`](game-server/src/test/scala/simulation/SirOfViceSimulation.scala)
   plays a whole encounter with the friendly bots, with the real server actions and AIs but no
   client, and prints a timeline (abilities, deaths, life totals, positions). It runs in real time,
   so it only runs on demand:

   ```bash
   BFF_SIMULATION=120 sbt "game-server/testOnly simulation.SirOfViceSimulation"
   ```

   Copy it for a new boss: it is the fastest way to check that the bots survive the mechanics.

To actually play, three processes (full details in [README.md](README.md#launching-all-the-required-programs)):

```bash
sbt game-server/assembly game-server-launcher/assembly
```

```bash
java -jar ./game-server-launcher/target/game-server-launcher.jar
```

then `server/reStart` in sbt, and the client with `sbt love-client/luaRun` (log in with the address
`127.0.0.1`).

Without a window, `tools/love-client-test.sh` runs the servers and plays a scripted game (log in,
create a game, get ready, start) against the boss of your choice, logging what the client sees every
second, then stops everything (after `sbt game-server/assembly love-client/luaLink`):

```bash
BFF_BOSS="Boss Xxx" BFF_CLASS=Square tools/love-client-test.sh
```

> ⚠️ The game-server runs from a **fat jar**. After changing anything in `shared` or `game-server`
> you must re-run `sbt game-server/assembly` before the change takes effect in a launched game.

---

## 9. Boss104 file index

The complete worked example. Boss104 has three mechanics: *twin debuffs* (coordinated Pentagon
dispels from inside matching coloured circles), *big guys* (adds the Triangle tanks, positions and
interrupts), and the *death mark triangle* — once three big guys have died, the triangle formed by
their death marks becomes a two-stage attack that first punishes standing inside it, then punishes
standing outside.

| Concern | File |
|---|---|
| Boss entity + factory | [Boss104.scala](shared/src/main/scala/gamelogic/entities/boss/dawnoftime/Boss104.scala) |
| Ability — twin debuffs | [TwinDebuffs.scala](shared/src/main/scala/gamelogic/abilities/boss/boss104/TwinDebuffs.scala) |
| Ability — spawn an add | [SpawnBigGuy.scala](shared/src/main/scala/gamelogic/abilities/boss/boss104/SpawnBigGuy.scala) |
| Ability — the add's interruptible cast | [BigGuyKick.scala](shared/src/main/scala/gamelogic/abilities/boss/boss104/BigGuyKick.scala) |
| Buff — dispel-position-sensitive DoT | [TwinDebuff.scala](shared/src/main/scala/gamelogic/buffs/boss/boss104/TwinDebuff.scala) |
| Buff — death trigger | [BigGuyCurse.scala](shared/src/main/scala/gamelogic/buffs/boss/boss104/BigGuyCurse.scala) |
| Entity — passive marker | [DebuffCircle.scala](shared/src/main/scala/gamelogic/entities/boss/boss104/DebuffCircle.scala), [BigGuyDeathMark.scala](shared/src/main/scala/gamelogic/entities/boss/boss104/BigGuyDeathMark.scala) |
| Entity — full-featured add | [BigGuy.scala](shared/src/main/scala/gamelogic/entities/boss/boss104/BigGuy.scala) |
| Ability — two-stage finisher | [DeathMarkTriangleAttack.scala](shared/src/main/scala/gamelogic/abilities/boss/boss104/DeathMarkTriangleAttack.scala) |
| Entity — zone *and* state machine | [DeathMarkTriangle.scala](shared/src/main/scala/gamelogic/entities/boss/boss104/DeathMarkTriangle.scala) |
| Actions | [PutTwinDebuff.scala](shared/src/main/scala/gamelogic/gamestate/gameactions/boss104/PutTwinDebuff.scala), [AddBigGuy.scala](shared/src/main/scala/gamelogic/gamestate/gameactions/boss104/AddBigGuy.scala), [AddBigGuyDeathMark.scala](shared/src/main/scala/gamelogic/gamestate/gameactions/boss104/AddBigGuyDeathMark.scala), [AddDeathMarkTriangle.scala](shared/src/main/scala/gamelogic/gamestate/gameactions/boss104/AddDeathMarkTriangle.scala), [DeathMarkTriangleNextStage.scala](shared/src/main/scala/gamelogic/gamestate/gameactions/boss104/DeathMarkTriangleNextStage.scala) |
| Boss AI | [Boss104Controller.scala](game-server/src/main/scala/application/ai/boss/Boss104Controller.scala) |
| Add AI | [BigGuyController.scala](game-server/src/main/scala/application/ai/boss/boss104units/BigGuyController.scala) |
| Friendly bots | [boss104/](game-server/src/main/scala/application/ai/goodais/bosses/boss104/) — see `PentagonForBoss104` (circle assignment via `index`), `TriangleForBoss104` (add tanking + interrupting) and `DeathMarkTriangleAware` (behaviour shared by all four classes) |
| Tests | [Boss104Specs.scala](shared/src/test/scala/gamelogic/gamestate/abilitiesstories/bosses/dawnoftime/Boss104Specs.scala), [BFFPicklersSpecs.scala](shared/src/test/scala/communication/BFFPicklersSpecs.scala) |
| Rendering, and the twin debuff colour on player frames | [Boss104Visuals.scala](love-client/src/main/scala/bffclient/render/bosses/Boss104Visuals.scala) |

---

### Boss105: a second worked example

`Boss105` (theme package `dawnoftime`, per-boss packages `boss105`) is designed for **two tanks**
and shows patterns that Boss104 does not:

- **An add that is part of the boss' state.** The `Shade` exists from the start
  (`initialBossActions` emits `AddShade`), and a `TwinBond` buff on the boss reduces its damage taken
  while the Shade lives. `ShadeBond.bearerDiedAction` removes the bond and starts `Mourning` on the
  boss.
- **Buffs as timers.** `Mourning` has a finite duration and its `endingAction` brings the Shade
  back: no controller has to remember anything, and a client joining mid-fight sees the truth
  (a `ReformationPoint` entity shows where). `Yearning` is a buff that changes nothing by itself: the
  Shade's *controller* reads it to decide how to move.
- **Geometry on the fly.** `Skewer` computes who is on a beam from the positions at completion time
  (`Skewer.isOnBeam`, shared by the ability and the bots; the client's visuals draw the beam from
  the cast info).
- **Dispel-as-a-choice.** `VolatileCharge.endingAction` explodes only when `maybeDispelledBy` is
  empty.
- **Bots that reason about roles.** `SquareForBoss105` picks which twin to tank from what the other
  Square does when it is not a bot (`Boss105Bot.playerClassIsBot`), `TriangleForBoss105` saves energy
  for its stun when `Reunion` is coming. `Boss105Simulation` plays a whole fight:

  ```bash
  BFF_SIMULATION=330 sbt "game-server/testOnly simulation.Boss105Simulation"
  ```

| Concern | File |
|---|---|
| Boss entity, factory, docs | [Boss105.scala](shared/src/main/scala/gamelogic/entities/boss/dawnoftime/Boss105.scala) |
| Add + marker entity | [Shade.scala](shared/src/main/scala/gamelogic/entities/boss/boss105/Shade.scala), [ReformationPoint.scala](shared/src/main/scala/gamelogic/entities/boss/boss105/ReformationPoint.scala) |
| Abilities | [abilities/boss/boss105/](shared/src/main/scala/gamelogic/abilities/boss/boss105/) |
| Buffs | [buffs/boss/boss105/](shared/src/main/scala/gamelogic/buffs/boss/boss105/) |
| Actions | [gameactions/boss105/](shared/src/main/scala/gamelogic/gamestate/gameactions/boss105/) |
| AIs | [Boss105Controller.scala](game-server/src/main/scala/application/ai/boss/Boss105Controller.scala), [ShadeController.scala](game-server/src/main/scala/application/ai/boss/boss105units/ShadeController.scala), [bots](game-server/src/main/scala/application/ai/goodais/bosses/boss105/) |
| Tests | [Boss105Specs.scala](shared/src/test/scala/gamelogic/gamestate/abilitiesstories/bosses/dawnoftime/Boss105Specs.scala), [Boss105Simulation.scala](game-server/src/test/scala/simulation/Boss105Simulation.scala) |
| Rendering | [Boss105Visuals.scala](love-client/src/main/scala/bffclient/render/bosses/Boss105Visuals.scala) |

---

## 10. Attack animations (optional polish)

For a one-shot visual flourish (a flash, a line between boss and target), add an effect in the
client's [`render/Effects.scala`](love-client/src/main/scala/bffclient/render/Effects.scala). Each
action received from the server goes through its `received` method once (even when the server sends
it again):

```scala
case UseAbility(_, time, casterId, _, _: CleansingNova) => ??? // an instance of a new `Effect`
```

An `Effect` is a small case class saying when it started, how long it lasts, and what it needs to be
drawn; its `draw` clause draws it with `love.graphics` at the screen position of the entities.
Effects are removed after their duration, and do not handle timeline rollback, which is currently
acceptable. This is cosmetic — do it last, if at all.
