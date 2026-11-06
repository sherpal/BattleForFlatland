package services.i18n

import gamelogic.abilities.Ability
import gamelogic.abilities.boss.boss101.{BigDot, BigHit}
import gamelogic.buffs.Buff
import gamelogic.buffs.abilities.classes.BraceShield
import gamelogic.entities.{Entity, Resource}
import gamelogic.entities.boss.Boss101
import gamelogic.entities.classes.{Hexagon, Pentagon, Square, Triangle}
import models.bff.ingame.Controls
import models.bff.outofgame.PlayerClasses

object DutchI18n extends I18n {

  // --- Common ---

  override def playerClassName(playerClass: PlayerClasses): String = playerClass match {
    case PlayerClasses.Triangle => "Driehoek"
    case PlayerClasses.Square => "Vierkant"
    case PlayerClasses.Pentagon => "Vijfhoek"
    case PlayerClasses.Hexagon => "Zeshoek"
  }

  override def resourceName(resource: Resource): String = resource match {
    case Resource.Mana => "mana"
    case Resource.Energy => "energie"
    case Resource.Rage => "woede"
    case Resource.NoResource => "niets"
  }

  override def mouseButton(number: String): String = s"muisknop $number"

  override def formatNumber(x: Double): String = super.formatNumber(x).replace('.', ',')

  override def formatSeconds(millis: Long): String =
    s"${formatNumber(millis / 1000.0)} ${if millis > 1000L then "seconden" else "seconde"}"

  override def settings: String    = "Instellingen"
  override def leaveGame: String   = "Spel verlaten"
  override def quit: String        = "Afsluiten"
  override def name: String        = "Naam"
  override def playerClass: String = "Klasse"
  override def colour: String      = "Kleur"
  override def ready: String       = "Klaar"
  override def notReady: String    = "Niet klaar"
  override def boss: String        = "Boss"
  override def victory: String     = "Overwinning!"
  override def defeat: String      = "Nederlaag..."
  override def startGame: String   = "Spel starten"

  override def castingTime(time: String): String = s"Casttijd: $time"
  override def cooldown(time: String): String    = s"Cooldown: $time"
  override def cost(amount: String): String      = s"Kost: $amount"

  // --- Menus ---

  override def fullscreenHint: String             = "F11: volledig scherm"
  override def connectingToGameServer: String     = "Verbinding maken met de spelserver"
  override def leavingTheGame: String             = "Het spel wordt verlaten"
  override def connectionToGameServerLost: String = "De verbinding met de spelserver is verbroken"
  override def cannotConnectToGameServer(error: String): String =
    s"Kan geen verbinding maken met de spelserver: $error"
  override def couldNotLeaveGame(error: String): String = s"Kon het spel niet verlaten: $error"

  override def chooseYourLanguage: String = "Kies je taal"

  // Login
  override def welcomeToBattleForFlatland: String = "Welkom bij Battle for Flatland!"
  override def serverAddress: String              = "Serveradres"
  override def serverAddressExample: String   = "bv. bff.example.com, of http://81.12.34.56:9000"
  override def choosePseudo: String           = "Kies een spelersnaam"
  override def yourPseudo: String             = "Je spelersnaam"
  override def loggingIn: String              = "Aanmelden"
  override def enterBattleForFlatland: String = "Betreed Battle for Flatland"
  override def serverError(message: String): String = s"Server: $message"
  override def pseudoError(message: String): String = s"Spelersnaam: $message"
  override def cannotLogIn(address: String, error: String): String =
    s"Kan niet aanmelden bij $address: $error"
  override def invalidPort(port: String): String = s"Ongeldige poort: $port"
  override def invalidAddress(address: String): String =
    s"Ongeldig adres: $address (verwacht: host of host:poort)"

  // Games
  override def welcomePlayer(playerName: String): String   = s"Welkom, $playerName"
  override def learnToPlay: String                         = "Leren spelen"
  override def joinItBack: String                          = "Terug naar je spel"
  override def gameNamePlaceholder: String                 = "Naam van het spel"
  override def defaultGameName(playerName: String): String = s"Spel van $playerName"
  override def createGame: String                          = "Spel aanmaken"
  override def createdBy: String                           = "Aangemaakt door"
  override def players: String                             = "Spelers"
  override def loadingTheGames: String                     = "De spellen worden geladen"
  override def noGames: String  = "Er zijn momenteel geen spellen. Maak er gerust een aan!"
  override def join: String     = "Deelnemen"
  override def joinBack: String = "Terugkeren"
  override def currentlyInGame(gameName: String): String =
    s"Je neemt momenteel deel aan het spel '$gameName'."

  // Lobby of a game
  override def loadingTheGame: String                 = "Het spel wordt geladen"
  override def loggedInAs(playerName: String): String = s"Aangemeld als $playerName"
  override def gameCreatedBy(creator: String, gameId: String): String =
    s"Aangemaakt door $creator - spel $gameId"
  override def kick: String                      = "Verwijderen"
  override def you(playerName: String): String   = s"$playerName (jij)"
  override def ai(playerName: String): String    = s"$playerName (AI)"
  override def classNotChosen: String            = "Niet gekozen"
  override def addAI: String                     = "AI toevoegen"
  override def fillWithAIs: String               = "Aanvullen met AI's"
  override def launchGame: String                = "Spel lanceren!"
  override def waitingForCreatorToLaunch: String = "Wachten tot de maker het spel lanceert"
  override def gameIsStarting: String            = "Het spel start, even wachten tot alles klaar is"
  override def gameServerNotReadyYet: String     = "De spelserver is nog niet klaar."
  override def gameServerRunsOnYourComputer: String = "De spelserver draait op jouw computer"
  override def gameServerRunsOnComputerOf(host: String): String =
    s"De spelserver draait op de computer van $host"
  override def downloadingJavaRuntime: String              = "De Java-runtime wordt gedownload"
  override def downloadingGameServer: String               = "De spelserver wordt gedownload"
  override def megabytes(received: Int, size: Int): String = s"$received / $size MB"
  override def gameClosedByCreator: String                 = "Het spel werd gesloten door de maker"
  override def notInThisGame: String                       = "Je zit niet (meer) in dit spel"
  override def everyoneReady: String         = "Iedereen is klaar en staat goed, daar gaan we!"
  override def thingsPreventingStart: String = "Het spel kan niet starten om de volgende redenen:"

  // Docs of the bosses and classes, in the lobby
  override def classes: String = "Klassen"
  override def bossFacts(life: Int, intendedFor: Int): String =
    s"$life levenspunten, voor $intendedFor speler(s)"
  override def classFacts(
      life: Int,
      resourceAmount: Int,
      resource: Resource,
      isYours: Boolean
  ): String =
    s"$life levenspunten, $resourceAmount ${resourceName(resource)}" +
      (if isYours then " (jouw klasse)" else "")
  override def abilities: String = "Vaardigheden"

  // Settings
  override def controls: String                       = "Besturing"
  override def general: String                        = "Algemeen"
  override def goUp: String                           = "Omhoog"
  override def goDown: String                         = "Omlaag"
  override def goLeft: String                         = "Naar links"
  override def goRight: String                        = "Naar rechts"
  override def nextTarget: String                     = "Volgend doelwit"
  override def faceTheTarget: String                  = "Doelwit aankijken"
  override def abilityNumber(number: Int): String     = s"Vaardigheid $number"
  override def markerCross: String                    = "Kruis"
  override def markerLozenge: String                  = "Ruit"
  override def markerMoon: String                     = "Maan"
  override def markerSquare: String                   = "Vierkant"
  override def markerStar: String                     = "Ster"
  override def markerTriangle: String                 = "Driehoek"
  override def markerOnTarget(marker: String): String = s"$marker op doelwit"
  override def markerOnGround(marker: String): String = s"$marker op de grond"
  override def bindingsHelp: String =
    "Klik op een toewijzing en druk dan op een toets (eventueel met Shift) of een muisknop. Wijzigingen gelden vanaf het volgende spel."
  override def mouseOver: String = "Mouse-over"
  override def mouseOverHelp: String =
    "Vaardigheden gaan naar het kader onder de muis als het doelwit er niet geldig voor is."
  override def resetToDefaults: String = "Standaard herstellen"
  override def escapeToCancel: String  = "Escape om te annuleren"
  override def pressKeyFor(action: String): String =
    s"Druk op een toets of een muisknop voor \"$action\""
  override def boundToSeveralActions(key: String): String = s"$key is aan meerdere acties gekoppeld"
  override def language: String                           = "Taal"
  override def languageHelp: String = "De taal van de menu's en van de spellen."
  override def hudScale: String     = "HUD-schaal"
  override def hudScaleHelp: String =
    "De grootte van de kaders en van de actiebalk, ongeacht de grootte van het venster. Geldt vanaf het volgende spel."
  override def hostGameServers: String = "Spelservers hosten"
  override def hostGameServersHelp: String =
    "De spelserver van een spel draait op de computer van een van de spelers. Om hem te kunnen " +
      "draaien, downloadt de client hem, samen met wat nodig is om hem te draaien (enkele tientallen " +
      "MB, eenmalig), zodra je aan een spel deelneemt. Zet dit uit om niets te downloaden, bv. op " +
      "een mobiel netwerk: de andere spelers draaien hem dan."
  override def mainVolume: String      = "Hoofdvolume"
  override def abilitiesVolume: String = "Vaardigheden"
  override def volumeOff: String       = "Uit"
  override def volumesHelp: String =
    "Van uit tot 10. Het volume van elk soort geluid is relatief ten opzichte van het hoofdvolume."
  override def done: String = "Klaar"

  // --- In game ---

  override def placeYourselves: String =
    "Neem jullie posities in en klik dan op \"Spel starten\": de boss komt 5 seconden later"
  override def bossArrivesIn: String    = "De boss komt over"
  override def clickToPlaceZone: String = "Klik om de zone te plaatsen (Escape om te annuleren)"
  override def waitingForGameToStart: String  = "Wachten tot het spel start..."
  override def bossAbilities: String          = "Vaardigheden van de boss"
  override def threat: String                 = "Threat"
  override def damage: String                 = "Damage"
  override def damageTo(name: String): String = s"Damage: $name"
  override def dead: String                   = "Dood"

  // Casting
  override def youAreDead: String                  = "Je bent dood"
  override def noTarget: String                    = "Geen doelwit"
  override def noAbilityOnKey(number: Int): String = s"Geen vaardigheid op toets $number"
  override def abilityNotSupported(ability: Ability.AbilityId): String =
    s"Vaardigheid $ability wordt nog niet ondersteund"
  override def abilityNotPlacedWithMouse(ability: Ability.AbilityId): String =
    s"Vaardigheid $ability wordt niet met de muis geplaatst"

  // Why an ability cannot be cast (shown to the player)
  override def alreadyCasting: String                = "Je bent al aan het casten"
  override def abilityNotOwned: String               = "Je hebt die vaardigheid niet"
  override def abilityOnCooldown: String             = "Vaardigheid in cooldown"
  override def notEnough(resource: Resource): String = s"Niet genoeg ${resourceName(resource)}"
  override def silenced: String                      = "Je bent tot zwijgen gebracht"
  override def notInRange: String                    = "Buiten bereik"
  override def notInSight: String                    = "Niet in zicht"
  override def positionNotInSight: String            = "Positie niet in zicht"
  override def notAnAlly: String                     = "Geen bondgenoot"
  override def notAnEnemy: String                    = "Geen vijand"
  override def cannotGuardYourself: String           = "Je kunt jezelf niet beschermen"
  override def needShieldStack: String               = "Je hebt een stack van je schild nodig"
  override def targetCannotBeStunned: String         = "Het doelwit kan niet gestund worden"
  override def targetDoesNotCast: String             = "Het doelwit cast niets"
  override def targetAbilityCannotBeCut: String =
    "De vaardigheid van het doelwit kan niet onderbroken worden"

  // Why a boss' ability cannot be cast
  override def noBombToExplode: String                  = "Je hebt geen enkele bom!"
  override def creepingShadowNotThere: String           = "Creeping Shadow is er niet"
  override def bossNotThere: String                     = "De boss is er niet"
  override def creepingShadowDoesNotCollideBoss: String = "Creeping Shadow raakt de boss niet"
  override def noDeathMarkTriangle: String              = "Er is geen doodsmerkdriehoek"
  override def noShadeToCall: String                    = "Er is geen Shade om te roepen"
  override def targetNotThere: String                   = "Het doelwit is er niet"
  override def twinsFarEnough: String = "De tweelingen staan ver genoeg uit elkaar"

  // Why an action is not legal (rather for the logs)
  override def gameAlreadyStarted: String                      = "Het spel is al begonnen"
  override def gameAlreadyEnded: String                        = "Het spel is al afgelopen"
  override def playerAlreadyExists: String                     = "De speler bestaat al"
  override def bossDoesNotExist(bossName: String): String      = s"Boss $bossName bestaat niet"
  override def entityDoesNotExist(entityId: Entity.Id): String = s"Entiteit $entityId bestaat niet"
  override def entitiesDoNotExist(entityId: Entity.Id, otherId: Entity.Id): String =
    s"Entiteit $entityId of $otherId bestaat niet"
  override def entityIsNotLiving(entityId: Entity.Id): String =
    s"Entiteit $entityId bestaat niet, of is niet levend"
  override def entityIsNotMoving(entityId: Entity.Id): String =
    s"Entiteit $entityId bestaat niet, of kan niet bewegen"
  override def entityHasNoAbilities(entityId: Entity.Id): String =
    s"Entiteit $entityId bestaat niet, of heeft geen vaardigheden"
  override def entityHasNoTarget(entityId: Entity.Id): String =
    s"Entiteit $entityId bestaat niet, of heeft geen doelwit"
  override def entityWouldCollide(entityId: Entity.Id): String =
    s"Entiteit $entityId zou tegen een obstakel botsen"
  override def buffDoesNotExist(buffId: Buff.Id, bearerId: Entity.Id): String =
    s"Buff $buffId bestaat niet op entiteit $bearerId"

  // Connection to the game server
  override def gameServerDoesNotAnswer: String = "de spelserver antwoordt niet"
  override def cannotSynchronizeClock: String  = "kan de klok niet synchroniseren met de spelserver"
  override def gameServerNotReady: String      = "de spelserver is niet klaar"
  override def gameServerRejectsCredentials: String =
    "de spelserver aanvaardt onze aanmeldgegevens niet"
  override def gameServerDoesNotKnowUs: String          = "de spelserver kent ons niet"
  override def gameCouldNotStart(error: String): String = s"het spel kon niet starten ($error)"

  // Game menu
  override def menu: String                  = "Menu"
  override def resume: String                = "Hervatten"
  override def surrender: String             = "Opgeven"
  override def clickAgainToSurrender: String = "Klik opnieuw om op te geven"
  override def surrenderEndsInDefeat: String = "Het gevecht eindigt in een nederlaag, voor iedereen"
  override def gameGoesOnMeanwhile: String   = "Het spel loopt intussen verder"
  override def windowed: String              = "Venster (F11)"
  override def fullscreen: String            = "Volledig scherm (F11)"

  // End of the game
  override def backToMenus: String    = "Terug naar de menu's"
  override def connectionLost: String = "Verbinding verbroken"
  override def fightLasted(minutes: Int, seconds: Int): String =
    f"Het gevecht duurde $minutes%d:$seconds%02d"
  override def gameServerClosedConnection: String = "De spelserver heeft de verbinding verbroken"

  // --- Tutorial ---

  override def tutorialIntro: String =
    "Battle for Flatland in enkele minuten: hoe het spel werkt, daarna elke klasse, met een " ++
      "oefengevecht tegen Boss 101 samen met bots."
  override def tutorialHint: String =
    "Nieuw hier? Begin met het overzicht en lees dan over de klasse die je wilt spelen."
  override def backToTheGames: String                    = "Terug naar de spellen"
  override def backToTheTutorial: String                 = "Terug naar de tutorial"
  override def next(title: String): String               = s"Volgende: $title"
  override def practiceAgainst(bossName: String): String = s"Oefenen tegen $bossName"
  override def settingUpTheGame: String                  = "Het spel wordt klaargezet..."
  override def alreadyInAGame: String =
    "Je zit al in een spel: verlaat het eerst, vanuit de lijst met spellen."
  override def practiceGameName(playerName: String): String = s"Oefening van $playerName"

  override def overview: String     = "Overzicht"
  override def overviewRole: String = "Hoe het spel werkt"
  override def overviewSummary: String =
    "Een team van vormen tegen een boss: wie wat doet, hoe je het scherm leest en hoe een gevecht verloopt."

  override def theClass(playerClass: PlayerClasses): String = playerClass match {
    case PlayerClasses.Square => s"Het ${playerClassName(playerClass)}"
    case _ => s"De ${playerClassName(playerClass)}"
  }

  override def classRole(playerClass: PlayerClasses): String = playerClass match {
    case PlayerClasses.Triangle => "Damage van dichtbij"
    case PlayerClasses.Square => "Tank"
    case PlayerClasses.Pentagon => "Damage op afstand"
    case PlayerClasses.Hexagon => "Healer"
  }

  override def classSummary(playerClass: PlayerClasses): String = playerClass match {
    case PlayerClasses.Triangle =>
      "Vecht vlak tegen zijn vijanden, besteedt zijn energie aan zware klappen en houdt de adds onder controle."
    case PlayerClasses.Square =>
      "Houdt de aandacht van de boss vast en vangt zijn klappen op, achter een schild dat het blijft opbouwen."
    case PlayerClasses.Pentagon =>
      "Beschiet zijn vijanden van ver, mikt zijn kogels en zones, en dispelt vervelende debuffs."
    case PlayerClasses.Hexagon =>
      "Houdt iedereen in leven en beheert een grote manavoorraad die maar traag aanvult."
  }

  override def overviewSections(controls: Controls): Seq[TextSection] = {
    val abilityKeys = controls.abilityKeys.take(4).map(key).mkString(", ")
    Seq(
      TextSection(
        "Welkom in Flatland",
        Seq(
          "In Battle for Flatland vecht een team van spelers, elk een geometrische vorm, samen " ++
            "tegen een boss. Het gevecht is gewonnen als de boss sterft, en verloren als alle " ++
            "spelers dood zijn: iedereen wint of verliest samen.",
          "Bosses zijn veel sterker dan welke speler ook. Wat hen verslaat, is een team waarin " ++
            "iedereen zijn rol speelt."
        )
      ),
      TextSection(
        "Tank, healer, damage",
        Seq(
          "Elke klasse heeft één rol, en elke rol steunt op de andere:",
          "- De tank (het Vierkant) houdt de aandacht van de boss vast. Hij is de enige die de " ++
            "klappen van de boss overleeft, dus de boss moet hem raken, en niemand anders.",
          "- De healer (de Zeshoek) houdt het team in leven, de tank op de eerste plaats, door " ++
            "de opgelopen schade te genezen.",
          "- De damage dealers (de Driehoek van dichtbij, de Vijfhoek van ver) halen de boss " ++
            "neer, voordat de healer zonder mana valt.",
          "Sterft de tank, dan keert de boss zich tegen spelers die zijn klappen niet aankunnen. " ++
            "Sterft de healer, dan stapelt de schade zich op. Is de damage te laag, dan duurt het " ++
            "gevecht tot de healer geen mana meer heeft."
        )
      ),
      TextSection(
        "Threat",
        Seq(
          "De boss valt de speler aan die hij het meest haat. Elk punt schade dat je hem " ++
            "toebrengt, levert je threat op. De vaardigheden van de tank genereren er veel, zodat " ++
            "de tank bovenaan de threat meter blijft (rechtsonder op het scherm).",
          "Damage dealers: hou hem in het oog, en kom niet boven de tank!"
        )
      ),
      TextSection(
        "Besturing",
        Seq(
          s"- Beweeg met ${key(controls.upKey)}, ${key(controls.leftKey)}, " ++
            s"${key(controls.downKey)} en ${key(controls.rightKey)}.",
          s"- Gebruik je vaardigheden met $abilityKeys, ... in de volgorde van je actiebalk, " ++
            "onderaan het scherm. Ga met de muis over een vaardigheid om te zien wat ze doet.",
          "- Klik op een speler of op de boss (in de arena, of op zijn kader) om hem als doelwit " ++
            s"te kiezen. ${key(controls.nextTargetKey)} kiest de dichtstbijzijnde vijand, en " ++
            s"${key(controls.targetLockInToggleKey)} laat je naar je doelwit kijken.",
          "- Sommige vaardigheden maken een zone: gebruik ze en klik dan waar je ze wilt " ++
            "plaatsen (Escape annuleert).",
          "- Escape opent het menu van het spel. Alle toetsen kun je wijzigen in de Instellingen."
        )
      ),
      TextSection(
        "Het scherm lezen",
        Seq(
          "Het tabblad \"Scherm\" toont wat je ziet tijdens een gevecht:",
          "- 1. De boss, met wat hij cast.",
          "- 2. Wanneer de vaardigheden van de boss weer beschikbaar zijn.",
          "- 3. Je team, vervaagd buiten je healbereik. Klik op een kader om de speler als doelwit te kiezen.",
          "- 4. Jij, je doelwit (met wat het cast), en je vaardigheden eronder.",
          "- 5. Wie de meeste schade aan je doelwit toebracht, en wie de boss het meest haat."
        )
      ),
      TextSection(
        "Hoe een spel verloopt",
        Seq(
          "Maak een spel aan (of neem deel aan een spel), kies je klasse en kleur, en maak je " ++
            "klaar. De maker van het spel kiest de boss, kan het team aanvullen met AI's en " ++
            "lanceert het spel.",
          "In de arena neemt iedereen zijn positie in, waarna iemand op \"Spel starten\" klikt: " ++
            "de boss komt 5 seconden later. De lobby beschrijft elke boss en zijn vaardigheden.",
          s"${Boss101.name} is de allereerste boss, en de beste plek om te leren: zijn Big Hit " ++
            s"(${formatNumber(BigHit.damageAmount)} schade) doodt iedereen behalve de tank, en " ++
            s"zijn Big Dot doet ${formatNumber(BigDot.damageOnTick)} schade elke " ++
            s"${formatPeriod(BigDot.tickRate)} bij iemand anders, wat de healer moet genezen."
        )
      ),
      TextSection(
        "Volgende",
        Seq(
          "Lees over de klasse die je wilt spelen, en oefen ze daarna tegen Boss 101 met bots."
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
    "Oefenen",
    Seq(
      s"Klaar om het te proberen? \"Oefenen tegen Boss 101\" zet een spel op waarin je als " ++
        s"${playerClassName(playerClass)} speelt samen met bots, die de andere rollen opnemen. " ++
        "Eenmaal in de arena neem je je positie in en klik je op \"Spel starten\": de boss komt " ++
        "5 seconden later. Na het gevecht kom je hier terug."
    )
  )

  private def triangle = Seq(
    TextSection(
      "Filosofie",
      Seq(
        "De Driehoek staat voor druk en precisie. Hij zit vlak op zijn vijanden, binnen " ++
          "melee-bereik, en zet zijn energie om in schade: waar de Vijfhoek moet mikken, raakt " ++
          "de Driehoek wat hij als doelwit heeft.",
        "Hij zorgt ook voor de controle in het team: als er adds opduiken, stunt hij ze en " ++
          "onderbreekt hij de spreuken die ze casten."
      )
    ),
    TextSection(
      "Energie",
      Seq(
        s"Je energie (${formatNumber(Triangle.initialResourceAmount.amount)}) vult zich in " ++
          "een vast tempo aan. Direct Hit is je belangrijkste damage, maar duur. Energy Kick " ++
          "geeft energie terug: gebruik die terwijl je wacht. Energie die aan het maximum zit, " ++
          "is verspilde energie.",
        "- Hou Upgrade Direct Hit actief: het maakt Direct Hit een tijdlang sterker.",
        "- Gebruik Direct Hit zodra je de energie hebt.",
        "- Gebruik Energy Kick als die klaar is en je te weinig energie hebt."
      )
    ),
    TextSection(
      "Blijf in leven",
      Seq(
        s"Met ${formatNumber(Triangle.initialMaxLife)} levenspunten kun je de zware klappen " ++
          "van de boss niet aan. Je damage bouwt snel threat op: hou de threat meter in het " ++
          "oog en laat het Vierkant bovenaan staan. Blijf binnen het bereik van de healer."
      )
    ),
    TextSection(
      "Controle",
      Seq(
        "Stun schakelt een vijand (maar geen boss) enkele seconden uit en onderbreekt zijn " ++
          "spreuk, tot hij schade krijgt. Cut onderbreekt een spreuk en legt de caster het " ++
          "zwijgen op. Bewaar ze voor de spreuken die ertoe doen."
      )
    )
  )

  private def square = Seq(
    TextSection(
      "Filosofie",
      Seq(
        "Het Vierkant is de muur van het team. Zijn taak is niet om schade aan te richten, " ++
          s"maar om de klappen van de boss te incasseren: het heeft " ++
          s"${formatNumber(Square.initialMaxLife)} levenspunten, twee keer zoveel als de " ++
          "anderen, een schild dat het blijft opbouwen, en vaardigheden die veel threat " ++
          "genereren.",
        "Een goede tank is de rust in de storm: zolang hij de boss vasthoudt, kan iedereen " ++
          "zijn werk doen."
      )
    ),
    TextSection(
      "Woede",
      Seq(
        "Je woede komt van de schade die je oploopt (en een beetje elke seconde). Geraakt " ++
          "worden is je brandstof! Als de boss je niet raakt, doet Enrage jezelf pijn om " ++
          "woede op te bouwen."
      )
    ),
    TextSection(
      "Brace",
      Seq(
        s"Brace voegt een stack toe aan je schild, tot ${BraceShield.maxStacks}. Elke stack " ++
          s"vermindert de schade die je oploopt met ${formatNumber((1 - BraceShield.damageMultiplier) * 100)}%. " ++
          s"Het schild verliest een stack na ${formatSeconds(BraceShield.duration)}, tenzij je " ++
          "opnieuw Brace gebruikt. De meter links van je kader toont je stacks: hou hem vol.",
        "Shield Slam verbruikt een stack voor een harde klap met extra threat."
      )
    ),
    TextSection(
      "Threat",
      Seq(
        "Gebruik Taunt op de boss zodra hij aankomt, en blijf hem dan raken (Hammer Hit, " ++
          "Shield Slam, Cleave) om bovenaan de threat meter te blijven. Komt iemand boven je, " ++
          "gebruik dan opnieuw Taunt.",
        "Guard beschermt een bondgenoot een tijdlang: een deel van de schade die hij oploopt, " ++
          "krijg jij in zijn plaats, bv. wanneer hij een gevaarlijke debuff draagt."
      )
    ),
    TextSection(
      "Tegen Boss 101",
      Seq(
        s"Zijn Big Hit doet ${formatNumber(BigHit.damageAmount)} schade, na een cast van " ++
          s"${formatSeconds(BigHit.castingTime)}: hou de castbalk van de boss in het oog, en " ++
          "zorg dat je stacks van Brace er staan wanneer hij inslaat."
      )
    )
  )

  private def pentagon = Seq(
    TextSection(
      "Filosofie",
      Seq(
        "De Vijfhoek is de artillerie van het team: hij richt zijn schade aan van ver, buiten " ++
          "het bereik van de boss. Zijn kracht vraagt precisie: zijn kogels gaan waar je mikt, " ++
          "en zijn zones moeten geplaatst worden.",
        "Hij is ook de enige die de vervelende debuffs kan dispellen die sommige bosses op " ++
          "spelers leggen."
      )
    ),
    TextSection(
      "Mikken",
      Seq(
        "- Pentagon Bullet vliegt naar je muis (niet naar je doelwit), en raakt de eerste " ++
          "vijand op zijn weg: zorg voor een vrije lijn naar de boss.",
        "- Pentagon Zone: gebruik ze en klik dan waar je ze wilt plaatsen. Vijanden die erin " ++
          "staan, krijgen schade over tijd: leg ze onder de boss, die de tank op zijn plaats houdt."
      )
    ),
    TextSection(
      "Mana",
      Seq(
        s"Je vaardigheden kosten mana (${formatNumber(Pentagon.initialResourceAmount.amount)}). " ++
          "Blijf schieten, maar hou wat over voor wanneer een dispel nodig is."
      )
    ),
    TextSection(
      "Dispel",
      Seq(
        "Penta Dispel verwijdert alle effecten die gedispeld kunnen worden van een bondgenoot, " ++
          "ook de goede (zoals de heal over time van de healer!). Latere bosses leggen debuffs " ++
          "die snel gedispeld moeten worden. De Big Dot van Boss 101 kan dat niet: de healer " ++
          "moet de schade wegwerken."
      )
    ),
    TextSection(
      "Blijf in leven",
      Seq(
        s"Met ${formatNumber(Pentagon.initialMaxLife)} levenspunten mag de boss zich niet " ++
          "tegen jou keren: hou de threat meter in het oog. Blijf weg van de boss, maar binnen " ++
          "het bereik van de healer."
      )
    )
  )

  private def hexagon = Seq(
    TextSection(
      "Filosofie",
      Seq(
        "De Zeshoek houdt het team overeind. Hij kijkt meer naar de kaders van het team dan " ++
          "naar de arena: wie verliest leven, wie staat op het punt een zware klap te krijgen.",
        s"Zijn manavoorraad is groot (${formatNumber(Hexagon.initialResourceAmount.amount)}), " ++
          "maar vult traag aan: de kunst van de healer is de juiste heal op de juiste speler " ++
          "te gebruiken, op het juiste moment."
      )
    ),
    TextSection(
      "Je heals",
      Seq(
        "- Flash Heal geneest veel, maar het casten duurt even: je moet stilstaan.",
        "- Heal Over Time is goedkoop en direct, en kan in beweging gebruikt worden: leg het " ++
          "op de spelers die voortdurend schade oplopen (de tank, de drager van Big Dot).",
        "- Hexagon Zone geneest iedereen die erin staat: voor wanneer het team bij elkaar staat."
      )
    ),
    TextSection(
      "Doelwit kiezen",
      Seq(
        "Klik op een kader links op het scherm om een speler als doelwit te kiezen. Een " ++
          "vervaagd kader is een speler buiten je healbereik: kom dichterbij.",
        "Met \"Mouse-over\" ingeschakeld in de Instellingen gaan je heals naar de speler wiens " ++
          "kader onder de muis staat, zonder dat je doelwit verandert."
      )
    ),
    TextSection(
      "Tegen Boss 101",
      Seq(
        "De tank krijgt de klappen van de boss: hou hem gezond, vooral voor een Big Hit. Big Dot " ++
          s"doet ${formatNumber(BigDot.damageOnTick)} schade elke ${formatPeriod(BigDot.tickRate)} " ++
          s"gedurende ${formatSeconds(BigDot.duration)} bij iemand anders: een heal over time op " ++
          "die speler vangt het meeste op. Genees geen spelers die vol leven zitten: dat is " ++
          "verspilde mana."
      )
    )
  )

  // Previews of the pages
  override def arena: String     = "Arena"
  override def screen: String    = "Scherm"
  override def hoverHint: String = "Ga met de muis over de vaardigheden, klik op de kaders"

  override def legendBoss: String          = "De boss: zijn leven, en wat hij cast."
  override def legendBossCooldowns: String = "Wanneer de vaardigheden van de boss terugkomen."
  override def legendTeam: String = "Je team. Klik op een kader om de speler als doelwit te kiezen."
  override def legendYouAndTarget: String      = "Jij en je doelwit, boven je vaardigheden."
  override def legendMeters: String            = "Damage op je doelwit, en de threat van de boss."
  override def legendTriangleEnergy: String    = "Je energie: gebruik ze, ze vult zich gestaag aan."
  override def legendTriangleAbilities: String = "Je vaardigheden: Direct Hit, Energy Kick, ..."
  override def legendTriangleDamage: String    = "Jouw plaats op de damage meter: bovenaan!"
  override def legendTriangleThreat: String =
    "Maar niet op de threat meter: die is voor het Vierkant."
  override def legendSquareStacks: String = "De stacks van je schild (Brace): hou ze actief."
  override def legendSquareRage: String   = "Je woede, verdiend door schade op te lopen."
  override def legendSquareBigHit: String =
    "De boss cast Big Hit op jou: zorg dat je stacks er staan!"
  override def legendSquareThreat: String = "Je moet bovenaan de threat meter blijven."
  override def legendPentagonMana: String = "Je mana."
  override def legendPentagonAim: String =
    "Kogels vliegen naar de muis, zones plaats je met een klik."
  override def legendPentagonDebuffs: String =
    "De debuffs van de spelers: dispel wat gedispeld kan worden."
  override def legendPentagonDamage: String = "Jouw plaats op de damage meter."
  override def legendHexagonTeam: String =
    "Je team: klik op een kader om de speler als doelwit te kiezen."
  override def legendHexagonFaded: String = "Vervaagd: buiten je healbereik. Kom dichterbij!"
  override def legendHexagonMana: String  = "Je mana: die vult traag aan."
  override def legendHexagonTarget: String =
    "Je doelwit, de tank, met je heal over time."

  override def arenaHealerRange: String = "Bereik van de healer"
  override def arenaBoss: String        = "De boss"
  override def arenaSquare: String      = "Het Vierkant houdt de boss vast en kijkt hem aan"
  override def arenaTriangle: String    = "De Driehoek raakt hem van dichtbij, van achteren"
  override def arenaPentagons: String   = "De Vijfhoeken schieten van ver"
  override def arenaHexagon: String     = "De Zeshoek geneest, binnen ieders bereik"
  override def arenaLegendPositions: String =
    "De tank draait de boss weg van de anderen, die hem van achteren of van ver raken."
  override def arenaLegendRange: String =
    "Iedereen blijft binnen het bereik van de healer (de kaders van het team vervagen erbuiten)."
}
