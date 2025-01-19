package application

import models.bff.ingame.ServerPerformanceSummary

final class PerformanceMonitor {

  private val numberOfFramesInfo                      = 200
  private val timesBetweenFrames                      = new Array[Long](numberOfFramesInfo)
  private val usedMemoryValues                        = new Array[Long](numberOfFramesInfo)
  @volatile() private var alreadyReachedNbrFramesInfo = false
  @volatile() private var infoIndex                   = 0

  def addInfo(frameTime: Long): Unit = {
    timesBetweenFrames(infoIndex) = frameTime
    usedMemoryValues(infoIndex) =
      Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
    infoIndex = (infoIndex + 1) % numberOfFramesInfo
    if infoIndex == 0 then alreadyReachedNbrFramesInfo = true
  }

  def retrieveSummary: Option[ServerPerformanceSummary] = Option.when(alreadyReachedNbrFramesInfo) {
    val times = timesBetweenFrames.toVector // freezing data
    println(times)
    val memoryValues         = usedMemoryValues.toVector
    val maxTimeBetweenFrames = times.max
    val fps                  = times.map(1000 / _.toDouble).map(_.toInt)
    val averageFps           = fps.sum / numberOfFramesInfo
    val medianFps            = fps.sorted.apply(numberOfFramesInfo / 2)
    val usedMemory           = memoryValues.sum / numberOfFramesInfo
    ServerPerformanceSummary(
      averageFps,
      medianFps,
      maxTimeBetweenFrames,
      usedMemory
    )
  }

}
