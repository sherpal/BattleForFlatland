package server

import services.crypto.Crypto

type BackendEnv = Crypto & services.menugames.MenuGames & services.localstorage.LocalStorage &
  services.events.Events
