package services

import zio.Has

package object toaster {

  type Toaster = Has[Toaster.Service]

  val toast = new Toast

}
