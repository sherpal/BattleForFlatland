package assets.fonts

import io.circe.Codec

final case class GlyphInfo(
    imageWidth: Int,
    imageHeight: Int,
    fontSize: Int,
    color: String,
    position: Vector[GlyphInfo.GlyphPosition]
)

object GlyphInfo {

  case class GlyphPosition(char: Char, x: Int, y: Int, width: Int, height: Int)

  private given Codec[GlyphPosition] = io.circe.generic.semiauto.deriveCodec

  given Codec[GlyphInfo] = io.circe.generic.semiauto.deriveCodec

}
