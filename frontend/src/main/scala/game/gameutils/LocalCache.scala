package game.gameutils

import scala.collection.mutable

class LocalCache[K, OtherInfo, V](f: (K, OtherInfo) => V) {

  private val cachedValues = mutable.Map[K, V]()

  inline def retrieve(k: K, other: => OtherInfo): V =
    cachedValues.getOrElseUpdate(k, f(k, other))

}
