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

object AlbanianI18n extends I18n {

  // --- Common ---

  override def playerClassName(playerClass: PlayerClasses): String = playerClass match {
    case PlayerClasses.Triangle => "Trekëndësh"
    case PlayerClasses.Square => "Katror"
    case PlayerClasses.Pentagon => "Pesëkëndësh"
    case PlayerClasses.Hexagon => "Gjashtëkëndësh"
  }

  override def resourceName(resource: Resource): String = resource match {
    case Resource.Mana => "mana"
    case Resource.Energy => "energji"
    case Resource.Rage => "tërbim"
    case Resource.NoResource => "asgjë"
  }

  override def mouseButton(number: String): String = s"butoni $number i miut"

  override def formatNumber(x: Double): String = super.formatNumber(x).replace('.', ',')

  override def formatSeconds(millis: Long): String =
    s"${formatNumber(millis / 1000.0)} ${if millis > 1000L then "sekonda" else "sekondë"}"

  override def settings: String    = "Cilësimet"
  override def leaveGame: String   = "Dil nga loja"
  override def quit: String        = "Dil"
  override def name: String        = "Emri"
  override def playerClass: String = "Klasa"
  override def colour: String      = "Ngjyra"
  override def ready: String       = "Gati"
  override def notReady: String    = "Jo gati"
  override def boss: String        = "Boss"
  override def victory: String     = "Fitore!"
  override def defeat: String      = "Humbje..."
  override def startGame: String   = "Fillo lojën"

  override def castingTime(time: String): String = s"Hedhja: $time"
  override def cooldown(time: String): String    = s"Rikthimi: $time"
  override def cost(amount: String): String      = s"Kostoja: $amount"

  // --- Menus ---

  override def fullscreenHint: String             = "F11: ekran i plotë"
  override def connectingToGameServer: String     = "Duke u lidhur me serverin e lojës"
  override def leavingTheGame: String             = "Duke dalë nga loja"
  override def connectionToGameServerLost: String = "Lidhja me serverin e lojës u humb"
  override def cannotConnectToGameServer(error: String): String =
    s"Lidhja me serverin e lojës dështoi: $error"
  override def couldNotLeaveGame(error: String): String = s"Dalja nga loja dështoi: $error"

  override def chooseYourLanguage: String = "Zgjidh gjuhën tënde"

  // Login
  override def welcomeToBattleForFlatland: String = "Mirë se erdhe në Battle for Flatland!"
  override def serverAddress: String              = "Adresa e serverit"
  override def serverAddressExample: String   = "p.sh. bff.example.com, ose http://81.12.34.56:9000"
  override def choosePseudo: String           = "Zgjidh një pseudonim"
  override def yourPseudo: String             = "Pseudonimi yt"
  override def loggingIn: String              = "Duke u identifikuar"
  override def enterBattleForFlatland: String = "Hyr në Battle for Flatland"
  override def serverError(message: String): String = s"Serveri: $message"
  override def pseudoError(message: String): String = s"Pseudonimi: $message"
  override def cannotLogIn(address: String, error: String): String =
    s"Identifikimi në $address dështoi: $error"
  override def invalidPort(port: String): String = s"Port i pavlefshëm: $port"
  override def invalidAddress(address: String): String =
    s"Adresë e pavlefshme: $address (pritej host ose host:port)"
  override def outdatedVersion: String =
    "Ka dalë një version i ri i Battle for Flatland! Shkarkoje përsëri nga faqja për të vazhduar lojën."
  override def downloadTheNewVersion: String = "Shkarko versionin e ri"

  // Games
  override def welcomePlayer(playerName: String): String   = s"Mirë se erdhe, $playerName"
  override def learnToPlay: String                         = "Mëso të luash"
  override def joinItBack: String                          = "Rikthehu në të"
  override def gameNamePlaceholder: String                 = "Emri i lojës"
  override def defaultGameName(playerName: String): String = s"Loja e $playerName"
  override def createGame: String                          = "Krijo lojë"
  override def createdBy: String                           = "Krijuar nga"
  override def players: String                             = "Lojtarët"
  override def loadingTheGames: String                     = "Duke ngarkuar lojërat"
  override def noGames: String  = "Për momentin nuk ka asnjë lojë. Krijo një!"
  override def join: String     = "Bashkohu"
  override def joinBack: String = "Rikthehu"
  override def currentlyInGame(gameName: String): String =
    s"Je aktualisht në lojën '$gameName'."

  // Lobby of a game
  override def loadingTheGame: String                 = "Duke ngarkuar lojën"
  override def loggedInAs(playerName: String): String = s"Identifikuar si $playerName"
  override def gameCreatedBy(creator: String, gameId: String): String =
    s"Krijuar nga $creator - loja $gameId"
  override def kick: String                      = "Përjashto"
  override def you(playerName: String): String   = s"$playerName (ti)"
  override def ai(playerName: String): String    = s"$playerName (AI)"
  override def classNotChosen: String            = "E pazgjedhur"
  override def addAI: String                     = "Shto AI"
  override def fillWithAIs: String               = "Plotëso me AI"
  override def launchGame: String                = "Nis lojën!"
  override def waitingForCreatorToLaunch: String = "Në pritje që krijuesi të nisë lojën"
  override def gameIsStarting: String            = "Loja po niset, prit sa të përfundojë përgatitja"
  override def gameServerNotReadyYet: String     = "Serveri i lojës nuk është ende gati."
  override def gameServerRunsOnYourComputer: String = "Serveri i lojës punon në kompjuterin tënd"
  override def gameServerRunsOnComputerOf(host: String): String =
    s"Serveri i lojës punon në kompjuterin e $host"
  override def downloadingJavaRuntime: String              = "Duke shkarkuar mjedisin Java"
  override def downloadingGameServer: String               = "Duke shkarkuar serverin e lojës"
  override def megabytes(received: Int, size: Int): String = s"$received / $size MB"
  override def gameClosedByCreator: String                 = "Loja u mbyll nga krijuesi i saj"
  override def notInThisGame: String                       = "Nuk je (më) në këtë lojë"
  override def everyoneReady: String                       = "Të gjithë janë gati, le të fillojmë!"
  override def thingsPreventingStart: String               = "Këto gjëra pengojnë nisjen e lojës:"

  // Docs of the bosses and classes, in the lobby
  override def classes: String = "Klasat"
  override def bossFacts(life: Int, intendedFor: Int): String =
    s"$life pikë jete, për $intendedFor lojtar(ë)"
  override def classFacts(
      life: Int,
      resourceAmount: Int,
      resource: Resource,
      isYours: Boolean
  ): String =
    s"$life pikë jete, $resourceAmount ${resourceName(resource)}" +
      (if isYours then " (klasa jote)" else "")
  override def abilities: String = "Aftësitë"

  // Settings
  override def controls: String                       = "Kontrollet"
  override def general: String                        = "Të përgjithshme"
  override def goUp: String                           = "Lëviz lart"
  override def goDown: String                         = "Lëviz poshtë"
  override def goLeft: String                         = "Lëviz majtas"
  override def goRight: String                        = "Lëviz djathtas"
  override def nextTarget: String                     = "Objektivi tjetër"
  override def faceTheTarget: String                  = "Kthehu nga objektivi"
  override def abilityNumber(number: Int): String     = s"Aftësia $number"
  override def markerCross: String                    = "Kryq"
  override def markerLozenge: String                  = "Romb"
  override def markerMoon: String                     = "Hënë"
  override def markerSquare: String                   = "Katror"
  override def markerStar: String                     = "Yll"
  override def markerTriangle: String                 = "Trekëndësh"
  override def markerOnTarget(marker: String): String = s"$marker mbi objektivin"
  override def markerOnGround(marker: String): String = s"$marker në tokë"
  override def bindingsHelp: String =
    "Kliko një caktim, pastaj shtyp një tast (me ose pa Shift) ose një buton të miut. Ndryshimet vlejnë nga loja e ardhshme."
  override def mouseOver: String = "Nën miun"
  override def mouseOverHelp: String =
    "Kur objektivi nuk vlen për një aftësi, ajo shkon te korniza që është nën miun."
  override def resetToDefaults: String = "Rivendos parazgjedhjet"
  override def escapeToCancel: String  = "Escape për anulim"
  override def pressKeyFor(action: String): String =
    s"Shtyp një tast ose një buton të miut për \"$action\""
  override def boundToSeveralActions(key: String): String = s"$key është caktuar për disa veprime"
  override def language: String                           = "Gjuha"
  override def languageHelp: String                       = "Gjuha e menyve dhe e lojërave."
  override def hudScale: String                           = "Madhësia e HUD-it"
  override def hudScaleHelp: String =
    "Madhësia e kornizave dhe e shiritit të veprimeve, pavarësisht nga madhësia e dritares. Vlen nga loja e ardhshme."
  override def hostGameServers: String = "Strehim i serverëve të lojës"
  override def hostGameServersHelp: String =
    "Serveri i një loje punon në kompjuterin e njërit prej lojtarëve të saj. Që të jetë gati ta " +
      "ekzekutojë, klienti e shkarkon atë, bashkë me atë që e ekzekuton (disa dhjetëra MB, një " +
      "herë), sapo bashkohesh në një lojë. Çaktivizoje këtë për të mos shkarkuar asgjë, p.sh. në " +
      "rrjet celular: do ta ekzekutojnë lojtarët e tjerë."
  override def mainVolume: String      = "Volumi kryesor"
  override def abilitiesVolume: String = "Aftësitë"
  override def volumeOff: String       = "Fikur"
  override def volumesHelp: String =
    "Nga fikur deri në 10. Volumi i çdo lloji tingulli është në raport me volumin kryesor."
  override def done: String = "U krye"

  // --- In game ---

  override def placeYourselves: String =
    "Zini vendet, pastaj klikoni \"Fillo lojën\": bossi mbërrin 5 sekonda më vonë"
  override def bossArrivesIn: String          = "Bossi mbërrin pas"
  override def clickToPlaceZone: String       = "Kliko për të vendosur zonën (Escape për anulim)"
  override def waitingForGameToStart: String  = "Në pritje të fillimit të lojës..."
  override def bossAbilities: String          = "Aftësitë e bossit"
  override def threat: String                 = "Kërcënimi"
  override def damage: String                 = "Dëmi"
  override def damageTo(name: String): String = s"Dëmi: $name"
  override def dead: String                   = "I vdekur"

  // Casting
  override def youAreDead: String                  = "Ke vdekur"
  override def noTarget: String                    = "Asnjë objektiv"
  override def noAbilityOnKey(number: Int): String = s"Asnjë aftësi në tastin $number"
  override def abilityNotSupported(ability: Ability.AbilityId): String =
    s"Aftësia $ability nuk mbështetet ende"
  override def abilityNotPlacedWithMouse(ability: Ability.AbilityId): String =
    s"Aftësia $ability nuk vendoset me miun"

  // Why an ability cannot be cast (shown to the player)
  override def alreadyCasting: String    = "Je duke hedhur tashmë"
  override def abilityNotOwned: String   = "Nuk e ke këtë aftësi"
  override def abilityOnCooldown: String = "Aftësia është në rikthim"
  override def notEnough(resource: Resource): String =
    s"Nuk ke mjaftueshëm ${resourceName(resource)}"
  override def silenced: String                 = "Je i heshtur"
  override def notInRange: String               = "Jashtë rrezes"
  override def notInSight: String               = "Jashtë fushëpamjes"
  override def positionNotInSight: String       = "Pozicioni është jashtë fushëpamjes"
  override def notAnAlly: String                = "Nuk është aleat"
  override def notAnEnemy: String               = "Nuk është armik"
  override def cannotGuardYourself: String      = "Nuk mund të mbrosh veten"
  override def needShieldStack: String          = "Të duhet një shtresë e mburojës sate"
  override def needAllFacets: String            = "Të gjitha faqet e tua duhet të jenë të ndezura"
  override def targetCannotBeStunned: String    = "Objektivi nuk mund të shtangohet"
  override def targetDoesNotCast: String        = "Objektivi nuk po hedh asgjë"
  override def targetAbilityCannotBeCut: String = "Aftësia e objektivit nuk mund të ndërpritet"

  // Why a boss' ability cannot be cast
  override def noBombToExplode: String                  = "Nuk ke asnjë bombë!"
  override def creepingShadowNotThere: String           = "Creeping Shadow nuk është aty"
  override def bossNotThere: String                     = "Bossi nuk është aty"
  override def creepingShadowDoesNotCollideBoss: String = "Creeping Shadow nuk e prek bossin"
  override def noDeathMarkTriangle: String              = "Nuk ka trekëndësh të shenjës së vdekjes"
  override def noShadeToCall: String                    = "Nuk ka asnjë Shade për të thirrur"
  override def targetNotThere: String                   = "Objektivi nuk është aty"
  override def twinsFarEnough: String                   = "Binjakët janë mjaft larg njëri-tjetrit"

  // Why an action is not legal (rather for the logs)
  override def gameAlreadyStarted: String                      = "Loja ka filluar tashmë"
  override def gameAlreadyEnded: String                        = "Loja ka përfunduar tashmë"
  override def playerAlreadyExists: String                     = "Lojtari ekziston tashmë"
  override def bossDoesNotExist(bossName: String): String      = s"Bossi $bossName nuk ekziston"
  override def entityDoesNotExist(entityId: Entity.Id): String = s"Entiteti $entityId nuk ekziston"
  override def entitiesDoNotExist(entityId: Entity.Id, otherId: Entity.Id): String =
    s"Entiteti $entityId ose $otherId nuk ekziston"
  override def entityIsNotLiving(entityId: Entity.Id): String =
    s"Entiteti $entityId nuk ekziston, ose nuk është i gjallë"
  override def entityIsNotMoving(entityId: Entity.Id): String =
    s"Entiteti $entityId nuk ekziston, ose nuk lëviz"
  override def entityHasNoAbilities(entityId: Entity.Id): String =
    s"Entiteti $entityId nuk ekziston, ose nuk ka aftësi"
  override def entityHasNoTarget(entityId: Entity.Id): String =
    s"Entiteti $entityId nuk ekziston, ose nuk ka objektiv"
  override def entityWouldCollide(entityId: Entity.Id): String =
    s"Entiteti $entityId do të përplasej me një pengesë"
  override def buffDoesNotExist(buffId: Buff.Id, bearerId: Entity.Id): String =
    s"Efekti $buffId nuk ekziston te entiteti $bearerId"

  // Connection to the game server
  override def gameServerDoesNotAnswer: String = "serveri i lojës nuk përgjigjet"
  override def cannotSynchronizeClock: String  = "ora nuk mund të sinkronizohet me serverin e lojës"
  override def gameServerNotReady: String      = "serveri i lojës nuk është gati"
  override def gameServerRejectsCredentials: String =
    "serveri i lojës nuk i pranon kredencialet tona"
  override def gameServerDoesNotKnowUs: String          = "serveri i lojës nuk na njeh"
  override def gameCouldNotStart(error: String): String = s"loja nuk arriti të nisej ($error)"

  // Game menu
  override def menu: String                  = "Menyja"
  override def resume: String                = "Vazhdo"
  override def surrender: String             = "Dorëzohu"
  override def clickAgainToSurrender: String = "Kliko përsëri për t'u dorëzuar"
  override def surrenderEndsInDefeat: String = "Beteja përfundon me humbje, për të gjithë"
  override def gameGoesOnMeanwhile: String   = "Ndërkohë loja vazhdon"
  override def windowed: String              = "Në dritare (F11)"
  override def fullscreen: String            = "Ekran i plotë (F11)"

  // End of the game
  override def backToMenus: String    = "Kthehu te menytë"
  override def connectionLost: String = "Lidhja u humb"
  override def fightLasted(minutes: Int, seconds: Int): String =
    f"Beteja zgjati $minutes%d:$seconds%02d"
  override def gameServerClosedConnection: String = "Serveri i lojës e mbylli lidhjen"

  // Statistikat e luftës, në fund të lojës
  override def fightSummary: String = "Përmbledhje"
  override def damageDone: String   = "Dëmi i shkaktuar"
  override def healing: String      = "Shërimi"
  override def damageTaken: String  = "Dëmi i marrë"
  override def lastMoments: String  = "Çastet e fundit"
  override def player: String       = "Lojtari"
  override def deaths: String       = "Vdekjet"
  override def bossLifeLeft(bossName: String, percent: Int): String =
    s"$bossName kishte ende $percent% të jetës"
  override def nobodyDied: String = "Askush nuk vdiq"
  override def diedAt(minutes: Int, seconds: Int, killedBy: String): String =
    f"Vdiq në $minutes%d:$seconds%02d ($killedBy)"
  override def lastSecondsOf(name: String, seconds: Int, died: Boolean): String =
    if died then s"$seconds sekondat e fundit të $name para vdekjes"
    else s"$seconds sekondat e fundit të $name"
  override def nothingInLastSeconds: String =
    "Asgjë nuk e goditi as e shëroi gjatë këtyre sekondave"
  override def life: String                   = "Jeta"
  override def overheal(percent: Int): String = s"$percent% shërim i tepërt"
  override def bulletsHit(count: Int): String = s"Plumba që goditën: $count"
  override def facetsWasted(count: Int, percent: Int): String =
    s"Të humbur, me të gjitha faqet të ndezura: $count ($percent%)"
  override def pentagramsCast(count: Int): String = s"Pentagrame: $count"
  override def braceStacks(stacks: Int): String   = s"$stacks shtresa"
  override def timeWithBraceStacks: String        = "Koha me çdo numër shtresash të Brace, gjallë"
  override def damagePreventedByBrace(amount: String): String =
    s"Dëmi i shmangur nga Brace: $amount"
  override def noClassStatistics: String = "Asnjë statistikë për klasat e këtij ekipi"

  // --- Tutorial ---

  override def tutorialIntro: String =
    "Battle for Flatland në pak minuta: si funksionon loja, pastaj çdo klasë, me një betejë " ++
      "stërvitore kundër Boss 101 krahas lojtarëve AI."
  override def tutorialHint: String =
    "I ri këtu? Fillo me përmbledhjen, pastaj lexo për klasën që do të doje të luash."
  override def backToTheGames: String                    = "Kthehu te lojërat"
  override def backToTheTutorial: String                 = "Kthehu te udhëzuesi"
  override def next(title: String): String               = s"Tjetra: $title"
  override def practiceAgainst(bossName: String): String = s"Stërvitu kundër $bossName"
  override def settingUpTheGame: String                  = "Duke përgatitur lojën..."
  override def alreadyInAGame: String =
    "Je tashmë në një lojë: së pari dil prej saj, nga lista e lojërave."
  override def practiceGameName(playerName: String): String = s"Stërvitja e $playerName"

  override def overview: String     = "Përmbledhje"
  override def overviewRole: String = "Si funksionon loja"
  override def overviewSummary: String =
    "Një ekip formash kundër një bossi: kush bën çfarë, si të lexosh ekranin dhe si zhvillohet një betejë."

  override def theClass(playerClass: PlayerClasses): String = s"${playerClassName(playerClass)}i"

  override def classRole(playerClass: PlayerClasses): String = playerClass match {
    case PlayerClasses.Triangle => "Dëm nga afër"
    case PlayerClasses.Square => "Tank"
    case PlayerClasses.Pentagon => "Dëm nga larg"
    case PlayerClasses.Hexagon => "Shërues"
  }

  override def classSummary(playerClass: PlayerClasses): String = playerClass match {
    case PlayerClasses.Triangle =>
      "Lufton ngjitur me armiqtë, shpenzon energjinë për goditje të forta dhe kontrollon armiqtë shtesë."
    case PlayerClasses.Square =>
      "Mban vëmendjen e bossit dhe pret goditjet e tij, pas një mburoje që e forcon vazhdimisht."
    case PlayerClasses.Pentagon =>
      "Qëllon armiqtë nga larg, shenjon plumbat dhe zonat e tij, dhe heq efektet negative të rrezikshme."
    case PlayerClasses.Hexagon =>
      "Mban të gjithë gjallë, me një rezervë të madhe mane që rigjenerohet ngadalë dhe duhet menaxhuar."
  }

  override def story: TextSection =
    TextSection(
      "Historia",
      Seq(
        "Flatland (nga romani i Edwin A. Abbott) është një botë me dy përmasa, ku banorët " ++
          "janë shumëkëndësha.",
        "Në fund të hierarkisë së saj janë trekëndëshat dybrinjënjëshëm. Brez pas brezi, ata " ++
          "i afrohen gjithnjë e më shumë trekëndëshit barabrinjës. Pastaj, çdo brez fiton " ++
          "edhe një brinjë dhe ngjitet një shkallë më lart.",
        "Në majë fare janë Rrathët: shumëkëndësha me aq shumë brinjë, sa askush nuk mund t'i " ++
          "dallojë nga një rreth. Ata janë Kryepriftërinjtë.",
        "Por kohët e fundit, një kalbëzim është përhapur mes Kryepriftërinjve. Ata nuk janë " ++
          "më barinjtë e mirë që ishin dikur: abuzojnë me pushtetin e tyre dhe shtypin " ++
          "popullin e Flatland-it.",
        "Një grup shumëkëndëshash më të thjeshtë (Trekëndësha, Katrorë, Pesëkëndësha dhe " ++
          "Gjashtëkëndësha) është ngritur kundër tyre. Ti je një nga këta rebelë, dhe ti " ++
          "godet i pari!",
        "Megjithatë, Kryepriftërinjtë janë tepër të fuqishëm: askush nuk mund t'i mposhtë i " ++
          "vetëm. Ti lufton kundër tyre me shokët e tu, dhe secili prej jush ka një rol për " ++
          "të luajtur.",
        "Hyr në Battle for Flatland!"
      )
    )

  override def overviewSections(controls: Controls): Seq[TextSection] = {
    val abilityKeys = controls.abilityKeys.take(4).map(key).mkString(", ")
    Seq(
      story,
      TextSection(
        "Mirë se erdhe në Flatland",
        Seq(
          "Në Battle for Flatland, një ekip lojtarësh, secili një formë gjeometrike, lufton së " ++
            "bashku kundër një bossi. Beteja fitohet kur bossi vdes dhe humbet kur vdesin të " ++
            "gjithë lojtarët: të gjithë fitojnë ose humbasin bashkë.",
          "Bossët janë shumë më të fortë se çdo lojtar. Ata mposhten nga një ekip ku secili " ++
            "luan rolin e vet."
        )
      ),
      TextSection(
        "Tank, shërues, sulmues",
        Seq(
          "Çdo klasë ka një rol, dhe çdo rol mbështetet te të tjerët:",
          "- Tanku (Katrori) mban vëmendjen e bossit. Është i vetmi që u mbijeton goditjeve të " ++
            "bossit, ndaj bossi duhet të godasë atë, dhe askënd tjetër.",
          "- Shëruesi (Gjashtëkëndëshi) e mban ekipin gjallë, tankun në radhë të parë, duke " ++
            "shëruar dëmin që marrin.",
          "- Sulmuesit (Trekëndëshi nga afër, Pesëkëndëshi nga larg) e rrëzojnë bossin, para se " ++
            "shëruesit t'i mbarojë mana.",
          "Nëse tanku vdes, bossi kthehet kundër lojtarëve që nuk i përballojnë goditjet e tij. " ++
            "Nëse shëruesi vdes, dëmi grumbullohet. Nëse dëmi është shumë i ulët, beteja zgjat " ++
            "derisa shëruesit t'i mbarojë mana."
        )
      ),
      TextSection(
        "Kërcënimi",
        Seq(
          "Bossi sulmon lojtarin që urren më shumë. Çdo pikë dëmi që i shkaktohet krijon " ++
            "kërcënim ndaj atij që e shkaktoi. Aftësitë e tankut krijojnë shumë kërcënim, që " ++
            "tanku të qëndrojë në krye të matësit të kërcënimit (poshtë djathtas në ekran).",
          "Sulmues: mbaje nën vëzhgim dhe mos e kalo tankun!"
        )
      ),
      TextSection(
        "Kontrollet",
        Seq(
          s"- Lëviz me ${key(controls.upKey)}, ${key(controls.leftKey)}, " ++
            s"${key(controls.downKey)} dhe ${key(controls.rightKey)}.",
          s"- Hidh aftësitë me $abilityKeys, ... sipas renditjes në shiritin e veprimeve, poshtë " ++
            "në ekran. Kalo miun mbi një aftësi për të lexuar çfarë bën.",
          "- Kliko një lojtar ose bossin (në arenë, ose kornizën e tij) për ta zgjedhur si " ++
            s"objektiv. ${key(controls.nextTargetKey)} zgjedh armikun më të afërt, dhe " ++
            s"${key(controls.targetLockInToggleKey)} të kthen nga objektivi yt.",
          "- Disa aftësi krijojnë një zonë: hidhi, pastaj kliko ku ta vendosësh (Escape anulon).",
          "- Escape hap menynë e lojës. Të gjitha tastet mund të ndryshohen te Cilësimet."
        )
      ),
      TextSection(
        "Leximi i ekranit",
        Seq(
          "Skeda \"Ekrani\" tregon çfarë sheh gjatë një beteje:",
          "- 1. Bossi, me atë që hedh.",
          "- 2. Kur rikthehen aftësitë e bossit.",
          "- 3. Ekipi yt, i zbehur kur është jashtë rrezes së shërimit tënd. Kliko një kornizë " ++
            "për të zgjedhur lojtarin si objektiv.",
          "- 4. Ti, objektivi yt (me atë që hedh) dhe aftësitë e tua poshtë.",
          "- 5. Kush i ka shkaktuar më shumë dëm objektivit tënd dhe kë urren më shumë bossi."
        )
      ),
      TextSection(
        "Si zhvillohet një lojë",
        Seq(
          "Krijo një lojë (ose bashkohu në një), zgjidh klasën dhe ngjyrën, dhe bëhu gati. " ++
            "Krijuesi i lojës zgjedh bossin, mund ta plotësojë ekipin me AI, dhe e nis lojën.",
          "Në arenë, secili zë vendin e vet, pastaj dikush klikon \"Fillo lojën\": bossi " ++
            "mbërrin 5 sekonda më vonë. Dhoma e pritjes përshkruan çdo boss dhe aftësitë e tij.",
          s"${Boss101.name} është bossi i parë fare, dhe vendi më i mirë për të mësuar: Big Hit " ++
            s"i tij (${formatNumber(BigHit.damageAmount)} dëm) vret këdo përveç tankut, dhe " ++
            s"Big Dot i tij shkakton ${formatNumber(BigDot.damageOnTick)} dëm çdo " ++
            s"${formatPeriod(BigDot.tickRate)} te dikush tjetër, të cilin shëruesi duhet ta shërojë."
        )
      ),
      TextSection(
        "Hapi tjetër",
        Seq(
          "Lexo për klasën që do të luash, pastaj stërvitu me të kundër Boss 101 me lojtarë AI."
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
    "Stërvitje",
    Seq(
      s"Gati për ta provuar? \"Stërvitu kundër Boss 101\" përgatit një lojë ku ti luan si " ++
        s"${playerClassName(playerClass)} krahas lojtarëve AI, që marrin rolet e tjera. Pasi të " ++
        "jesh në arenë, zër vendin, pastaj kliko \"Fillo lojën\": bossi mbërrin 5 sekonda më " ++
        "vonë. Kur beteja mbaron, kthehesh këtu."
    )
  )

  private def triangle = Seq(
    TextSection(
      "Filozofia",
      Seq(
        "Trekëndëshi është presion dhe precizion. Jeton ngjitur me armiqtë, në luftim trup më " ++
          "trup, dhe e kthen energjinë në dëm: aty ku Pesëkëndëshi duhet të shenjojë, " ++
          "Trekëndëshi godet atë që ka në shënjestër.",
        "Ai është edhe kontrolli i ekipit: kur shfaqen armiq shtesë, i shtang ata dhe ua " ++
          "ndërpret magjitë që hedhin."
      )
    ),
    TextSection(
      "Energjia",
      Seq(
        s"Energjia jote (${formatNumber(Triangle.initialResourceAmount.amount)}) rimbushet me " ++
          "ritëm të qëndrueshëm. Direct Hit është dëmi yt kryesor, por i kushtueshëm. Energy " ++
          "Kick të kthen energji: përdore ndërkohë që pret. Energjia e lënë në maksimum është " ++
          "energji e humbur.",
        "- Mbaje aktiv Upgrade Direct Hit: e bën Direct Hit më të fortë për një kohë.",
        "- Përdor Direct Hit sa herë që ke energji.",
        "- Përdor Energy Kick kur është gati dhe ke pak energji."
      )
    ),
    TextSection(
      "Qëndro gjallë",
      Seq(
        s"Me ${formatNumber(Triangle.initialMaxLife)} pikë jete, nuk i përballon goditjet e " ++
          "forta të bossit. Dëmi yt krijon kërcënim shpejt: mbaj sytë te matësi i kërcënimit " ++
          "dhe lëre Katrorin të qëndrojë në krye. Qëndro brenda rrezes së shëruesit."
      )
    ),
    TextSection(
      "Kontrolli",
      Seq(
        "Stun ndalon një armik (por jo një boss) për disa sekonda, duke ia ndërprerë magjinë, " ++
          "derisa të marrë dëm. Cut ndërpret një magji dhe e bën të heshtë atë që e hodhi. " ++
          "Ruaji për magjitë që kanë rëndësi."
      )
    )
  )

  private def square = Seq(
    TextSection(
      "Filozofia",
      Seq(
        "Katrori është muri i ekipit. Detyra e tij nuk është të shkaktojë dëm, por të jetë ai " ++
          s"që godet bossi: ka ${formatNumber(Square.initialMaxLife)} pikë jete, dyfishin e të " ++
          "tjerëve, një mburojë që e forcon vazhdimisht, dhe aftësi që krijojnë shumë " ++
          "kërcënim.",
        "Një tank i mirë është qetësia në mes të stuhisë: për sa kohë mban bossin, të gjithë " ++
          "të tjerët mund të bëjnë punën e tyre."
      )
    ),
    TextSection(
      "Tërbimi",
      Seq(
        "Tërbimi yt vjen nga dëmi që merr (dhe pak çdo sekondë). Goditjet që merr janë ato që " ++
          "të ushqejnë! Kur bossi nuk po të godet, Enrage të lëndon vetë për të mbledhur " ++
          "tërbim."
      )
    ),
    TextSection(
      "Brace",
      Seq(
        s"Brace i shton një shtresë mburojës sate, deri në ${BraceShield.maxStacks}. Çdo shtresë " ++
          s"e ul dëmin që merr me ${formatNumber((1 - BraceShield.damageMultiplier) * 100)}%. " ++
          s"Mburoja humbet një shtresë pas ${formatSeconds(BraceShield.duration)}, nëse nuk " ++
          "përdor Brace përsëri. Treguesi në të majtë të kornizës sate tregon shtresat: mbaje " ++
          "plot.",
        "Shield Slam shpenzon një shtresë për një goditje të fortë me kërcënim shtesë."
      )
    ),
    TextSection(
      "Kërcënimi",
      Seq(
        "Përdor Taunt mbi bossin sapo të mbërrijë, pastaj vazhdo ta godasësh (Hammer Hit, " ++
          "Shield Slam, Cleave) për të qëndruar në krye të matësit të kërcënimit. Nëse dikush " ++
          "të kalon, përdor Taunt përsëri.",
        "Guard mbron një aleat për një kohë: një pjesë të dëmit që merr ai, e merr ti në vend " ++
          "të tij, p.sh. kur mbart një efekt negativ të rrezikshëm."
      )
    ),
    TextSection(
      "Kundër Boss 101",
      Seq(
        s"Big Hit i tij shkakton ${formatNumber(BigHit.damageAmount)} dëm, pas një hedhjeje prej " ++
          s"${formatSeconds(BigHit.castingTime)}: ndiq shiritin e hedhjes së bossit, dhe ki " ++
          "shtresat e Brace gati kur të godasë."
      )
    )
  )

  private def pentagon = Seq(
    TextSection(
      "Filozofia",
      Seq(
        "Pesëkëndëshi është artileria e ekipit: e shkakton dëmin nga larg, jashtë mundësisë së " ++
          "bossit për ta arritur. Fuqia e tij kërkon mjeshtëri: plumbat e tij shkojnë aty ku " ++
          "shenjon ti, dhe zonat e tij duhen vendosur.",
        "Ai është edhe i vetmi që mund të heqë efektet negative (debuff) të rrezikshme që " ++
          "disa bossë u vendosin lojtarëve."
      )
    ),
    TextSection(
      "Shenjimi",
      Seq(
        "- Pentagon Bullet fluturon drejt miut tënd (jo objektivit tënd) dhe godet armikun e " ++
          "parë në rrugën e tij: mbaj një vijë të lirë drejt bossit. Fracture fluturon në " ++
          "të njëjtën mënyrë.",
        "- Pentagon Zone: hidhe, pastaj kliko ku ta vendosësh. Armiqtë që qëndrojnë në të " ++
          s"marrin dëm me kalimin e kohës, por mund të vendosësh vetëm një çdo ${formatSeconds(CreatePentagonZone.cooldown)}: " ++
          "vendose aty ku bossi do të qëndrojë, jo pak para se të lëvizë."
      )
    ),
    TextSection(
      "Shenjim i qëndrueshëm",
      Seq(
        s"Çdo ${formatPeriod(SteadyAim.stackEvery)} pa lëvizur, aftësitë e tua shkaktojnë ${formatNumber(SteadyAim.damageIncreasePerStack * 100)}% më shumë dëm, " ++
          s"deri në ${formatNumber(SteadyAim.maxStacks * SteadyAim.damageIncreasePerStack * 100)}% (matësi në të majtë fare të kornizës sate). Lëvizja t'i humbet " ++
          "të gjitha, dhe ndërpret atë që po hedh: zgjidh vendin herët, dhe shmang hapat e vegjël."
      )
    ),
    TextSection(
      "Faqet",
      Seq(
        s"Çdo Pentagon Bullet që godet ndez një nga ${Facets.maxStacks} faqet e tua (në anët e tua, dhe " ++
          "në matësin në të majtë të kornizës sate). Kur janë ndezur të gjitha, Pentagram i " ++
          "shpenzon për një goditje të menjëhershme: ruaje për kur duhet të lëvizësh. Por një " ++
          "plumb që godet kur janë ndezur të gjitha shkon dëm.",
        s"Fracture shkakton më pak dëm, por armiku që godet merr ${formatNumber(Fractured.damageIncrease * 100)}% më shumë dëm nga " ++
          s"ti për ${formatSeconds(Fractured.duration)}. Hidhe sërish pak para fundit: deri në ${formatSeconds(Fractured.maxCarriedOver)} që kanë " ++
          "mbetur shtohen."
      )
    ),
    TextSection(
      "Mana",
      Seq(
        s"Aftësitë e tua kushtojnë mana (${formatNumber(Pentagon.initialResourceAmount.amount)}). " ++
          "Vazhdo të qëllosh, por ruaj pak për kur duhet hequr një efekt."
      )
    ),
    TextSection(
      "Heqja e efekteve",
      Seq(
        "Penta Dispel heq nga një aleat të gjitha efektet që mund të hiqen, përfshirë edhe ato " ++
          "të mirat (si Heal Over Time i shëruesit!). Bossët e mëvonshëm vendosin efekte " ++
          "negative që duhen hequr shpejt. Big Dot i Boss 101 nuk mund të hiqet: shëruesi e " ++
          "përballon me shërime."
      )
    ),
    TextSection(
      "Qëndro gjallë",
      Seq(
        s"Me ${formatNumber(Pentagon.initialMaxLife)} pikë jete, mos e lër bossin të kthehet " ++
          "kundër teje: ndiq matësin e kërcënimit. Qëndro larg bossit, por brenda rrezes së " ++
          "shëruesit."
      )
    )
  )

  private def hexagon = Seq(
    TextSection(
      "Filozofia",
      Seq(
        "Gjashtëkëndëshi e mban ekipin në këmbë. Ai i ndjek kornizat e ekipit më shumë se " ++
          "arenën: kush po humbet jetë, kush është gati të marrë një goditje të fortë.",
        s"Rezerva e tij e manës është e madhe (${formatNumber(Hexagon.initialResourceAmount.amount)}) " ++
          "por rigjenerohet ngadalë: loja e shëruesit është të shpenzojë shërimin e duhur te " ++
          "lojtari i duhur, në kohën e duhur."
      )
    ),
    TextSection(
      "Shërimet e tua",
      Seq(
        "- Flash Heal shëron shumë, por kërkon kohë për t'u hedhur: duhet të qëndrosh në vend.",
        "- Heal Over Time është i lirë dhe i menjëhershëm, dhe mund të hidhet duke lëvizur: " ++
          "vendose te lojtarët që marrin dëm vazhdimisht (tanku, mbartësi i Big Dot).",
        "- Hexagon Zone shëron këdo që qëndron në të: për kur ekipi është i grumbulluar."
      )
    ),
    TextSection(
      "Zgjedhja e objektivit",
      Seq(
        "Kliko një kornizë në të majtë të ekranit për të zgjedhur një lojtar si objektiv. Një " ++
          "kornizë e zbehur është një lojtar jashtë rrezes së shërimit tënd: afrohu.",
        "Me \"Nën miun\" të aktivizuar te Cilësimet, shërimet e tua shkojnë te lojtari, korniza " ++
          "e të cilit është nën miun, pa ndryshuar objektivin tënd."
      )
    ),
    TextSection(
      "Kundër Boss 101",
      Seq(
        "Tanku merr goditjet e bossit: mbaje me jetë të plotë, sidomos para një Big Hit. Big " ++
          s"Dot shkakton ${formatNumber(BigDot.damageOnTick)} dëm çdo ${formatPeriod(BigDot.tickRate)} " ++
          s"për ${formatSeconds(BigDot.duration)} te dikush tjetër: një Heal Over Time mbi të " ++
          "mbulon pjesën më të madhe. Mos shëro lojtarë me jetë të plotë: është mana e humbur."
      )
    )
  )

  // Previews of the pages
  override def arena: String     = "Arena"
  override def screen: String    = "Ekrani"
  override def hoverHint: String = "Kalo miun mbi aftësitë, kliko kornizat"

  override def legendBoss: String          = "Bossi: jeta e tij dhe çfarë hedh."
  override def legendBossCooldowns: String = "Kur rikthehen aftësitë e bossit."
  override def legendTeam: String          = "Ekipi yt. Kliko një kornizë për të zgjedhur lojtarin."
  override def legendYouAndTarget: String  = "Ti dhe objektivi yt, mbi aftësitë e tua."
  override def legendMeters: String = "Dëmi i shkaktuar objektivit tënd dhe kërcënimi te bossi."
  override def legendTriangleEnergy: String    = "Energjia jote: shpenzoje, rimbushet vazhdimisht."
  override def legendTriangleAbilities: String = "Aftësitë e tua: Direct Hit, Energy Kick, ..."
  override def legendTriangleDamage: String    = "Vendi yt në matësin e dëmit: në krye!"
  override def legendTriangleThreat: String =
    "Por jo në matësin e kërcënimit: ai i takon Katrorit."
  override def legendSquareStacks: String = "Shtresat e mburojës sate (Brace): mbaji aktive."
  override def legendSquareRage: String   = "Tërbimi yt, që fitohet duke marrë dëm."
  override def legendSquareBigHit: String =
    "Bossi hedh Big Hit mbi ty: ki shtresat gati!"
  override def legendSquareThreat: String = "Duhet të qëndrosh në krye të matësit të kërcënimit."
  override def legendPentagonMana: String = "Mana jote."
  override def legendPentagonAim: String =
    "Plumbat fluturojnë drejt miut, zonat vendosen me një klik."
  override def legendPentagonDebuffs: String =
    "Efektet negative të lojtarëve: hiq ato që mund të hiqen."
  override def legendPentagonDamage: String = "Vendi yt në matësin e dëmit."
  override def legendPentagonFacets: String =
    "Faqet e tua: Pentagram është gati kur janë ndezur të gjitha."
  override def legendPentagonSteadyAim: String =
    "Shenjimi yt i qëndrueshëm: qëndro pa lëvizur për ta mbushur."
  override def legendHexagonTeam: String  = "Ekipi yt: kliko një kornizë për të zgjedhur lojtarin."
  override def legendHexagonFaded: String = "E zbehur: jashtë rrezes së shërimit tënd. Afrohu!"
  override def legendHexagonMana: String  = "Mana jote: rigjenerohet ngadalë."
  override def legendHexagonTarget: String =
    "Objektivi yt, tanku, me Heal Over Time që i ke vendosur."

  override def arenaHealerRange: String = "Rrezja e shëruesit"
  override def arenaBoss: String        = "Bossi"
  override def arenaSquare: String      = "Katrori mban bossin, përballë tij"
  override def arenaTriangle: String    = "Trekëndëshi e godet nga afër, nga pas"
  override def arenaPentagons: String   = "Pesëkëndëshat qëllojnë nga larg"
  override def arenaHexagon: String     = "Gjashtëkëndëshi shëron, duke i arritur të gjithë"
  override def arenaLegendPositions: String =
    "Tanku e kthen bossin larg të tjerëve, që e godasin nga pas ose nga larg."
  override def arenaLegendRange: String =
    "Të gjithë qëndrojnë brenda rrezes së shëruesit (kornizat e ekipit zbehen jashtë saj)."

  // --- Landing page ---
  override def landingTagline: String =
    "Ngrihu kundër Kryepriftërinjve të Flatland-it, së bashku."
  override def landingPitch: String =
    "Një lojë bashkëpunimi me beteja kundër bossëve si në World of Warcraft: një tank, një " ++
      "shërues dhe sulmues kundër tiranëve të Flatland-it. Mëso aftësitë e tyre, luaj rolin tënd dhe mposhti në beteja " ++
      "që zgjasin minuta, jo mbrëmje të tëra."
  override def landingDownload: String             = "Shkarko"
  override def downloadFor(system: String): String = s"Shkarko për $system"
  override def landingLoveFile: String             = "E ke tashmë LÖVE 12? Merr skedarin .love"
  override def landingFreeAndOpen: String          = "Falas dhe me burim të hapur, tani në beta."
  override def landingUnsigned: String =
    "Loja nuk është ende e nënshkruar: Windows mund të të paralajmërojë (\"More info\", pastaj " ++
      "\"Run anyway\"), dhe macOS të kërkon ta lejosh te System Settings, nën Privacy & " ++
      "Security (\"Open Anyway\")."
  override def landingAnimation: String = "Një betejë kundër një bossi në Battle for Flatland"

  override def landingFeaturesTitle: String = "Raide si në WoW, pa grind"
  override def landingFeatures: Seq[(String, String)] = Seq(
    "Ndjesia e një raidi" ->
      ("Një tank që mban bossin, një shërues që i mban të gjithë në këmbë, sulmues që garojnë " ++
        "me kohën, dhe një boss, aftësitë e të cilit i mësoni dhe i kundërshtoni së bashku."),
    "Pa farmim, kurrë" ->
      ("Asnjë pajisje për të mbledhur, asnjë nivel për të ngjitur: të gjithë nisin njësoj, dhe " ++
        "ka rëndësi vetëm si luani së bashku."),
    "Bërë për seanca të shkurtra" ->
      ("Pa përgatitje të gjata: krijo një lojë, zini vendet, luftoni. Një betejë zgjat disa " ++
        "minuta, futet mes dy përrallave para gjumit, dhe mund të ndalosh kur thërrasin " ++
        "fëmijët."),
    "Kurrë pa lojtarë" ->
      ("S'ka shërues sonte? Plotëso ekipin me lojtarë AI. Dhe mëso çdo klasë në tutorial, me " ++
        "një betejë stërvitore kundër bossit të parë.")
  )
  override def landingClassesTitle: String     = "Katër klasa, katër role"
  override def landingScreenshotsTitle: String = "Në arenë"
  override def landingCaption(picture: LandingPicture): String = picture match {
    case LandingPicture.Fight =>
      "I gjithë ekipi kundër një bossi: tanku përpara, të tjerët prapa, brenda rrezes së " ++
        "shëruesit."
    case LandingPicture.Boss =>
      "Çdo boss ka aftësitë e veta, për t'i mësuar dhe kundërshtuar së bashku."
    case LandingPicture.Lobby =>
      "Krijo një lojë, zgjidh klasën tënde dhe bossin, dhe plotëso vendet bosh me lojtarë AI."
    case LandingPicture.Tutorial =>
      "Tutoriali shpjegon çdo klasë, pastaj përgatit një betejë stërvitore kundër lojtarëve AI."
  }

  override def landingStepsTitle: String = "Nga shkarkimi te bossi, në pak minuta"
  override def landingSteps: Seq[String] = Seq(
    "Shkarko lojën për sistemin tënd, hape arkivin dhe nise.",
    "Zgjidh një pseudonim: pa llogari, pa fjalëkalim.",
    "Lexo për klasën tënde në tutorial dhe stërvitu kundër Boss 101.",
    "Krijo një lojë, lër miqtë e tu të bashkohen (ose shto lojtarë AI), zgjidh një boss dhe " ++
      "lufto!"
  )
  override def landingOpenSourceBadge: String = "Burim i hapur"
  override def landingOpenSourceTitle: String = "Me burim të hapur, nga fillimi deri në fund"
  override def landingOpenSourceText: String =
    "Battle for Flatland është me burim të hapur, nga menutë deri te bossi i fundit: asnjë kod " ++
      "i fshehur, dhe një lojë që u përket lojtarëve të saj po aq sa autorit të saj."
  override def landingOpenSource: Seq[(String, String)] = Seq(
    "I gjithë kodi, haptas" ->
      ("Çdo rresht i lojës është në GitHub, nga menutë deri te aftësitë e bossëve: lexoje, " ++
        "mëso prej tij, ndryshoje."),
    "Strehoje vetë" ->
      ("Serveri vjen me udhëzimet për ta nisur në kompjuterin tënd: askush nuk mund të ta " ++
        "marrë lojën."),
    "Bëj bossin tënd" ->
      ("Një udhëzues hap pas hapi shpjegon si të implementosh një boss të ri. Propozoje tëndin " ++
        "në GitHub, dhe ndoshta do të hyjë në lojë!")
  )

  override def landingFaqTitle: String = "Pyetje të shpeshta"
  override def landingFaq: Seq[(String, String)] = Seq(
    "A mund të luaj vetëm?" ->
      ("Po: çdo boss vjen me lojtarë AI, që mund të zënë të gjitha vendet e tjera të ekipit. " ++
        "Është një mënyrë e mirë për të zbuluar një boss dhe për t'u stërvitur me ritmin tënd. " ++
        "Por loja është bërë për t'u luajtur së bashku: lojtarët AI nuk të japin përvojën më " ++
        "të mirë, që është të mposhtësh një boss me miqtë."),
    "A duhet ta njoh World of Warcraft?" ->
      ("Aspak. Nëse ke bërë raide në WoW, do të ndihesh menjëherë si në shtëpi. Nëse jo, " ++
        "tutoriali shpjegon çdo rol dhe të lejon ta stërvitësh kundër një bossi të parë."),
    "A është loja bërë për një ekip prej pesë vetash?" ->
      ("Po dhe jo. Shumica e bossëve janë menduar për pesë lojtarë: një tank, një shërues dhe " ++
        "tre sulmues. Por disa kërkojnë një ekip më të madh: tashmë dhjetë lojtarë, dhe me " ++
        "gjasë deri në njëzet më vonë."),
    "A do të luaj gjithmonë të njëjtën klasë?" ->
      ("Aspak! Çdo boss kërkon përbërjen e vet të ekipit, ndaj secili do të luajë edhe klasa " ++
        "të tjera. Dhe meqë nuk ka progresion, mund ta ndërrosh klasën kur të duash."),
    "Si të luaj me miqtë e mi?" ->
      ("Krijo një lojë në menu, dhe miqtë e tu i bashkohen nga lista e lojërave. Asnjë server " ++
        "për të ngritur: kur fillon beteja, kompjuteri i njërit prej lojtarëve e strehon atë, " ++
        "vetvetiu."),
    "A më duhet një llogari?" ->
      ("Jo, jo për momentin: thjesht zgjidh një pseudonim, me të cilin të njohin të tjerët në " ++
        "menu dhe në lojë. Llogaritë me gjasë do të vijnë më vonë."),
    "Në cilat sisteme punon?" ->
      ("Windows, macOS dhe Linux. Dhe kudo ku punon LÖVE 12, me skedarin .love."),
    "A është loja falas, dhe a do të mbetet e tillë?" ->
      ("Sot është falas, e shkarkuar nga kjo faqe. Pas një ose dy vjetësh, loja duhet të " ++
        "kalojë në një platformë si Steam, ku do të jetë me pagesë. Por do të mbetet me burim " ++
        "të hapur, me udhëzimet për ta strehuar vetë: nëse do, do të mund të luash gjithmonë " ++
        "falas."),
    "A do të jetë loja në Steam?" ->
      "Ky është qëllimi, por jo në të ardhmen e afërt: me gjasë pas një ose dy vjetësh.",
    "A mund të bëj bossin tim?" ->
      ("Po, nëse di të programosh: loja është me burim të hapur, dhe depoja e saj ka një " ++
        "udhëzues hap pas hapi për të implementuar një boss të ri. Propozoje në GitHub, dhe " ++
        "ndoshta do të hyjë në lojë!")
  )

  override def landingBetaTitle: String = "Bëhu beta-testues"
  override def landingBetaText: String =
    "Battle for Flatland është në beta dhe vazhdon të rritet. Shkarkoje, luaj disa beteja me " ++
      "miqtë dhe na thuaj çfarë mendon: çdo raportim gabimi, ide dhe dëshirë ndihmon në " ++
      "formimin e lojës."
  override def landingFeedback: String   = "Jep mendimin tënd"
  override def landingSourceCode: String = "Kodi burim në GitHub"
  override def landingFooter: String =
    "Battle for Flatland është falas dhe me burim të hapur, bërë me Scala dhe LÖVE."
  override def landingTrademark: String =
    "World of Warcraft është markë tregtare e Blizzard Entertainment, me të cilën Battle for " ++
      "Flatland nuk ka lidhje."
}
