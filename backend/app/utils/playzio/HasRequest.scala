package utils.playzio

import zio.{UIO, ZIO}

import scala.language.higherKinds

trait HasRequest[+R[_], A] {
  def request: UIO[R[A]]
}

object HasRequest {
  def apply[R[_], A](r: R[A]): HasRequest[R, A] = new HasRequest[R, A] {
    def request: UIO[R[A]] = ZIO.succeed(r)
  }
}
