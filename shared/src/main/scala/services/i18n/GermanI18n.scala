package services.i18n

import gamelogic.abilities.Ability
import gamelogic.abilities.boss.boss101.{BigDot, BigHit}
import gamelogic.abilities.pentagon.CreatePentagonZone
import gamelogic.buffs.Buff
import gamelogic.buffs.abilities.classes.{BraceShield, Facets, Fractured}
import gamelogic.entities.{Entity, Resource}
import gamelogic.entities.boss.Boss101
import gamelogic.entities.classes.{Hexagon, Pentagon, Square, Triangle}
import gamelogic.entities.classes.pentagon.SteadyAim
import models.bff.ingame.Controls
import models.bff.outofgame.PlayerClasses

object GermanI18n extends I18n {

  // --- Common ---

  override def playerClassName(playerClass: PlayerClasses): String = playerClass match {
    case PlayerClasses.Triangle => "Dreieck"
    case PlayerClasses.Square => "Quadrat"
    case PlayerClasses.Pentagon => "Fünfeck"
    case PlayerClasses.Hexagon => "Sechseck"
  }

  override def resourceName(resource: Resource): String = resource match {
    case Resource.Mana => "Mana"
    case Resource.Energy => "Energie"
    case Resource.Rage => "Wut"
    case Resource.NoResource => "nichts"
  }

  override def mouseButton(number: String): String = s"Maustaste $number"

  override def formatNumber(x: Double): String = super.formatNumber(x).replace('.', ',')

  override def formatSeconds(millis: Long): String =
    s"${formatNumber(millis / 1000.0)} ${if millis > 1000L then "Sekunden" else "Sekunde"}"

  override def settings: String    = "Einstellungen"
  override def leaveGame: String   = "Spiel verlassen"
  override def quit: String        = "Beenden"
  override def name: String        = "Name"
  override def playerClass: String = "Klasse"
  override def colour: String      = "Farbe"
  override def ready: String       = "Bereit"
  override def notReady: String    = "Nicht bereit"
  override def boss: String        = "Boss"
  override def victory: String     = "Sieg!"
  override def defeat: String      = "Niederlage..."
  override def startGame: String   = "Kampf starten"

  override def castingTime(time: String): String = s"Zauberzeit: $time"
  override def cooldown(time: String): String    = s"Abklingzeit: $time"
  override def cost(amount: String): String      = s"Kosten: $amount"

  // --- Menus ---

  override def fullscreenHint: String         = "F11: Vollbild"
  override def connectingToGameServer: String = "Verbindung zum Spielserver wird hergestellt"
  override def leavingTheGame: String         = "Spiel wird verlassen"
  override def connectionToGameServerLost: String =
    "Die Verbindung zum Spielserver wurde unterbrochen"
  override def cannotConnectToGameServer(error: String): String =
    s"Keine Verbindung zum Spielserver möglich: $error"
  override def couldNotLeaveGame(error: String): String =
    s"Das Spiel konnte nicht verlassen werden: $error"

  override def chooseYourLanguage: String = "Wähle deine Sprache"

  // Login
  override def welcomeToBattleForFlatland: String = "Willkommen bei Battle for Flatland!"
  override def serverAddress: String              = "Serveradresse"
  override def serverAddressExample: String   = "z. B. bff.example.com oder http://81.12.34.56:9000"
  override def choosePseudo: String           = "Bitte wähle einen Spielernamen"
  override def yourPseudo: String             = "Dein Spielername"
  override def loggingIn: String              = "Anmeldung läuft"
  override def enterBattleForFlatland: String = "Battle for Flatland betreten"
  override def serverError(message: String): String = s"Server: $message"
  override def pseudoError(message: String): String = s"Spielername: $message"
  override def cannotLogIn(address: String, error: String): String =
    s"Anmeldung bei $address nicht möglich: $error"
  override def invalidPort(port: String): String = s"Ungültiger Port: $port"
  override def invalidAddress(address: String): String =
    s"Ungültige Adresse: $address (erwartet: Host oder Host:Port)"
  override def outdatedVersion: String =
    "Eine neue Version von Battle for Flatland ist da! Lade sie erneut von der Website herunter, um weiterzuspielen."
  override def downloadTheNewVersion: String = "Neue Version herunterladen"

  // Games
  override def welcomePlayer(playerName: String): String   = s"Willkommen, $playerName"
  override def learnToPlay: String                         = "Spielanleitung"
  override def joinItBack: String                          = "Zurückkehren"
  override def gameNamePlaceholder: String                 = "Spielname"
  override def defaultGameName(playerName: String): String = s"Spiel von $playerName"
  override def createGame: String                          = "Spiel erstellen"
  override def createdBy: String                           = "Erstellt von"
  override def players: String                             = "Spieler"
  override def loadingTheGames: String                     = "Spiele werden geladen"
  override def noGames: String  = "Derzeit gibt es keine Spiele. Erstelle doch eins!"
  override def join: String     = "Beitreten"
  override def joinBack: String = "Zurückkehren"
  override def currentlyInGame(gameName: String): String =
    s"Du bist gerade im Spiel „$gameName“."

  // Lobby of a game
  override def loadingTheGame: String                 = "Spiel wird geladen"
  override def loggedInAs(playerName: String): String = s"Angemeldet als $playerName"
  override def gameCreatedBy(creator: String, gameId: String): String =
    s"Erstellt von $creator - Spiel $gameId"
  override def kick: String                    = "Kicken"
  override def you(playerName: String): String = s"$playerName (du)"
  override def ai(playerName: String): String  = s"$playerName (KI)"
  override def classNotChosen: String          = "Nicht gewählt"
  override def addAI: String                   = "KI hinzufügen"
  override def fillWithAIs: String             = "Mit KIs füllen"
  override def launchGame: String              = "Spiel starten!"
  override def waitingForCreatorToLaunch: String =
    "Warte darauf, dass der Ersteller das Spiel startet"
  override def gameIsStarting: String = "Das Spiel startet, bitte warte, bis alles eingerichtet ist"
  override def gameServerNotReadyYet: String        = "Der Spielserver ist noch nicht bereit."
  override def gameServerRunsOnYourComputer: String = "Der Spielserver läuft auf deinem Computer"
  override def gameServerRunsOnComputerOf(host: String): String =
    s"Der Spielserver läuft auf dem Computer von $host"
  override def downloadingJavaRuntime: String = "Java-Laufzeitumgebung wird heruntergeladen"
  override def downloadingGameServer: String  = "Spielserver wird heruntergeladen"
  override def megabytes(received: Int, size: Int): String = s"$received / $size MB"
  override def gameClosedByCreator: String   = "Das Spiel wurde von seinem Ersteller geschlossen"
  override def notInThisGame: String         = "Du bist nicht (mehr) in diesem Spiel"
  override def everyoneReady: String         = "Alle sind bereit und aufgestellt, los geht's!"
  override def thingsPreventingStart: String = "Folgendes verhindert den Start des Spiels:"

  // Docs of the bosses and classes, in the lobby
  override def classes: String = "Klassen"
  override def bossFacts(life: Int, intendedFor: Int): String =
    s"$life Lebenspunkte, für $intendedFor Spieler"
  override def classFacts(
      life: Int,
      resourceAmount: Int,
      resource: Resource,
      isYours: Boolean
  ): String =
    s"$life Lebenspunkte, $resourceAmount ${resourceName(resource)}" +
      (if isYours then " (deine Klasse)" else "")
  override def abilities: String = "Fähigkeiten"

  // Settings
  override def controls: String                       = "Steuerung"
  override def general: String                        = "Allgemein"
  override def goUp: String                           = "Nach oben"
  override def goDown: String                         = "Nach unten"
  override def goLeft: String                         = "Nach links"
  override def goRight: String                        = "Nach rechts"
  override def nextTarget: String                     = "Nächstes Ziel"
  override def faceTheTarget: String                  = "Zum Ziel drehen"
  override def abilityNumber(number: Int): String     = s"Fähigkeit $number"
  override def markerCross: String                    = "Kreuz"
  override def markerLozenge: String                  = "Raute"
  override def markerMoon: String                     = "Mond"
  override def markerSquare: String                   = "Quadrat"
  override def markerStar: String                     = "Stern"
  override def markerTriangle: String                 = "Dreieck"
  override def markerOnTarget(marker: String): String = s"$marker auf Ziel"
  override def markerOnGround(marker: String): String = s"$marker auf Boden"
  override def bindingsHelp: String =
    "Klicke auf eine Belegung und drücke dann eine Taste (auch mit Shift) oder eine Maustaste. Änderungen gelten ab dem nächsten Spiel."
  override def mouseOver: String = "Mouseover"
  override def mouseOverHelp: String =
    "Ist das Ziel für eine Fähigkeit ungültig, wirkt sie auf den Rahmen unter der Maus."
  override def resetToDefaults: String = "Zurücksetzen"
  override def escapeToCancel: String  = "Escape zum Abbrechen"
  override def pressKeyFor(action: String): String =
    s"Drücke eine Taste oder Maustaste für „$action“"
  override def boundToSeveralActions(key: String): String = s"$key ist mehreren Aktionen zugewiesen"
  override def language: String                           = "Sprache"
  override def languageHelp: String                       = "Die Sprache der Menüs und der Spiele."
  override def hudScale: String                           = "HUD-Größe"
  override def hudScaleHelp: String =
    "Die Größe der Rahmen und der Aktionsleiste, unabhängig von der Fenstergröße. Gilt ab dem nächsten Spiel."
  override def hostGameServers: String = "Spielserver hosten"
  override def hostGameServersHelp: String =
    "Der Spielserver eines Spiels läuft auf dem Computer eines seiner Spieler. Damit er ihn ausführen " +
      "kann, lädt der Client ihn samt allem, was er dafür braucht (einige Dutzend MB, einmalig), herunter, " +
      "sobald du einem Spiel beitrittst. Schalte dies aus, um nichts herunterzuladen, z. B. im " +
      "Mobilfunknetz: Dann führen ihn die anderen Spieler aus."
  override def mainVolume: String      = "Gesamtlautstärke"
  override def abilitiesVolume: String = "Fähigkeiten"
  override def volumeOff: String       = "Aus"
  override def volumesHelp: String =
    "Von „Aus“ bis 10. Die Lautstärke jeder Geräuschart ist relativ zur Gesamtlautstärke."
  override def done: String = "Fertig"

  // --- In game ---

  override def placeYourselves: String =
    "Stellt euch auf und klickt dann auf „Kampf starten“: Der Boss erscheint 5 Sekunden später"
  override def bossArrivesIn: String    = "Der Boss erscheint in"
  override def clickToPlaceZone: String = "Klicke, um die Zone zu platzieren (Escape zum Abbrechen)"
  override def waitingForGameToStart: String  = "Warte auf den Spielstart..."
  override def bossAbilities: String          = "Boss-Fähigkeiten"
  override def threat: String                 = "Bedrohung"
  override def damage: String                 = "Schaden"
  override def damageTo(name: String): String = s"Schaden: $name"
  override def dead: String                   = "Tot"

  // Casting
  override def youAreDead: String                  = "Du bist tot"
  override def noTarget: String                    = "Kein Ziel"
  override def noAbilityOnKey(number: Int): String = s"Keine Fähigkeit auf Taste $number"
  override def abilityNotSupported(ability: Ability.AbilityId): String =
    s"Fähigkeit $ability wird noch nicht unterstützt"
  override def abilityNotPlacedWithMouse(ability: Ability.AbilityId): String =
    s"Fähigkeit $ability wird nicht mit der Maus platziert"

  // Why an ability cannot be cast (shown to the player)
  override def alreadyCasting: String                = "Du wirkst bereits einen Zauber"
  override def abilityNotOwned: String               = "Du hast diese Fähigkeit nicht"
  override def abilityOnCooldown: String             = "Fähigkeit klingt noch ab"
  override def notEnough(resource: Resource): String = s"Nicht genug ${resourceName(resource)}"
  override def silenced: String                      = "Du bist zum Schweigen gebracht"
  override def notInRange: String                    = "Außer Reichweite"
  override def notInSight: String                    = "Nicht in Sicht"
  override def positionNotInSight: String            = "Position nicht in Sicht"
  override def notAnAlly: String                     = "Kein Verbündeter"
  override def notAnEnemy: String                    = "Kein Gegner"
  override def cannotGuardYourself: String           = "Du kannst dich nicht selbst beschützen"
  override def needShieldStack: String               = "Du brauchst einen Stapel deines Schilds"
  override def needAllFacets: String                 = "Alle deine Facetten müssen leuchten"
  override def targetCannotBeStunned: String         = "Das Ziel kann nicht betäubt werden"
  override def targetDoesNotCast: String             = "Das Ziel wirkt keinen Zauber"
  override def targetAbilityCannotBeCut: String =
    "Die Fähigkeit des Ziels kann nicht unterbrochen werden"

  // Why a boss' ability cannot be cast
  override def noBombToExplode: String                  = "Du besitzt keine Bombe!"
  override def creepingShadowNotThere: String           = "Creeping Shadow ist nicht da"
  override def bossNotThere: String                     = "Der Boss ist nicht da"
  override def creepingShadowDoesNotCollideBoss: String = "Creeping Shadow berührt den Boss nicht"
  override def noDeathMarkTriangle: String              = "Es gibt kein Todesmal-Dreieck"
  override def noShadeToCall: String                    = "Es gibt keine Shade zum Herbeirufen"
  override def targetNotThere: String                   = "Das Ziel ist nicht da"
  override def twinsFarEnough: String = "Die Zwillinge sind weit genug voneinander entfernt"

  // Why an action is not legal (rather for the logs)
  override def gameAlreadyStarted: String                 = "Das Spiel hat bereits begonnen"
  override def gameAlreadyEnded: String                   = "Das Spiel ist bereits beendet"
  override def playerAlreadyExists: String                = "Der Spieler existiert bereits"
  override def bossDoesNotExist(bossName: String): String = s"Boss $bossName existiert nicht"
  override def entityDoesNotExist(entityId: Entity.Id): String =
    s"Entität $entityId existiert nicht"
  override def entitiesDoNotExist(entityId: Entity.Id, otherId: Entity.Id): String =
    s"Entität $entityId oder $otherId existiert nicht"
  override def entityIsNotLiving(entityId: Entity.Id): String =
    s"Entität $entityId existiert nicht oder ist nicht lebendig"
  override def entityIsNotMoving(entityId: Entity.Id): String =
    s"Entität $entityId existiert nicht oder kann sich nicht bewegen"
  override def entityHasNoAbilities(entityId: Entity.Id): String =
    s"Entität $entityId existiert nicht oder hat keine Fähigkeiten"
  override def entityHasNoTarget(entityId: Entity.Id): String =
    s"Entität $entityId existiert nicht oder hat kein Ziel"
  override def entityWouldCollide(entityId: Entity.Id): String =
    s"Entität $entityId würde mit einem Hindernis kollidieren"
  override def buffDoesNotExist(buffId: Buff.Id, bearerId: Entity.Id): String =
    s"Buff $buffId existiert nicht auf Entität $bearerId"

  // Connection to the game server
  override def gameServerDoesNotAnswer: String = "der Spielserver antwortet nicht"
  override def cannotSynchronizeClock: String =
    "die Uhr kann nicht mit dem Spielserver synchronisiert werden"
  override def gameServerNotReady: String = "der Spielserver ist nicht bereit"
  override def gameServerRejectsCredentials: String =
    "der Spielserver akzeptiert unsere Anmeldedaten nicht"
  override def gameServerDoesNotKnowUs: String          = "der Spielserver kennt uns nicht"
  override def gameCouldNotStart(error: String): String = s"das Spiel konnte nicht starten ($error)"

  // Game menu
  override def menu: String                  = "Menü"
  override def resume: String                = "Fortsetzen"
  override def surrender: String             = "Aufgeben"
  override def clickAgainToSurrender: String = "Zum Aufgeben erneut klicken"
  override def surrenderEndsInDefeat: String = "Der Kampf endet mit einer Niederlage, für alle"
  override def gameGoesOnMeanwhile: String   = "Das Spiel läuft währenddessen weiter"
  override def windowed: String              = "Fenster (F11)"
  override def fullscreen: String            = "Vollbild (F11)"

  // End of the game
  override def backToMenus: String    = "Zurück zum Menü"
  override def connectionLost: String = "Verbindung verloren"
  override def fightLasted(minutes: Int, seconds: Int): String =
    f"Der Kampf dauerte $minutes%d:$seconds%02d"
  override def gameServerClosedConnection: String = "Der Spielserver hat die Verbindung getrennt"

  // Statistiken des Kampfes, am Ende des Spiels
  override def fightSummary: String = "Übersicht"
  override def damageDone: String   = "Schaden"
  override def healing: String      = "Heilung"
  override def damageTaken: String  = "Erlitten"
  override def lastMoments: String  = "Letzte Momente"
  override def player: String       = "Spieler"
  override def deaths: String       = "Tode"
  override def bossLifeLeft(bossName: String, percent: Int): String =
    s"$bossName hatte noch $percent % seines Lebens"
  override def nobodyDied: String = "Niemand ist gestorben"
  override def diedAt(minutes: Int, seconds: Int, killedBy: String): String =
    f"Gestorben bei $minutes%d:$seconds%02d ($killedBy)"
  override def lastSecondsOf(name: String, seconds: Int, died: Boolean): String =
    if died then s"Die letzten $seconds Sekunden von $name vor dem Tod"
    else s"Die letzten $seconds Sekunden von $name"
  override def nothingInLastSeconds: String =
    "In diesen Sekunden wurde die Figur weder getroffen noch geheilt"
  override def life: String                   = "Leben"
  override def overheal(percent: Int): String = s"$percent % Überheilung"
  override def bulletsHit(count: Int): String = s"Treffende Geschosse: $count"
  override def facetsWasted(count: Int, percent: Int): String =
    s"Verschwendet, da alle Facetten leuchteten: $count ($percent %)"
  override def pentagramsCast(count: Int): String = s"Pentagramme: $count"
  override def braceStacks(stacks: Int): String =
    if stacks == 1 then "1 Stapel" else s"$stacks Stapel"
  override def timeWithBraceStacks: String =
    "Zeit mit jeder Anzahl an Stapeln von Brace, am Leben"
  override def damagePreventedByBrace(amount: String): String =
    s"Durch Brace verhinderter Schaden: $amount"
  override def noClassStatistics: String = "Keine Statistiken für die Klassen dieses Teams"

  // --- Tutorial ---

  override def tutorialIntro: String =
    "Battle for Flatland in wenigen Minuten: wie das Spiel funktioniert, dann jede Klasse, mit einem " ++
      "Übungskampf gegen Boss 101 an der Seite von Bots."
  override def tutorialHint: String =
    "Neu hier? Beginne mit dem Überblick und lies dann alles über die Klasse, die du spielen möchtest."
  override def backToTheGames: String                    = "Zu den Spielen"
  override def backToTheTutorial: String                 = "Zur Anleitung"
  override def next(title: String): String               = s"Weiter: $title"
  override def practiceAgainst(bossName: String): String = s"Gegen $bossName üben"
  override def settingUpTheGame: String                  = "Spiel wird eingerichtet..."
  override def alreadyInAGame: String =
    "Du bist bereits in einem Spiel: Verlasse es zuerst, in der Spieleliste."
  override def practiceGameName(playerName: String): String = s"Übung von $playerName"

  override def overview: String     = "Überblick"
  override def overviewRole: String = "So funktioniert das Spiel"
  override def overviewSummary: String =
    "Ein Team aus Formen gegen einen Boss: wer was tut, wie man den Bildschirm liest und wie ein Kampf abläuft."

  override def theClass(playerClass: PlayerClasses): String = s"Das ${playerClassName(playerClass)}"

  override def classRole(playerClass: PlayerClasses): String = playerClass match {
    case PlayerClasses.Triangle => "Nahkampfschaden"
    case PlayerClasses.Square => "Tank"
    case PlayerClasses.Pentagon => "Fernkampfschaden"
    case PlayerClasses.Hexagon => "Heiler"
  }

  override def classSummary(playerClass: PlayerClasses): String = playerClass match {
    case PlayerClasses.Triangle =>
      "Kämpft direkt am Gegner, setzt seine Energie in harte Treffer um und kontrolliert die Adds."
    case PlayerClasses.Square =>
      "Zieht die Aufmerksamkeit des Bosses auf sich und steckt seine Schläge ein, hinter einem Schild, den es ständig verstärkt."
    case PlayerClasses.Pentagon =>
      "Beschießt seine Gegner aus der Ferne, zielt mit seinen Geschossen und Zonen und bannt fiese Debuffs."
    case PlayerClasses.Hexagon =>
      "Hält alle am Leben, mit einem großen, aber sich langsam regenerierenden Manavorrat, den es einzuteilen gilt."
  }

  override def story: TextSection =
    TextSection(
      "Die Geschichte",
      Seq(
        "Flatland (aus dem Roman von Edwin A. Abbott) ist eine Welt in zwei Dimensionen, " ++
          "deren Bewohner Vielecke sind.",
        "Ganz unten in ihrer Rangordnung stehen die gleichschenkligen Dreiecke. Von " ++
          "Generation zu Generation kommen sie dem gleichseitigen Dreieck näher. Danach " ++
          "gewinnt jede Generation eine Seite hinzu und steigt einen Rang auf.",
        "Ganz oben stehen die Kreise: Vielecke mit so vielen Seiten, dass niemand sie von " ++
          "einem Kreis unterscheiden kann. Sie sind die Hohepriester.",
        "Doch seit Kurzem breitet sich eine Fäulnis unter den Hohepriestern aus. Sie sind " ++
          "nicht mehr die guten Hirten von einst: Sie missbrauchen ihre Macht und " ++
          "unterdrücken das Volk von Flatland.",
        "Eine Schar einfacherer Vielecke (Dreiecke, Quadrate, Fünfecke und Sechsecke) hat " ++
          "sich gegen sie erhoben. Du bist einer dieser Rebellen, und du schlägst als Erster " ++
          "zu!",
        "Doch die Hohepriester sind unglaublich mächtig: Niemand kann sie allein besiegen. Du " ++
          "bekämpfst sie mit deinen Gefährten, und jeder von euch hat eine Rolle zu spielen.",
        "Tritt ein in die Battle for Flatland!"
      )
    )

  override def overviewSections(controls: Controls): Seq[TextSection] = {
    val abilityKeys = controls.abilityKeys.take(4).map(key).mkString(", ")
    Seq(
      story,
      TextSection(
        "Willkommen in Flatland",
        Seq(
          "In Battle for Flatland kämpft ein Team von Spielern, jeder eine geometrische Form, " ++
            "gemeinsam gegen einen Boss. Der Kampf ist gewonnen, wenn der Boss stirbt, und verloren, " ++
            "wenn alle Spieler tot sind: Alle gewinnen oder verlieren gemeinsam.",
          "Bosse sind viel stärker als jeder einzelne Spieler. Besiegen kann sie nur ein Team, in dem " ++
            "jeder seine Rolle spielt."
        )
      ),
      TextSection(
        "Tank, Heiler, Schaden",
        Seq(
          "Jede Klasse hat eine Rolle, und jede Rolle ist auf die anderen angewiesen:",
          "- Der Tank (das Quadrat) zieht die Aufmerksamkeit des Bosses auf sich. Nur er überlebt " ++
            "die Schläge des Bosses, also muss der Boss ihn treffen und niemanden sonst.",
          "- Der Heiler (das Sechseck) hält das Team am Leben, zuerst den Tank, indem er den " ++
            "erlittenen Schaden heilt.",
          "- Die Schadensverursacher (das Dreieck im Nahkampf, das Fünfeck aus der Ferne) bringen " ++
            "den Boss zu Fall, bevor dem Heiler das Mana ausgeht.",
          "Stirbt der Tank, wendet sich der Boss Spielern zu, die seine Treffer nicht aushalten. " ++
            "Stirbt der Heiler, summiert sich der Schaden. Ist der Schaden zu gering, dauert der " ++
            "Kampf, bis dem Heiler das Mana ausgeht."
        )
      ),
      TextSection(
        "Bedrohung",
        Seq(
          "Der Boss greift den Spieler an, den er am meisten hasst. Jeder Schadenspunkt, den er " ++
            "erleidet, erzeugt Bedrohung gegenüber dem Verursacher. Die Fähigkeiten des Tanks " ++
            "erzeugen viel davon, damit der Tank oben auf der Bedrohungsanzeige bleibt (unten " ++
            "rechts auf dem Bildschirm).",
          "Schadensverursacher: Behaltet sie im Auge und kommt nicht über den Tank!"
        )
      ),
      TextSection(
        "Steuerung",
        Seq(
          s"- Bewege dich mit ${key(controls.upKey)}, ${key(controls.leftKey)}, " ++
            s"${key(controls.downKey)} und ${key(controls.rightKey)}.",
          s"- Wirke deine Fähigkeiten mit $abilityKeys, ... in der Reihenfolge deiner Aktionsleiste " ++
            "unten auf dem Bildschirm. Fahre mit der Maus über eine Fähigkeit, um zu lesen, was sie bewirkt.",
          "- Klicke auf einen Spieler oder den Boss (in der Arena oder auf seinen Rahmen), um ihn " ++
            s"anzuvisieren. ${key(controls.nextTargetKey)} visiert den nächstgelegenen Gegner an, und " ++
            s"${key(controls.targetLockInToggleKey)} dreht dich zu deinem Ziel.",
          "- Manche Fähigkeiten erzeugen eine Zone: Wirke sie und klicke dann, wohin sie soll " ++
            "(Escape bricht ab).",
          "- Escape öffnet das Spielmenü. Alle Tasten lassen sich in den „Einstellungen“ ändern."
        )
      ),
      TextSection(
        "Den Bildschirm lesen",
        Seq(
          "Der Reiter „Bildschirm“ zeigt, was du während eines Kampfes siehst:",
          "- 1. Der Boss, mit dem, was er wirkt.",
          "- 2. Wann die Fähigkeiten des Bosses wieder bereit sind.",
          "- 3. Dein Team, verblasst, wenn außerhalb deiner Heilreichweite. Klicke auf einen Rahmen, " ++
            "um den Spieler anzuvisieren.",
          "- 4. Du, dein Ziel (mit dem, was es wirkt) und darunter deine Fähigkeiten.",
          "- 5. Wer deinem Ziel den meisten Schaden zugefügt hat und wen der Boss am meisten hasst."
        )
      ),
      TextSection(
        "Ablauf eines Spiels",
        Seq(
          "Erstelle ein Spiel (oder tritt einem bei), wähle deine Klasse und Farbe und mach dich " ++
            "bereit. Der Ersteller des Spiels wählt den Boss, kann das Team mit KIs auffüllen und " ++
            "startet das Spiel.",
          "In der Arena stellen sich alle auf, dann klickt jemand auf „Kampf starten“: Der Boss " ++
            "erscheint 5 Sekunden später. Die Lobby beschreibt jeden Boss und seine Fähigkeiten.",
          s"${Boss101.name} ist der allererste Boss und der beste Ort zum Lernen: Sein Big Hit " ++
            s"(${formatNumber(BigHit.damageAmount)} Schaden) tötet jeden außer dem Tank, und sein " ++
            s"Big Dot verursacht ${formatNumber(BigDot.damageOnTick)} Schaden alle " ++
            s"${formatPeriod(BigDot.tickRate)} bei jemand anderem, den der Heiler heilen muss."
        )
      ),
      TextSection(
        "Weiter",
        Seq(
          "Lies alles über die Klasse, die du spielen möchtest, und übe sie dann mit Bots gegen Boss 101."
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
    "Übung",
    Seq(
      s"Bereit für einen Versuch? „Gegen Boss 101 üben“ richtet ein Spiel ein, in dem du das " ++
        s"${playerClassName(playerClass)} an der Seite von Bots spielst, die die anderen Rollen " ++
        "übernehmen. Stell dich in der Arena auf und klicke dann auf „Kampf starten“: Der Boss " ++
        "erscheint 5 Sekunden später. Wenn der Kampf endet, kommst du hierher zurück."
    )
  )

  private def triangle = Seq(
    TextSection(
      "Philosophie",
      Seq(
        "Das Dreieck steht für Druck und Präzision. Es lebt direkt am Gegner, in Nahkampfreichweite, " ++
          "und verwandelt seine Energie in Schaden: Wo das Fünfeck zielen muss, trifft das Dreieck, " ++
          "was es anvisiert.",
        "Es ist auch die Kontrolle des Teams: Wenn Adds auftauchen, betäubt es sie und unterbricht " ++
          "die Zauber, die sie wirken."
      )
    ),
    TextSection(
      "Energie",
      Seq(
        s"Deine Energie (${formatNumber(Triangle.initialResourceAmount.amount)}) füllt sich " ++
          "gleichmäßig wieder auf. Direct Hit ist dein Hauptschaden, aber teuer. Energy Kick gibt " ++
          "dir Energie zurück: Nutze ihn, während du wartest. Energie am Maximum ist verschwendete Energie.",
        "- Halte Upgrade Direct Hit aktiv: Es macht Direct Hit eine Zeit lang stärker.",
        "- Setze Direct Hit ein, wann immer du genug Energie hast.",
        "- Setze Energy Kick ein, wenn er bereit ist und dir Energie fehlt."
      )
    ),
    TextSection(
      "Am Leben bleiben",
      Seq(
        s"Mit ${formatNumber(Triangle.initialMaxLife)} Lebenspunkten hältst du die schweren Treffer " ++
          "des Bosses nicht aus. Dein Schaden baut schnell Bedrohung auf: Behalte die " ++
          "Bedrohungsanzeige im Auge und lass das Quadrat oben bleiben. Bleib in Reichweite des Heilers."
      )
    ),
    TextSection(
      "Kontrolle",
      Seq(
        "Stun hält einen Gegner (aber keinen Boss) für ein paar Sekunden auf und unterbricht seinen " ++
          "Zauber, bis er Schaden erleidet. Cut unterbricht einen Zauber und bringt den Zaubernden " ++
          "zum Schweigen. Heb sie dir für die Zauber auf, auf die es ankommt."
      )
    )
  )

  private def square = Seq(
    TextSection(
      "Philosophie",
      Seq(
        "Das Quadrat ist die Mauer des Teams. Seine Aufgabe ist nicht, Schaden zu verursachen, " ++
          s"sondern vom Boss getroffen zu werden: Es hat ${formatNumber(Square.initialMaxLife)} " ++
          "Lebenspunkte, doppelt so viele wie die anderen, einen Schild, den es ständig verstärkt, " ++
          "und Fähigkeiten, die viel Bedrohung erzeugen.",
        "Ein guter Tank ist der Ruhepol im Sturm: Solange er den Boss hält, können alle anderen " ++
          "ihre Arbeit machen."
      )
    ),
    TextSection(
      "Wut",
      Seq(
        "Deine Wut entsteht durch den Schaden, den du erleidest (und ein wenig jede Sekunde). " ++
          "Getroffen zu werden ist dein Antrieb! Wenn der Boss dich nicht trifft, verletzt Enrage " ++
          "dich selbst, um Wut aufzubauen."
      )
    ),
    TextSection(
      "Brace",
      Seq(
        s"Brace fügt deinem Schild einen Stapel hinzu, bis zu ${BraceShield.maxStacks}. Jeder Stapel " ++
          s"verringert deinen erlittenen Schaden um ${formatNumber((1 - BraceShield.damageMultiplier) * 100)} %. " ++
          s"Der Schild verliert nach ${formatSeconds(BraceShield.duration)} einen Stapel, außer du " ++
          "wirkst erneut Brace. Die Anzeige links an deinem Rahmen zeigt deine Stapel: Halte sie voll.",
        "Shield Slam verbraucht einen Stapel für einen starken Treffer mit zusätzlicher Bedrohung."
      )
    ),
    TextSection(
      "Bedrohung",
      Seq(
        "Wirke Taunt auf den Boss, sobald er erscheint, und greif ihn dann weiter an (Hammer Hit, " ++
          "Shield Slam, Cleave), um oben auf der Bedrohungsanzeige zu bleiben. Kommt jemand über " ++
          "dich, wirke erneut Taunt.",
        "Guard schützt einen Verbündeten eine Zeit lang: Einen Teil des Schadens, den er erleidet, " ++
          "steckst stattdessen du ein, z. B. wenn er einen gefährlichen Debuff trägt."
      )
    ),
    TextSection(
      "Gegen Boss 101",
      Seq(
        s"Sein Big Hit verursacht ${formatNumber(BigHit.damageAmount)} Schaden, nach einer Zauberzeit " ++
          s"von ${formatSeconds(BigHit.castingTime)}: Beobachte die Zauberleiste des Bosses und " ++
          "halte deine Brace-Stapel bereit, wenn er einschlägt."
      )
    )
  )

  private def pentagon = Seq(
    TextSection(
      "Philosophie",
      Seq(
        "Das Fünfeck ist die Artillerie des Teams: Es verursacht seinen Schaden aus der Ferne, " ++
          "außerhalb der Reichweite des Bosses. Seine Stärke verlangt Geschick: Seine Geschosse " ++
          "fliegen dorthin, wohin du zielst, und seine Zonen müssen platziert werden.",
        "Es kann auch als Einziges die fiesen Debuffs bannen, die manche Bosse den Spielern " ++
          "auferlegen."
      )
    ),
    TextSection(
      "Zielen",
      Seq(
        "- Pentagon Bullet fliegt in Richtung deiner Maus (nicht deines Ziels) und trifft den " ++
          "ersten Gegner auf seinem Weg: Halte die Schusslinie zum Boss frei. Fracture fliegt " ++
          "genauso.",
        "- Pentagon Zone: Wirke sie und klicke dann, wohin sie soll. Gegner, die darin stehen, " ++
          s"erleiden Schaden über Zeit, aber du kannst nur alle ${formatSeconds(CreatePentagonZone.cooldown)} eine legen: Leg " ++
          "sie dorthin, wo der Boss bleiben wird, nicht kurz bevor er sich bewegt."
      )
    ),
    TextSection(
      "Ruhige Hand",
      Seq(
        s"Alle ${formatPeriod(SteadyAim.stackEvery)}, die du dich nicht bewegst, verursachen deine Fähigkeiten " ++
          s"${formatNumber(SteadyAim.damageIncreasePerStack * 100)} % mehr Schaden, bis zu ${formatNumber(SteadyAim.maxStacks * SteadyAim.damageIncreasePerStack * 100)} % (die Anzeige ganz links neben deinem " ++
          "Rahmen). Bewegung lässt alles verfallen und unterbricht, was du wirkst: Such dir früh " ++
          "deinen Platz und vermeide kleine Schritte."
      )
    ),
    TextSection(
      "Facetten",
      Seq(
        s"Jede Pentagon Bullet, die trifft, lässt eine deiner ${Facets.maxStacks} Facetten aufleuchten " ++
          "(an deinen Seiten und in der Anzeige links neben deinem Rahmen). Leuchten alle, " ++
          "verbraucht Pentagram sie für einen sofortigen Treffer: Heb ihn dir auf, wenn du dich " ++
          "bewegen musst. Aber ein Geschoss, das trifft, während alle leuchten, ist verschwendet.",
        s"Fracture verursacht weniger Schaden, aber der getroffene Gegner erleidet ${formatSeconds(Fractured.duration)} " ++
          s"lang ${formatNumber(Fractured.damageIncrease * 100)} % mehr Schaden von dir. Wirke es kurz vor dem Ende erneut: Bis zu " ++
          s"${formatSeconds(Fractured.maxCarriedOver)} Restzeit werden übernommen."
      )
    ),
    TextSection(
      "Mana",
      Seq(
        s"Deine Fähigkeiten kosten Mana (${formatNumber(Pentagon.initialResourceAmount.amount)}). " ++
          "Schieß weiter, aber spar dir etwas für den Fall, dass gebannt werden muss."
      )
    ),
    TextSection(
      "Bannen",
      Seq(
        "Penta Dispel entfernt alle bannbaren Effekte von einem Verbündeten, auch die guten " ++
          "(wie die Heilung über Zeit des Heilers!). Spätere Bosse verteilen Debuffs, die schnell " ++
          "gebannt werden müssen. Der Big Dot von Boss 101 lässt sich nicht bannen: Der Heiler heilt dagegen an."
      )
    ),
    TextSection(
      "Am Leben bleiben",
      Seq(
        s"Mit ${formatNumber(Pentagon.initialMaxLife)} Lebenspunkten darfst du den Boss nicht auf " ++
          "dich ziehen: Behalte die Bedrohungsanzeige im Auge. Halte Abstand zum Boss, aber bleib " ++
          "in Reichweite des Heilers."
      )
    )
  )

  private def hexagon = Seq(
    TextSection(
      "Philosophie",
      Seq(
        "Das Sechseck hält das Team auf den Beinen. Es beobachtet mehr die Rahmen des Teams als " ++
          "die Arena: wer Leben verliert, wer gleich einen schweren Treffer abbekommt.",
        s"Sein Manavorrat ist groß (${formatNumber(Hexagon.initialResourceAmount.amount)}), " ++
          "regeneriert sich aber langsam: Die Kunst des Heilers ist es, die richtige Heilung auf " ++
          "den richtigen Spieler zur richtigen Zeit zu wirken."
      )
    ),
    TextSection(
      "Deine Heilungen",
      Seq(
        "- Flash Heal heilt viel, braucht aber Zeit zum Wirken: Du musst stillstehen.",
        "- Heal Over Time ist günstig und sofort, und lässt sich in der Bewegung wirken: Leg es " ++
          "auf die Spieler, die stetig Schaden erleiden (den Tank, den Träger von Big Dot).",
        "- Hexagon Zone heilt alle, die darin stehen: ideal, wenn das Team beisammen steht."
      )
    ),
    TextSection(
      "Anvisieren",
      Seq(
        "Klicke auf einen Rahmen links auf dem Bildschirm, um einen Spieler anzuvisieren. Ein " ++
          "verblasster Rahmen ist ein Spieler außerhalb deiner Heilreichweite: Geh näher heran.",
        "Ist „Mouseover“ in den „Einstellungen“ aktiviert, gehen deine Heilungen an den Spieler, " ++
          "dessen Rahmen unter der Maus liegt, ohne dass sich dein Ziel ändert."
      )
    ),
    TextSection(
      "Gegen Boss 101",
      Seq(
        "Der Tank steckt die Treffer des Bosses ein: Halte ihn gesund, besonders vor einem Big Hit. " ++
          s"Big Dot verursacht ${formatNumber(BigDot.damageOnTick)} Schaden alle ${formatPeriod(BigDot.tickRate)} " ++
          s"für ${formatSeconds(BigDot.duration)} bei jemand anderem: Eine Heilung über Zeit auf ihm " ++
          "deckt das meiste davon ab. Heile keine Spieler mit voller Gesundheit: Das ist verschwendetes Mana."
      )
    )
  )

  // Previews of the pages
  override def arena: String     = "Arena"
  override def screen: String    = "Bildschirm"
  override def hoverHint: String = "Fahre über die Fähigkeiten, klicke auf die Rahmen"

  override def legendBoss: String          = "Der Boss: sein Leben und was er wirkt."
  override def legendBossCooldowns: String = "Wann die Fähigkeiten des Bosses wieder bereit sind."
  override def legendTeam: String =
    "Dein Team. Klicke auf einen Rahmen, um den Spieler anzuvisieren."
  override def legendYouAndTarget: String = "Du und dein Ziel, über deinen Fähigkeiten."
  override def legendMeters: String       = "Schaden an deinem Ziel und die Bedrohung des Bosses."
  override def legendTriangleEnergy: String =
    "Deine Energie: Gib sie aus, sie füllt sich stetig auf."
  override def legendTriangleAbilities: String = "Deine Fähigkeiten: Direct Hit, Energy Kick, ..."
  override def legendTriangleDamage: String    = "Dein Platz in der Schadensanzeige: ganz oben!"
  override def legendTriangleThreat: String =
    "Aber nicht in der Bedrohungsanzeige: Die gehört dem Quadrat."
  override def legendSquareStacks: String = "Die Stapel deines Schilds (Brace): Halte sie aufrecht."
  override def legendSquareRage: String   = "Deine Wut, gewonnen durch erlittenen Schaden."
  override def legendSquareBigHit: String =
    "Der Boss wirkt Big Hit auf dich: Halte deine Stapel bereit!"
  override def legendSquareThreat: String = "Du musst oben auf der Bedrohungsanzeige bleiben."
  override def legendPentagonMana: String = "Dein Mana."
  override def legendPentagonAim: String =
    "Geschosse fliegen zur Maus, Zonen werden per Klick platziert."
  override def legendPentagonDebuffs: String =
    "Die Debuffs der Spieler: Banne die, die sich bannen lassen."
  override def legendPentagonDamage: String = "Dein Platz in der Schadensanzeige."
  override def legendPentagonFacets: String =
    "Deine Facetten: Pentagram ist bereit, wenn alle leuchten."
  override def legendPentagonSteadyAim: String =
    "Deine ruhige Hand: Bleib stehen, um sie zu füllen."
  override def legendHexagonTeam: String =
    "Dein Team: Klicke auf einen Rahmen, um den Spieler anzuvisieren."
  override def legendHexagonFaded: String =
    "Verblasst: außerhalb deiner Heilreichweite. Geh näher heran!"
  override def legendHexagonMana: String = "Dein Mana: Es regeneriert sich langsam."
  override def legendHexagonTarget: String =
    "Dein Ziel, der Tank, mit deiner Heilung über Zeit."

  override def arenaHealerRange: String = "Reichweite des Heilers"
  override def arenaBoss: String        = "Der Boss"
  override def arenaSquare: String      = "Das Quadrat hält den Boss, ihm zugewandt"
  override def arenaTriangle: String    = "Das Dreieck trifft ihn im Nahkampf, von hinten"
  override def arenaPentagons: String   = "Die Fünfecke schießen aus der Ferne"
  override def arenaHexagon: String     = "Das Sechseck heilt, in Reichweite aller"
  override def arenaLegendPositions: String =
    "Der Tank dreht den Boss von den anderen weg, die ihn von hinten oder aus der Ferne treffen."
  override def arenaLegendRange: String =
    "Alle bleiben in Reichweite des Heilers (außerhalb davon verblassen die Rahmen des Teams)."

  // --- Landing page ---
  override def landingTagline: String =
    "Erhebe dich gegen die Hohepriester von Flatland, gemeinsam."
  override def landingPitch: String =
    "Ein kooperatives Spiel mit Bosskämpfen wie in World of Warcraft: ein Tank, ein Heiler und " ++
      "Schadensverursacher gegen die Tyrannen von Flatland. Lerne ihre Fähigkeiten, spiele deine Rolle und bezwinge sie in " ++
      "Kämpfen, die Minuten dauern, nicht ganze Abende."
  override def landingDownload: String             = "Herunterladen"
  override def downloadFor(system: String): String = s"Für $system herunterladen"
  override def landingLoveFile: String    = "Hast du schon LÖVE 12? Hol dir die .love-Datei"
  override def landingFreeAndOpen: String = "Kostenlos und Open Source, jetzt in der Beta."
  override def landingUnsigned: String =
    "Das Spiel ist noch nicht signiert: Windows warnt dich vielleicht („Weitere Informationen“, " ++
      "dann „Trotzdem ausführen“), und macOS bittet dich, es in den Systemeinstellungen unter " ++
      "Datenschutz & Sicherheit zu erlauben („Dennoch öffnen“)."
  override def landingAnimation: String = "Ein Kampf gegen einen Boss in Battle for Flatland"

  override def landingFeaturesTitle: String = "Raids wie in WoW, ohne den Grind"
  override def landingFeatures: Seq[(String, String)] = Seq(
    "Das Gefühl eines Raids" ->
      ("Ein Tank, der den Boss hält, ein Heiler, der alle am Leben hält, Schadensverursacher im " ++
        "Wettlauf gegen die Zeit, und ein Boss, dessen Fähigkeiten ihr gemeinsam lernt und " ++
        "kontert."),
    "Kein Farmen, niemals" ->
      ("Keine Ausrüstung zu sammeln, keine Stufen zu grinden: Alle starten gleich, und es zählt " ++
        "nur, wie ihr zusammen spielt."),
    "Für kurze Sessions gemacht" ->
      ("Keine lange Vorbereitung: Spiel erstellen, Plätze einnehmen, kämpfen. Ein Kampf dauert " ++
        "Minuten, er passt zwischen zwei Gutenachtgeschichten, und du kannst aufhören, wenn die " ++
        "Kinder rufen."),
    "Nie zu wenige Spieler" ->
      ("Heute Abend kein Heiler? Fülle das Team mit Bots auf. Und lerne jede Klasse im " ++
        "Tutorial, mit einem Übungskampf gegen den ersten Boss.")
  )
  override def landingClassesTitle: String     = "Vier Klassen, vier Rollen"
  override def landingScreenshotsTitle: String = "In der Arena"
  override def landingCaption(picture: LandingPicture): String = picture match {
    case LandingPicture.Fight =>
      "Das ganze Team gegen einen Boss: der Tank vorne, die anderen dahinter, in Reichweite des " ++
        "Heilers."
    case LandingPicture.Boss =>
      "Jeder Boss hat seine eigenen Fähigkeiten, die ihr gemeinsam lernt und kontert."
    case LandingPicture.Lobby =>
      "Erstelle ein Spiel, wähle deine Klasse und den Boss, und fülle freie Plätze mit Bots auf."
    case LandingPicture.Tutorial =>
      "Das Tutorial erklärt jede Klasse und richtet dann einen Übungskampf gegen Bots ein."
  }

  override def landingStepsTitle: String = "Vom Download zum Boss, in wenigen Minuten"
  override def landingSteps: Seq[String] = Seq(
    "Lade das Spiel für dein System herunter, entpacke es und starte es.",
    "Wähle einen Spielernamen: kein Konto, kein Passwort.",
    "Lies im Tutorial über deine Klasse und übe sie gegen Boss 101.",
    "Erstelle ein Spiel, lass deine Freunde beitreten (oder füge Bots hinzu), wähle einen Boss " ++
      "und kämpfe!"
  )
  override def landingOpenSourceBadge: String = "Open Source"
  override def landingOpenSourceTitle: String = "Open Source, durch und durch"
  override def landingOpenSourceText: String =
    "Battle for Flatland ist Open Source, von den Menüs bis zum letzten Boss: kein versteckter " ++
      "Code, und ein Spiel, das seinen Spielern ebenso gehört wie seinem Autor."
  override def landingOpenSource: Seq[(String, String)] = Seq(
    "Der ganze Code, offen" ->
      ("Jede Zeile des Spiels ist auf GitHub, von den Menüs bis zu den Fähigkeiten der Bosse: " ++
        "lies sie, lerne daraus, ändere sie."),
    "Selbst hosten" ->
      ("Der Server kommt mit der Anleitung, ihn auf deinem eigenen Rechner zu betreiben: " ++
        "Niemand kann dir das Spiel wegnehmen."),
    "Dein eigener Boss" ->
      ("Eine Schritt-für-Schritt-Anleitung erklärt, wie man einen neuen Boss implementiert. " ++
        "Schlag deinen auf GitHub vor, und vielleicht kommt er ins Spiel!")
  )

  override def landingFaqTitle: String = "Häufige Fragen"
  override def landingFaq: Seq[(String, String)] = Seq(
    "Kann ich allein spielen?" ->
      ("Ja: Jeder Boss kommt mit Bots, die alle anderen Plätze im Team einnehmen können. So " ++
        "kannst du einen Boss in deinem eigenen Tempo entdecken und üben. Aber das Spiel ist " ++
        "fürs Zusammenspiel gemacht: Mit Bots erlebst du nicht das Beste daran, nämlich einen " ++
        "Boss mit Freunden zu bezwingen."),
    "Muss ich World of Warcraft kennen?" ->
      ("Überhaupt nicht. Wenn du in WoW geraidet hast, fühlst du dich sofort zu Hause. Wenn " ++
        "nicht, erklärt das Tutorial jede Rolle und lässt dich sie gegen einen ersten Boss üben."),
    "Ist das Spiel für ein Team von fünf gemacht?" ->
      ("Ja und nein. Die meisten Bosse sind für fünf Spieler gedacht: ein Tank, ein Heiler und " ++
        "drei Schadensverursacher. Manche verlangen aber ein größeres Team: schon zehn Spieler, " ++
        "und später wohl bis zu zwanzig."),
    "Spiele ich immer dieselbe Klasse?" ->
      ("Auf keinen Fall! Jeder Boss verlangt seine eigene Teamzusammenstellung, also wird " ++
        "jeder auch andere Klassen spielen. Und da es keinen Fortschritt gibt, kannst du die " ++
        "Klasse wechseln, wann du willst."),
    "Wie spiele ich mit meinen Freunden?" ->
      ("Erstelle ein Spiel in den Menüs, und deine Freunde treten ihm aus der Liste der Spiele " ++
        "bei. Kein Server einzurichten: Wenn der Kampf beginnt, hostet ihn der Rechner eines " ++
        "der Spieler, ganz von selbst."),
    "Brauche ich ein Konto?" ->
      ("Nein, im Moment nicht: Wähle einfach einen Spielernamen, an dem dich die anderen in " ++
        "den Menüs und im Spiel erkennen. Konten kommen wahrscheinlich später."),
    "Auf welchen Systemen läuft es?" ->
      ("Windows, macOS und Linux. Und überall, wo LÖVE 12 läuft, mit der .love-Datei."),
    "Ist das Spiel kostenlos, und bleibt es das?" ->
      ("Heute ist es kostenlos, von dieser Website heruntergeladen. In ein oder zwei Jahren " ++
        "soll das Spiel auf eine Plattform wie Steam umziehen, wo es kostenpflichtig sein wird. " ++
        "Aber es bleibt Open Source, mit der Anleitung zum Selbsthosten: Wenn du willst, kannst " ++
        "du immer kostenlos spielen."),
    "Kommt das Spiel auf Steam?" ->
      "Das ist das Ziel, aber nicht in naher Zukunft: wahrscheinlich in ein oder zwei Jahren.",
    "Kann ich meinen eigenen Boss machen?" ->
      ("Ja, wenn du programmieren kannst: Das Spiel ist Open Source, und sein Repository hat " ++
        "eine Schritt-für-Schritt-Anleitung, um einen neuen Boss zu implementieren. Schlag ihn " ++
        "auf GitHub vor, und vielleicht kommt er ins Spiel!")
  )

  override def landingBetaTitle: String = "Werde Betatester"
  override def landingBetaText: String =
    "Battle for Flatland ist in der Beta und wächst weiter. Lade es herunter, spiele ein paar " ++
      "Kämpfe mit Freunden und sag uns, was du denkst: Jeder Fehlerbericht, jede Idee und jeder " ++
      "Wunsch hilft, das Spiel zu formen."
  override def landingFeedback: String   = "Feedback geben"
  override def landingSourceCode: String = "Quellcode auf GitHub"
  override def landingFooter: String =
    "Battle for Flatland ist kostenlos und Open Source, gemacht mit Scala und LÖVE."
  override def landingTrademark: String =
    "World of Warcraft ist eine Marke von Blizzard Entertainment, mit der Battle for Flatland " ++
      "nicht verbunden ist."
}
