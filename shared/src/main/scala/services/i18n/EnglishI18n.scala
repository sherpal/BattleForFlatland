package services.i18n

import gamelogic.abilities.Ability
import gamelogic.abilities.boss.boss101.{BigDot, BigHit}
import gamelogic.buffs.Buff
import gamelogic.buffs.abilities.classes.BraceShield
import gamelogic.docs
import gamelogic.entities.{Entity, Resource}
import gamelogic.entities.boss.Boss101
import gamelogic.entities.classes.{Hexagon, Pentagon, Square, Triangle}
import models.bff.ingame.Controls
import models.bff.outofgame.PlayerClasses

object EnglishI18n extends I18n {

  // --- Common ---

  override def playerClassName(playerClass: PlayerClasses): String = playerClass match {
    case PlayerClasses.Triangle => "Triangle"
    case PlayerClasses.Square => "Square"
    case PlayerClasses.Pentagon => "Pentagon"
    case PlayerClasses.Hexagon => "Hexagon"
  }

  override def resourceName(resource: Resource): String = resource match {
    case Resource.Mana => "mana"
    case Resource.Energy => "energy"
    case Resource.Rage => "rage"
    case Resource.NoResource => "nothing"
  }

  override def mouseButton(number: String): String = s"mouse button $number"

  override def formatSeconds(millis: Long): String = docs.formatSeconds(millis)
  override def formatPeriod(millis: Long): String  = docs.formatPeriod(millis)

  override def settings: String    = "Settings"
  override def leaveGame: String   = "Leave game"
  override def quit: String        = "Quit"
  override def name: String        = "Name"
  override def playerClass: String = "Class"
  override def colour: String      = "Colour"
  override def ready: String       = "Ready"
  override def notReady: String    = "Not ready"
  override def boss: String        = "Boss"
  override def victory: String     = "Victory!"
  override def defeat: String      = "Defeat..."
  override def startGame: String   = "Start game"

  override def castingTime(time: String): String = s"Cast: $time"
  override def cooldown(time: String): String    = s"Cooldown: $time"
  override def cost(amount: String): String      = s"Cost: $amount"

  // --- Menus ---

  override def fullscreenHint: String             = "F11: fullscreen"
  override def connectingToGameServer: String     = "Connecting to the game server"
  override def leavingTheGame: String             = "Leaving the game"
  override def connectionToGameServerLost: String = "The connection to the game server was lost"
  override def cannotConnectToGameServer(error: String): String =
    s"Cannot connect to the game server: $error"
  override def couldNotLeaveGame(error: String): String = s"Could not leave the game: $error"

  override def chooseYourLanguage: String = "Choose your language"

  // Login
  override def welcomeToBattleForFlatland: String = "Welcome to Battle for Flatland!"
  override def serverAddress: String              = "Server address"
  override def serverAddressExample: String   = "e.g. bff.example.com, or http://81.12.34.56:9000"
  override def choosePseudo: String           = "Please choose a pseudo"
  override def yourPseudo: String             = "Your pseudo"
  override def loggingIn: String              = "Logging in"
  override def enterBattleForFlatland: String = "Enter Battle for Flatland"
  override def serverError(message: String): String = s"Server: $message"
  override def pseudoError(message: String): String = s"Pseudo: $message"
  override def cannotLogIn(address: String, error: String): String =
    s"Cannot log in to $address: $error"
  override def invalidPort(port: String): String = s"Invalid port: $port"
  override def invalidAddress(address: String): String =
    s"Invalid address: $address (expected host or host:port)"

  // Games
  override def welcomePlayer(playerName: String): String   = s"Welcome, $playerName"
  override def learnToPlay: String                         = "Learn to play"
  override def joinItBack: String                          = "Join it back"
  override def gameNamePlaceholder: String                 = "Game name"
  override def defaultGameName(playerName: String): String = s"$playerName's game"
  override def createGame: String                          = "Create game"
  override def createdBy: String                           = "Created by"
  override def players: String                             = "Players"
  override def loadingTheGames: String                     = "Loading the games"
  override def noGames: String  = "There are currently no games. Go ahead and create one!"
  override def join: String     = "Join"
  override def joinBack: String = "Join back"
  override def currentlyInGame(gameName: String): String =
    s"You are currently involved in the game '$gameName'."

  // Lobby of a game
  override def loadingTheGame: String                 = "Loading the game"
  override def loggedInAs(playerName: String): String = s"Logged in as $playerName"
  override def gameCreatedBy(creator: String, gameId: String): String =
    s"Created by $creator - game $gameId"
  override def kick: String                      = "Kick"
  override def you(playerName: String): String   = s"$playerName (you)"
  override def ai(playerName: String): String    = s"$playerName (AI)"
  override def classNotChosen: String            = "Not chosen"
  override def addAI: String                     = "Add AI"
  override def fillWithAIs: String               = "Fill with AIs"
  override def launchGame: String                = "Launch game!"
  override def waitingForCreatorToLaunch: String = "Waiting for the game creator to launch the game"
  override def gameIsStarting: String        = "Game is starting, please wait for setup to finish"
  override def gameServerNotReadyYet: String = "The game server is not ready yet."
  override def gameServerRunsOnYourComputer: String = "The game server runs on your computer"
  override def gameServerRunsOnComputerOf(host: String): String =
    s"The game server runs on the computer of $host"
  override def downloadingJavaRuntime: String              = "Downloading the Java runtime"
  override def downloadingGameServer: String               = "Downloading the game server"
  override def megabytes(received: Int, size: Int): String = s"$received / $size MB"
  override def gameClosedByCreator: String                 = "The game was closed by its creator"
  override def notInThisGame: String                       = "You are not in this game (anymore)"
  override def everyoneReady: String         = "Everyone is ready and well set, let's go!"
  override def thingsPreventingStart: String = "The following things prevent the game to start:"

  // Docs of the bosses and classes, in the lobby
  override def classes: String = "Classes"
  override def bossFacts(life: Int, intendedFor: Int): String =
    s"$life life points, for $intendedFor player(s)"
  override def classFacts(
      life: Int,
      resourceAmount: Int,
      resource: Resource,
      isYours: Boolean
  ): String =
    s"$life life points, $resourceAmount ${resourceName(resource)}" +
      (if isYours then " (your class)" else "")
  override def abilities: String = "Abilities"

  // Settings
  override def controls: String                       = "Controls"
  override def general: String                        = "General"
  override def goUp: String                           = "Go up"
  override def goDown: String                         = "Go down"
  override def goLeft: String                         = "Go left"
  override def goRight: String                        = "Go right"
  override def nextTarget: String                     = "Next target"
  override def faceTheTarget: String                  = "Face the target"
  override def abilityNumber(number: Int): String     = s"Ability $number"
  override def markerCross: String                    = "Cross"
  override def markerLozenge: String                  = "Lozenge"
  override def markerMoon: String                     = "Moon"
  override def markerSquare: String                   = "Square"
  override def markerStar: String                     = "Star"
  override def markerTriangle: String                 = "Triangle"
  override def markerOnTarget(marker: String): String = s"$marker on target"
  override def markerOnGround(marker: String): String = s"$marker on ground"
  override def bindingsHelp: String =
    "Click a binding, then press a key (possibly with Shift) or a mouse button. Changes apply to the next game."
  override def mouseOver: String = "Mouse over"
  override def mouseOverHelp: String =
    "Abilities fall back to the frame under the mouse when the target is not valid for them."
  override def resetToDefaults: String = "Reset to defaults"
  override def escapeToCancel: String  = "Escape to cancel"
  override def pressKeyFor(action: String): String =
    s"Press a key or a mouse button for \"$action\""
  override def boundToSeveralActions(key: String): String = s"$key is bound to several actions"
  override def language: String                           = "Language"
  override def languageHelp: String = "The language of the menus and of the games."
  override def hudScale: String     = "HUD scale"
  override def hudScaleHelp: String =
    "The size of the frames and of the action bar, whatever the window's size. It applies to the next game."
  override def hostGameServers: String = "Host game servers"
  override def hostGameServersHelp: String =
    "The game server of a game runs on the computer of one of its players. To be ready to run it, " +
      "the client downloads it, with what runs it (several tens of MB, once), as soon as you join " +
      "a game. Turn this off to download nothing, e.g. on a mobile network: the other players " +
      "will run it."
  override def mainVolume: String      = "Main volume"
  override def abilitiesVolume: String = "Abilities"
  override def volumeOff: String       = "Off"
  override def volumesHelp: String =
    "From off to 10. The volume of each kind of sound is relative to the main volume."
  override def done: String = "Done"

  // --- In game ---

  override def placeYourselves: String =
    "Place yourselves, then click \"Start game\": the boss arrives 5 seconds later"
  override def bossArrivesIn: String          = "The boss arrives in"
  override def clickToPlaceZone: String       = "Click to place the zone (Escape to cancel)"
  override def waitingForGameToStart: String  = "Waiting for the game to start..."
  override def bossAbilities: String          = "Boss abilities"
  override def threat: String                 = "Threat"
  override def damage: String                 = "Damage"
  override def damageTo(name: String): String = s"Damage: $name"
  override def dead: String                   = "Dead"

  // Casting
  override def youAreDead: String                  = "You are dead"
  override def noTarget: String                    = "No target"
  override def noAbilityOnKey(number: Int): String = s"No ability on key $number"
  override def abilityNotSupported(ability: Ability.AbilityId): String =
    s"Ability $ability is not supported yet"
  override def abilityNotPlacedWithMouse(ability: Ability.AbilityId): String =
    s"Ability $ability is not placed with the mouse"

  // Why an ability cannot be cast (shown to the player)
  override def alreadyCasting: String                = "Already casting"
  override def abilityNotOwned: String               = "You don't have that ability"
  override def abilityOnCooldown: String             = "Ability on cooldown"
  override def notEnough(resource: Resource): String = s"Not enough ${resourceName(resource)}"
  override def silenced: String                      = "You are silenced"
  override def notInRange: String                    = "Not in range"
  override def notInSight: String                    = "Not in sight"
  override def positionNotInSight: String            = "Position is not in sight"
  override def notAnAlly: String                     = "Not an ally"
  override def notAnEnemy: String                    = "Not an enemy"
  override def cannotGuardYourself: String           = "You can't guard yourself"
  override def needShieldStack: String               = "You need a stack of your shield"
  override def targetCannotBeStunned: String         = "Target can't be stunned"
  override def targetDoesNotCast: String             = "Target does not cast anything"
  override def targetAbilityCannotBeCut: String      = "The target's ability can't be cut"

  // Why a boss' ability cannot be cast
  override def noBombToExplode: String                  = "You don't own any bomb!"
  override def creepingShadowNotThere: String           = "Creeping Shadow is not there"
  override def bossNotThere: String                     = "The boss is not there"
  override def creepingShadowDoesNotCollideBoss: String = "Creeping Shadow does not touch the boss"
  override def noDeathMarkTriangle: String              = "There is no death mark triangle"
  override def noShadeToCall: String                    = "There is no Shade to call"
  override def targetNotThere: String                   = "The target is not there"
  override def twinsFarEnough: String                   = "The twins are far enough from each other"

  // Why an action is not legal (rather for the logs)
  override def gameAlreadyStarted: String                      = "The game has already started"
  override def gameAlreadyEnded: String                        = "The game has already ended"
  override def playerAlreadyExists: String                     = "The player already exists"
  override def bossDoesNotExist(bossName: String): String      = s"Boss $bossName does not exist"
  override def entityDoesNotExist(entityId: Entity.Id): String = s"Entity $entityId does not exist"
  override def entitiesDoNotExist(entityId: Entity.Id, otherId: Entity.Id): String =
    s"Entity $entityId or $otherId does not exist"
  override def entityIsNotLiving(entityId: Entity.Id): String =
    s"Entity $entityId does not exist, or is not a living entity"
  override def entityIsNotMoving(entityId: Entity.Id): String =
    s"Entity $entityId does not exist, or is not a moving body"
  override def entityHasNoAbilities(entityId: Entity.Id): String =
    s"Entity $entityId does not exist, or does not have abilities"
  override def entityHasNoTarget(entityId: Entity.Id): String =
    s"Entity $entityId does not exist, or does not have a target"
  override def entityWouldCollide(entityId: Entity.Id): String =
    s"Entity $entityId would collide with an obstacle"
  override def buffDoesNotExist(buffId: Buff.Id, bearerId: Entity.Id): String =
    s"Buff $buffId does not exist on entity $bearerId"

  // Connection to the game server
  override def gameServerDoesNotAnswer: String = "the game server does not answer"
  override def cannotSynchronizeClock: String  = "cannot synchronize the clock with the game server"
  override def gameServerNotReady: String      = "the game server is not ready"
  override def gameServerRejectsCredentials: String =
    "the game server does not accept our credentials"
  override def gameServerDoesNotKnowUs: String          = "the game server does not know us"
  override def gameCouldNotStart(error: String): String = s"the game could not start ($error)"

  // Game menu
  override def menu: String                  = "Menu"
  override def resume: String                = "Resume"
  override def surrender: String             = "Surrender"
  override def clickAgainToSurrender: String = "Click again to surrender"
  override def surrenderEndsInDefeat: String = "The fight ends in a defeat, for everyone"
  override def gameGoesOnMeanwhile: String   = "The game goes on meanwhile"
  override def windowed: String              = "Windowed (F11)"
  override def fullscreen: String            = "Fullscreen (F11)"

  // End of the game
  override def backToMenus: String    = "Back to menus"
  override def connectionLost: String = "Connection lost"
  override def fightLasted(minutes: Int, seconds: Int): String =
    f"The fight lasted $minutes%d:$seconds%02d"
  override def gameServerClosedConnection: String = "The game server closed the connection"

  // --- Tutorial ---

  override def tutorialIntro: String =
    "Battle for Flatland in a few minutes: how the game works, then each class, with a practice " ++
      "fight against Boss 101 alongside bots."
  override def tutorialHint: String =
    "New here? Start with the overview, then read about the class you'd like to play."
  override def backToTheGames: String                    = "Back to the games"
  override def backToTheTutorial: String                 = "Back to the tutorial"
  override def next(title: String): String               = s"Next: $title"
  override def practiceAgainst(bossName: String): String = s"Practice against $bossName"
  override def settingUpTheGame: String                  = "Setting up the game..."
  override def alreadyInAGame: String =
    "You are already in a game: leave it first, from the games."
  override def practiceGameName(playerName: String): String = s"$playerName's practice"

  override def overview: String     = "Overview"
  override def overviewRole: String = "How the game works"
  override def overviewSummary: String =
    "A team of shapes against a boss: who does what, how to read the screen, and how a fight goes."

  override def theClass(playerClass: PlayerClasses): String = s"The ${playerClassName(playerClass)}"

  override def classRole(playerClass: PlayerClasses): String = playerClass match {
    case PlayerClasses.Triangle => "Melee damage"
    case PlayerClasses.Square => "Tank"
    case PlayerClasses.Pentagon => "Ranged damage"
    case PlayerClasses.Hexagon => "Healer"
  }

  override def classSummary(playerClass: PlayerClasses): String = playerClass match {
    case PlayerClasses.Triangle =>
      "Fights in contact with its enemies, spends its energy on big hits, and controls the adds."
    case PlayerClasses.Square =>
      "Keeps the boss' attention and takes its blows, behind a shield it keeps building up."
    case PlayerClasses.Pentagon =>
      "Shoots its enemies from afar, aims its bullets and zones, and dispels nasty debuffs."
    case PlayerClasses.Hexagon =>
      "Keeps everyone alive, with a big but slowly regenerating pool of mana to manage."
  }

  override def overviewSections(controls: Controls): Seq[TextSection] = {
    val abilityKeys = controls.abilityKeys.take(4).map(key).mkString(", ")
    Seq(
      TextSection(
        "Welcome to Flatland",
        Seq(
          "In Battle for Flatland, a team of players, each one a geometric shape, fights a boss " ++
            "together. The fight is won when the boss dies, and lost when every player is dead: " ++
            "everyone wins or loses together.",
          "Bosses are much stronger than any player. What beats them is a team where everyone " ++
            "plays their role."
        )
      ),
      TextSection(
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
      TextSection(
        "Threat",
        Seq(
          "The boss attacks the player it hates the most. Every point of damage dealt to it " ++
            "generates threat towards its dealer. The tank's abilities generate a lot of it, so " ++
            "that the tank stays on top of the threat meter (bottom right of the screen).",
          "Damage dealers: watch it, and don't get above the tank!"
        )
      ),
      TextSection(
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
      TextSection(
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
      TextSection(
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
      TextSection(
        "Next",
        Seq(
          "Read about the class you want to play, then practice it against Boss 101 with bots."
        )
      )
    )
  }

  override def classSections(playerClass: PlayerClasses): Seq[TextSection] = (playerClass match {
    case PlayerClasses.Triangle => triangle
    case PlayerClasses.Square => square
    case PlayerClasses.Pentagon => pentagon
    case PlayerClasses.Hexagon => hexagon
  }) :+ TextSection(
    "Practice",
    Seq(
      s"Ready to try? \"Practice against Boss 101\" sets up a game where you play the " ++
        s"${playerClassName(playerClass)} alongside bots, who take the other roles. Once in the " ++
        "arena, place yourself, then click \"Start game\": the boss arrives 5 seconds later. " ++
        "When the fight ends, you come back here."
    )
  )

  private def triangle = Seq(
    TextSection(
      "Philosophy",
      Seq(
        "The Triangle is pressure and precision. It lives right against its enemies, in melee " ++
          "range, and turns its energy into damage: where the Pentagon has to aim, the Triangle " ++
          "hits what it targets.",
        "It is also the team's control: when adds show up, it stuns them, and cuts the spells " ++
          "they cast."
      )
    ),
    TextSection(
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
    TextSection(
      "Stay alive",
      Seq(
        s"With ${formatNumber(Triangle.initialMaxLife)} life points, you cannot take the boss' " ++
          "big hits. Your damage builds threat fast: keep an eye on the threat meter, and let " ++
          "the Square stay on top. Stay within the healer's range."
      )
    ),
    TextSection(
      "Control",
      Seq(
        "Stun stops an enemy (but not a boss) for a few seconds, interrupting its spell, until " ++
          "it takes damage. Cut interrupts a spell and silences its caster. Save them for the " ++
          "spells that matter."
      )
    )
  )

  private def square = Seq(
    TextSection(
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
    TextSection(
      "Rage",
      Seq(
        "Your rage comes from the damage you take (and a little every second). Being hit is " ++
          "what fuels you! When the boss is not hitting you, Enrage hurts yourself to build " ++
          "rage up."
      )
    ),
    TextSection(
      "Brace",
      Seq(
        s"Brace adds a stack to your shield, up to ${BraceShield.maxStacks}. Each stack " ++
          s"reduces the damage you take by ${formatNumber((1 - BraceShield.damageMultiplier) * 100)}%. " ++
          s"The shield loses a stack after ${formatSeconds(BraceShield.duration)} unless you " ++
          "Brace again. The gauge on the left of your frame shows your stacks: keep it full.",
        "Shield Slam spends a stack for a strong hit with extra threat."
      )
    ),
    TextSection(
      "Threat",
      Seq(
        "Taunt the boss as soon as it arrives, then keep hitting it (Hammer Hit, Shield Slam, " ++
          "Cleave) to stay on top of the threat meter. If someone gets above you, Taunt again.",
        "Guard protects an ally for a while: part of the damage it takes is taken by you " ++
          "instead, e.g. when it carries a dangerous debuff."
      )
    ),
    TextSection(
      "Against Boss 101",
      Seq(
        s"Its Big Hit deals ${formatNumber(BigHit.damageAmount)} damage, after a cast of " ++
          s"${formatSeconds(BigHit.castingTime)}: watch the boss' cast bar, and have your stacks " ++
          "of Brace up when it lands."
      )
    )
  )

  private def pentagon = Seq(
    TextSection(
      "Philosophy",
      Seq(
        "The Pentagon is the team's artillery: it deals its damage from afar, out of the boss' " ++
          "reach. Its power comes with skill: its bullets go where you aim, and its zones have " ++
          "to be placed.",
        "It is also the only one able to dispel the nasty debuffs that some bosses put on " ++
          "players."
      )
    ),
    TextSection(
      "Aim",
      Seq(
        "- Pentagon Bullet flies towards your mouse (not your target), and hits the first enemy " ++
          "on its way: keep a clear line to the boss.",
        "- Pentagon Zone: cast it, then click where to put it. Enemies standing in it take " ++
          "damage over time: put it under the boss, which the tank keeps in place."
      )
    ),
    TextSection(
      "Mana",
      Seq(
        s"Your abilities cost mana (${formatNumber(Pentagon.initialResourceAmount.amount)}). " ++
          "Keep shooting, but keep some for when a dispel is needed."
      )
    ),
    TextSection(
      "Dispel",
      Seq(
        "Penta Dispel removes all the effects that can be dispelled from an ally, the good ones " ++
          "included (like the healer's heal over time!). Later bosses put debuffs that must be " ++
          "dispelled quickly. Boss 101's Big Dot cannot be: the healer heals through it."
      )
    ),
    TextSection(
      "Stay alive",
      Seq(
        s"With ${formatNumber(Pentagon.initialMaxLife)} life points, don't let the boss turn " ++
          "on you: watch the threat meter. Stay away from the boss, but within the healer's " ++
          "range."
      )
    )
  )

  private def hexagon = Seq(
    TextSection(
      "Philosophy",
      Seq(
        "The Hexagon keeps the team standing. It watches the team's frames more than the " ++
          "arena: who is losing life, who is about to take a big hit.",
        s"Its mana pool is big (${formatNumber(Hexagon.initialResourceAmount.amount)}) but " ++
          "regenerates slowly: the healer's game is to spend the right heal on the right " ++
          "player, at the right time."
      )
    ),
    TextSection(
      "Your heals",
      Seq(
        "- Flash Heal heals a lot, but takes time to cast: you must stand still.",
        "- Heal Over Time is cheap and instant, and can be cast while moving: put it on the " ++
          "players who take damage steadily (the tank, the bearer of Big Dot).",
        "- Hexagon Zone heals everyone standing in it: for when the team is grouped."
      )
    ),
    TextSection(
      "Targeting",
      Seq(
        "Click a frame on the left of the screen to target a player. A faded frame is a " ++
          "player out of your healing range: get closer.",
        "With \"mouse over\" enabled in the Settings, your heals go to the player whose frame " ++
          "is under the mouse, without changing your target."
      )
    ),
    TextSection(
      "Against Boss 101",
      Seq(
        "The tank takes the boss' hits: keep it healthy, especially before a Big Hit. Big Dot " ++
          s"deals ${formatNumber(BigDot.damageOnTick)} damage every ${formatPeriod(BigDot.tickRate)} " ++
          s"for ${formatSeconds(BigDot.duration)} to someone else: a heal over time on them " ++
          "covers most of it. Don't heal players who are full: that's wasted mana."
      )
    )
  )

  // Previews of the pages
  override def arena: String     = "Arena"
  override def screen: String    = "Screen"
  override def hoverHint: String = "Hover the abilities, click the frames"

  override def legendBoss: String           = "The boss: its life, and what it casts."
  override def legendBossCooldowns: String  = "When the boss' abilities come back."
  override def legendTeam: String           = "Your team. Click a frame to target the player."
  override def legendYouAndTarget: String   = "You and your target, above your abilities."
  override def legendMeters: String         = "Damage dealt to your target, and the boss' threat."
  override def legendTriangleEnergy: String = "Your energy: spend it, it refills steadily."
  override def legendTriangleAbilities: String = "Your abilities: Direct Hit, Energy Kick, ..."
  override def legendTriangleDamage: String    = "Your place on the damage meter: the top!"
  override def legendTriangleThreat: String =
    "But not on the threat meter: that's the Square's."
  override def legendSquareStacks: String = "Your shield's stacks (Brace): keep them up."
  override def legendSquareRage: String   = "Your rage, earned by taking damage."
  override def legendSquareBigHit: String =
    "The boss casts Big Hit on you: have your stacks up!"
  override def legendSquareThreat: String = "You must stay on top of the threat meter."
  override def legendPentagonMana: String = "Your mana."
  override def legendPentagonAim: String =
    "Bullets fly towards the mouse, zones are placed with a click."
  override def legendPentagonDebuffs: String =
    "The players' debuffs: dispel the ones that can be."
  override def legendPentagonDamage: String = "Your place on the damage meter."
  override def legendHexagonTeam: String    = "Your team: click a frame to target the player."
  override def legendHexagonFaded: String   = "Faded: out of your healing range. Get closer!"
  override def legendHexagonMana: String    = "Your mana: it regenerates slowly."
  override def legendHexagonTarget: String =
    "Your target, the tank, with your heal over time."

  override def arenaHealerRange: String = "The healer's range"
  override def arenaBoss: String        = "The boss"
  override def arenaSquare: String      = "The Square holds the boss, facing it"
  override def arenaTriangle: String    = "The Triangle hits it in melee, from behind"
  override def arenaPentagons: String   = "The Pentagons shoot from afar"
  override def arenaHexagon: String     = "The Hexagon heals, within reach of everyone"
  override def arenaLegendPositions: String =
    "The tank turns the boss away from the others, who hit it from behind or from afar."
  override def arenaLegendRange: String =
    "Everyone stays within the healer's range (the team's frames fade outside of it)."
}
