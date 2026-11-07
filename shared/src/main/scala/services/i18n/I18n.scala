package services.i18n

import gamelogic.abilities.Ability
import gamelogic.buffs.Buff
import gamelogic.entities.{Entity, Resource}
import models.bff.ingame.Controls
import models.bff.ingame.Controls.InputCode
import models.bff.outofgame.PlayerClasses

/** The texts of the UI, in one language (one implementation per language).
  *
  * The names of the bosses and of the abilities are not translated (they come with the game logic).
  */
trait I18n {

  // --- Common ---

  /** The name of a class (in the lobby, the tutorial...). */
  def playerClassName(playerClass: PlayerClasses): String

  /** The name of a resource (mana, energy, rage). */
  def resourceName(resource: Resource): String

  /** A mouse button, as a key of the controls in a sentence, e.g. "mouse button 2". */
  def mouseButton(number: String): String

  /** A key of the controls in a sentence: KeyW is W, Digit1 is 1, Shift KeyQ is Shift Q, and the
    * mouse buttons are [[mouseButton]].
    */
  def key(code: InputCode): String = code.label match {
    case s"Button $number" => mouseButton(number)
    case label => label.replace("Digit", "").replace("Key", "")
  }

  /** A number in a text, without its decimal part when there is none (and the language's decimal
    * separator otherwise).
    */
  def formatNumber(x: Double): String = gamelogic.docs.formatNumber(x)

  /** A duration in a text, e.g. "8 seconds". */
  def formatSeconds(millis: Long): String

  /** A period, after the language's "every", e.g. "3 seconds". */
  def formatPeriod(millis: Long): String = formatSeconds(millis)

  def settings: String
  def leaveGame: String
  def quit: String
  def name: String
  def playerClass: String
  def colour: String
  def ready: String
  def notReady: String
  def boss: String
  def victory: String
  def defeat: String
  def startGame: String

  /** What an ability takes, in its tooltips and docs. */
  def castingTime(time: String): String
  def cooldown(time: String): String
  def cost(amount: String): String

  // --- Menus ---

  def fullscreenHint: String
  def connectingToGameServer: String
  def leavingTheGame: String
  def connectionToGameServerLost: String
  def cannotConnectToGameServer(error: String): String
  def couldNotLeaveGame(error: String): String

  /** On the first launch, under the button of this language. */
  def chooseYourLanguage: String

  // Login
  def welcomeToBattleForFlatland: String
  def serverAddress: String
  def serverAddressExample: String
  def choosePseudo: String
  def yourPseudo: String
  def loggingIn: String
  def enterBattleForFlatland: String
  def serverError(message: String): String
  def pseudoError(message: String): String
  def cannotLogIn(address: String, error: String): String
  def invalidPort(port: String): String
  def invalidAddress(address: String): String

  /** When the menu server has another version of the game than the player's. */
  def outdatedVersion: String
  def downloadTheNewVersion: String

  // Games
  def welcomePlayer(playerName: String): String
  def learnToPlay: String
  def joinItBack: String
  def gameNamePlaceholder: String
  def defaultGameName(playerName: String): String
  def createGame: String
  def createdBy: String
  def players: String
  def loadingTheGames: String
  def noGames: String
  def join: String
  def joinBack: String
  def currentlyInGame(gameName: String): String

  // Lobby of a game
  def loadingTheGame: String
  def loggedInAs(playerName: String): String
  def gameCreatedBy(creator: String, gameId: String): String
  def kick: String
  def you(playerName: String): String
  def ai(playerName: String): String
  def classNotChosen: String
  def addAI: String
  def fillWithAIs: String
  def launchGame: String
  def waitingForCreatorToLaunch: String
  def gameIsStarting: String
  def gameServerNotReadyYet: String
  def gameServerRunsOnYourComputer: String
  def gameServerRunsOnComputerOf(host: String): String
  def downloadingJavaRuntime: String
  def downloadingGameServer: String
  def megabytes(received: Int, size: Int): String
  def gameClosedByCreator: String
  def notInThisGame: String
  def everyoneReady: String
  def thingsPreventingStart: String

  // Docs of the bosses and classes, in the lobby
  def classes: String
  def bossFacts(life: Int, intendedFor: Int): String
  def classFacts(life: Int, resourceAmount: Int, resource: Resource, isYours: Boolean): String
  def abilities: String

  // Settings
  def controls: String
  def general: String
  def goUp: String
  def goDown: String
  def goLeft: String
  def goRight: String
  def nextTarget: String
  def faceTheTarget: String
  def abilityNumber(number: Int): String
  def markerCross: String
  def markerLozenge: String
  def markerMoon: String
  def markerSquare: String
  def markerStar: String
  def markerTriangle: String
  def markerOnTarget(marker: String): String
  def markerOnGround(marker: String): String
  def bindingsHelp: String
  def mouseOver: String
  def mouseOverHelp: String
  def resetToDefaults: String
  def escapeToCancel: String
  def pressKeyFor(action: String): String
  def boundToSeveralActions(key: String): String
  def language: String
  def languageHelp: String
  def hudScale: String
  def hudScaleHelp: String
  def hostGameServers: String
  def hostGameServersHelp: String
  def mainVolume: String
  def abilitiesVolume: String
  def volumeOff: String
  def volumesHelp: String
  def done: String

  // --- In game ---

  def placeYourselves: String
  def bossArrivesIn: String
  def clickToPlaceZone: String
  def waitingForGameToStart: String
  def bossAbilities: String
  def threat: String
  def damage: String
  def damageTo(name: String): String
  def dead: String

  // Casting
  def youAreDead: String
  def noTarget: String
  def noAbilityOnKey(number: Int): String
  def abilityNotSupported(ability: Ability.AbilityId): String
  def abilityNotPlacedWithMouse(ability: Ability.AbilityId): String

  // Why an ability cannot be cast (shown to the player)
  def alreadyCasting: String
  def abilityNotOwned: String
  def abilityOnCooldown: String
  def notEnough(resource: Resource): String
  def silenced: String
  def notInRange: String
  def notInSight: String
  def positionNotInSight: String
  def notAnAlly: String
  def notAnEnemy: String
  def cannotGuardYourself: String
  def needShieldStack: String
  def needAllFacets: String
  def targetCannotBeStunned: String
  def targetDoesNotCast: String
  def targetAbilityCannotBeCut: String

  // Why a boss' ability cannot be cast
  def noBombToExplode: String
  def creepingShadowNotThere: String
  def bossNotThere: String
  def creepingShadowDoesNotCollideBoss: String
  def noDeathMarkTriangle: String
  def noShadeToCall: String
  def targetNotThere: String
  def twinsFarEnough: String

  // Why an action is not legal (rather for the logs, these should not happen in a game)
  def gameAlreadyStarted: String
  def gameAlreadyEnded: String
  def playerAlreadyExists: String
  def bossDoesNotExist(bossName: String): String
  def entityDoesNotExist(entityId: Entity.Id): String
  def entitiesDoNotExist(entityId: Entity.Id, otherId: Entity.Id): String
  def entityIsNotLiving(entityId: Entity.Id): String
  def entityIsNotMoving(entityId: Entity.Id): String
  def entityHasNoAbilities(entityId: Entity.Id): String
  def entityHasNoTarget(entityId: Entity.Id): String
  def entityWouldCollide(entityId: Entity.Id): String
  def buffDoesNotExist(buffId: Buff.Id, bearerId: Entity.Id): String

  // Connection to the game server
  def gameServerDoesNotAnswer: String
  def cannotSynchronizeClock: String
  def gameServerNotReady: String
  def gameServerRejectsCredentials: String
  def gameServerDoesNotKnowUs: String
  def gameCouldNotStart(error: String): String

  // Game menu
  def menu: String
  def resume: String
  def surrender: String
  def clickAgainToSurrender: String
  def surrenderEndsInDefeat: String
  def gameGoesOnMeanwhile: String
  def windowed: String
  def fullscreen: String

  // End of the game
  def backToMenus: String
  def connectionLost: String
  def fightLasted(minutes: Int, seconds: Int): String
  def gameServerClosedConnection: String

  // --- Tutorial ---

  def tutorialIntro: String
  def tutorialHint: String
  def backToTheGames: String
  def backToTheTutorial: String
  def next(title: String): String
  def practiceAgainst(bossName: String): String
  def settingUpTheGame: String
  def alreadyInAGame: String
  def practiceGameName(playerName: String): String

  /** The pages: the overview, then one per class. */
  def overview: String
  def overviewRole: String
  def overviewSummary: String

  /** The title of the class' page, e.g. "The Triangle". */
  def theClass(playerClass: PlayerClasses): String
  def classRole(playerClass: PlayerClasses): String
  def classSummary(playerClass: PlayerClasses): String

  /** The story of Flatland: on the overview's page, and on the landing page. */
  def story: TextSection

  /** What a page says (with the player's keys, from their controls). */
  def overviewSections(controls: Controls): Seq[TextSection]
  def classSections(playerClass: PlayerClasses): Seq[TextSection]

  // Previews of the pages
  def arena: String
  def screen: String
  def hoverHint: String

  def legendBoss: String
  def legendBossCooldowns: String
  def legendTeam: String
  def legendYouAndTarget: String
  def legendMeters: String
  def legendTriangleEnergy: String
  def legendTriangleAbilities: String
  def legendTriangleDamage: String
  def legendTriangleThreat: String
  def legendSquareStacks: String
  def legendSquareRage: String
  def legendSquareBigHit: String
  def legendSquareThreat: String
  def legendPentagonMana: String
  def legendPentagonAim: String
  def legendPentagonDebuffs: String
  def legendPentagonDamage: String
  def legendPentagonFacets: String
  def legendPentagonSteadyAim: String
  def legendHexagonTeam: String
  def legendHexagonFaded: String
  def legendHexagonMana: String
  def legendHexagonTarget: String

  def arenaHealerRange: String
  def arenaBoss: String
  def arenaSquare: String
  def arenaTriangle: String
  def arenaPentagons: String
  def arenaHexagon: String
  def arenaLegendPositions: String
  def arenaLegendRange: String

  // --- Landing page ---
  // The website's page, to download the game (see the server's `LandingPage`)

  /** The story in one line, under the title. */
  def landingTagline: String

  /** What the game is, in two sentences (also the page's description for search engines). */
  def landingPitch: String
  def landingDownload: String

  /** E.g. "Download for Windows". */
  def downloadFor(system: String): String
  def landingLoveFile: String
  def landingFreeAndOpen: String

  /** That the packages are not signed, and how to run them anyway. */
  def landingUnsigned: String

  /** The alternative text of the animation of a fight. */
  def landingAnimation: String

  /** What the game offers to its players, as (title, text). */
  def landingFeaturesTitle: String
  def landingFeatures: Seq[(String, String)]
  def landingClassesTitle: String
  def landingScreenshotsTitle: String
  def landingCaption(picture: LandingPicture): String

  /** From the download to the first fight. */
  def landingStepsTitle: String
  def landingSteps: Seq[String]
  def landingBetaTitle: String
  def landingBetaText: String
  def landingFeedback: String
  def landingSourceCode: String
  def landingFooter: String
}
