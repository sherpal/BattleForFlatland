package services

type FrontendEnv = services.http.HttpClient & services.routing.Routing &
  services.errorreporting.ErrorReporting & services.menugames.MenuGames &
  services.localstorage.LocalStorage & services.logging.Logging
