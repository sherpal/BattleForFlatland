package gamelogic.gamestate

import gamelogic.gamestate.gameactions.{AddPlayer, UpdateTimestamp}
import zio.UIO
import zio.test.Assertion._
import zio.test._
import io.circe.syntax._
import io.circe.parser.decode

object GameActionSerializerSpecs extends DefaultRunnableSpec {

  def spec = suite("Game action serializer")(
    testM("Decoder is left inverse of encoder") {
      checkM(
        utils.ziohelpers.CustomGenerators.complexGen,
        Gen.anyLong,
        Gen.anyLong,
        Gen.anyLong,
        Gen.int(0, 0xFFFFFF),
        Gen.boolean
      ) { (position, actionId, entityId, time, colour, b) =>
        val action: GameAction =
          if (b) AddPlayer(actionId, time, entityId, position, colour)
          else UpdateTimestamp(actionId, time)
        assertM(UIO(decode[GameAction](action.asJson.noSpaces)))(equalTo(Right(action)))
      }

    }
  )

}
