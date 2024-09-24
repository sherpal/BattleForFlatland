package game.drawers.bossspecificdrawers

import gamelogic.entities.boss.BossEntity
import game.drawers.Drawer
import gamelogic.entities.boss.dawnoftime.Boss102
import gamelogic.entities.boss.Boss101

def drawerMapping(boss: BossEntity): Drawer = boss.name match {
  case Boss101.name => Drawer.empty
  case Boss102.name => Boss102Drawer
}
