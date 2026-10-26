package scalalove.linker

import org.scalajs.ir.Names.*
import org.scalajs.ir.Traversers.Traverser
import org.scalajs.ir.Trees.*
import org.scalajs.ir.Types.*
import org.scalajs.ir.WellKnownNames.*
import org.scalajs.linker.standard.{ModuleSet, SymbolRequirement}

/** Lua functions returning multiple values.
  *
  * A facade method declared with a Scala tuple as result type, e.g. `def getDimensions(): (Int,
  * Int) = js.native`, is compiled by Scala.js to a JS call followed by a cast to `scala.Tuple2`. We
  * emit that pattern as `R.tuple(C.scala_Tuple2, <ctor>, love.graphics.getDimensions())`, which
  * packs all the values returned by the Lua function into a tuple.
  */
object MultipleReturns {
  val MaxArity = 22

  def tupleClass(arity: Int): ClassName = ClassName(s"scala.Tuple$arity")

  def tupleConstructor(arity: Int): MethodName =
    MethodName.constructor(List.fill(arity)(ClassRef(ObjectClass)))

  /** The arity of `cls` if it is one of the `scala.TupleN` classes. */
  def tupleArity(cls: ClassName): Option[Int] = {
    val name = cls.nameString
    if (name.startsWith("scala.Tuple"))
      name.stripPrefix("scala.Tuple").toIntOption.filter(n => n >= 1 && n <= MaxArity)
    else None
  }

  /** The `TupleN` class of a multiple-return call, if `tree` is one. */
  def unapply(tree: AsInstanceOf): Option[(Tree, Int)] = tree match {
    case AsInstanceOf(call @ (_: JSMethodApply | _: JSFunctionApply), ClassType(cls, _, _)) =>
      tupleArity(cls).map(call -> _)
    case _ =>
      None
  }

  /** Arities of the multiple-return calls in the linked program. */
  def aritiesUsed(moduleSet: ModuleSet): Set[Int] = {
    var arities   = Set.empty[Int]
    val traverser = new Traverser {
      override def traverse(tree: Tree): Unit = {
        tree match {
          case cast: AsInstanceOf =>
            MultipleReturns.unapply(cast).foreach((_, arity) => arities += arity)
          case _ =>
        }
        super.traverse(tree)
      }
    }
    for {
      module <- moduleSet.modules
      c      <- module.classDefs
      m      <- c.methods
      body   <- m.body
    }
      traverser.traverse(body)
    arities
  }

  /** Requirements so that the tuple classes of these arities can be instantiated by the runtime. */
  def requirements(arities: Set[Int]): SymbolRequirement = {
    val factory = SymbolRequirement.factory("lua-multiple-returns")
    factory.multiple(
      arities.toList.map(n => factory.instantiateClass(tupleClass(n), tupleConstructor(n)))*
    )
  }
}
