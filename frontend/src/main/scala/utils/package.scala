import services.http.{FHttpClient, HttpClient}
import services.localstorage.{FLocalStorage, LocalStorage}
import services.logging.{FLogging, Logging}
import services.routing.{FRouting, Routing}
import zio.Has
import zio.clock.Clock

package object utils {

  type GlobalEnv = Clock with HttpClient with LocalStorage with Logging with Routing

  val runtime: zio.Runtime[GlobalEnv] = zio.Runtime(
    Has(Clock.Service.live) ++
      Has(FHttpClient.serviceLive) ++
      Has(FLocalStorage.serviceLive(Clock.Service.live)) ++
      Has(FLogging.serviceLive) ++
      Has(FRouting.serviceLive),
    zio.internal.Platform.default
  )

}
