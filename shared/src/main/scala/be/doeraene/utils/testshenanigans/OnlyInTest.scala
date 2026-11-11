package be.doeraene.utils.testshenanigans

sealed trait OnlyInTest

private[testshenanigans] object OnlyInTest {
  val onlyInTest: OnlyInTest = new OnlyInTest {}
}
