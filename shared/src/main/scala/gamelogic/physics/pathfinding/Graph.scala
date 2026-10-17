package gamelogic.physics.pathfinding

import gamelogic.physics.Complex

/** A [[Graph]] answers navigation questions for a disk of a fixed `radius` living among static
  * obstacles. "Legal" always means "a disk of that radius centred here does not overlap any
  * obstacle".
  */
trait Graph {

  /** Radius of the disk-shaped entities this graph was built for. */
  def radius: Double

  /** Whether a disk of `radius` centred at `z` overlaps no obstacle. */
  def isLegal(z: Complex): Boolean

  /** Whether a disk of the given `clearance` can slide from `from` to `to` in a straight line
    * without overlapping any obstacle. Use a clearance of 0 for a pure line of sight check.
    */
  def isSegmentClear(from: Complex, to: Complex, clearance: Double): Boolean

  /** Returns a legal position close to `z` (`z` itself if it is legal). */
  def nearestLegalPosition(z: Complex): Complex

  /** Finds a shortest path from `start` to `goal`, both included.
    *
    * If `goal` is not legal, the path ends at `nearestLegalPosition(goal)` instead. Returns None if
    * the goal can not be reached.
    */
  def findPath(start: Complex, goal: Complex): Option[Vector[Complex]]

}
