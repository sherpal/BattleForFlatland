package game.drawers.bossspecificdrawers

import gamelogic.entities.boss.BossEntity
import game.drawers.DrawerWithCloneBlanks
import gamelogic.entities.boss.dawnoftime.Boss102
import gamelogic.entities.boss.Boss101
import gamelogic.entities.boss.dawnoftime.Boss104

def drawerMapping(boss: BossEntity): DrawerWithCloneBlanks = boss.name match {
  case Boss101.name => DrawerWithCloneBlanks.empty
  case Boss102.name => Boss102Drawer
  case Boss104.name => DrawerWithCloneBlanks.empty
}
