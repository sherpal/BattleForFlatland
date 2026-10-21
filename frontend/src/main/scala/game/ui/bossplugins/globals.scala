package game.ui.bossplugins

import gamelogic.entities.boss.BossEntity
import gamelogic.entities.boss.dawnoftime.{Boss104, Boss105}

/** The [[BossGUIPlugin]] of each boss. Unlike `drawerMapping` and `containerMapping`, a boss does
  * not need a clause here: most bosses don't alter the generic UI, and fall back to
  * [[BossGUIPlugin.empty]].
  */
def pluginMapping(boss: BossEntity): BossGUIPlugin = boss.name match
  case Boss104.name => Boss104GUIPlugin
  case Boss105.name => Boss105GUIPlugin
  case _            => BossGUIPlugin.empty
