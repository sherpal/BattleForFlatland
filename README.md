# Battle For Flatland

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

Replace the fields `???` with fields you want for your personnal account inside the web app, and fill the `<...>` information in the JDBC connection string.

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
Alternatively, you can simply run
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
- `amm game-server-launcher.sc`: runs the game-server-launcher.

Head over `http://localhost:8080` and after 10s you should be able to connect with the super user credentials.

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
-

### Adding an asset

- Add the image in the `frontend/src/main/resources/assets` folder
- Add the `Asset` object in `frontend/src/main/scala/assets/Asset.scala`
- "Touch" the asset in that same object below (and possibly add it to the map of conrresponding assets)
- Add the asset reference in the `game/GameAssetLoader` loading list
