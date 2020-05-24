# Battle For Flatland



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


