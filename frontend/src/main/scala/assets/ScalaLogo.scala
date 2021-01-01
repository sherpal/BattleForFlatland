package assets

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.language.implicitConversions

//@js.native @JSImport("resources/assets/scala.png", JSImport.Default)
object ScalaLogo {
  val name: String = "assets/scala.png"

  // todo[scala3] replace this ugly thing with a top level def
  implicit def intoAsset(t: ScalaLogo.type): Asset = Asset(name)
}
