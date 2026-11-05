package services.i18n

import gamelogic.abilities.boss.boss101.{BigDot, BigHit}
import gamelogic.buffs.abilities.classes.BraceShield
import gamelogic.entities.Resource
import gamelogic.entities.boss.Boss101
import gamelogic.entities.classes.{Hexagon, Pentagon, Square, Triangle}
import models.bff.ingame.Controls
import models.bff.outofgame.PlayerClasses

object FrenchI18n extends I18n {

  // --- Common ---

  override def playerClassName(playerClass: PlayerClasses): String = playerClass match {
    case PlayerClasses.Triangle => "Triangle"
    case PlayerClasses.Square => "Carré"
    case PlayerClasses.Pentagon => "Pentagone"
    case PlayerClasses.Hexagon => "Hexagone"
  }

  override def resourceName(resource: Resource): String = resource match {
    case Resource.Mana => "mana"
    case Resource.Energy => "énergie"
    case Resource.Rage => "rage"
    case Resource.NoResource => "rien"
  }

  override def mouseButton(number: String): String = s"bouton $number de la souris"

  override def formatNumber(x: Double): String = super.formatNumber(x).replace('.', ',')

  override def formatSeconds(millis: Long): String =
    s"${formatNumber(millis / 1000.0)} ${if millis >= 2000L then "secondes" else "seconde"}"

  override def settings: String    = "Paramètres"
  override def leaveGame: String   = "Quitter la partie"
  override def quit: String        = "Quitter"
  override def name: String        = "Nom"
  override def playerClass: String = "Classe"
  override def colour: String      = "Couleur"
  override def ready: String       = "Prêt"
  override def notReady: String    = "Pas prêt"
  override def boss: String        = "Boss"
  override def victory: String     = "Victoire !"
  override def defeat: String      = "Défaite..."
  override def startGame: String   = "Commencer la partie"

  override def castingTime(time: String): String = s"Incantation : $time"
  override def cooldown(time: String): String    = s"Recharge : $time"
  override def cost(amount: String): String      = s"Coût : $amount"

  // --- Menus ---

  override def fullscreenHint: String             = "F11 : plein écran"
  override def connectingToGameServer: String     = "Connexion au serveur de jeu"
  override def leavingTheGame: String             = "Sortie de la partie"
  override def connectionToGameServerLost: String = "La connexion au serveur de jeu a été perdue"
  override def cannotConnectToGameServer(error: String): String =
    s"Impossible de se connecter au serveur de jeu : $error"
  override def couldNotLeaveGame(error: String): String =
    s"Impossible de quitter la partie : $error"

  override def chooseYourLanguage: String = "Choisissez votre langue"

  // Login
  override def welcomeToBattleForFlatland: String = "Bienvenue dans Battle for Flatland !"
  override def serverAddress: String              = "Adresse du serveur"
  override def serverAddressExample: String   = "ex. bff.example.com, ou http://81.12.34.56:9000"
  override def choosePseudo: String           = "Veuillez choisir un pseudo"
  override def yourPseudo: String             = "Votre pseudo"
  override def loggingIn: String              = "Connexion"
  override def enterBattleForFlatland: String = "Entrer dans Battle for Flatland"
  override def serverError(message: String): String = s"Serveur : $message"
  override def pseudoError(message: String): String = s"Pseudo : $message"
  override def cannotLogIn(address: String, error: String): String =
    s"Impossible de se connecter à $address : $error"
  override def invalidPort(port: String): String = s"Port invalide : $port"
  override def invalidAddress(address: String): String =
    s"Adresse invalide : $address (attendu : hôte ou hôte:port)"

  // Games
  override def welcomePlayer(playerName: String): String   = s"Bienvenue, $playerName"
  override def learnToPlay: String                         = "Tutoriel"
  override def joinItBack: String                          = "Rejoindre la partie"
  override def gameNamePlaceholder: String                 = "Nom de la partie"
  override def defaultGameName(playerName: String): String = s"Partie de $playerName"
  override def createGame: String                          = "Créer une partie"
  override def createdBy: String                           = "Créée par"
  override def players: String                             = "Joueurs"
  override def loadingTheGames: String                     = "Chargement des parties"
  override def noGames: String  = "Il n'y a aucune partie pour le moment. Créez-en une !"
  override def join: String     = "Rejoindre"
  override def joinBack: String = "Revenir"
  override def currentlyInGame(gameName: String): String =
    s"Vous participez actuellement à la partie « $gameName »."

  // Lobby of a game
  override def loadingTheGame: String                 = "Chargement de la partie"
  override def loggedInAs(playerName: String): String = s"Connecté en tant que $playerName"
  override def gameCreatedBy(creator: String, gameId: String): String =
    s"Créée par $creator - partie $gameId"
  override def kick: String                    = "Exclure"
  override def you(playerName: String): String = s"$playerName (vous)"
  override def ai(playerName: String): String  = s"$playerName (IA)"
  override def classNotChosen: String          = "Non choisie"
  override def addAI: String                   = "Ajouter une IA"
  override def fillWithAIs: String             = "Compléter avec des IA"
  override def launchGame: String              = "Lancer la partie !"
  override def waitingForCreatorToLaunch: String =
    "En attente du lancement de la partie par son créateur"
  override def gameIsStarting: String =
    "La partie démarre, veuillez patienter pendant sa mise en place"
  override def gameServerNotReadyYet: String = "Le serveur de jeu n'est pas encore prêt."
  override def gameServerRunsOnYourComputer: String =
    "Le serveur de jeu tourne sur votre ordinateur"
  override def gameServerRunsOnComputerOf(host: String): String =
    s"Le serveur de jeu tourne sur l'ordinateur de $host"
  override def downloadingJavaRuntime: String = "Téléchargement de l'environnement Java"
  override def downloadingGameServer: String  = "Téléchargement du serveur de jeu"
  override def megabytes(received: Int, size: Int): String = s"$received / $size Mo"
  override def gameClosedByCreator: String   = "La partie a été fermée par son créateur"
  override def notInThisGame: String         = "Vous n'êtes pas (ou plus) dans cette partie"
  override def everyoneReady: String         = "Tout le monde est prêt et en place, c'est parti !"
  override def thingsPreventingStart: String = "Ce qui empêche la partie de commencer :"

  // Docs of the bosses and classes, in the lobby
  override def classes: String = "Classes"
  override def bossFacts(life: Int, intendedFor: Int): String =
    s"$life points de vie, pour $intendedFor joueur(s)"
  override def classFacts(
      life: Int,
      resourceAmount: Int,
      resource: Resource,
      isYours: Boolean
  ): String =
    s"$life points de vie, $resourceAmount ${resourceName(resource)}" +
      (if isYours then " (votre classe)" else "")
  override def abilities: String = "Capacités"

  // Settings
  override def controls: String                       = "Commandes"
  override def general: String                        = "Général"
  override def goUp: String                           = "Aller en haut"
  override def goDown: String                         = "Aller en bas"
  override def goLeft: String                         = "Aller à gauche"
  override def goRight: String                        = "Aller à droite"
  override def nextTarget: String                     = "Cible suivante"
  override def faceTheTarget: String                  = "Faire face à la cible"
  override def abilityNumber(number: Int): String     = s"Capacité $number"
  override def markerCross: String                    = "Croix"
  override def markerLozenge: String                  = "Losange"
  override def markerMoon: String                     = "Lune"
  override def markerSquare: String                   = "Carré"
  override def markerStar: String                     = "Étoile"
  override def markerTriangle: String                 = "Triangle"
  override def markerOnTarget(marker: String): String = s"$marker sur la cible"
  override def markerOnGround(marker: String): String = s"$marker au sol"
  override def bindingsHelp: String =
    "Cliquez sur un raccourci, puis appuyez sur une touche (éventuellement avec Maj) ou un bouton de la souris. Les changements s'appliquent à la partie suivante."
  override def mouseOver: String = "Ciblage au survol"
  override def mouseOverHelp: String =
    "Les capacités se rabattent sur le cadre sous la souris quand la cible ne leur convient pas."
  override def resetToDefaults: String = "Réinitialiser"
  override def escapeToCancel: String  = "Échap pour annuler"
  override def pressKeyFor(action: String): String =
    s"Appuyez sur une touche ou un bouton de la souris pour « $action »"
  override def boundToSeveralActions(key: String): String = s"Plusieurs actions utilisent $key"
  override def language: String                           = "Langue"
  override def languageHelp: String                       = "La langue des menus et des parties."
  override def hudScale: String                           = "Taille de l'interface"
  override def hudScaleHelp: String =
    "La taille des cadres et de la barre d'action, quelle que soit la taille de la fenêtre. S'applique à la partie suivante."
  override def hostGameServers: String = "Héberger les serveurs de jeu"
  override def hostGameServersHelp: String =
    "Le serveur de jeu d'une partie tourne sur l'ordinateur de l'un de ses joueurs. Pour être prêt " +
      "à le faire tourner, le client le télécharge, avec de quoi l'exécuter (plusieurs dizaines de " +
      "Mo, une seule fois), dès que vous rejoignez une partie. Désactivez cette option pour ne rien " +
      "télécharger, par ex. sur un réseau mobile : les autres joueurs le feront tourner."
  override def mainVolume: String      = "Volume principal"
  override def abilitiesVolume: String = "Capacités"
  override def volumeOff: String       = "Coupé"
  override def volumesHelp: String =
    "De coupé à 10. Le volume de chaque type de son est relatif au volume principal."
  override def done: String = "Terminé"

  // --- In game ---

  override def placeYourselves: String =
    "Placez-vous, puis cliquez sur « Commencer la partie » : le boss arrive 5 secondes plus tard"
  override def bossArrivesIn: String          = "Le boss arrive dans"
  override def clickToPlaceZone: String       = "Cliquez pour placer la zone (Échap pour annuler)"
  override def waitingForGameToStart: String  = "En attente du début de la partie..."
  override def bossAbilities: String          = "Capacités du boss"
  override def threat: String                 = "Menace"
  override def damage: String                 = "Dégâts"
  override def damageTo(name: String): String = s"Dégâts : $name"
  override def dead: String                   = "Mort"

  // Casting
  override def youAreDead: String                  = "Vous êtes mort"
  override def noTarget: String                    = "Aucune cible"
  override def noAbilityOnKey(number: Int): String = s"Aucune capacité sur la touche $number"
  override def abilityNotSupported(ability: String): String =
    s"La capacité $ability n'est pas encore prise en charge"
  override def abilityNotPlacedWithMouse(ability: String): String =
    s"La capacité $ability ne se place pas avec la souris"

  // Connection to the game server
  override def gameServerDoesNotAnswer: String = "le serveur de jeu ne répond pas"
  override def cannotSynchronizeClock: String =
    "impossible de synchroniser l'horloge avec le serveur de jeu"
  override def gameServerNotReady: String = "le serveur de jeu n'est pas prêt"
  override def gameServerRejectsCredentials: String =
    "le serveur de jeu n'accepte pas nos identifiants"
  override def gameServerDoesNotKnowUs: String          = "le serveur de jeu ne nous connaît pas"
  override def gameCouldNotStart(error: String): String = s"la partie n'a pas pu démarrer ($error)"

  // Game menu
  override def menu: String                  = "Menu"
  override def resume: String                = "Reprendre"
  override def surrender: String             = "Abandonner"
  override def clickAgainToSurrender: String = "Cliquez à nouveau pour abandonner"
  override def surrenderEndsInDefeat: String =
    "Le combat se termine par une défaite, pour tout le monde"
  override def gameGoesOnMeanwhile: String = "La partie continue pendant ce temps"
  override def windowed: String            = "Fenêtré (F11)"
  override def fullscreen: String          = "Plein écran (F11)"

  // End of the game
  override def backToMenus: String    = "Retour aux menus"
  override def connectionLost: String = "Connexion perdue"
  override def fightLasted(minutes: Int, seconds: Int): String =
    f"Le combat a duré $minutes%d:$seconds%02d"
  override def gameServerClosedConnection: String = "Le serveur de jeu a fermé la connexion"

  // --- Tutorial ---

  override def tutorialIntro: String =
    "Battle for Flatland en quelques minutes : le fonctionnement du jeu, puis chaque classe, avec " ++
      "un combat d'entraînement contre Boss 101 aux côtés de bots."
  override def tutorialHint: String =
    "Nouveau venu ? Commencez par la présentation, puis lisez la page de la classe que vous " ++
      "aimeriez jouer."
  override def backToTheGames: String                    = "Retour aux parties"
  override def backToTheTutorial: String                 = "Retour au tutoriel"
  override def next(title: String): String               = s"Suivant : $title"
  override def practiceAgainst(bossName: String): String = s"S'entraîner contre $bossName"
  override def settingUpTheGame: String                  = "Préparation de la partie..."
  override def alreadyInAGame: String =
    "Vous êtes déjà dans une partie : quittez-la d'abord, depuis la liste des parties."
  override def practiceGameName(playerName: String): String = s"Entraînement de $playerName"

  override def overview: String     = "Présentation"
  override def overviewRole: String = "Le fonctionnement du jeu"
  override def overviewSummary: String =
    "Une équipe de formes contre un boss : qui fait quoi, comment lire l'écran, et comment se " ++
      "déroule un combat."

  override def theClass(playerClass: PlayerClasses): String = playerClass match {
    case PlayerClasses.Hexagon => s"L'${playerClassName(playerClass)}"
    case _ => s"Le ${playerClassName(playerClass)}"
  }

  override def classRole(playerClass: PlayerClasses): String = playerClass match {
    case PlayerClasses.Triangle => "Dégâts en mêlée"
    case PlayerClasses.Square => "Tank"
    case PlayerClasses.Pentagon => "Dégâts à distance"
    case PlayerClasses.Hexagon => "Soigneur"
  }

  override def classSummary(playerClass: PlayerClasses): String = playerClass match {
    case PlayerClasses.Triangle =>
      "Combat au contact de ses ennemis, dépense son énergie en gros coups, et contrôle les adds."
    case PlayerClasses.Square =>
      "Retient l'attention du boss et encaisse ses coups, derrière un bouclier qu'il ne cesse de " ++
        "renforcer."
    case PlayerClasses.Pentagon =>
      "Tire sur ses ennemis à distance, vise avec ses projectiles et ses zones, et dissipe les " ++
        "affaiblissements gênants."
    case PlayerClasses.Hexagon =>
      "Garde tout le monde en vie, avec une grande réserve de mana à gérer, qui se régénère " ++
        "lentement."
  }

  override def overviewSections(controls: Controls): Seq[TextSection] = {
    val abilityKeys = controls.abilityKeys.take(4).map(key).mkString(", ")
    Seq(
      TextSection(
        "Bienvenue à Flatland",
        Seq(
          "Dans Battle for Flatland, une équipe de joueurs, chacun une forme géométrique, affronte " ++
            "un boss ensemble. Le combat est gagné quand le boss meurt, et perdu quand tous les " ++
            "joueurs sont morts : tout le monde gagne ou perd ensemble.",
          "Les boss sont bien plus forts que n'importe quel joueur. Ce qui les bat, c'est une " ++
            "équipe où chacun joue son rôle."
        )
      ),
      TextSection(
        "Tank, soigneur, dégâts",
        Seq(
          "Chaque classe a un rôle, et chaque rôle compte sur les autres :",
          "- Le tank (le Carré) retient l'attention du boss. C'est le seul à survivre aux coups " ++
            "du boss : le boss doit donc frapper le tank, et personne d'autre.",
          "- Le soigneur (l'Hexagone) garde l'équipe en vie, le tank en premier, en soignant les " ++
            "dégâts qu'elle subit.",
          "- Les attaquants (le Triangle en mêlée, le Pentagone à distance) abattent le boss, " ++
            "avant que le soigneur ne soit à court de mana.",
          "Si le tank meurt, le boss se tourne vers des joueurs qui ne peuvent pas encaisser ses " ++
            "coups. Si le soigneur meurt, les dégâts s'accumulent. Si les dégâts sont trop " ++
            "faibles, le combat dure jusqu'à ce que le soigneur n'ait plus de mana."
        )
      ),
      TextSection(
        "Menace",
        Seq(
          "Le boss attaque le joueur qu'il déteste le plus. Chaque point de dégâts qui lui est " ++
            "infligé génère de la menace envers son auteur. Les capacités du tank en génèrent " ++
            "beaucoup, pour que le tank reste en tête du compteur de menace (en bas à droite de " ++
            "l'écran).",
          "Attaquants : surveillez-le, et ne passez pas devant le tank !"
        )
      ),
      TextSection(
        "Commandes",
        Seq(
          s"- Déplacez-vous avec ${key(controls.upKey)}, ${key(controls.leftKey)}, " ++
            s"${key(controls.downKey)} et ${key(controls.rightKey)}.",
          s"- Lancez vos capacités avec $abilityKeys, ... dans l'ordre de votre barre d'action, en " ++
            "bas de l'écran. Survolez une capacité pour lire ce qu'elle fait.",
          "- Cliquez sur un joueur ou sur le boss (dans l'arène, ou sur son cadre) pour le cibler. " ++
            s"${key(controls.nextTargetKey)} cible l'ennemi le plus proche, et " ++
            s"${key(controls.targetLockInToggleKey)} vous fait faire face à votre cible.",
          "- Certaines capacités créent une zone : lancez-les, puis cliquez là où la placer " ++
            "(Échap annule).",
          "- Échap ouvre le menu du jeu. Toutes les touches peuvent être modifiées dans les " ++
            "Paramètres."
        )
      ),
      TextSection(
        "Lire l'écran",
        Seq(
          "L'onglet « Écran » montre ce que vous voyez pendant un combat :",
          "- 1. Le boss, avec ce qu'il lance.",
          "- 2. Quand les capacités du boss seront de nouveau prêtes.",
          "- 3. Votre équipe, estompée hors de portée de vos soins. Cliquez sur un cadre pour " ++
            "cibler le joueur.",
          "- 4. Vous, votre cible (avec ce qu'elle lance), et vos capacités en dessous.",
          "- 5. Qui a infligé le plus de dégâts à votre cible, et qui le boss déteste le plus."
        )
      ),
      TextSection(
        "Déroulement d'une partie",
        Seq(
          "Créez une partie (ou rejoignez-en une), choisissez votre classe et votre couleur, et " ++
            "préparez-vous. Le créateur de la partie choisit le boss, peut compléter l'équipe " ++
            "avec des IA, et lance la partie.",
          "Dans l'arène, chacun se place, puis quelqu'un clique sur « Commencer la partie » : le " ++
            "boss arrive 5 secondes plus tard. Le salon décrit chaque boss et ses capacités.",
          s"${Boss101.name} est le tout premier boss, et le meilleur pour apprendre : son Big Hit " ++
            s"(${formatNumber(BigHit.damageAmount)} dégâts) tue n'importe qui sauf le tank, et son " ++
            s"Big Dot inflige ${formatNumber(BigDot.damageOnTick)} dégâts toutes les " ++
            s"${formatPeriod(BigDot.tickRate)} à quelqu'un d'autre, que le soigneur doit soigner."
        )
      ),
      TextSection(
        "Et ensuite",
        Seq(
          "Lisez la page de la classe que vous voulez jouer, puis entraînez-vous à la jouer " ++
            "contre Boss 101, avec des bots."
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
    "Entraînement",
    Seq(
      s"Prêt à essayer ? « S'entraîner contre Boss 101 » prépare une partie où vous jouez la " ++
        s"classe ${playerClassName(playerClass)}, aux côtés de bots qui prennent les autres " ++
        "rôles. Une fois dans l'arène, placez-vous, puis cliquez sur « Commencer la partie » : le " ++
        "boss arrive 5 secondes plus tard. Quand le combat se termine, vous revenez ici."
    )
  )

  private def triangle = Seq(
    TextSection(
      "Philosophie",
      Seq(
        "Le Triangle, c'est la pression et la précision. Il vit au contact de ses ennemis, à " ++
          "portée de mêlée, et transforme son énergie en dégâts : là où le Pentagone doit viser, " ++
          "le Triangle frappe ce qu'il cible.",
        "C'est aussi le contrôle de l'équipe : quand des adds apparaissent, il les étourdit, et " ++
          "interrompt les sorts qu'ils lancent."
      )
    ),
    TextSection(
      "Énergie",
      Seq(
        s"Votre énergie (${formatNumber(Triangle.initialResourceAmount.amount)}) se recharge à " ++
          "un rythme régulier. Direct Hit est votre principale source de dégâts, mais elle coûte " ++
          "cher. Energy Kick rend de l'énergie : utilisez-la en attendant. De l'énergie au " ++
          "maximum, c'est de l'énergie gaspillée.",
        "- Maintenez Upgrade Direct Hit : elle renforce Direct Hit pendant un moment.",
        "- Direct Hit dès que vous en avez l'énergie.",
        "- Energy Kick quand elle est prête et que vous manquez d'énergie."
      )
    ),
    TextSection(
      "Rester en vie",
      Seq(
        s"Avec ${formatNumber(Triangle.initialMaxLife)} points de vie, vous ne pouvez pas " ++
          "encaisser les gros coups du boss. Vos dégâts génèrent vite de la menace : gardez un " ++
          "œil sur le compteur de menace, et laissez le Carré en tête. Restez à portée du " ++
          "soigneur."
      )
    ),
    TextSection(
      "Contrôle",
      Seq(
        "Stun immobilise un ennemi (mais pas un boss) pendant quelques secondes, en interrompant " ++
          "son sort, jusqu'à ce qu'il subisse des dégâts. Cut interrompt un sort et réduit son " ++
          "lanceur au silence. Gardez-les pour les sorts qui comptent."
      )
    )
  )

  private def square = Seq(
    TextSection(
      "Philosophie",
      Seq(
        "Le Carré est le mur de l'équipe. Son travail n'est pas d'infliger des dégâts, mais " ++
          s"d'être celui que le boss frappe : il a ${formatNumber(Square.initialMaxLife)} points " ++
          "de vie, deux fois plus que les autres, un bouclier qu'il ne cesse de renforcer, et des " ++
          "capacités qui génèrent beaucoup de menace.",
        "Un bon tank est le calme dans la tempête : tant qu'il retient le boss, tous les autres " ++
          "peuvent faire leur travail."
      )
    ),
    TextSection(
      "Rage",
      Seq(
        "Votre rage vient des dégâts que vous subissez (et un peu chaque seconde). Être frappé, " ++
          "c'est ce qui vous alimente ! Quand le boss ne vous frappe pas, Enrage vous blesse " ++
          "vous-même pour accumuler de la rage."
      )
    ),
    TextSection(
      "Brace",
      Seq(
        s"Brace ajoute une charge à votre bouclier, jusqu'à ${BraceShield.maxStacks}. Chaque " ++
          s"charge réduit les dégâts que vous subissez de ${formatNumber((1 - BraceShield.damageMultiplier) * 100)} %. " ++
          s"Le bouclier perd une charge après ${formatSeconds(BraceShield.duration)}, à moins que " ++
          "vous ne relanciez Brace. La jauge à gauche de votre cadre montre vos charges : " ++
          "gardez-la pleine.",
        "Shield Slam consomme une charge pour un coup puissant, avec de la menace en plus."
      )
    ),
    TextSection(
      "Menace",
      Seq(
        "Lancez Taunt sur le boss dès qu'il arrive, puis continuez à le frapper (Hammer Hit, " ++
          "Shield Slam, Cleave) pour rester en tête du compteur de menace. Si quelqu'un passe " ++
          "devant vous, relancez Taunt.",
        "Guard protège un allié pendant un moment : une partie des dégâts qu'il subit vous est " ++
          "transférée, par ex. quand il porte un affaiblissement dangereux."
      )
    ),
    TextSection(
      "Contre Boss 101",
      Seq(
        s"Son Big Hit inflige ${formatNumber(BigHit.damageAmount)} dégâts, après une incantation " ++
          s"de ${formatSeconds(BigHit.castingTime)} : surveillez la barre d'incantation du boss, " ++
          "et ayez vos charges de Brace en place quand il frappe."
      )
    )
  )

  private def pentagon = Seq(
    TextSection(
      "Philosophie",
      Seq(
        "Le Pentagone est l'artillerie de l'équipe : il inflige ses dégâts à distance, hors de " ++
          "portée du boss. Sa puissance demande de l'adresse : ses projectiles vont là où vous " ++
          "visez, et ses zones doivent être placées.",
        "C'est aussi le seul capable de dissiper les affaiblissements gênants que certains boss " ++
          "infligent aux joueurs."
      )
    ),
    TextSection(
      "Viser",
      Seq(
        "- Pentagon Bullet vole vers votre souris (pas vers votre cible), et touche le premier " ++
          "ennemi sur son chemin : gardez une ligne dégagée vers le boss.",
        "- Pentagon Zone : lancez-la, puis cliquez là où la placer. Les ennemis qui s'y trouvent " ++
          "subissent des dégâts sur la durée : placez-la sous le boss, que le tank maintient en " ++
          "place."
      )
    ),
    TextSection(
      "Mana",
      Seq(
        s"Vos capacités coûtent du mana (${formatNumber(Pentagon.initialResourceAmount.amount)}). " ++
          "Continuez à tirer, mais gardez-en pour quand une dissipation est nécessaire."
      )
    ),
    TextSection(
      "Dissipation",
      Seq(
        "Penta Dispel retire d'un allié tous les effets qui peuvent être dissipés, y compris les " ++
          "bons (comme le soin sur la durée du soigneur !). Les boss suivants infligent des " ++
          "affaiblissements qu'il faut dissiper rapidement. Le Big Dot de Boss 101 ne peut pas " ++
          "l'être : le soigneur le compense par ses soins."
      )
    ),
    TextSection(
      "Rester en vie",
      Seq(
        s"Avec ${formatNumber(Pentagon.initialMaxLife)} points de vie, ne laissez pas le boss " ++
          "se retourner contre vous : surveillez le compteur de menace. Restez loin du boss, mais " ++
          "à portée du soigneur."
      )
    )
  )

  private def hexagon = Seq(
    TextSection(
      "Philosophie",
      Seq(
        "L'Hexagone maintient l'équipe debout. Il surveille les cadres de l'équipe plus que " ++
          "l'arène : qui perd de la vie, qui est sur le point de subir un gros coup.",
        s"Sa réserve de mana est grande (${formatNumber(Hexagon.initialResourceAmount.amount)}) " ++
          "mais se régénère lentement : tout l'art du soigneur est de lancer le bon soin sur le " ++
          "bon joueur, au bon moment."
      )
    ),
    TextSection(
      "Vos soins",
      Seq(
        "- Flash Heal soigne beaucoup, mais prend du temps à lancer : vous devez rester immobile.",
        "- Heal Over Time est peu coûteuse et instantanée, et peut être lancée en mouvement : " ++
          "placez-la sur les joueurs qui subissent des dégâts en continu (le tank, le porteur du " ++
          "Big Dot).",
        "- Hexagon Zone soigne tous ceux qui s'y trouvent : pour quand l'équipe est groupée."
      )
    ),
    TextSection(
      "Ciblage",
      Seq(
        "Cliquez sur un cadre à gauche de l'écran pour cibler un joueur. Un cadre estompé est " ++
          "celui d'un joueur hors de portée de vos soins : rapprochez-vous.",
        "Avec « Ciblage au survol » activé dans les Paramètres, vos soins vont au joueur dont le " ++
          "cadre est sous la souris, sans changer votre cible."
      )
    ),
    TextSection(
      "Contre Boss 101",
      Seq(
        "Le tank encaisse les coups du boss : gardez-le en bonne santé, surtout avant un Big " ++
          s"Hit. Big Dot inflige ${formatNumber(BigDot.damageOnTick)} dégâts toutes les " ++
          s"${formatPeriod(BigDot.tickRate)} pendant ${formatSeconds(BigDot.duration)} à " ++
          "quelqu'un d'autre : un soin sur la durée sur cette personne en couvre l'essentiel. Ne " ++
          "soignez pas les joueurs qui ont toute leur vie : c'est du mana gaspillé."
      )
    )
  )

  // Previews of the pages
  override def arena: String     = "Arène"
  override def screen: String    = "Écran"
  override def hoverHint: String = "Survolez les capacités, cliquez sur les cadres"

  override def legendBoss: String          = "Le boss : sa vie, et ce qu'il lance."
  override def legendBossCooldowns: String = "Quand les capacités du boss seront de nouveau prêtes."
  override def legendTeam: String = "Votre équipe. Cliquez sur un cadre pour cibler le joueur."
  override def legendYouAndTarget: String = "Vous et votre cible, au-dessus de vos capacités."
  override def legendMeters: String = "Les dégâts infligés à votre cible, et la menace du boss."
  override def legendTriangleEnergy: String =
    "Votre énergie : dépensez-la, elle se recharge en continu."
  override def legendTriangleAbilities: String = "Vos capacités : Direct Hit, Energy Kick, ..."
  override def legendTriangleDamage: String    = "Votre place au compteur de dégâts : la première !"
  override def legendTriangleThreat: String =
    "Mais pas au compteur de menace : celle-là revient au Carré."
  override def legendSquareStacks: String = "Les charges de votre bouclier (Brace) : maintenez-les."
  override def legendSquareRage: String   = "Votre rage, gagnée en subissant des dégâts."
  override def legendSquareBigHit: String =
    "Le boss lance Big Hit sur vous : ayez vos charges en place !"
  override def legendSquareThreat: String = "Vous devez rester en tête du compteur de menace."
  override def legendPentagonMana: String = "Votre mana."
  override def legendPentagonAim: String =
    "Les projectiles volent vers la souris, les zones se placent d'un clic."
  override def legendPentagonDebuffs: String =
    "Les affaiblissements des joueurs : dissipez ceux qui peuvent l'être."
  override def legendPentagonDamage: String = "Votre place au compteur de dégâts."
  override def legendHexagonTeam: String =
    "Votre équipe : cliquez sur un cadre pour cibler le joueur."
  override def legendHexagonFaded: String =
    "Estompé : hors de portée de vos soins. Rapprochez-vous !"
  override def legendHexagonMana: String = "Votre mana : il se régénère lentement."
  override def legendHexagonTarget: String =
    "Votre cible, le tank, avec votre soin sur la durée."

  override def arenaHealerRange: String = "La portée du soigneur"
  override def arenaBoss: String        = "Le boss"
  override def arenaSquare: String      = "Le Carré retient le boss, face à lui"
  override def arenaTriangle: String    = "Le Triangle le frappe en mêlée, par derrière"
  override def arenaPentagons: String   = "Les Pentagones tirent à distance"
  override def arenaHexagon: String     = "L'Hexagone soigne, à portée de tous"
  override def arenaLegendPositions: String =
    "Le tank détourne le boss des autres, qui le frappent par derrière ou à distance."
  override def arenaLegendRange: String =
    "Tout le monde reste à portée du soigneur (les cadres de l'équipe s'estompent en dehors)."
}
