package services.routing

import scala.scalajs.js

def baseStrSpecific: String = js.`import`.meta.env.BASE_URL.asInstanceOf[String]
