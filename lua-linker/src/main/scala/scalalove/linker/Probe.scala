package scalalove.linker

import java.nio.file.Paths

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration
import ExecutionContext.Implicits.global

import org.scalajs.ir.Trees.*
import org.scalajs.ir.Traversers.Traverser
import org.scalajs.linker.*
import org.scalajs.linker.interface.*
import org.scalajs.linker.standard.*
import org.scalajs.logging.*

/** Debugging tool: prints the linked classes and IR node statistics. Set DUMP=1 to dump methods
  * using JS interop, DUMP_CLASS=a.B,c.D to dump classes.
  */
object Probe {
  def main(args: Array[String]): Unit = {
    val Array(mainClass, cp) = args
    val classpath            = cp.split(java.io.File.pathSeparator).toSeq.map(Paths.get(_))
    val config               = StandardConfig()
      .withSemantics(LuaBackend.semantics)
      .withESFeatures(LuaBackend.esFeatures)
      .withModuleKind(ModuleKind.CommonJSModule)
      .withOptimizer(false)
      .withCheckIR(true)
    val frontend = StandardLinkerFrontend(config)
    val logger   = new ScalaConsoleLogger(Level.Warn)
    val f        = for {
      (containers, _) <- PathIRContainer.fromClasspath(classpath)
      irFiles         <- StandardImpl.irFileCache().newCache.cached(containers)
      ms              <- frontend.link(
        irFiles,
        Seq(ModuleInitializer.mainMethodWithArgs(mainClass, "main")),
        LuaBackend.symbolRequirements,
        logger
      )
    } yield ms
    val ms      = Await.result(f, Duration.Inf)
    val counts  = scala.collection.mutable.Map.empty[String, Int].withDefaultValue(0)
    val classes = ms.modules.flatMap(_.classDefs)
    val t       = new Traverser {
      override def traverse(tree: Tree): Unit = {
        counts(tree.getClass.getSimpleName) += 1
        super.traverse(tree)
      }
    }
    for {
      c <- classes
      m <- c.methods
    } m.body.foreach(t.traverse)
    println(s"classes: ${classes.size}, methods: ${classes.map(_.methods.size).sum}")
    println(classes.map(c => s"${c.className.nameString}(${c.kind})").sorted.mkString(" "))
    val jsNodes = Set(
      "JSMethodApply",
      "JSSelect",
      "JSGlobalRef",
      "JSNew",
      "JSObjectConstr",
      "JSBinaryOp",
      "Closure",
      "JSFunctionApply",
      "Undefined",
      "TryCatch",
      "TryFinally",
      "LongLiteral",
      "Match",
      "NewArray",
      "ArrayValue",
      "IsInstanceOf"
    )
    for {
      c <- classes
      m <- c.methods
      b <- m.body
    } {
      var found = false
      new Traverser {
        override def traverse(t: Tree): Unit = {
          if (jsNodes(t.getClass.getSimpleName)) found = true; super.traverse(t)
        }
      }.traverse(b)
      if (found && sys.env.contains("DUMP"))
        println(s"==== ${c.className.nameString}.${m.methodName.nameString}\n" + b.show)
    }
    for {
      name <- sys.env.get("DUMP_CLASS").toList.flatMap(_.split(","))
      c    <- classes if c.className.nameString == name
    } {
      println(s"#### ${c.className.nameString} fields=${c.fields} super=${c.superClass}")
      for (m <- c.methods)
        println(
          s"== ${m.flags.namespace} ${m.methodName.nameString}\n" + m.body.fold("<abstract>")(
            _.show
          )
        )
    }
    println(counts.toList.sortBy(-_._2).map((k, v) => s"$k=$v").mkString(" "))
  }
}
