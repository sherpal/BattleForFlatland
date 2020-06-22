package game.ui.gui.reactivecomponents

import com.raquo.airstream.eventstream.EventStream
import gamelogic.gamestate.GameState

final class StatusBar(gameStateUpdates: EventStream[(GameState, Long)]) extends GUIComponent {}
