package game.gameutils

import indigo.*
import gamelogic.physics.Complex
import utils.misc.Colour

def localToGame(point: Point)(bounds: Rectangle): Complex = {
  val x = point.x - bounds.center.x
  val y = bounds.center.y - point.y
  Complex(x, y)
}

def gameToLocal(z: Complex)(bounds: Rectangle): Point = {
  val x = z.re + bounds.center.x
  val y = bounds.center.y - z.im
  Point(x.toInt, y.toInt)
}

extension (colour: Colour) {
  def toIndigo: RGBA =
    RGBA.fromColorInts(colour.red, colour.green, colour.blue, (colour.alpha * 256).toInt)
}
