package bffclient.hud

import bffclient.gui.*
import bffclient.ui.{Colour, Theme}
import gamelogic.abilities.Ability
import gamelogic.buffs.abilities.classes.BraceShield
import gamelogic.docs.AbilityDoc
import gamelogic.entities.Entity
import gamelogic.entities.classes.PlayerClass
import gamelogic.gameextras.FightLog
import gamelogic.gameextras.FightLog.{Entry, LifeChange}
import gamelogic.gamestate.GameState
import models.bff.outofgame.PlayerClasses
import services.i18n.I18n
import utils.misc.RGBColour

/** The statistics of the fight, shown at the end of the game (see [[EndOfGame]]), one tab at a
  * time: what everyone dealt, healed and took, the last moments of the players who died, and a few
  * things specific to the classes. They come from the [[FightLog]] of the game, and from its last
  * game state, which still knows every player (even the dead ones).
  */
final class FightReport(log: FightLog, gameState: GameState, myId: Option[Entity.Id], i18n: I18n) {
  import FightReport.*

  private val players: Vector[PlayerClass] = gameState.deadPlayers.values.toVector.sortBy(_.name)

  private def isPlayer(id: Entity.Id): Boolean = gameState.deadPlayers.contains(id)

  private val start   = gameState.startTime.getOrElse(0L)
  private val end     = gameState.endTime.getOrElse(gameState.time)
  private val seconds = math.max(1.0, (end - start) / 1000.0)

  private val damage = log.damageEntries
  private val heals  = log.healEntries

  private def damageDoneBy(id: Entity.Id): Vector[Entry] =
    damage.filter(e => e.sourceId == id && !isPlayer(e.targetId))
  private def healsDoneBy(id: Entity.Id): Vector[Entry] =
    heals.filter(e => e.sourceId == id && isPlayer(e.targetId))
  private def damageTakenBy(id: Entity.Id): Vector[Entry] = damage.filter(_.targetId == id)

  private def nameOf(id: Entity.Id): String =
    gameState.deadPlayers.get(id).map(_.name).orElse(log.nameOf(id)).getOrElse("?")
  private def abilityName(id: Ability.AbilityId): String = AbilityDoc.names.getOrElse(id, "?")

  /** The name shared by these entities, with their number when there are several (e.g. the hounds
    * of Boss 102).
    */
  private def counted(name: String, ids: Vector[Entity.Id]): String = {
    val count = ids.distinct.size
    if count > 1 then s"$name ×$count" else name
  }

  private def colourOf(player: PlayerClass): Colour =
    HudStyle.softened(RGBColour.fromIntColour(player.colour))

  private def clock(time: Long): String = {
    val elapsed = math.max(0L, time - start) / 1000
    f"${elapsed / 60}%d:${elapsed % 60}%02d"
  }

  private def percent(part: Double, total: Double): Int =
    if total <= 0 then 0 else math.round(100 * part / total).toInt

  /** The life changes shown in the last moments: whose, among the players who died and ourselves.
    */
  private val recapChoices: Vector[Entity.Id] =
    (log.deaths.map(_.playerId) ++ myId.filter(isPlayer)).distinct

  private def died(id: Entity.Id): Boolean = log.deaths.exists(_.playerId == id)

  /** Ours if we died, otherwise the first death (the one which started the defeat, most of the
    * time).
    */
  private var recapOf: Option[Entity.Id] =
    myId.filter(died).orElse(log.deaths.headOption.map(_.playerId)).orElse(myId.filter(isPlayer))

  /** The tab to show first: the last moments of a death in a defeat, the summary otherwise. */
  def firstTab: Tab =
    if gameState.ended && !gameState.playersWon && log.deaths.nonEmpty then Tab.LastMoments
    else Tab.Summary

  /** Fills `frame` with the contents of `tab`, `width` wide, and returns their height. `changed` is
    * called when the contents must be shown again (another player was chosen).
    */
  def render(tab: Tab, frame: Frame, width: Double, changed: () => Unit): Double = {
    val rows = Rows(frame, width)
    tab match {
      case Tab.Summary => summary(rows)
      case Tab.DamageDone => damageDone(rows)
      case Tab.Healing => healing(rows)
      case Tab.DamageTaken => damageTaken(rows)
      case Tab.LastMoments => lastMoments(rows, frame, changed)
      case Tab.Classes => classes(rows)
    }
    rows.height
  }

  private def amount(x: Double): String = math.round(x).toString

  private def summary(rows: Rows): Unit = {
    for boss <- gameState.bosses.values.headOption do {
      rows.bar(
        i18n.bossLifeLeft(boss.name, percent(boss.life, boss.maxLife)),
        "",
        boss.life / boss.maxLife,
        Theme.negative
      )
      rows.space(8)
    }
    // the name, then four columns of numbers (aligned on their right), across the width
    val column = (rows.width - 250) / 4
    def line(texts: Vector[String], colours: Vector[Colour]): Unit =
      rows.cells(texts.indices.map { i =>
        if i == 0 then Cell(texts(i), 0, 240, colours(i), false)
        else Cell(texts(i), 250 + column * (i - 1), column - 10, colours(i), true)
      })
    line(
      Vector(i18n.player, i18n.damageDone, i18n.healing, i18n.damageTaken, i18n.deaths),
      Vector.fill(5)(Theme.muted)
    )
    val byDamage = players.sortBy(p => -damageDoneBy(p.id).map(_.totals.amount).sum)
    for player <- byDamage do {
      val dealt  = damageDoneBy(player.id).map(_.totals.amount).sum
      val healed = healsDoneBy(player.id).map(_.totals.effective).sum
      val taken  = damageTakenBy(player.id).map(_.totals.amount).sum
      val death  = log.deaths.find(_.playerId == player.id).fold("-")(d => clock(d.time))
      line(
        Vector(
          player.name,
          s"${amount(dealt)} (${amount(dealt / seconds)}/s)",
          amount(healed),
          amount(taken),
          death
        ),
        colourOf(player) +: Vector.fill(4)(Theme.text)
      )
    }
    rows.space(12)
    rows.heading(i18n.deaths)
    if log.deaths.isEmpty then rows.text(i18n.nobodyDied)
    for death <- log.deaths do {
      val elapsed = math.max(0L, death.time - start) / 1000
      rows.text(
        s"${nameOf(death.playerId)}: " +
          i18n.diedAt((elapsed / 60).toInt, (elapsed % 60).toInt, killedBy(death.recap)),
        Theme.text
      )
    }
  }

  private def killedBy(recap: Vector[LifeChange]): String =
    recap
      .findLast(!_.healing)
      .fold("?")(hit => s"${abilityName(hit.abilityId)}, ${nameOf(hit.sourceId)}")

  /** One bar per player for `entries` (with its value), the highest first, with bars under it for
    * each target, and a line with the share of each ability.
    */
  private def perPlayer(
      rows: Rows,
      entriesOf: Entity.Id => Vector[Entry],
      valueOf: Entry => Double,
      label: Vector[Entry] => String
  ): Unit = {
    val totals = players
      .map(p => (p, entriesOf(p.id)))
      .map((p, entries) => (p, entries, entries.map(valueOf).sum))
      .filter(_._3 > 0)
      .sortBy(-_._3)
    val highest = totals.headOption.fold(1.0)(_._3)
    if totals.isEmpty then rows.text("-")
    for (player, entries, total) <- totals do {
      rows.bar(player.name, label(entries), total / highest, colourOf(player))
      // the adds of a kind together (they have the same name)
      val byTarget = entries
        .groupBy(e => nameOf(e.targetId))
        .toVector
        .map((name, es) => (counted(name, es.map(_.targetId)), es.map(valueOf).sum))
        .sortBy(-_._2)
      if byTarget.size > 1 then
        for (target, value) <- byTarget do
          rows.bar(
            target,
            amount(value),
            value / total,
            colourOf(player).withAlpha(0.6),
            indent = 24,
            height = 16
          )
      abilityShares(rows, entries, valueOf, total)
      rows.space(8)
    }
  }

  private def abilityShares(
      rows: Rows,
      entries: Vector[Entry],
      valueOf: Entry => Double,
      total: Double
  ): Unit = {
    val byAbility = entries.groupMapReduce(_.abilityId)(valueOf)(_ + _).toVector.sortBy(-_._2)
    rows.text(
      byAbility.map((id, value) => s"${abilityName(id)} ${percent(value, total)}%").mkString(" · "),
      indent = 24
    )
  }

  private def damageDone(rows: Rows): Unit =
    perPlayer(
      rows,
      damageDoneBy,
      _.totals.amount,
      entries => {
        val total = entries.map(_.totals.amount).sum
        s"${amount(total)} (${amount(total / seconds)}/s)"
      }
    )

  private def healing(rows: Rows): Unit =
    perPlayer(
      rows,
      healsDoneBy,
      _.totals.effective,
      entries => {
        val effective = entries.map(_.totals.effective).sum
        val overheal  = entries.map(_.totals.amount).sum - effective
        s"${amount(effective)} (${i18n.overheal(percent(overheal, effective + overheal))})"
      }
    )

  /** Per player, what hurt them the most: by ability (and who used it). */
  private def damageTaken(rows: Rows): Unit = {
    val totals = players
      .map(p => (p, damageTakenBy(p.id)))
      .map((p, entries) => (p, entries, entries.map(_.totals.amount).sum))
      .filter(_._3 > 0)
      .sortBy(-_._3)
    val highest = totals.headOption.fold(1.0)(_._3)
    if totals.isEmpty then rows.text("-")
    for (player, entries, total) <- totals do {
      rows.bar(player.name, amount(total), total / highest, colourOf(player))
      // the adds of a kind together (they have the same name)
      val bySource = entries
        .groupBy(e => (e.abilityId, nameOf(e.sourceId)))
        .toVector
        .map { case ((abilityId, name), es) =>
          (abilityId, counted(name, es.map(_.sourceId)), es.map(_.totals.amount).sum)
        }
        .sortBy(-_._3)
      for (abilityId, source, value) <- bySource.take(6) do
        rows.bar(
          s"${abilityName(abilityId)} ($source)",
          amount(value),
          value / total,
          HudStyle.softened(Ability.abilityColour(abilityId)),
          indent = 24,
          height = 16
        )
      rows.space(8)
    }
  }

  private def lastMoments(rows: Rows, frame: Frame, changed: () => Unit): Unit =
    recapOf match {
      case None => rows.text("-")
      case Some(playerId) =>
        if recapChoices.size > 1 then {
          val choice = Dropdown[Entity.Id](frame, nameOf)
          choice.setOptions(recapChoices, recapOf)
          choice.setSize(260, 30)
          choice.setPoint(Point.TopRight, frame, Point.TopRight, -8, 0)
          choice.setScript(choice.OnValueChanged) { chosen =>
            recapOf = Some(chosen)
            changed()
          }
        }
        val death = log.deaths.findLast(_.playerId == playerId)
        rows.heading(
          i18n.lastSecondsOf(
            nameOf(playerId),
            (FightLog.recapDuration / 1000).toInt,
            death.isDefined
          )
        )
        rows.space(6)
        val until   = death.fold(end)(_.time)
        val recap   = log.recapOf(playerId, until)
        val maxLife = gameState.deadPlayers.get(playerId).fold(1.0)(_.maxLife)
        if recap.isEmpty then rows.text(i18n.nothingInLastSeconds)
        for change <- recap do {
          val sign = if change.healing then "+" else "-"
          val colour =
            if change.healing then Theme.positive.lighter(0.4) else Theme.negative.lighter(0.4)
          val lost  = change.amount - change.effective
          val extra = if lost >= 1 then s" (${amount(change.effective)})" else ""
          // the time, what, how much, then the life afterwards, across the width
          val amountEnd = 0.68 * rows.width
          rows.cells(
            Vector(
              Cell(f"${(change.time - until) / 1000.0}%.1f s", 0, 60, Theme.muted, true),
              Cell(
                s"${abilityName(change.abilityId)} (${nameOf(change.sourceId)})",
                80,
                amountEnd - 200,
                Theme.text,
                false
              ),
              Cell(s"$sign${amount(change.amount)}$extra", amountEnd - 110, 110, colour, true)
            ),
            lifeBar = Some(
              (
                amountEnd + 20,
                change.lifeAfter / maxLife,
                s"${i18n.life} ${amount(change.lifeAfter)}"
              )
            )
          )
        }
        for d <- death do {
          rows.space(6)
          val elapsed = math.max(0L, d.time - start) / 1000
          rows.text(
            i18n.diedAt((elapsed / 60).toInt, (elapsed % 60).toInt, killedBy(d.recap)),
            Theme.negative.lighter(0.4)
          )
        }
    }

  private def classes(rows: Rows): Unit = {
    var any = false
    for player <- players do
      player.cls match {
        case PlayerClasses.Pentagon =>
          any = true
          rows.heading(player.name, colourOf(player))
          val bullets = damage
            .filter(e => e.sourceId == player.id && e.abilityId == Ability.pentagonPentagonBullet)
            .map(_.totals.count)
            .sum
          val wasted = log.facetsWastedBy(player.id)
          val pentagrams = damage
            .filter(e => e.sourceId == player.id && e.abilityId == Ability.pentagonPentagramId)
            .map(_.totals.count)
            .sum
          rows.text(i18n.bulletsHit(bullets), Theme.text)
          rows.text(i18n.facetsWasted(wasted, percent(wasted, bullets)), Theme.text)
          rows.text(i18n.pentagramsCast(pentagrams), Theme.text)
          rows.space(10)
        case PlayerClasses.Square =>
          any = true
          rows.heading(player.name, colourOf(player))
          rows.text(i18n.timeWithBraceStacks)
          val times = log.braceTimeOf(player.id, end)
          val alive = math.max(1L, times.sum)
          for (time, stacks) <- times.zipWithIndex.reverse do
            rows.bar(
              i18n.braceStacks(stacks),
              s"${percent(time.toDouble, alive.toDouble)}%",
              time.toDouble / alive,
              if stacks == BraceShield.maxStacks.toInt then BraceGauge.fullColour
              else BraceGauge.colour,
              height = 16
            )
          rows.text(
            i18n.damagePreventedByBrace(amount(log.bracePreventedBy(player.id))),
            Theme.text
          )
          rows.space(10)
        case PlayerClasses.Hexagon =>
          any = true
          rows.heading(player.name, colourOf(player))
          val entries   = healsDoneBy(player.id)
          val effective = entries.map(_.totals.effective).sum
          val total     = entries.map(_.totals.amount).sum
          rows.text(
            s"${i18n.healing}: ${amount(effective)} (${i18n.overheal(percent(total - effective, total))})",
            Theme.text
          )
          val byAbility = entries.groupBy(_.abilityId).toVector.map { (id, es) =>
            (id, es.map(_.totals.effective).sum, es.map(_.totals.amount).sum)
          }
          for (id, eff, all) <- byAbility.sortBy(-_._2) do
            rows.bar(
              abilityName(id),
              s"${amount(eff)} (${i18n.overheal(percent(all - eff, all))})",
              eff / math.max(1.0, effective),
              HudStyle.softened(Ability.abilityColour(id)),
              height = 16
            )
          rows.space(10)
        case _ => ()
      }
    if !any then rows.text(i18n.noClassStatistics)
  }

}

object FightReport {

  enum Tab {
    case Summary, DamageDone, Healing, DamageTaken, LastMoments, Classes

    def label(i18n: I18n): String = this match {
      case Summary => i18n.fightSummary
      case DamageDone => i18n.damageDone
      case Healing => i18n.healing
      case DamageTaken => i18n.damageTaken
      case LastMoments => i18n.lastMoments
      case Classes => i18n.classes
    }
  }

  /** A text in a line of [[Rows.cells]], from `x`, `width` wide. */
  private final case class Cell(
      text: String,
      x: Double,
      width: Double,
      colour: Colour,
      alignedRight: Boolean
  )

  /** Lays out rows (texts, bars, lines of cells) one under the other in `frame`, from its top. */
  private final class Rows(frame: Frame, val width: Double) {
    private var y = 0.0

    def height: Double = y

    def space(height: Double): Unit = y += height

    def heading(text: String, colour: Colour = Theme.text): Unit = {
      frame.createFontString(text, Theme.body, colour).setPoint(Point.TopLeft, 0, y + 4)
      y += 30
    }

    def text(text: String, colour: Colour = Theme.muted, indent: Double = 0): Unit = {
      val fontString = frame.createFontString(text, Theme.small, colour)
      fontString.setPoint(Point.TopLeft, indent, y + 2)
      y += 20
    }

    /** A bar filled up to `fraction`, with `label` on its left and `value` on its right. */
    def bar(
        label: String,
        value: String,
        fraction: Double,
        colour: Colour,
        indent: Double = 0,
        height: Double = 22
    ): Unit = {
      val bar = StatusBar(frame)
      bar.setPoint(Point.TopLeft, indent, y)
      bar.setSize(width - indent, height)
      bar.background.setCornerRadius(3)
      bar.bar.setCornerRadius(3)
      bar.bar.setColour(colour.withAlpha(colour.a * 0.75))
      bar.setValue(fraction)
      val size = if height < 20 then Theme.small else Theme.body
      val left = bar.createFontString(label, size, layer = Layer.Overlay)
      left.setPoint(Point.Left, bar, Point.Left, 8, 0)
      val right = bar.createFontString(value, size, layer = Layer.Overlay)
      right.setPoint(Point.Right, bar, Point.Right, -8, 0)
      y += height + 4
    }

    /** Texts side by side, and maybe a small bar of life (from its x, its fraction and its text).
      */
    def cells(cells: Seq[Cell], lifeBar: Option[(Double, Double, String)] = None): Unit = {
      for cell <- cells do {
        val fontString = frame.createFontString(cell.text, Theme.small, cell.colour)
        fontString.setWidth(cell.width)
        fontString.setJustifyH(if cell.alignedRight then JustifyH.Right else JustifyH.Left)
        fontString.setPoint(Point.TopLeft, cell.x, y + 2)
      }
      for (x, fraction, text) <- lifeBar do {
        val bar = StatusBar(frame)
        bar.setPoint(Point.TopLeft, x, y + 1)
        bar.setSize(width - x, 18)
        bar.background.setCornerRadius(3)
        bar.bar.setCornerRadius(3)
        bar.bar.setColour(HudStyle.lifeColour(fraction))
        bar.setValue(fraction)
        bar
          .createFontString(text, Theme.small, layer = Layer.Overlay)
          .setPoint(Point.Left, bar, Point.Left, 8, 0)
      }
      y += 22
    }
  }

}
