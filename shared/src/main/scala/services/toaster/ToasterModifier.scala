package services.toaster

trait ToasterModifier {

  /** Describes how to modify the given options. */
  def apply(options: ToastOptions): ToastOptions

}

object ToasterModifier {

  /** Creates a [[ToasterModifier]] always creating the given [[ToastOptions]]. */
  def constant(toastOptions: ToastOptions): ToasterModifier = (_: ToastOptions) => toastOptions

  /**
    * Creates a [[ToastOptions]] by applying all the [[ToasterModifier]] in the list.
    *
    * Note: if multiple modifiers affect the same option member, the further away in the list wins.
    *
    * @param modifiers all modifiers to apply, from left to right
    * @return the folded [[ToastOptions]], starting from the empty one.
    */
  def seqBuild(modifiers: Iterable[ToasterModifier]): ToastOptions =
    modifiers.foldLeft(ToastOptions())((options, modifier) => modifier(options))

}
