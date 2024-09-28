package assets.fonts

import indigo.*
import scala.scalajs.js
import io.circe.Decoder
import org.scalajs.dom

class Fonts(glyphsInfoStrings: Map[(Fonts.AllowedColor, Fonts.AllowedSize), String]) {
  val glyphsInfo: Map[(Fonts.AllowedColor, Fonts.AllowedSize), GlyphInfo] = glyphsInfoStrings.map {
    case ((color: Fonts.AllowedColor, size: Fonts.AllowedSize), json: String) =>
      val giDecoder = Decoder[GlyphInfo]
      def parseGlyphInfo(str: String): GlyphInfo =
        io.circe.parser.decode[GlyphInfo](str).toTry.get

      ((color, size), parseGlyphInfo(json))
  }

  val fontsInfo: Map[(Fonts.AllowedColor, Fonts.AllowedSize), FontInfo] = (for {
    size      <- Fonts.allowedSizes
    color     <- Fonts.allowedColors
    glyphInfo <- glyphsInfo.get(color, size)
  } yield (((color, size), Fonts.fontInfoFromGlyphInfo(glyphInfo)): (
      (Fonts.AllowedColor, Fonts.AllowedSize),
      FontInfo
  ))).toMap

}

object Fonts {

  type AllowedSize  = 8 | 12 | 16 | 20
  type AllowedColor = "black" | "white" | "green" | "red"

  val xs: AllowedSize = 8
  val s: AllowedSize  = 12
  val m: AllowedSize  = 16
  val l: AllowedSize  = 20

  val fontName = "Quicksand"

  def makeUrlInfo(color: AllowedColor, size: AllowedSize): dom.URL =
    dom.URL(
      assetPathFromName(assetNames(color, size)).toString.dropRight(4) ++ ".json",
      js.`import`.meta.url.asInstanceOf[String]
    )

  private def fontInfoFromGlyphInfo(glyphInfo: GlyphInfo): FontInfo = {
    val key = fontKeys(
      glyphInfo.color.asInstanceOf[AllowedColor],
      glyphInfo.fontSize.asInstanceOf[AllowedSize]
    )

    val baseInfo = FontInfo(
      key,
      glyphInfo.imageWidth,
      glyphInfo.imageHeight,
      glyphInfo.position
        .find(_.char == '?')
        .map(position =>
          FontChar(position.char.toString, position.x, position.y, position.width, position.height)
        )
        .get
    )

    glyphInfo.position.foldLeft(baseInfo)((fontInfo, position) =>
      fontInfo.addChar(
        FontChar(position.char.toString, position.x, position.y, position.width, position.height)
      )
    )
  }

  val allowedSizes: js.Array[AllowedSize]   = js.Array(8, 16)
  val allowedColors: js.Array[AllowedColor] = js.Array("black", "white", "green", "red")

  val fontKeys: Map[(AllowedColor, AllowedSize), FontKey] = (for {
    size  <- allowedSizes
    color <- allowedColors
  } yield (((color, size), FontKey(s"the-font-$color-$size")): (
      (AllowedColor, AllowedSize),
      FontKey
  ))).toMap

  val assetNames: Map[(AllowedColor, AllowedSize), AssetName] = (
    for {
      size  <- allowedSizes
      color <- allowedColors
    } yield ((color, size), AssetName(s"$fontName-$color-$size")): (
        (AllowedColor, AllowedSize),
        AssetName
    )
  )
    .toMap[(AllowedColor, AllowedSize), AssetName]

  private def assetPathFromName(assetName: AssetName): AssetPath =
    AssetPath(s"/assets/in-game/gui/fonts/${assetName}.png")

  val fontImages: Map[(AllowedColor, AllowedSize), AssetType] = assetNames.map {
    case ((color: AllowedColor, size: AllowedSize), assetName) =>
      (
        (
          (color, size),
          AssetType.Image(assetName, assetPathFromName(assetName))
        ): (
            (AllowedColor, AllowedSize),
            AssetType
        )
      )
  }

}
