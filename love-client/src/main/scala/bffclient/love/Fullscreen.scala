package bffclient.love

/** Fullscreen (at the desktop's resolution) or windowed, toggled with F11 (in the menus and in the
  * game) and from the game menu.
  */
object Fullscreen {
  val key = "f11"

  def isOn: Boolean = Window.getFullscreen().fullscreen

  def toggle(): Unit = Window.setFullscreen(!isOn, "desktop")
}
