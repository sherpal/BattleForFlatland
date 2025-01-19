package concurrent

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.reflect.ClassTag

trait BufferSpecs { self: munit.FunSuite =>

  def buffer[T](using ClassTag[T]): Buffer[T]

  test("Buffer is initally empty") {
    val ab = buffer[Int]
    assertEquals(ab.flush().toVector, Vector.empty)
  }

  test("I can produce and read a value") {
    val ab = buffer[Int]
    ab.addActions(Vector(1))
    assertEquals(ab.flush().toVector, Vector(1))
  }

  test("Producing can be done concurrently") {
    val values = scala.util.Random.shuffle((1 to 1000).toVector).grouped(3)
    val ab     = buffer[Int]

    for {
      _ <- Future.sequence(values.map(group => Future(ab.addActions(group))))
      result = ab.flush().toVector
    } yield assertEquals(result.sorted, (1 to 1000).toVector)

  }

  test("Small number of producers can read concurrently for a long time") {
    def oneIteration = {
      val ab                  = buffer[Int]
      val numberOfProducers   = 15
      val producingLength     = 1_000
      val elemsPerProductions = 3

      val reader = Future {
        var all = Vector.empty[Int]
        while (all.length < elemsPerProductions * producingLength * numberOfProducers)
          all ++= ab.flush()
        all
      }

      val producers = (1 to numberOfProducers).map { _ =>
        Future {
          val values = LazyList
            .from(0)
            .take(producingLength)
            .map(_ =>
              Vector(
                scala.util.Random.nextInt(),
                scala.util.Random.nextInt(),
                scala.util.Random.nextInt()
              )
            )
          values.foreach(ab.addActions)
        }
      }

      for {
        _      <- Future.sequence(producers)
        result <- reader
      } yield assertEquals(
        result.length,
        elemsPerProductions * producingLength * numberOfProducers
      )
    }

    (1 to 1000)
      .foldLeft(Future.successful(())) { (acc, _) =>
        acc.flatMap { _ =>
          oneIteration
        }
      }
  }

  test("Producing and Reading (with one reader) can be done concurrently") {
    (1 to 1000)
      .foldLeft(Future.successful(0L)) { (acc, _) =>
        acc.flatMap { readerWaitedFor =>
          val values = someValues
          val ab     = buffer[Int]

          val readerF = Future {
            var all                    = Vector.empty[Int]
            var totalWaitingTime: Long = 0
            while (all.length < valuesPool.length) {
              val start = nanoNow
              all ++= ab.flush()
              val end = nanoNow
              totalWaitingTime += end - start
            }
            (all, totalWaitingTime)
          }

          for {
            _ <- Future.sequence(
              values.map(group => Future(ab.addActions(group)))
            )
            (result, totalWaitingTime) <- readerF
            inLeftNotRight = () => (result.toSet -- valuesPool.toSet).toVector.sorted
            inRightNotLeft = () => (valuesPool.toSet -- result.toSet).toVector.sorted
          } yield {
            assertEquals(
              result.sorted,
              valuesPool,
              s"""|Difference in groups:
                  |Left not right: ${inLeftNotRight()}
                  |Right not Left: ${inRightNotLeft()}
                  |""".stripMargin
            )
            totalWaitingTime + readerWaitedFor
          }
        }
      }
      .andThen { case scala.util.Success(totalWaitingTime) =>
        println(s"Total waiting time is ms: ${totalWaitingTime / 1_000_000}")
      }
  }

  val valuesPool = (1 to 1000).toVector

  def someValues = scala.util.Random.shuffle(valuesPool).grouped(3)

  def nanoNow = System.nanoTime()

}
