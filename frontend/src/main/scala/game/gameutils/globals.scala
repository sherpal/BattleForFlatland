package game.gameutils

import indigo.*
import gamelogic.physics.Complex

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
