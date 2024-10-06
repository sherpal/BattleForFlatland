package assets.fonts

import indigo.*
import io.circe.Decoder
import org.scalajs.dom
import zio.ZIO

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

class Fonts(glyphsInfo: Map[(Fonts.AllowedColor, Fonts.AllowedSize), (String, GlyphInfo)]) {

  val fontsInfo: Map[(Fonts.AllowedColor, Fonts.AllowedSize), FontInfo] = (for {
    size      <- Fonts.allowedSizes
    color     <- Fonts.allowedColors
    glyphInfo <- glyphsInfo.get(color, size).map(_._2)
  } yield (((color, size), Fonts.fontInfoFromGlyphInfo(glyphInfo)): (
      (Fonts.AllowedColor, Fonts.AllowedSize),
      FontInfo
  ))).toMap

  val fontImages = for {
    (key, (imageData, _)) <- glyphsInfo
    assetName = Fonts.assetNames(key._1, key._2)
  } yield AssetType.Image(assetName, AssetPath(imageData))

}

object Fonts {

  type AllowedSize  = 8 | 12 | 16 | 20
  type AllowedColor = "black" | "white" | "green" | "red"

  val xs: AllowedSize = 8
  val s: AllowedSize  = 12
  val m: AllowedSize  = 16
  val l: AllowedSize  = 20

  val fontName = "Quicksand"

  def allGlyphFontData = ZIO
    .foreachPar(for {
      color <- allowedColors
      size  <- allowedSizes
      key: (AllowedColor, AllowedSize) = (color, size)
    } yield key)(key =>
      createGlyphFontData(quicksand, key._2, key._1, alphabet).map((data, _, glyphInfo) =>
        key -> (data, glyphInfo)
      )
    )
    .map(_.toMap)
    .map(Fonts(_))

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
    ).makeCaseSensitive(sensitive = true)

    glyphInfo.position.foldLeft(baseInfo)((fontInfo, position) =>
      fontInfo.addChar(
        FontChar(position.char.toString, position.x, position.y, position.width, position.height)
      )
    )
  }

  val allowedSizes: js.Array[AllowedSize]   = js.Array(8, 12, 16, 20)
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

}
