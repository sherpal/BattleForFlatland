package bffclient.tutorial

import gamelogic.abilities.boss.boss101.{BigDot, BigHit}
import gamelogic.buffs.abilities.classes.BraceShield
import gamelogic.docs.{formatNumber, formatPeriod, formatSeconds}
import gamelogic.entities.boss.Boss101
import gamelogic.entities.classes.{Hexagon, Pentagon, Square, Triangle}
import models.bff.ingame.Controls
import models.bff.ingame.Controls.InputCode

/** A part of a page of the tutorial: a heading, then paragraphs (a line starting with "- " is an
  * item of a list).
  */
final case class Section(heading: String, paragraphs: Seq[String])

/** What the tutorial says, with the player's own keys (from their controls). */
object TutorialTexts {

  /** Short enough for a sentence: KeyW is W, Digit1 is 1, Shift KeyQ is Shift Q, Button 2 (of the
    * mouse) is "mouse button 2".
    */
  def key(code: InputCode): String =
    code.label.replace("Digit", "").replace("Key", "").replace("Button ", "mouse button ")

  def sections(topic: Topic, controls: Controls): Seq[Section] = topic match {
    case Topic.Overview => overview(controls)
    case Topic.Triangle => triangle
    case Topic.Square => square
    case Topic.Pentagon => pentagon
    case Topic.Hexagon => hexagon
  }

  private def practice(className: String) = Section(
    "Practice",
    Seq(
      s"Ready to try? \"Practice against Boss 101\" sets up a game where you play the $className " ++
        "alongside bots, who take the other roles. Once in the arena, place yourself, then click " ++
        "\"Start game\": the boss arrives 5 seconds later. When the fight ends, you come back here."
    )
  )

  private def overview(controls: Controls) = {
    val abilityKeys = controls.abilityKeys.take(4).map(key).mkString(", ")
    Seq(
      Section(
        "Welcome to Flatland",
        Seq(
          "In Battle for Flatland, a team of players, each one a geometric shape, fights a boss " ++
            "together. The fight is won when the boss dies, and lost when every player is dead: " ++
            "everyone wins or loses together.",
          "Bosses are much stronger than any player. What beats them is a team where everyone " ++
            "plays their role."
        )
      ),
      Section(
        "Tank, healer, damage",
        Seq(
          "Every class has one role, and each role relies on the others:",
          "- The tank (the Square) keeps the boss' attention. It is the only one who survives the " ++
            "boss' blows, so the boss must hit it, and no one else.",
          "- The healer (the Hexagon) keeps the team alive, the tank first, by healing the damage " ++
            "they take.",
          "- The damage dealers (the Triangle in melee, the Pentagon from afar) bring the boss " ++
            "down, before the healer runs out of mana.",
          "If the tank dies, the boss turns to players who cannot take its hits. If the healer " ++
            "dies, the damage adds up. If the damage is too low, the fight lasts until the " ++
            "healer is out of mana."
        )
      ),
      Section(
        "Threat",
        Seq(
          "The boss attacks the player it hates the most. Every point of damage dealt to it " ++
            "generates threat towards its dealer. The tank's abilities generate a lot of it, so " ++
            "that the tank stays on top of the threat meter (bottom right of the screen).",
          "Damage dealers: watch it, and don't get above the tank!"
        )
      ),
      Section(
        "Controls",
        Seq(
          s"- Move with ${key(controls.upKey)}, ${key(controls.leftKey)}, " ++
            s"${key(controls.downKey)} and ${key(controls.rightKey)}.",
          s"- Cast your abilities with $abilityKeys, ... in the order of your action bar, at the " ++
            "bottom of the screen. Hover an ability to read what it does.",
          "- Click a player or the boss (in the arena, or its frame) to target it. " ++
            s"${key(controls.nextTargetKey)} targets the closest enemy, and " ++
            s"${key(controls.targetLockInToggleKey)} makes you face your target.",
          "- Some abilities create a zone: cast them, then click where to put it (Escape cancels).",
          "- Escape opens the game's menu. All the keys can be changed in the Settings."
        )
      ),
      Section(
        "Reading the screen",
        Seq(
          "The \"Screen\" tab shows what you see during a fight:",
          "- 1. The boss, with what it casts.",
          "- 2. When the boss' abilities come back.",
          "- 3. Your team, faded when out of your healing range. Click a frame to target the player.",
          "- 4. You, your target (with what it casts), and your abilities below.",
          "- 5. Who dealt the most damage to your target, and who the boss hates the most."
        )
      ),
      Section(
        "How a game goes",
        Seq(
          "Create a game (or join one), choose your class and colour, and get ready. The " ++
            "game's creator chooses the boss, may fill the team with AIs, and launches the game.",
          "In the arena, everyone places themselves, then someone clicks \"Start game\": the " ++
            "boss arrives 5 seconds later. The lobby describes each boss and its abilities.",
          s"${Boss101.name} is the very first boss, and the best place to learn: its Big Hit " ++
            s"(${formatNumber(BigHit.damageAmount)} damage) kills anyone but the tank, and its " ++
            s"Big Dot deals ${formatNumber(BigDot.damageOnTick)} damage every " ++
            s"${formatPeriod(BigDot.tickRate)} to someone else, which the healer has to heal."
        )
      ),
      Section(
        "Next",
        Seq(
          "Read about the class you want to play, then practice it against Boss 101 with bots."
        )
      )
    )
  }

  private def triangle = Seq(
    Section(
      "Philosophy",
      Seq(
        "The Triangle is pressure and precision. It lives right against its enemies, in melee " ++
          "range, and turns its energy into damage: where the Pentagon has to aim, the Triangle " ++
          "hits what it targets.",
        "It is also the team's control: when adds show up, it stuns them, and cuts the spells " ++
          "they cast."
      )
    ),
    Section(
      "Energy",
      Seq(
        s"Your energy (${formatNumber(Triangle.initialResourceAmount.amount)}) refills at a " ++
          "steady pace. Direct Hit is your main damage, but costly. Energy Kick gives energy " ++
          "back: use it while waiting. Energy left at its maximum is energy wasted.",
        "- Keep Upgrade Direct Hit up: it makes Direct Hit stronger for a while.",
        "- Direct Hit whenever you have the energy.",
        "- Energy Kick when it is ready and you are short on energy."
      )
    ),
    Section(
      "Stay alive",
      Seq(
        s"With ${formatNumber(Triangle.initialMaxLife)} life points, you cannot take the boss' " ++
          "big hits. Your damage builds threat fast: keep an eye on the threat meter, and let " ++
          "the Square stay on top. Stay within the healer's range."
      )
    ),
    Section(
      "Control",
      Seq(
        "Stun stops an enemy (but not a boss) for a few seconds, interrupting its spell, until " ++
          "it takes damage. Cut interrupts a spell and silences its caster. Save them for the " ++
          "spells that matter."
      )
    ),
    practice("Triangle")
  )

  private def square = Seq(
    Section(
      "Philosophy",
      Seq(
        "The Square is the team's wall. Its job is not to deal damage, but to be the one the " ++
          s"boss hits: it has ${formatNumber(Square.initialMaxLife)} life points, twice as much " ++
          "as the others, a shield it keeps building up, and abilities that generate a lot of " ++
          "threat.",
        "A good tank is the calm in the storm: as long as it holds the boss, everyone else can " ++
          "do their job."
      )
    ),
    Section(
      "Rage",
      Seq(
        "Your rage comes from the damage you take (and a little every second). Being hit is " ++
          "what fuels you! When the boss is not hitting you, Enrage hurts yourself to build " ++
          "rage up."
      )
    ),
    Section(
      "Brace",
      Seq(
        s"Brace adds a stack to your shield, up to ${BraceShield.maxStacks}. Each stack " ++
          s"reduces the damage you take by ${formatNumber((1 - BraceShield.damageMultiplier) * 100)}%. " ++
          s"The shield loses a stack after ${formatSeconds(BraceShield.duration)} unless you " ++
          "Brace again. The gauge on the left of your frame shows your stacks: keep it full.",
        "Shield Slam spends a stack for a strong hit with extra threat."
      )
    ),
    Section(
      "Threat",
      Seq(
        "Taunt the boss as soon as it arrives, then keep hitting it (Hammer Hit, Shield Slam, " ++
          "Cleave) to stay on top of the threat meter. If someone gets above you, Taunt again.",
        "Guard protects an ally for a while: part of the damage it takes is taken by you " ++
          "instead, e.g. when it carries a dangerous debuff."
      )
    ),
    Section(
      "Against Boss 101",
      Seq(
        s"Its Big Hit deals ${formatNumber(BigHit.damageAmount)} damage, after a cast of " ++
          s"${formatSeconds(BigHit.castingTime)}: watch the boss' cast bar, and have your stacks " ++
          "of Brace up when it lands."
      )
    ),
    practice("Square")
  )

  private def pentagon = Seq(
    Section(
      "Philosophy",
      Seq(
        "The Pentagon is the team's artillery: it deals its damage from afar, out of the boss' " ++
          "reach. Its power comes with skill: its bullets go where you aim, and its zones have " ++
          "to be placed.",
        "It is also the only one able to dispel the nasty debuffs that some bosses put on " ++
          "players."
      )
    ),
    Section(
      "Aim",
      Seq(
        "- Pentagon Bullet flies towards your mouse (not your target), and hits the first enemy " ++
          "on its way: keep a clear line to the boss.",
        "- Pentagon Zone: cast it, then click where to put it. Enemies standing in it take " ++
          "damage over time: put it under the boss, which the tank keeps in place."
      )
    ),
    Section(
      "Mana",
      Seq(
        s"Your abilities cost mana (${formatNumber(Pentagon.initialResourceAmount.amount)}). " ++
          "Keep shooting, but keep some for when a dispel is needed."
      )
    ),
    Section(
      "Dispel",
      Seq(
        "Penta Dispel removes all the effects that can be dispelled from an ally, the good ones " ++
          "included (like the healer's heal over time!). Later bosses put debuffs that must be " ++
          "dispelled quickly. Boss 101's Big Dot cannot be: the healer heals through it."
      )
    ),
    Section(
      "Stay alive",
      Seq(
        s"With ${formatNumber(Pentagon.initialMaxLife)} life points, don't let the boss turn " ++
          "on you: watch the threat meter. Stay away from the boss, but within the healer's " ++
          "range."
      )
    ),
    practice("Pentagon")
  )

  private def hexagon = Seq(
    Section(
      "Philosophy",
      Seq(
        "The Hexagon keeps the team standing. It watches the team's frames more than the " ++
          "arena: who is losing life, who is about to take a big hit.",
        s"Its mana pool is big (${formatNumber(Hexagon.initialResourceAmount.amount)}) but " ++
          "regenerates slowly: the healer's game is to spend the right heal on the right " ++
          "player, at the right time."
      )
    ),
    Section(
      "Your heals",
      Seq(
        "- Flash Heal heals a lot, but takes time to cast: you must stand still.",
        "- Heal Over Time is cheap and instant, and can be cast while moving: put it on the " ++
          "players who take damage steadily (the tank, the bearer of Big Dot).",
        "- Hexagon Zone heals everyone standing in it: for when the team is grouped."
      )
    ),
    Section(
      "Targeting",
      Seq(
        "Click a frame on the left of the screen to target a player. A faded frame is a " ++
          "player out of your healing range: get closer.",
        "With \"mouse over\" enabled in the Settings, your heals go to the player whose frame " ++
          "is under the mouse, without changing your target."
      )
    ),
    Section(
      "Against Boss 101",
      Seq(
        "The tank takes the boss' hits: keep it healthy, especially before a Big Hit. Big Dot " ++
          s"deals ${formatNumber(BigDot.damageOnTick)} damage every ${formatPeriod(BigDot.tickRate)} " ++
          s"for ${formatSeconds(BigDot.duration)} to someone else: a heal over time on them " ++
          "covers most of it. Don't heal players who are full: that's wasted mana."
      )
    ),
    practice("Hexagon")
  )
}
