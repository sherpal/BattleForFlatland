# Battle For Flatland

## Game controls

The following default controls are coded (you need an AZERTY keyboard).

- zqsd for moving
- keys 1, 2, 3,... for using ability, from left to right as their symbols appear at the bottom left of the game.
- keys rfvtgb to put markers on target, and shift these to put fixed markers at the mouse position.

You can easily change them with the "Controls" button (in the list of games, and when you join a game), at which point they are saved in the client's `controls.json` (in LÖVE's save directory, e.g. `~/.local/share/love/battle-for-flatland/` on Linux), and remembered from then on.

## History was made on June, 3rd, 2020

The first games of BFF were made on the 3rd of June, 2020. 
The first boss "Boss101" was defeated at 22:12 CEST in presence of

- Justin Dekeyser, Square
- Antoine Doeraene, Triangle
- Sébastien Doeraene, Hexagon
- Souad Lepoivre, Pentagon
- Nicolas Radu, Hexagon

## History was made (again) on January, 15, 2025

After the complete overhaul of the implementation, switching from the old tech stack to the new one, the boss "Boss102" was defeated in presence of

- Rémi, Pentagon
- Sébastien, Hexagon
- Damien, Pentagon
- Florent, Square
- Antoine, Pentagon

This closes the renewal chapter of the game.

## Run locally

You want to play locally with your friends? 
Or perhaps contribute?
That's great!
We'll walk you through the steps to make it happen.

In order to proceed, make sure you have sbt and [LÖVE](https://love2d.org) (11.5, for the `love` command) installed on your machine.

The game's client is a LÖVE game written in Scala, in the `love-client` sub-project (see [its README](love-client/README.md)), compiled to Lua by the `lua-linker` sub-project (see [its README](lua-linker/README.md)).

### Set up the game server launcher

While in development, we have a "game-server-launcher" to launch the game servers when a game is launched. 
This is a kind of a "mock up" for a more robust setup, involving, e.g., an Azure gaming service.

The `game-server-launcher` sub-project is a cask server dedicated to launch games servers on demand.
This project should basically not change (or very few) and hence, even in dev, we package it as a fat jar and launch that.

Run

```
sbt game-server-launcher/assembly
```

Then run, from the root of the project (the launcher looks for the game server jar relatively to it)

```
java -jar .\game-server-launcher\target\game-server-launcher.jar
```

### Compile the game server

The game-server-launcher will launch the fat game-server jar. You can obtained the latter with

```
sbt game-server/assembly
```

The jar is written to `game-server/target/game-server.jar` (whatever the Scala version), which is where the launcher picks it up.
Since games run from that jar, **you need to run this command again after any change in the `shared` or `game-server` sub-projects**, otherwise the games you launch will not see your changes.

### Launching all the required programs

There are three programs that must be ran in dev mode:

- `server/reStart` in `sbt`: runs the backend (port 9000) (If you want to play with other people, you should use `sbt "server/reStart -DisProd=true"` instead).
- `java -jar .\game-server-launcher\target\game-server-launcher.jar`: runs the game-server-launcher (after you compiled it once with `sbt game-server-launcher/assembly`)
- `sbt love-client/luaRun`: runs the client, in which you log in by typing the address of the backend (`127.0.0.1` locally).

### Playing with friends

`sbt love-client/luaPackage` packages the client for Windows, macOS and Linux (in `love-client/target/dist`), with LÖVE inside, so that your friends do not need to install anything.
They then type the address of your backend on the login screen.
See [the client's README](love-client/README.md#playing-with-friends) for the details (and how to host the game at home).

To package the backend itself (only its API: the client is the LÖVE one), `sbt packageApplication` writes the fat jar `dist/app.jar`.

## Contribute (in construction)

Below, we try to describe how you can contribute in parts of the developments.
Certain things will not be acceptable (such as "I rewrote the client in another engine") so be sure to always first raise an issue.

Before going further, be sure to be able to run the server and the game locally.

### Create a new Boss

> **If you are an AI agent (or simply want a condensed, task-oriented reference rather than the
> diary below), read [make-a-boss.md](make-a-boss.md) instead.** It covers the same ground in a
> structured way: engine model, the list of places where things must be registered, phase-by-phase
> procedure with acceptance checks, code recipes, and a file index of the Boss104 reference
> implementation.

One of the easiest way to contribute to the repo is probably to implement a new Boss for the game.
Implementing a new boss is completely orthogonal to the rest of the code, and hence, there are no real consequence of doing things wrong, if possible.

Writing a new boss is rather straightforward and the most difficult part will perhaps be to design and fine tune it.

I'm ~~writing~~ editting this guide while implementing Boss104.
It is not guaranteed that the following is the optimal strategy, but, if Boss104 comes to light, it is at least working.

#### Package and Boss class

The class representing the boss itself should live in a subpackage of the `gamelogic.entities.boss` package (in the `shared` sub-project).
This subpackage should have a name corresponding to a theme for a group of boss.
For example, if you wanted to reproduce in Battle For Flatland a raid coming from your favourite MMO, this package could be named after that raid, in a parodic manner!
The Boss104 will sit in the `dawnoftime` package, gathering bosses that where made at the beginning of development and who mostly served as proof of concepts (although we tried to make them interesting, still).
The `firstraid` package follows the raid idea: it gathers the bosses of the first raid of the game, starting with Sir of Vice.

The boss class should extend `gamelogic.entities.boss.BossEntity` and have a companion object extending `gamelogic.entities.boss.BossFactory[T] with gamelogic.docs.BossMetadata` with `T` the class of your boss.
It is also convenient to make it a `case class` for copy method.

#### Implementing abstract members for boss and boss factory classes.

Let your IDE fill the gaps for the abstract methods, and implement them one by one.
They should either be straightforward to implement (sometimes they can even be copy-pasted from previous bosses), or they can not be implemented right away (for example the `abilityNames` method), but we will come back to them later on.

Note that many abstract member should be filled in the constructor (another reason to be a case class) and many methods ask to return the super trait type.
For those particular methods, it is best to put the return type to the type of the boss.

After filling the blanks, only a few members are still not implemented (`???`):

- `abilities` and `abilityNames` in the boss class itself
- `initialBoss` and `stagingBossActions` in the factory

#### Filling the `stagingBossActions` method

The previous actions where made in "auto-pilot" mode, and could even be done automatically via an `sbt` command.
Now is the time to begin implementing stuff for your boss specifically.

The main goal of the `stagingBossActions` method is to setup the topology of the room.
That is, create all the actions to put obstacles into the game.
In the case of the Boss104, the room will be a simple big square to simply limit the space of the game.

#### Seeing the staging in action

We can already test that the walls of the boss gets spawned at the beginning of the game.

To that end:

- in the companion object, set a value for `playersStartingPosition` (0 is probably ok at first)
- fill the `abilities` and `abilityNames` section with empty collections
- fill the `initialBossActions` to be `healAndDamageAwareActions(entityId, time)`
- fill the `initialBoss` with the `unit` of the `Pointed` type class of your boss, changing at least the `id`, the `life` and `maxLife` (so that the boss doesn't die instantly)
Then add your `BossFactory` instance to the `factoriesByBossName` of the `BossFactory` companion object, so that the boss will be available to the `GameMaster` and to the client for users to select it.
(The client draws a boss without any specific visuals until you give it some, see below.)

You can launch the game and you should see all your obstacles. Clicking on "start game" will make the boss appear. However, since we didn't code any AI for it yet, il will stay put (and you can freely kill it).

#### Making the boss move

In order to make the boss move, we need to create an AI controller for it, and register that controller in the AIManager.
In order to have an AI that simply moves, we can take the code from, for example, the `Boss102Controller` and copy paste it, being sure to

- change all occurences to "Boss102" (in all its forms) into "Boss104"
- remove all the decisions of Boss102 involving abilities

Then we can add the following lines to the `AIManager`:

```scala
case action: SpawnBoss if action.bossName == Boss104.name =>
  aiControllers.addOne(action.entityId -> boss.Boss104Controller)
```

Note that this way of doing could change in the future.
In that case, I will hopefully not forget to change this doc.

Note that we are here using the pathfinding algorithm to make the boss move (via `aiMovementToTargetWithGraph`).
The `AIController` receives a `gamelogic.physics.pathfinding.Graph` (a `VisibilityGraph`, built from the obstacles of the room for the radius of the entity) which finds the shortest path around obstacles, and is cheap enough to be asked at every frame.
The boss only considers itself in range of its target when nothing stands in between.
You can chose not to use it, and instead (for example) go in straight line to target with `aiMovementToTarget`, but that only works if the room is convex.
For rooms with pillars or walls in the middle (like the one of Sir of Vice), the graph is what makes the boss walk around them.

#### Adding the first ability

Now that the boss moves towards its target, it is time to make it attack.
As many other bosses in the game, Boss 104 will have a small "auto-attack".
Usually the goal of the auto-attack is to keep healers busy in quiet phases, and give the tank some rage.
Of course, for your own bosses, you can opt out of an auto-attack, or to have a "default" attack that the boss does when its has nothing else to do, and which could be a range attack (like casting a ball in a random direction, whatever pleases you).

Adding the auto-attack to the boss is straightforward since it is already implemented.
You simply need to add the `Ability.autoAttackId` to `abilities` member and `Ability.autoAttackId -> "Auto attack"` to the `abilityNames` member.
Alternatively, you could implement the `abilities` method as the set of keys of the `abilityNames` Map.
The `abilityNames` map is used by the client to display the names of the attacks in the UI (e.g. the boss's cooldowns).

What happens now?
Well, the `abilities` member is the list of all abilities that the entity is allowed to use.
By adding the auto-attack id, we inform the game that this entity (the Boss 104) can indeed use the auto-attack ability.
Are we done, then?
Can we launch the game and see it in action?
Not quite, because we only define the legality of the action, we didn't learn (or tell) the AI Boss 104 controller to actually use it.
In order to do that, it's convenient to set a method `maybeAutoAttack` taking as input the current time and the current game state, and (maybe) returning the auto attack that can happen in that case.
A possible implementation is as follows:

```scala
def maybeAutoAttack(time: Long, gameState: GameState): Option[AutoAttack] =
  Some(
    AutoAttack(
      0L,
      time,
      id,
      targetId,
      Boss104.autoAttackDamage,
      Boss104.autoAttackTickRate,
      NoResource,
      Boss104.meleeRange
    )
  ).filter(_.canBeCast(gameState, time)).filter(canUseAbility(_, time))
```

As you can guess, the members of `Boss104` that we are using need to be defined.

We thus now go to the `Boss104Controller.scala`.
Previously, the potential actions that the boss need to take where defined using the line

```scala
Vector(maybeChangeTarget, maybeMove).flatten
```

We need to take into account that the AI could use its auto-attack ability.
The `AIController` trait has a utility method `useAbility` to do just that:

```scala
useAbility(
  Vector(
    me.maybeAutoAttack(startTime, currentGameState)
      .map(ability =>
        EntityStartsCasting(GameAction.Id.dummy, startTime, ability.castingTime, ability)
      )
  ),
  maybeChangeTarget,
  maybeMove
)
```

The first argument specifies all the attacks to try, in order.
In this case, there is currently only one attack.
That means that if `maybeAutoAttack` returns something defined, the boss is going to use its ability.
Otherwise it will do as before (maybe change target and maybe move towards its destination).

You may now launch the game, and you'll see that the boss, when in range, is going to attack you.
You can also see on the (currently) top right of your screen that the "cooldown" (aka the time before the ability is usable again) will be properly displayed as a status bar.

This is about as involved as the AI in Battle for Flatland are going to get.
You can of course go crazy and implement very complex AIs, with behaviour changing depending on their opponents, but most of the time it will be that: defining abilities and checking in order whether the boss can use it.
Speaking of defining abilities, let us defined our first ability specifically for Boss 104.

#### Twin Debuffs

The first ability that we are going to implement will require some coordination between and reactivity from the two Pentagon Players.
The Boss will put two debuffs (that is, a "curse" on a player that has an negative effect) of different colours on two different players, and two circles on the ground of the same two colours.
These debuffs will deal a certain amount of damage every second to the bearer.
The Pentagons will need to "dispell" them (remove them thanks to their dedicated ability), but they will need to do so on from within the corresponding circle, otherwise they will take a huge chunk of damage.

The ways it will play out is thus the following.
The debuffs and the circle appears.
Each Pentagon must determine which is closest to them, and go inside.
Then, they must use their dispell ability on the player with the corresponding debuff.

Let's see how we can implement that.

We need to

- implement the `gamelogic.abilities.Ability` representing the game
- add its `gamelogic.abilities.Ability.AbilityId` to the list of abilities that the boss have
- define in `Boss104.scala` how much time before the first use of that ability (could be instantly, but usually we let players "warm up", just like a JVM, before going to business).
- tell the `Boss104Controller.scala` to use it when it is legal
- on top of that, because this ability involves debuffs and entities (the circles on the ground), we will need to implement them as well

##### Implement the ability

First, let us create a package `boss104` inside `gamelogic.abilities.boss`.
Then, we create a new case class, `TwinDebuffs` extending `gamelogic.abilities.Ability`.
Most the required methods can be readily implemented.

We need to implement a bunch of stuff left abstract by the `Ability` trait.
The `useId`, `time` and `casterId` should be taken as constructor arguments.
The `cost` ability will simply be 0 of `NoResource`, and the `copyWithNewTimeAndId` method is implemented using the `copy` method acquired by being a case class.
The `abilityId` member is a unique (across the application) Int identifier for the ability.
In order to define it, we simply add a `boss104TwinDebuffs: AbilityId = nextAbilityId()` to the companion object of the `Ability` trait.

The `cooldown` and `castingTime` member are constant that can for example be defined in the companion object of the `TwinDebuffs` class. Note that, in some circomstances, these values could also be defined in the constructor arguments. It could make sense to do that if the cooldown or the casting time depend on the status of the game when the ability is used. These times must be defined as a `Long` in milliseconds, and in this case will respectively be 20000L and 1000L (subject to change when testing the boss!).

The `canBeCast` method checks whether the caster is legally authorized to use the ability at the given time. We are simply implement it by returning `None`, as the actual validity will be taken care of by the `Boss104Controller`.

Now the pièce de résistance is the implementation of the `createActions` method.
This method is the most important, as it determines exactly what happens when the boss uses the ability.
It will create a debuff on two different players, with two different colours, and place the corresponding circles.
This method will be called by the `GameMaster.scala` and thus rely on the fact that it is always right.
In particular, we do not need to check legality of actions, and we can use random effects in there.
Indeed, when the ability finished being cast, the game master creates the abilities with that method and send them as is to all players and AIs.

Before being in a position to implement the function, we need to create the debuff class and the circle entity that will be created.
Let's go ahead and do that.


1. **The DebuffCircle entity.**
   The `DebuffCircle` entity is a circle with a position.
   We can thus create a `DebuffCircle` class in the gamelogic.entities.boss.boss104` package, extending `Body`.
   
   The only "extra" thing that it has (that is, more than the abstract `Body` members), is a colour.
2. **The TwinDebuff debuff.**
   The `TwinDebuff` is created in the `gamelogic.buffs.boss.boss104` package and extends `TickerBuff`.
   The `tickEffect` will deal constant damage to the bearer (every second, for example).
   The `endingAction` method will check whether it was dispelled and, if so, check whether the bearer is indeed in the circle of the proper colour.
   Otherwise, they take 90 damage.
3. **The PutTwinDebuff.**
   With these two things, we can create a `PutTwinDebuff` action that will be generated from using the ability.
   Note that the `createGameStateTransformer` implementation has to be pure (no side effect!).
   Therefore, we put all the required information (who receives the debuff, where the `DebuffCircle` is placed, what colour) as members of the action, so that we can make the game state transformer in a deterministic manner.

We can now create the actions in the ability:

```scala
override def createActions(
    gameState: GameState
)(using IdGeneratorContainer): Vector[GameAction] = {
  val colours = Random.shuffle(TwinDebuffs.possibleColours).take(2)
  val colour1 = colours(0)
  val colour2 = colours(1)

  val chosenPlayers = Random.shuffle(gameState.players.values.toVector).take(2)
  def makeTwinDebuff(player: PlayerClass, colour: RGBColour): PutTwinDebuff = {
    val size     = Boss104.size * 0.8
    val position = Complex(Random.between(-size, size), Random.between(-size, size))

    PutTwinDebuff(
      genActionId(),
      time,
      genBuffId(),
      player.id,
      casterId,
      colour,
      genEntityId(),
      position
    )
  }
  Vector(
    chosenPlayers.headOption.map(makeTwinDebuff(_, colour1)),
    chosenPlayers.lastOption.map(makeTwinDebuff(_, colour2))
  ).flatten
}
```

(The member `time` comes from the action and will be fed by the game master as the time at which the ability finished being cast.)

The last tiny bit of stuff that we need to do, without which the game will crash, is to inform the boopickle pickler that this class exists. You do that by adding the line

```scala
.addConcreteType[boss104.PutTwinDebuff]
// [...]
.addConcreteType[boss.boss104.TwinDebuffs]
```

to the `communication.BFFPicklers` object.
This has to be done for any ability or action created.

The gamelogic now has knowledge of this ability and the surrounding debuffs and entities.
It is already a big chunk, but there are still "configuration" issues that need to be addressed, among which make the boss actually use the ability, and make the game UI to reflect on that ability.

##### Adding the ability id and time before first use

This step takes no time. Simply update the `abilities` and `abilityNames` member of the `Boss104` class and we are done for adding the ability.

Then, in order to set a time before first use, we need to change the value returned by the `initialBoss` method in the companion object of `Boss104`. The trick is to add this ability to the map of `relevantUsedAbilities`, with a time before the beginning of the game that will take into account the cooldown of the ability. Here is an example:

```scala
relevantUsedAbilities = Map(
  Ability.boss104TwinDebuffs -> Pointed[TwinDebuffs].unit.copy(
    time = time - TwinDebuffs.cooldown + TwinDebuffs.timeToFirstUse
  )
)
```

##### Making the Boss 104 controller use it

This is litteraly seven lines of codes.
The four six maybe define the action of starting casting the ability:

```scala
val maybeUseTwinDebuffs =
  Some(TwinDebuffs(UseId.dummy, startTime, me.id))
    .filter(me.canUseAbilityBoolean(_, startTime))
    .map(ability =>
      EntityStartsCasting(GameAction.Id.dummy, startTime, ability.castingTime, ability)
    )
```

and the seventh is to add it to the queue of possible abilities to use, by adding `maybeUseTwinDebuffs` to the list passed as argument to the `useAbility` method.
Usually abilities with longer cooldowns get higher priority, so we put it first on the list (before the auto-attack, that is).

And that's it! Now the `Boss104Controller` will cast the twin debuffs whenever it can.
the `canUseAbility` method takes care of checking that the cooldown since last ability is passed.

You can now launch a game (preferably with a healer to heal the auto-attacks) and you'll see that after 10s, the boss will cast its first twin debuffs.

When we say "see", you will actually not see anything...
Because we didn't adapt the UI to show all these things (the debuff and the circle).
Let us do that now.

##### Seeing the effect of the ability

In order for us to see that the boss uses the ability, we need to do three things:

1. give an image for the debuff, so that it is displayed in the debuff section of player frames
2. display *something* where the boss puts the Debuff Circles
3. Have some indication of the colour of the debuffs on the player.

The first thing is one by adding a (32x32) image in the `love-client/src/main/lua/assets/boss/dawn-of-time/boss104`.
Let's call this image `twin-debuff.png`.
Then, in the client's `render/Sprites.scala`, we add it to the `buffIcons` map (`Buff.boss104TwinDebuff -> "boss/dawn-of-time/boss104/twin-debuff"`), so that it can be used by the UI to display it.
Creating the image is (to me) the hardest thing, but fortunately these days we can ask ChatGPT to do it for us.

For the second and third points, we create, in the client's `render.bosses` package, a `Boss104Visuals` object (extending `BossVisuals`) that will collect everything that needs to be displayed when we play with that boss.
Then, we add it to the visuals chosen by boss name, in the `BossVisuals` companion object:

```diff
 private val byBossName: Map[String, BossVisuals] =
-  Seq(Boss102Visuals, Boss105Visuals, SirOfViceVisuals).map(v => v.bossName -> v).toMap
+  Seq(Boss102Visuals, Boss104Visuals, Boss105Visuals, SirOfViceVisuals).map(v => v.bossName -> v).toMap
```

And in there, we draw a basic coloured circle (in `ground`) at the places where a Circle Debuff is placed, and one as well beneath affected players, and we tint the frames of the affected players (in `playerFrameTint`).

#### Testing by making AIs

At this point, we already have a very functional boss that you could try to beat with friends.
But before gathering your party, it would be nice to thoroughly test the boss, to see that the twin debuff works from start to end, and that the boss is actually killable.

Of course it's not possible to launch five different clients and play yourself the five players.
That's why the game engine allow you to define friendly AIs that can play the game with you (or for you).

Small word of caution: these AIs will be **much** better than actual players.
They will react faster, do stuff with infinite precisions and will make no mistake.
Therefore, AIs are not a good thing to fine tune the boss, more than "if even they can't do it, then it's too hard".

##### Defining that the boss has AI

The first thing to do is to define what will be the AI team composition for the boss.
This is done by giving an implementation for the `maybeAIComposition` method of `Boss104`.
Up to now, it returned `None` which indicates "this boss has no AI implementation" (the UI then forbids the players to add AIs).
We then change the implementation to return a list containing 1 square, 1 hexagon, 1 triangle and 2 pentagons.

If you now go to create a game and select the Boss 104, you will see that the "Add AI" and "Fill with AIs" buttons are enabled.
If you click on them, you will see that fellow AIs will be added to join you.
Of course, if you launch the game like that, they won't do anything since we did not implement them yet.

##### Implementation the AI for the Square

This is not the most difficult boss for the Square, as the boss itself will be rather static.
Which makes it an excellent starting point for this "tutorial".

We create a `SquareForBoss104` in package `application.ai.goodais.bosses.boss104` (in the `game-server` project) inheriting from `SquareAIController`.
This class request an `index: Int` in the constructor, and has to implement the `entityId: Entity.Id`.

The index is used to identify precisely which AI of that class this instance controls.
It is only useful if there are several times the same class in the AI configuration (this always happens for bosses requiring at least 5 players, due to the [pigeonhole principle](https://en.wikipedia.org/wiki/Pigeonhole_principle)).
For example, the index 0 could go to the left and the index 1 could go to the right.

The `entityId` is used by the AI to know exactly which entity it controls, and send messages to the game master accordingly.

Coding the AI actually requires you to implement the `takeActions` method.
It is similar as for the one for the boss.
It returns a list of actions that the AI takes at this instant, given the current game state.

> A note about movement: the `preGameMovement` method used by most bots walks in a straight line. If the room of your boss has obstacles in the middle, use `maybeMoveTowards` instead, which goes around them thanks to the graph given to `takeActions`. It also tells you when the bot has arrived and stopped, which is when it can start casting (moving interrupts casts). The bots of Sir of Vice are built that way.

> A note about state: ideally everything should be pure here, but you can be assured that for a given entity id (and index), there will be only one instance created when the game starts and staying alive until the end of the game. Moreover, it is single thread and, as such, you can put state in variables within the class if you so desire.

Since this is a boss where the tank does not have a tremendously exciting job, we can re-use the code we had for `SquareForBoss101`.

##### Implementation the AI for the Pentagon

For the Pentagons, things become a bit more interesting.
When the twin debuff zones are not there, both pentagon can go back to around the boss and send bullets to it.
Once the twin debuff zones appear, each Pentagon has to go to one of the circle, and debuff the corresponding player.

Again, this is implemented in the `application.ai.goodais.bosses.boss104` package, alongside the Square.

#### Another ability

For the sake of completeness, let's move on to the second ability.
This ability will be interesting because it will involve the creation of "adds".
And "add" (short for additional unit) is some kind of "sidekick" that the boss will summon to help itself.

This will involve

- creating a new `Entity`
- make a small Bot implementation for that `Entity`
- and further down the line, implement a more complex behaviour for the friendly Triangle Bot.

##### First, the new Entity

When the boss uses that other ability, it will spawn a new entity, that we will call `BigGuy`.
In future bosses, maybe we will use cooler names, but for now that will do.
It therefore makes sense to first implement the entity, before moving on to the ability itself.

We create the entity in the `shared/src/main/scala/gamelogic/entities/boss/boss104` package, where we created the `DebuffCircle` entity earlier.
However, compared to the `DebuffCircle` which was only a `Body`, the Big Guy will be a mixture of more things, to reflect its capabilities.

It will be:
- a `MovingBody`: a body that can also move
- a `LivingEntity`: it has a life, can lose life and eventually die
- a `WithThreat`: it has some threat level for each of the players. That will be used to dictate its Bot behaviour
- a `WithTarget`: it has a target, again, that will dictate its Bot behaviour
- a `WithAbilities`: because it will be able to use abilities.

We therefore create a `case class` extending all these traits.
We are left with a bunch of methods to implement.
Fortunately, most of them can just be copy-pasted from a previous example (in that case, the `BossHound` that the Boss 102 had).

The three decisions that we have to take are:

- its `maxLife`: Intuitively, that will be around 1000, but tests will tell
- its `shape`: usually adds will be triangles, because in the lore of Battle for Flatland, these are the lowest in hierarchy. But the radius of that triangle will often change from entities to entities.
- its `abilities`: Big Guy will have two abilities. The usual auto-attack, but also a big "kick" that the Triangle will need to interrupt (often we say "cut" because it's shorter).

For the abilities, we can therefore return a `Set` containing `Ability.autoAttackId` as well as a freshly created `Ability.boss104BigGuyKick`.

That's currently it for the Big Guy.
We now need to implement two new abilities: one for the boss to spawn the entity, and one for the Big Guy to do its kick.

##### Two new abilities

Let's start by the new Boss104 ability.
You know the drill by now.
We need to

- add a new ability id (`boss104SpawnBigGuy`)
- add a new case class representing the ability (`SpawnBigGuy` in `shared/src/main/scala/gamelogic/abilities/boss/boss104`, next to the `TwinDebuffs`)
- implement what the ability has to do
- add the id to Boss104's abilities set and set its time to first use
- make the Boss104 bot use it. (in `game-server/src/main/scala/application/ai/boss/Boss104Controller.scala`)
- adapt the `Boss104Visuals` to also draw the big guys (in `adds`, in `love-client/src/main/scala/bffclient/render/bosses/Boss104Visuals.scala`)

Implementing the ability is just a matter of returning a new instance of a new action, `AddBigGuy`, that creates an `GameStateTransformer` which adds the Big Guy entity.

Now that the boss will use the ability, that the big guy will be spawned, and that it will even be drawn on the screen, we need to

- implement its bot
- implement its ability (again, doing all the shenanigans)

##### BigGuy bot programming

In a new package `boss104units` in `game-server/src/main/scala/application/ai/boss`, we are going to add a `BigGuyController`, similar to the `Boss104Controller`.

This is a matter of implementing the two abstract methods of `AIController`, and "registering" it in `AIController`.

##### Last part of that ability

Now we have the boss spawning the big guys and the big guys AI will do their work properly.
The boss will however have a last ability.
The big guys will leave a small area at the place of their death.
When three big guys will be dead, the boss will launch an attack that will first basically kill anyone inside and after some time kill anyone outside

So, the way players will need to handle that (and that will make us code the AI for the Triangle for that boss) is :

- the triangle must take the aggro on the big guy and start killing it
- move the big guy to a spot that is not too far (otherwise the area will be too big) and not to close (otherwise it will be impossible for people to stand in the area)
- finish killing the big guy (probably with the help of pentagons) at that place
- after the third big guy is dead, all players should move outside (they should do it anyway) but should be close enough to not take too much damage.

We are going to prepare for that other ability (that won't be documented here, but will be (or should be by the time you read) implemented).
We will make a debuff on the big guy such that, when it dies, we can put a new entity zone at the place of death.

#### Other abilities

We need to do that procedure once for each ability that the boss will have. Some abilities will for example require you to create entities (see, for example, the `PutDamageZones` ability of Boss102). Others will perhaps require an AI a little bit more invovled. However, the general idea stays the same.

#### Attack animation

> this section is from and old version of the guide. It's mostly correct though, even though `CleansingNova` is not referenced above: it's an ability of Sir of Vice (which, in the end, shows it with its drawer rather than with an effect).

One thing that may be missing in the case of the cleansing nova is an animation to show that it actually happened. To do that, we could for example show a line for half a second between the boss and each target.

We will implement that in what follows. Note that it will most likely have changed by the time you read this, as (hopefully) I will improve graphics in the future (or ask help from people actually qualified to do it).

Currently, the implementation "does the job", but that's about it. Also, it does not yet take into account that actions could be cancelled due to a change in the timeline. Currently this is not really an issue.

The effects of the abilities live in the client's `render/Effects.scala`. Each action received from the server goes through its `received` method once (even when the server sends it again), and the first thing to do is to add a new "match" clause there. For the `CleansingNova` ability, the match clause will be

```scala
case UseAbility(_, time, casterId, _, _: CleansingNova) => ???
```

The `???` will need to be filled with (some) instance of a new case class extending `Effect` (like the existing `FloatingText`, `CleaveFlash` and `DirectHitSwing`), which tells

- when the effect started, and how long it lasts (it is removed afterwards)
- what it needs to be drawn (e.g. the caster's id, the targets' ids)

Then, in the `draw` method, add a clause drawing it with `love.graphics` (through the `Graphics` facade), at the screen position of the entities given by the `toScreen` and `position` functions it receives.

When you implemented all of this, your effect will be triggered and removed accordingly.

## Tests

### Game logic

Testing the game logic is actually not that hard, since everything is immutable. Moreover, if all actions are known, the game is completely deterministic! The randomness that occurs during the game are on AIs' side. But once the actions have been create, they determine the game entirely.

The tests contains a class `testutils.ActionComposer` which allows you to create a pipeline of actions, and "peak" at any point in time to check that everything works as expected (by using asserts). For example, you could have one action creating an entity, following with an action killing the entity. If you look at the game state after the first action, you should witness that the entity is indeed there, and after the second action it should have disappeared. The `Boss104Specs` and `SirOfViceSpecs` tell the stories of the mechanics of these bosses that way.

Every ability and action must be registered in `communication.BFFPicklers`, otherwise the game crashes the first time they are sent. The `BFFPicklersSpecs` round trips catch a forgotten registration: add the abilities and actions of your boss to them.

All of these run with

```
sbt sharedJVM/test
```

### Simulating a whole fight

The `simulation.SirOfViceSimulation` (in the `game-server` tests) plays a whole fight against Sir of Vice with the friendly bots, with the actual game master logic and AIs but without any client, and prints what happens (abilities used, deaths, life of everyone...).
It runs in real time, so it only runs when asked to, with the number of seconds to simulate:

```
BFF_SIMULATION=120 sbt "game-server/testOnly simulation.SirOfViceSimulation"
```

It's the quickest way to check that the bots of a new boss survive its mechanics. Copy it for your boss!

## Internal

### Adding a new Service

Steps to add a new service called `MyService`:

- in the shared project, add a new package inside the package `services`, called `myservice`
- in the `myservice` package, create an `object` `MyService` with a inner `trait` called `Service`
- create a package object
- add a `type MyService = Has[MyService.Service]` into the package object
- describes the methods and members the inner `Service` trait must have
- for each method, create an accessor method inside the package object
- by convention, backend (JVM) versions start with a B (and versions for the former browser frontend started with an F).

### Adding an asset

- Add the image in the `love-client/src/main/lua/assets` folder (it is copied next to the client's `main.lua` when linking)
- Load it in the client's `render/Sprites.scala` (e.g. as a `lazy val`, or in the `buffIcons` or `abilityIcons` maps)

The client has no sounds yet (the former browser frontend had some, see the credits below).

## Credits

### Sounds

A great deal of sound effect you hear in the game are generously offered by [mixkit.co](https://mixkit.co/free-sound-effects/).

The bars that you see in the game were made by wenakiri.
