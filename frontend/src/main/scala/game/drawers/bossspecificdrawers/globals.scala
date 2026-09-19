package game.drawers.bossspecificdrawers

import gamelogic.entities.boss.BossEntity
import game.drawers.DrawerWithCloneBlanks
import gamelogic.entities.boss.dawnoftime.Boss102
import gamelogic.entities.boss.Boss101
import gamelogic.entities.boss.dawnoftime.Boss104

/** Everything that needs to be drawn specifically for a given boss (its adds, its zones, its
  * markers...). Every boss registered in `BossFactory.factoriesByBossName` must have a clause here,
  * otherwise the game throws a `MatchError` while rendering. Use `DrawerWithCloneBlanks.empty` as a
  * placeholder while implementing a new boss.
  */
def drawerMapping(boss: BossEntity): DrawerWithCloneBlanks = boss.name match
  case Boss101.name => DrawerWithCloneBlanks.empty
  case Boss102.name => Boss102Drawer
  case Boss104.name => Boss104Drawer
