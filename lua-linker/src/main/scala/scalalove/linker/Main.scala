package scalalove.linker

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration
import ExecutionContext.Implicits.global

import org.scalajs.ir.Names.*
import org.scalajs.ir.Types.*
import org.scalajs.ir.WellKnownNames.*
import org.scalajs.linker.*
import org.scalajs.linker.interface.*
import org.scalajs.linker.standard.*
import org.scalajs.logging.*

object LuaBackend {
  val JavaScriptExceptionClass: ClassName  = ClassName("scala.scalajs.js.JavaScriptException")
  val AnyArgConstructorName: MethodName    = MethodName.constructor(List(ClassRef(ObjectClass)))
  val StringArgConstructorName: MethodName =
    MethodName.constructor(List(ClassRef(BoxedStringClass)))
  val toStringMethodName: MethodName = MethodName("toString", Nil, ClassRef(BoxedStringClass))
  val hashCodeMethodName: MethodName = MethodName("hashCode", Nil, IntRef)

  /** Symbols the generated code (and runtime) uses directly. */
  val symbolRequirements: SymbolRequirement = {
    val factory = SymbolRequirement.factory("lua-backend")
    import factory.*
    multiple(
      instantiateClass(JavaScriptExceptionClass, AnyArgConstructorName),
      instantiateClass(ArithmeticExceptionClass, StringArgConstructorName),
      callMethod(BoxedDoubleClass, hashCodeMethodName),
      callMethod(BoxedStringClass, hashCodeMethodName),
      classData(ClassClass)
    )
  }

  /** Semantics: no checks (like Scala.js' fullLink), they would need extra runtime support. */
  val semantics: Semantics = Semantics.Defaults
    .withAsInstanceOfs(CheckedBehavior.Unchecked)
    .withArrayIndexOutOfBounds(CheckedBehavior.Unchecked)
    .withArrayStores(CheckedBehavior.Unchecked)
    .withNegativeArraySizes(CheckedBehavior.Unchecked)
    .withNullPointers(CheckedBehavior.Unchecked)
    .withStringIndexOutOfBounds(CheckedBehavior.Unchecked)
    .withModuleInit(CheckedBehavior.Unchecked)

  /** Targeting ES 5.1 makes the Scala.js library use its pure Scala fallbacks instead of newer JS
    * built-ins (e.g. heap `ByteBuffer`s instead of typed arrays).
    */
  @annotation.nowarn(
    "cat=deprecation"
  ) // ES 5.1 is deprecated as a JS target, which does not matter for Lua
  val esFeatures: ESFeatures = ESFeatures.Defaults.withESVersion(ESVersion.ES5_1)

  def runtime: String = {
    val in = getClass.getResourceAsStream("/scalalove/runtime.lua")
    try new String(in.readAllBytes(), StandardCharsets.UTF_8)
    finally in.close()
  }
}

/** Usage: Main <main class> <output dir> <classpath> */
object Main {
  def main(args: Array[String]): Unit = {
    val Array(mainClass, outDir, cp) = args
    val classpath = cp.split(java.io.File.pathSeparator).toSeq.filter(_.nonEmpty).map(Paths.get(_))

    val config = StandardConfig()
      .withSemantics(LuaBackend.semantics)
      .withESFeatures(LuaBackend.esFeatures)
      .withModuleKind(ModuleKind.CommonJSModule)
      .withOptimizer(false)
      .withCheckIR(true)
    val frontend = StandardLinkerFrontend(config)
    val logger   = new ScalaConsoleLogger(Level.Warn)

    val initializers = Seq(ModuleInitializer.mainMethodWithArgs(mainClass, "main"))
    val irFiles      = Await.result(
      PathIRContainer
        .fromClasspath(classpath)
        .flatMap((containers, _) => StandardImpl.irFileCache().newCache.cached(containers)),
      Duration.Inf
    )

    def link(extra: SymbolRequirement): ModuleSet =
      Await.result(
        frontend.link(irFiles, initializers, LuaBackend.symbolRequirements ++ extra, logger),
        Duration.Inf
      )

    // Multiple-return calls need the corresponding TupleN classes to be instantiable.
    // We only know whether there are such calls after linking, so link again if necessary.
    val firstModuleSet = link(SymbolRequirement.factory("lua-backend").none())
    val arities        = MultipleReturns.aritiesUsed(firstModuleSet)
    val tuplesReady    = arities.forall { n =>
      firstModuleSet.modules.flatMap(_.classDefs).exists { c =>
        c.className == MultipleReturns.tupleClass(n) && c.hasDirectInstances &&
        c.methods.exists(_.methodName == MultipleReturns.tupleConstructor(n))
      }
    }
    val moduleSet =
      if (tuplesReady) firstModuleSet
      else link(MultipleReturns.requirements(arities))

    val lua = new LuaEmitter(moduleSet).emit(LuaBackend.runtime, msg => System.err.println(msg))

    val outPath = Paths.get(outDir)
    Files.createDirectories(outPath)
    val mainLua = outPath.resolve("main.lua")
    Files.write(mainLua, lua.getBytes(StandardCharsets.UTF_8))
    val classCount = moduleSet.modules.map(_.classDefs.size).sum
    println(s"Wrote $mainLua ($classCount classes, ${lua.linesIterator.size} lines)")
  }
}
