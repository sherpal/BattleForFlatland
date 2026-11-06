package services.i18n

/** Empty [[I18n]] implementation used when generating text does not matter.
  */
object DummyI18n extends I18n {
  def abilities: String = ""

  def abilitiesVolume: String = ""

  def abilityNotOwned: String = ""

  def abilityNotPlacedWithMouse(ability: gamelogic.abilities.Ability.AbilityId): String = ""

  def abilityNotSupported(ability: gamelogic.abilities.Ability.AbilityId): String = ""

  def abilityNumber(number: Int): String = ""

  def abilityOnCooldown: String = ""

  def addAI: String = ""

  def ai(playerName: String): String = ""

  def alreadyCasting: String = ""

  def alreadyInAGame: String = ""

  def arena: String = ""

  def arenaBoss: String = ""

  def arenaHealerRange: String = ""

  def arenaHexagon: String = ""

  def arenaLegendPositions: String = ""

  def arenaLegendRange: String = ""

  def arenaPentagons: String = ""

  def arenaSquare: String = ""

  def arenaTriangle: String = ""

  def backToMenus: String = ""

  def backToTheGames: String = ""

  def backToTheTutorial: String = ""

  def bindingsHelp: String = ""

  def boss: String = ""

  def bossAbilities: String = ""

  def bossArrivesIn: String = ""

  def bossDoesNotExist(bossName: String): String = ""

  def bossFacts(life: Int, intendedFor: Int): String = ""

  def bossNotThere: String = ""

  def boundToSeveralActions(key: String): String = ""

  def buffDoesNotExist(
      buffId: gamelogic.buffs.Buff.Id,
      bearerId: gamelogic.entities.Entity.Id
  ): String = ""

  def cannotConnectToGameServer(error: String): String = ""

  def cannotGuardYourself: String = ""

  def cannotLogIn(address: String, error: String): String = ""

  def cannotSynchronizeClock: String = ""

  def castingTime(time: String): String = ""

  def choosePseudo: String = ""

  def chooseYourLanguage: String = ""

  def classFacts(
      life: Int,
      resourceAmount: Int,
      resource: gamelogic.entities.Resource,
      isYours: Boolean
  ): String = ""

  def classNotChosen: String = ""

  def classRole(playerClass: models.bff.outofgame.PlayerClasses): String = ""

  def classSections(
      playerClass: models.bff.outofgame.PlayerClasses
  ): Seq[services.i18n.TextSection] = Seq.empty

  def classSummary(playerClass: models.bff.outofgame.PlayerClasses): String = ""

  def classes: String = ""

  def clickAgainToSurrender: String = ""

  def clickToPlaceZone: String = ""

  def colour: String = ""

  def connectingToGameServer: String = ""

  def connectionLost: String = ""

  def connectionToGameServerLost: String = ""

  def controls: String = ""

  def cooldown(time: String): String = ""

  def cost(amount: String): String = ""

  def couldNotLeaveGame(error: String): String = ""

  def createGame: String = ""

  def createdBy: String = ""

  def creepingShadowDoesNotCollideBoss: String = ""

  def creepingShadowNotThere: String = ""

  def currentlyInGame(gameName: String): String = ""

  def damage: String = ""

  def damageTo(name: String): String = ""

  def dead: String = ""

  def defaultGameName(playerName: String): String = ""

  def defeat: String = ""

  def done: String = ""

  def downloadingGameServer: String = ""

  def downloadingJavaRuntime: String = ""

  def enterBattleForFlatland: String = ""

  def entitiesDoNotExist(
      entityId: gamelogic.entities.Entity.Id,
      otherId: gamelogic.entities.Entity.Id
  ): String = ""

  def entityDoesNotExist(entityId: gamelogic.entities.Entity.Id): String = ""

  def entityHasNoAbilities(entityId: gamelogic.entities.Entity.Id): String = ""

  def entityHasNoTarget(entityId: gamelogic.entities.Entity.Id): String = ""

  def entityIsNotLiving(entityId: gamelogic.entities.Entity.Id): String = ""

  def entityIsNotMoving(entityId: gamelogic.entities.Entity.Id): String = ""

  def entityWouldCollide(entityId: gamelogic.entities.Entity.Id): String = ""

  def escapeToCancel: String = ""

  def everyoneReady: String = ""

  def faceTheTarget: String = ""

  def fightLasted(minutes: Int, seconds: Int): String = ""

  def fillWithAIs: String = ""

  def formatSeconds(millis: Long): String = ""

  def fullscreen: String = ""

  def fullscreenHint: String = ""

  def gameAlreadyEnded: String = ""

  def gameAlreadyStarted: String = ""

  def gameClosedByCreator: String = ""

  def gameCouldNotStart(error: String): String = ""

  def gameCreatedBy(creator: String, gameId: String): String = ""

  def gameGoesOnMeanwhile: String = ""

  def gameIsStarting: String = ""

  def gameNamePlaceholder: String = ""

  def gameServerClosedConnection: String = ""

  def gameServerDoesNotAnswer: String = ""

  def gameServerDoesNotKnowUs: String = ""

  def gameServerNotReady: String = ""

  def gameServerNotReadyYet: String = ""

  def gameServerRejectsCredentials: String = ""

  def gameServerRunsOnComputerOf(host: String): String = ""

  def gameServerRunsOnYourComputer: String = ""

  def general: String = ""

  def goDown: String = ""

  def goLeft: String = ""

  def goRight: String = ""

  def goUp: String = ""

  def hostGameServers: String = ""

  def hostGameServersHelp: String = ""

  def hoverHint: String = ""

  def hudScale: String = ""

  def hudScaleHelp: String = ""

  def invalidAddress(address: String): String = ""

  def invalidPort(port: String): String = ""

  def join: String = ""

  def joinBack: String = ""

  def joinItBack: String = ""

  def kick: String = ""

  def language: String = ""

  def languageHelp: String = ""

  def launchGame: String = ""

  def learnToPlay: String = ""

  def leaveGame: String = ""

  def leavingTheGame: String = ""

  def legendBoss: String = ""

  def legendBossCooldowns: String = ""

  def legendHexagonFaded: String = ""

  def legendHexagonMana: String = ""

  def legendHexagonTarget: String = ""

  def legendHexagonTeam: String = ""

  def legendMeters: String = ""

  def legendPentagonAim: String = ""

  def legendPentagonDamage: String    = ""
  def legendPentagonFacets: String    = ""
  def legendPentagonSteadyAim: String = ""

  def legendPentagonDebuffs: String = ""

  def legendPentagonMana: String = ""

  def legendSquareBigHit: String = ""

  def legendSquareRage: String = ""

  def legendSquareStacks: String = ""

  def legendSquareThreat: String = ""

  def legendTeam: String = ""

  def legendTriangleAbilities: String = ""

  def legendTriangleDamage: String = ""

  def legendTriangleEnergy: String = ""

  def legendTriangleThreat: String = ""

  def legendYouAndTarget: String = ""

  def loadingTheGame: String = ""

  def loadingTheGames: String = ""

  def loggedInAs(playerName: String): String = ""

  def loggingIn: String = ""

  def mainVolume: String = ""

  def markerCross: String = ""

  def markerLozenge: String = ""

  def markerMoon: String = ""

  def markerOnGround(marker: String): String = ""

  def markerOnTarget(marker: String): String = ""

  def markerSquare: String = ""

  def markerStar: String = ""

  def markerTriangle: String = ""

  def megabytes(received: Int, size: Int): String = ""

  def menu: String = ""

  def mouseButton(number: String): String = ""

  def mouseOver: String = ""

  def mouseOverHelp: String = ""

  def name: String = ""

  def needShieldStack: String = ""
  def needAllFacets: String   = ""

  def next(title: String): String = ""

  def nextTarget: String = ""

  def noAbilityOnKey(number: Int): String = ""

  def noBombToExplode: String = ""

  def noDeathMarkTriangle: String = ""

  def noGames: String = ""

  def noShadeToCall: String = ""

  def notAnAlly: String = ""

  def notAnEnemy: String = ""

  def noTarget: String = ""

  def notEnough(resource: gamelogic.entities.Resource): String = ""

  def notInRange: String = ""

  def notInSight: String = ""

  def notInThisGame: String = ""

  def notReady: String = ""

  def overview: String = ""

  def overviewRole: String = ""

  def overviewSections(controls: models.bff.ingame.Controls): Seq[services.i18n.TextSection] =
    Seq.empty

  def overviewSummary: String = ""

  def placeYourselves: String = ""

  def playerAlreadyExists: String = ""

  def playerClass: String = ""

  def playerClassName(playerClass: models.bff.outofgame.PlayerClasses): String = ""

  def players: String = ""

  def positionNotInSight: String = ""

  def practiceAgainst(bossName: String): String = ""

  def practiceGameName(playerName: String): String = ""

  def pressKeyFor(action: String): String = ""

  def pseudoError(message: String): String = ""

  def quit: String = ""

  def ready: String = ""

  def resetToDefaults: String = ""

  def resourceName(resource: gamelogic.entities.Resource): String = ""

  def resume: String = ""

  def screen: String = ""

  def serverAddress: String = ""

  def serverAddressExample: String = ""

  def serverError(message: String): String = ""

  def settingUpTheGame: String = ""

  def settings: String = ""

  def silenced: String = ""

  def startGame: String = ""

  def surrender: String = ""

  def surrenderEndsInDefeat: String = ""

  def targetAbilityCannotBeCut: String = ""

  def targetCannotBeStunned: String = ""

  def targetDoesNotCast: String = ""

  def targetNotThere: String = ""

  def theClass(playerClass: models.bff.outofgame.PlayerClasses): String = ""

  def thingsPreventingStart: String = ""

  def threat: String = ""

  def tutorialHint: String = ""

  def tutorialIntro: String = ""

  def twinsFarEnough: String = ""

  def victory: String = ""

  def volumeOff: String = ""

  def volumesHelp: String = ""

  def waitingForCreatorToLaunch: String = ""

  def waitingForGameToStart: String = ""

  def welcomePlayer(playerName: String): String = ""

  def welcomeToBattleForFlatland: String = ""

  def windowed: String = ""

  def you(playerName: String): String = ""

  def youAreDead: String = ""

  def yourPseudo: String = ""

  def story: services.i18n.TextSection = services.i18n.TextSection("", Seq.empty)

  def landingTagline: String = ""

  def landingPitch: String = ""

  def landingDownload: String = ""

  def downloadFor(system: String): String = ""

  def landingLoveFile: String = ""

  def landingFreeAndOpen: String = ""

  def landingUnsigned: String = ""

  def landingAnimation: String = ""

  def landingFeaturesTitle: String = ""

  def landingFeatures: Seq[(String, String)] = Seq.empty

  def landingClassesTitle: String = ""

  def landingScreenshotsTitle: String = ""

  def landingCaption(picture: services.i18n.LandingPicture): String = ""

  def landingStepsTitle: String = ""

  def landingSteps: Seq[String] = Seq.empty

  def landingBetaTitle: String = ""

  def landingBetaText: String = ""

  def landingFeedback: String = ""

  def landingSourceCode: String = ""

  def landingFooter: String = ""
}
