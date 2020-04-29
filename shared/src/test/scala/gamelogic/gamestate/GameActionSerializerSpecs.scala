package gamelogic.gamestate

import gamelogic.gamestate.gameactions.{AddPlayer, UpdateTimestamp}
import gamelogic.physics.Complex
import utils.ziohelpers.failIfWith
import zio.UIO
import zio.test.Assertion._
import zio.test._
import io.circe.syntax._
import io.circe.parser.decode
import zio.random.Random
import zio.test.environment._

object GameActionSerializerSpecs extends DefaultRunnableSpec {

  val complexGen: Gen[Random, Complex] = Gen.anyDouble.zip(Gen.anyDouble).map(Complex.fromTuple)

  def spec = suite("Game action serializer")(
    testM("Decoder is left inverse of encoder") {
      checkM(complexGen, Gen.anyLong, Gen.anyLong, Gen.anyLong, Gen.alphaNumericString, Gen.boolean) {
        (position, actionId, entityId, time, colour, b) =>
          val action: GameAction =
            if (b) AddPlayer(actionId, time, entityId, position, colour)
            else UpdateTimestamp(actionId, time)
          assertM(UIO(decode[GameAction](action.asJson.noSpaces)))(equalTo(Right(action)))
      }

    }
  )

}
