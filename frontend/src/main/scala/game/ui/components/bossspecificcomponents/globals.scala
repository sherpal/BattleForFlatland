package game.ui.components.bossspecificcomponents

import gamelogic.entities.boss.BossEntity
import game.ui.Component
import game.IndigoViewModel
import indigo.shared.FrameContext
import game.scenes.ingame.InGameScene.StartupData
import gamelogic.entities.boss.Boss101
import gamelogic.entities.boss.dawnoftime.Boss102
import gamelogic.entities.boss.dawnoftime.Boss104

def containerMapping(
    boss: BossEntity
)(using IndigoViewModel, FrameContext[StartupData]): Component = boss.name match {
  case Boss101.name => Component.empty
  case Boss102.name => Boss102Component()
  case Boss104.name => Component.empty
}
