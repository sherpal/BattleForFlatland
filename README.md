# Battle For Flatland

## Game controls

The following default controls are coded (you need an AZERTY keyboard).

- zqsd for moving
- keys 1, 2, 3,... for using ability, from left to right as their symbols appear at the bottom left of the game.
- keys rfvtgb to put markers on target, and shift these to put fixed markers at the mouse position.

You can easily change them when you join a game, at which point they are saved in your browser's local storage (meaning: if you keep the same browser and don't clear data, they will be remembered forever).

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

In order to proceed, make sure you have sbt and npm installed on your machine.

### Install npm dependencies

The frontend part of the game relies on a certain amount of npm dependencies.
These must be installed via `npm ci` in the `frontend` directory.

### Set up the game server launcher

While in development, we have a "game-server-launcher" to launch the game servers when a game is launched. 
This is a kind of a "mock up" for a more robust setup, involving, e.g., an Azure gaming service.

The `game-server-launcher` sub-project is an cask server dedicated to launch games servers on demand.
This project should basically not change (or very few) and hence, even in dev, we package it as a fat jar and launch that.

Run

```
sbt game-server-launcher/assembly
```

Then run

```
java -jar .\game-server-launcher\target\scala-3.5.0\game-server-launcher.jar
```

### Compile the game server

The game-server-launcher will launch the fat game-server jar. You can obtained the latter with

```
sbt game-server/assembly
```

### Launching all the required programs

There are three programs that must be ran in dev mode:

- `sbt server/reStart`: runs the backend (port 9000) (If you want to play with other people, you should use `sbt "server/reStart -DisProd=true"` instead).
- `sbt ~frontend/fastLinkJS` and, in the `frontend` directory, `npm run dev`: runs the frontend with hot reload (port 3000) (If you want to play with other people, you should instead use the `build` alias command to build the frontend inside the backend's public directory.)
- `java -jar .\game-server-launcher\target\scala-3.5.0\game-server-launcher.jar`: runs the game-server-launcher (after you compiled it once with `sbt game-server-launcher/assembly`)

You should be redirected to `http://localhost:3000`.

## Contribute (in construction)

Below, we try to describe how you can contribute in parts of the developments. Certain things will not be acceptable (such as "I rewrote the frontend in React") so be sure to always first raise an issue.

Before going further, be sure to be able to run the server and the game locally.

### Create a new Boss

One of the easiest way to contribute to the repo is probably to implement a new Boss for the game. Implementing a new boss is completely orthogonal to the rest of the code, and hence, there are no real consequence of doing things wrong, if possible.

Writing a new boss is rather straightforward and the most difficult part will perhaps be to design and fine tune it.

I'm writing this guide while implementing Boss103. It is not guaranteed that the following is the optimal strategy, but, if Boss103 comes to light, it is at least working.

#### Package and Boss class

The class representing the boss itself should live in a subpackage of the `gamelogic.entities.boss` package. This subpackage should have a name corresponding to a theme for a group of boss. For example, if you wanted to reproduce in Battle For Flatland a raid coming from your favourite MMO, this package could be named after that raid, in a parodic manner! The Boss103 will sit in the `dawnoftime` package, gathering bosses that where made at the beginning of development and who mostly served as proof of concepts (although we tried to make them interesting, still).

The boss class should extend `gamelogic.entities.boss.BossEntity` and have a companion object extending `gamelogic.entities.boss.BossFactory[T]` with `T` the class of your boss. It is also convenient to make it a `case class` for copy method.

#### Implementing abstract members for boss and boss factory classes.

Let your IDE fill the gaps for the abstract methods, and implement them one by one. They should either be straightforward to implement (sometimes they can even be copy-pasted from previous bosses), or they can not be implemented right away (for example the `abilityNames` method), but we will come back to them later on.

Note that many abstract member should be filled in the constructor (another reason to be a case class) and many methods ask to return the super trait type. For those particular methods, it is best to put the return type to the type of the boss.

After filling the blanks, only a few members are still not implemented (`???`):

- `abilities` and `abilityNames` in the boss class itself
- `initialBoss` and `stagingBossActions` in the factory

#### Filling the `stagingBossActions` method

The previous actions where made in "auto-pilot" mode, and could even be done automatically via an `sbt` command. Now is the time to begin implementing stuff for your boss specifically.

The main goal of the `stagingBossActions` method is to setup the topology of the room. That is, create all the actions to put obstacles into the game. In the case of the Boss103, the room will have the shape of a Hexagon, with 6 triangle "pillars" placed in an inner hexagon, pointing towards the center.

#### Seeing the staging in action

We can already test that the walls of the boss gets spawned at the beginning of the game.

To that end:

- fill the `abilities` and `abilityNames` section with empty collections
- fill the `initialBoss` with the `unit` of the `Pointed` type class of your boss, changing at least the `id`, the `life` and `maxLife` (so that the boss doesn't die instantly)

Then add your `BossFactory` instance to the `factoriesByBossName` of the `BossFactory` companion object, so that the boss will be available to the `GameMaster` and to the web frontend for users to select it.

You can launch the game and you should see all your obstacles. Clicking on "start game" will make the boss appear. However, since we didn't code any AI for it yet, il will stay put (and you can freely kill it).

#### Making the boss move

In order to make the boss move, we need to create an AI controller for it, and register that controller in the AIManager. In order to have an AI that simply moves, we can take the code from, for example, the `Boss102Controller` and copy paste it, being sure to

- change all occurences to "Boss102" (in all its forms) into "Boss103"
- remove all the decisions of Boss102 involving abilities.

Then we can add the following lines to the `AIManager`:

```scala
case action: SpawnBoss if action.bossName == Boss103.name =>
  val ref = context.spawn(
    Boss103Controller.apply(
      receiverInfo.actionTranslator,
      action,
      receiverInfo.onlyObstaclesPathFinders(Constants.bossRadius)
    ),
    s"Boss103-${action.entityId}"
  )
  context.watchWith(ref, ControllerDied(ref))
  ref
```

Note that this way of doing could change in the future. In that case, I will hopefully not forget to change this doc.

Note that we are here using the pathfinding algorithm to make the boss move. You can chose not to do that, and instead (for example) go in straight line to target (if the boss domain is convex) or implement your own. In that case, you should give an implementation of the `gamelogic.physics.pathfinding.Graph` trait.

#### Adding the first ability

Now that the boss moves towards its target, it is time to make it attack. As many other bosses in the game, Boss 103 will have a small "auto-attack". Usually the goal of the auto-attack is to keep healers busy in quiet phases, and give the tank some rage. Of course, for your own bosses, you can opt in not to have an auto-attack, or to have a "default" attack that the boss does when its has nothing else to do, and which could be a range attack (like casting a ball in a random direction, whatever pleases you).

Adding the auto-attack to the boss is straightforward since it is already implemented. You simply need to that the `Ability.autoAttackId` to `abilities` member and `Ability.autoAttackId -> "Auto attack"` to the `abilityNames` member. Alternatively, you could implement the `abilities` method as the set of keys of the `abilityNames` Map. The `abilityNames` map is used by the frontend to display the names of the attacks in the UI.

What happens now? Well, the `abilities` member is the list of all abilities that the entity is allowed to use. By adding the auto-attack id, we inform the game that this entity (the Boss 103) can indeed use the auto-attack ability. Are we done, then? Can we launch the game and see it in action? Not quite, because we only define the legality of the action, we didn't learn (or tell) the AI Boss 103 controller to actually use it. In order to do that, it's convenient to set a method `maybeAutoAttack` taking as input the current time and the current game state, and (maybe) returning the auto attack that can happen in that case. A possible implementation is as follows:

```scala
def maybeAutoAttack(time: Long, gameState: GameState): Option[AutoAttack] =
  Some(
    AutoAttack(
      0L,
      time,
      id,
      targetId,
      Boss103.autoAttackDamage,
      Boss103.autoAttackTickRate,
      NoResource,
      Boss103.meleeRange
    )
  ).filter(_.canBeCast(gameState, time)).filter(canUseAbility(_, time))
```

As you can guess, the members of `Boss103` that we are using need to be defined.

We thus now go to the `Boss103Controller.scala`. Previously, the potential actions that the boss need to take where defined using the line

```scala
List(maybeChangeTarget, maybeMove).flatten
```

We need to take into account that the AI could use its auto-attack ability. The `AIController` trait has a utility method `useAbility` to do just that:

```scala
        useAbility(
          List(
            me.maybeAutoAttack(startTime, currentGameState)
              .map(ability => EntityStartsCasting(0L, startTime, ability.castingTime, ability))
          ),
          maybeChangeTarget,
          maybeMove
        )
```

The first argument specifies all the attacks to try, in order. In this case, there is currently only one attack. That means that if `maybeAutoAttack` returns something defined, the boss is going to use its ability. Otherwise it will do as before (maybe change target and maybe move towards its destination).

You may now launch the game, and you'll see that the boss, when in range, is going to attack you. You can also see on the (currently) top right of your screen that the "cooldown" (aka the time before the ability is usable again) will be properly displayed as a status bar.

This is about as involved as the AI in Battle for Flatland are going to get. You can of course go crazy and implement very complex AIs, with behaviour changing depending on their opponents, but most of the time it will be that: defining abilities and checking in order whether the boss can use it. Speaking of defining abilities, let us defined our first ability specifically for Boss 103.

#### Cleansing nova

The first ability is a classic in games like this. Regularly, the boss will cast a big ability which kills every one in sight. Remember that Boss 103's room has pillars spread in a circle. One goal of these pillars is for the players to hide against this ability.

We need to

- implement the `gamelogic.abilities.Ability` representing the game
- add its `gamelogic.abilities.Ability.AbilityId` to the list of abilities that the boss have
- define in `Boss103.scala` how much time before the first use of that ability (could be instantly, but usually we let players "warm up", just like a JVM, before going to business).
- tell the `Boss103Controller.scala` to use it when it is legal

##### Implement the ability

First, let us create a package `boss103` inside `gamelogic.abilities.boss`. Then, we create a new case class, `CleansingNova` extending `gamelogic.abilities.Ability`.

We need to implement a bunch of stuff left abstract by the `Ability` trait. The `useId`, `time` and `casterId` should be taken as constructor arguments. The `cost` ability will simply be 0 of `NoResource`, and the `copyWithNewTimeAndId` method is implemented using the `copy` method acquired by being a case class. The `abilityId` member is a unique (across the application) Int identifier for the ability. In order to define it, we simply add a `boss103CleansingNovaId: AbilityId = [...]` to the companion object of the `Ability` trait. The value of `[...]` simply depends on what is already present (we simply add 1 to the previous id). As you can see, this is a potential source of conflicts while merging branches. However, these conflicts will be extremely easy to fix.

The `cooldown` and `castingTime` member are constant that can for example be defined in the companion object of the `CleansingNova` class. Note that, in some circomstances, these values could also be defined in the constructor arguments. It could make sense to do that if the cooldown or the casting time depend on the status of the game when the ability is used. These times must be defined as a `Long` in milliseconds, and in this case will respectively be 60000L and 4000L (subject to change when testing the boss!).

The `canBeCast` method checks whether the caster is legally authorized to use the ability at the given time. We are simply implement it by returning `true`, as the actual validity will be taken care of by the `Boss103Controller`.

Now the pièce de résistance is the implementation of the `createActions` method. This method will be called by the `GameMaster.scala` and thus rely on the fact that it is always right. In particular, we do not need to check legality of actions, and we can use random effects in there. Indeed, when the ability finished being cast, the game master creates the abilities with that method and send them as is to all players and AIs. In this case, the implementation is straightforward. We filter all players to keep only those who are in sight, and we deal them 300 damages, which is enough to kill them all (unless they use some ability they may have to protect them). For reference, here it is:

```scala
def createActions(gameState: GameState)(implicit idGeneratorContainer: IdGeneratorContainer): List[GameAction] =
  gameState.players.valuesIterator
    .filter(player => gameState.areTheyInSight(casterId, player.id, time).getOrElse(false))
    .map { player =>
      EntityTakesDamage(idGeneratorContainer.gameActionIdGenerator(), time, player.id, 300.0, casterId)
    }
    .toList
```

(The member `time` comes from the action and will be fed by the game master as the time at which the ability finished being cast.)

The last tiny bit of stuff that we need to do, without which the game will crash, is to inform the boopickle pickler that this class exists. You do that by adding the line

```scala
.addConcreteType[boss.boss103.CleansingNova]
```

to the `communication.BFFPickler` object.

##### Adding the ability id and time before first use

This step takes no time. Simply update the `abilities` and `abilityNames` member of the `Boss103` class and we are done for adding the ability.

Then, in order to set a time before first use, we need to change the value returned by the `initialBoss` method in the companion object of `Boss103`. The trick is to add this ability to the map of `relevantUsedAbilities`, with a time before the beginning of the game that will take into account the cooldown of the ability. Here is an example:

```scala
relevantUsedAbilities = Map(
  Ability.boss103CleansingNovaId -> Pointed[CleansingNova].unit.copy(
    time = time - CleansingNova.cooldown + CleansingNova.timeToFirstAbility
  )
)
```

##### Making the Boss 103 controller use it

This is litteraly five lines of codes. The four first maybe define the action of starting casting the ability:

```scala
val maybeUseCleansingNova =
  Some(CleansingNova(0L, startTime, me.id))
    .filter(me.canUseAbility(_, startTime))
    .map(ability => EntityStartsCasting(0L, startTime, ability.castingTime, ability))
```

and the fifth is to add it to the queue of possible ability to use, by adding `maybeUseCleansingNova` to the list passed as argument to the `useAbility` method. Usually abilities with longer cooldowns get higher priority, so we put it first on the list (before the auto-attack, that is).

And that's it! Now the `Boss103Controller` will cast cleansing nova whenever it can. the `canUseAbility` method takes care of checking that the cooldown since last ability is passed.

You can now launch a game (preferably with a healer to heal the auto-attacks) and you'll see that:

- after 30s, the boss will cast its first cleansing nova
- if you manage to hide behind a pillar before the end of the cast, you live
- if you however stay in sight (no matter the distance), you will die.

#### Other abilities

We need to do that procedure once for each ability that the boss will have. Some abilities will for example require you to create entities (see, for example, the `PutDamageZones` ability of Boss102). Others will perhaps require an AI a little bit more invovled. However, the general idea stays the same.

#### Attack animation

One thing that may be missing in the case of the cleansing nova is an animation to show that it actually happened. To do that, we could for example show a line for half a second between the boss and each target.

We will implement that in what follows. Note that it will most likely have changed by the time you read this, as (hopefully) I will improve graphics in the future (or ask help from people actually qualified to do it).

Currently the implementation "does the job", but that's about it. Also, it does not yet take into account that actions could be cancelled due to a change in the timeline. Currently this is not really an issue.

The first thing to do is to add a new "match" clause in the `game.ui.effect.EffectsManager`. For the `CleansingNova` ability, the match clause will be

```scala
case UseAbility(_, time, casterId, _, _: CleansingNova) => ???
```

The `???` will need to be filled with (some) an instance of `game.ui.effects.GameEffect`.

To that end, we add a package `game.ui.effects.boss.boss103` and we create a class `CleansingNovaEffect` extending `GameEffect`. A `GameEffect` is a purely mutable object that is quite low level and very close to pixi.js, the drawing library used by BFF. Basically, it is asked from you to

- inform the ui when and how to destroy the effect (usually after some amount of time)
- how to update it
- how to add it to the game scene.

When you implemented all of this, your effect will be triggered and destroyed accordingly.

#### Bonus: Let's implement the "Punishment"

For the sake of having more example at our disposal in this "tutorial", let us implement the "Punishment" ability together. This ability will place a debuff (i.e., a malus for some time) on each player (regardless of where they are) that will prevent them from doing anything (stunned, technical term) for 20s. However, if they take damage, the debuff is removed. This ability will be paired with the "Sacred ground" ability (you can go check the boss description to see why).

This ability will be a good opportunity to learn how to create and add buffs into the game.

##### Add the package and create the class

We start by create a package `boss103` in `gamelogic.buffs.boss`, and a class called `Punished` inside it ("Punished" will be the name of the debuff). This buff will be a "passive" buff, hence we extend the `PassiveBuff` trait, and, as always, we need to implement the members that are left abstract.

The way passive buffs works is that they modify actions happening while they are present. In this case, the passive buff will

- replace each occurrence of the player moving or starting casting by doing nothing
- replace each occurrence of the player taking damage by itself plus the action of removing the buff

##### Implement abstract members.

In a passive buff, besides some metadata, we need to implement the `endingActions` and the `actionTransformer` methods. In the metadata, the `buffId`, `bearerId` and `apperanceTime` should be placed inside the constructor, because they depend on the state of the game when they are created.

The `resourceIdentifier` is similar to the `AbilityId`. It's a unique identifier of the buff as a buff, and it is used by the game UI to know what icon to display when an entity bears this buff. Finally, the `duration` is the time (in millisecond) that the buff will stay on the bearer. The special value `-1` means that the buff will stay forever.

##### Create actions and abilities

We need to create a `gamelogic.gamestate.GameAction` which will add the punished buff to an entity. This action will be placed in the `gamelogic.gamestate.gameactions.boss103` package, called `PutPunishedDebuff`. Then, we need to register it in the `communication.BFFPicklers` by adding a concrete type for it.

And finally, we create the corresponding ability, as above. We simply need not to forget to make the `Boss103Controller` to use the ability. In this case, we are going to it a little bit differently: instead of casting the ability as soon as it's ready, we will toss a coin each time with a rather small chance of success (remember that the AI actor runs at 30 FPS). This will require more reactivity from players since they can't know exactly when it occurs. (Remark: when you toss a coin until success, the underlying random variable is a Geometric distribution, which is the desired behaviour, and will be easily tuned to get what we want!)

##### Adding the asset for the buff

The "Punished" buff will be display in player life bars. This means that the buff needs to have an image. See the "adding an asset" section below in order to do that.

#### What about static abilities?

## Tests

### Game logic

Testing the game logic is actually not that hard, since everything is immutable. Moreover, if all actions are known, the game is completely deterministic! The randomness that occurs during the game are on AIs' side. But once the actions have been create, they determine the game entirely.

The tests contains a class `testutils.ActionComposer` which allows you to create a pipeline of actions, and "peak" at any point in time to check that everything works as expected (by using asserts). For example, you could have one action creating an entity, following with an action killing the entity. If you look at the game state after the first action, you should witness that the entity is indeed there, and after the second action it should have disappeared.

## Internal

### Adding a new Service

Steps to add a new service called `MyService`:

- in the shared project, add a new package inside the package `services`, called `myservice`
- in the `myservice` package, create an `object` `MyService` with a inner `trait` called `Service`
- create a package object
- add a `type MyService = Has[MyService.Service]` into the package object
- describes the methods and members the inner `Service` trait must have
- for each method, create an accessor method inside the package object
- by convention, live version in the frontend start with an F, and backend (JVM) version start with a B.

### Adding an asset

- Add the image in the `frontend/src/main/resources/assets` folder
- Add the `Asset` object in `frontend/src/main/scala/assets/Asset.scala`
- Add the asset reference in the `game/GameAssetLoader` loading list

### Sound assets

The game is filled with small sounds. These sounds are loaded from the backend before the game starts. The [SoundAssetLoader](frontend/src/main/scala/game/loaders/SoundAssetLoader.scala) allows one to load all sounds, and to track the progress along the way.

If a sound asset fails to load from some reason, a warning will be emitted in the console, but the game will work normally, with the missing sound simply not playing.

The supported extensions are all in the [SoundFileExtension](frontend/src/main/scala/assets/sounds/SoundFileExtension.scala) enum, and adding new one should be easy. You can, if you want, specify several extensions for a sound asset. In which case they are tried to be load sequentially.

In order to add a new asset, do the following:

- add the sound file into `frontend/src/main/resources/asserts/in-game/sounds` directory, at the right place
- add a new instance in the [SoundAsset](frontend/src/main/scala/assets/sounds/SoundAsset.scala), mirroring the directory structure in the resources foldre via object
- add this new instance in the corresponding maps or raw list at the bottom
- use the `SoundAsset` in the code, probably in the [SoundEffectsManager](frontend/src/main/scala/game/ui/effects/soundeffects/SoundEffectsManager.scala) class.

Note that the current implementation does _not_ allow to run several sounds at the same time. Therefore, if a sound needs to be run often, try to keep it as short as possible, possibly by trimming an unecessary long tail (Audacity is a good software to do that easily).

### Adding a new npm dependencies.

- Go to either `frontend` or `game-server-launcher`, depending on which you want to install things.
- run `npm install --save the-deps` for dependency and `npm install --save-dev the-deps` for a development dependency, such as the typings of a library.

## Credits

### Sounds

A great deal of sound effect you hear in the game are generously offered by [mixkit.co](https://mixkit.co/free-sound-effects/).

The bars that you see in the game were made by wenakiri.
