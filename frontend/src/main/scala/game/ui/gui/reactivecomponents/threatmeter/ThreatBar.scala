package game.ui.gui.reactivecomponents.threatmeter

import com.raquo.airstream.signal.{Signal, Val}
import game.ui.gui.reactivecomponents.{GUIComponent, StatusBar}
import gamelogic.entities.WithThreat.ThreatAmount
import typings.pixiJs.PIXI.Texture
import utils.misc.{Colour, RGBColour}
import game.ui.reactivepixi.AttributeModifierBuilder._
import game.ui.reactivepixi.Binder
import game.ui.reactivepixi.ReactivePixiElement.{pixiText, ReactiveText}
import gamelogic.physics.Complex
import typings.pixiJs.anon.Align
import typings.pixiJs.mod.TextStyle

final class ThreatBar(
    playerName: Signal[String],
    barTexture: Texture,
    playerColour: Signal[Colour],
    threatAmounts: Signal[ThreatAmount],
    fillingAmount: Signal[Double], // between 0 and 1
    dimensions: Signal[(Double, Double)]
) extends GUIComponent {

  val bar = new StatusBar(
    fillingAmount,
    playerColour,
    Val(true),
    barTexture,
    dimensions
  )

  val settingTextStyle: Binder[ReactiveText] = textStyle <--
    playerColour
      .map(colour => if (colour.isBright) RGBColour.black else RGBColour.white)
      .map(
        colour =>
          new TextStyle(
            Align(
              fontSize = 13.0,
              fill     = colour.rgb
            )
          )
      )

  val nameText: ReactiveText = pixiText(
    "",
    text <-- playerName,
    settingTextStyle,
    position := Complex(1, 1)
  )

  val amountText: ReactiveText = pixiText(
    "",
    text <-- threatAmounts.map(_.toInt.toString),
    settingTextStyle,
    y := 1,
    x <-- dimensions.map(_._1 - 1),
    anchorXY := (1, 0)
  )

  container.amend(bar, nameText, amountText)

}
