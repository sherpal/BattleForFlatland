# Battle For Flatland

## Game controls

Currently the controls are hard coded.

It is in my plans to allow the user to chose their owns. However, if you wanted to change them, you can head over `frontend/src/main/scala/game/Keyboard.scala`.

The current controls are:

- zqsd (or qasd on QWERTY) for moving
- keys 1, 2, 3,... for using ability, from left to right as their symbols appear at the bottom left of the game.

## History was made on June, 3rd, 2020

The first games of BFF were made on the 3rd of June, 2020. The first boss "Boss101" was defeated at 22:12 CEST in presence of

- Justin Dekeyser, Square
- Antoine Doeraene, Triangle
- Sébastien Doeraene, Hexagon
- Souad Lepoivre, Pentagon
- Nicolas Radu, Hexagon

## Run locally

You want to play locally with your friends? Or perhaps contribute? That's great! We'll walk you through the steps to make it happen.

In order to proceed, make sure you have sbt and npm installed on your machine.

### Set up the database

#### I don't have [docker](https://docs.docker.com/) installed

The best is to install docker and jump to the next section. If not, you will need to install postgresql on your machine. Then, within the `shared-backend/src/main/resources`, create a file named `dev.conf` and add the lines

```
slick.dbs.default.db.url = "jdbc:postgresql://localhost:<port>/<db-name>?user=<user-name>&password=<db-password>"

superUser {
  name = ???
  mail = ???
  password = ???
}
```

Replace the fields `???` with fields you want for your personal account inside the web app, and fill the `<...>` information in the JDBC connection string.

#### I have docker installed

Within the `shared-backend/src/main/resources`, create a file named `dev.conf` and add the lines

```
slick.dbs.default.db.url = "jdbc:postgresql://localhost:30000/battleforflatland?user=postgres&password=somepassword"

superUser {
  name = ???
  mail = ???
  password = ???
}
```

Replace the fields `???` with fields you want for your personal account inside the web app. If you have fancy characters, and in any case for the email address, you should enclose your variables in double-quotes.

Then execute the following command

```
docker run --name some-postgres -p 30000:5432 -e POSTGRES_DB=battleforflatland -e POSTGRES_PASSWORD=somepassword -d postgres
```

If you are not familiar with docker, here are a few explanations of what this does:

- `--name some-postgres` gives a name to the container (doesn't matter much)
- `-p 30000:5432` binds the port 30000 in your machine to the port 5432 inside the docker container. 5432 is the port on which the postgres databases runs. Which means that on your machine the database is available at port 30000
- `-e key=value` These instructions adds the `key` environment variable inside the docker container and binds it to the specified `value`
- `-d` means "detached". That is, the docker container runs in the background
- `postgres` this is the name of the [docker image](https://hub.docker.com/_/postgres/). If you don't have it already, docker will automatically download it for you.

If you want to see the list of current docker containers, you should issue the command `docker ps`. Within the first column of what you see, you have the id of the container, which allows you to `docker kill <id>` (stop the docker from running), `docker rm <id>` (remove it entirely), `docker restart <id>`.

### Set up the game server launcher

While in development, we have a "game-server-launcher" to launch the game servers when a game is launched. This is a kind of a "mock up" for a more robust setup, involving, e.g., an Azure gaming service.

The `game-server-launcher` sub-project is an express server written in Scala-js. You can first compile it using, in sbt,

```
game-server-launcher/fullOptJS
```

and you can then launch it using (outside sbt!)

```
node game-server-launcher/target/scala-2.13/scalajs-bundler/main/game-server-launcher-opt.js
```

Alternatively, you can simply run (in sbt)

```
game-server-launcher/run
```

but then you leave an extra sbt console open, which is using resources for basically nothing.

If you have [ammonite](http://ammonite.io/) installed (and not on Windows apparently), you can launch the `game-server-launcher.sc`. Simply issue the
`amm game-server-launcher.sc`
command.

If your game-launcher-server doesn't work, it's not a big deal, and you'll see a message saying what to copy-paste in an sbt shell. Something like

```
Could not reach game-server-launcher, fall back to manual launch:
Game secret for 595c2cf3-4349-4ff3-a10d-0f7dfc83cf13 is 24b1cb9c-9ce6-4c9b-9755-7ee3edfaab9c.
Game server can be launched in sbt with the command:
game-server/run -i 595c2cf3-4349-4ff3-a10d-0f7dfc83cf13 -s 24b1cb9c-9ce6-4c9b-9755-7ee3edfaab9c
```

(You can add `-h 0.0.0.0` if you want to play with other people. Using the game-server-launcher, it is done by default.)

### Adding the game assets

The game canvas will need game assets. You'll need to create images corresponding to all the paths in `frontend/src/main/scala/assets/Asset.scala`. The referenced `resources` folder is the `frontend/src/main/resources` folder.

### Launching all the required programs

There are three programs that must be ran in dev mode:

- `sbt dev`: runs the frontend with hot reload (port 8080) (If you want to play with other people, you should instead use the `build` alias command to build the frontend inside the backend's public directory.)
- `sbt backend/run`: runs the backend (port 9000) (If you want to play with other people, you should use `sbt "backend/run -Dhttp.address=0.0.0.0"` instead)
- `node game-server-launcher/target/scala-2.13/scalajs-bundler/main/game-server-launcher-opt.js`: runs the game-server-launcher (after you compiled it once with `sbt game-server-launcher/fullOptJS`)

Head over `http://localhost:8080` and after 10s you should be able to connect with the super user credentials.

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

The previous actions where made in "auto-pilot" mode, and could even be down automatically via an `sbt` command. Now is the time to begin implementing stuff for your boss specifically.

The main goal of the `stagingBossActions` method is to setup the topology of the room. That is, create all the actions to put obstacles into the game. In the case of the Boss103, the room will have the shape of an Hexagon, with 6 triangle "pillars" placed in an inner hexagon, pointing towards the center.

#### Seeing the staging in action

We can already test that the walls of the boss gets spawned at the beginning of the game.

To that end:

- fill the `abilities` and `abilityNames` section with empty collections
- fill the `initialBoss` with the `unit` of the `Pointed` type class of your boss, changing at least the `id`, the `life` and `maxLife` (so that the boss doesn't die instantly)

Then add your `BossFactory` instance to the `allBossesFactories` of the `BossEntity` companion object and the `factoriesByBossName` of the `BossFactory` companion object, so that the boss will be available to the `GameMaster` and to the web frontend for users to select it.

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
- "Touch" the asset in that same object below (and possibly add it to the map of corresponding assets)
- Add the asset reference in the `game/GameAssetLoader` loading list
