package scalalove.linker

import scala.collection.mutable

import org.scalajs.ir.Names.*
import org.scalajs.ir.Trees.*
import org.scalajs.ir.Types.*
import org.scalajs.ir.WellKnownNames.*

/** Translates the body of one IR method (or top-level function) to Lua.
  *
  * Lua limits a function to 200 local variables. When `spill` is true, the local variables (but not
  * the parameters) are stored in a table `L` instead, which is used for the (rare) huge methods,
  * typically from inlined derivations.
  *
  * The translation is a classic "unnesting" one: `expr` returns a Lua expression for a tree,
  * possibly emitting statements first (into temporary locals) when the tree contains statement-only
  * constructs (blocks, ifs, labeled blocks, try/catch, ...). `emitInto` pushes the final value of a
  * tree into a [[Target]] (discard it, return it, or assign it to a variable), which avoids most
  * temporaries.
  */
final class FunctionEmitter(g: GlobalContext, enclosingClass: ClassName, spill: Boolean = false) {
  import FunctionEmitter.*

  // ---------------------------------------------------------------------------
  // Output

  private var out         = new StringBuilder
  private var indentLevel = 1

  private var localCount = 0
  private var maxLocals  = 0

  /** Maximum number of locals declared in one Lua function (an upper bound of the live ones). */
  def maxLocalCount: Int = maxLocals

  private def line(s: String): Unit = {
    if (s.startsWith("local ")) {
      localCount += s.takeWhile(_ != '=').count(_ == ',') + 1
      if (localCount > maxLocals) maxLocals = localCount
    }
    var i = 0
    while (i < indentLevel) { out.append("  "); i += 1 }
    // A statement starting with `(` would be parsed as a call of the previous line's expression
    if (s.startsWith("(")) out.append("do ").append(s).append(" end\n")
    else out.append(s).append('\n')
  }

  private def indented(f: => Unit): Unit = {
    indentLevel += 1
    try f
    finally indentLevel -= 1
  }

  // ---------------------------------------------------------------------------
  // Names

  private val usedIdents   = mutable.Set.empty[String] ++ Naming.Reserved
  private val localIdents  = mutable.Map.empty[LocalName, String]
  private var tempCounter  = 0
  private var labelCounter = 0

  private def fresh(base: String): String = {
    var ident = base
    var i     = 1
    while (usedIdents(ident) || Naming.LuaKeywords(ident)) { ident = s"$base$i"; i += 1 }
    usedIdents += ident
    ident
  }

  private def newTemp(): String = {
    tempCounter += 1
    val t = s"_t$tempCounter"
    usedIdents += t
    if (spill) s"L.$t" else t
  }

  /** Parameters stay Lua locals even in spill mode. */
  private val paramNames = mutable.Set.empty[LocalName]

  private def paramIdent(name: LocalName): String = {
    paramNames += name
    localIdent(name)
  }

  /** Declares a local variable (or, in spill mode, initializes a field of `L`). */
  private def declare(ident: String, value: Option[String]): Unit =
    if (spill) value.foreach(v => line(s"$ident = $v"))
    else line(s"local $ident${value.fold("")(v => s" = $v")}")

  private def localIdent(name: LocalName): String =
    if (name == LocalName.This) "this"
    else if (spill && !paramNames(name)) "L." + baseLocalIdent(name)
    else baseLocalIdent(name)

  private def baseLocalIdent(name: LocalName): String =
    localIdents.getOrElseUpdate(
      name, {
        var base = Naming.sanitize(name.nameString).replaceAll("_+", "_")
        if (base.startsWith("_")) base = "v" + base
        fresh(base)
      }
    )

  // ---------------------------------------------------------------------------
  // Labels, functions and try blocks

  private final class LabelInfo(
      val luaLabel: String,
      val target: Target,
      val tpe: Type,
      val fnDepth: Int
  )

  /** A `pcall`ed function created for a try block, and the outer labels its body jumps to. */
  private final class TryContext(val fnDepth: Int) {
    val escapes = mutable.LinkedHashMap.empty[LabelName, Int]
  }

  private var labels                     = Map.empty[LabelName, LabelInfo]
  private var fnDepth                    = 0
  private var tryStack: List[TryContext] = Nil

  // ---------------------------------------------------------------------------
  // Lifted functions

  /** Scala lambdas (typed closures) lifted out of this method: (key in `F`, code, has captures). */
  val liftedFunctions    = mutable.ListBuffer.empty[(String, String, Boolean)]
  private var liftPrefix = "fn"

  // ---------------------------------------------------------------------------
  // Entry points

  /** Emits a method definition `function <fullName>(params) body end`. */
  def emitMethod(
      fullName: String,
      thisParam: Option[String],
      params: List[ParamDef],
      resultType: Type,
      body: Tree,
      returnThis: Boolean
  ): String = {
    liftPrefix = Naming.sanitize(fullName.stripPrefix("C."))
    val paramIdents = thisParam.toList ++ params.map(p => paramIdent(p.name.name))
    out.append(s"function $fullName(${paramIdents.mkString(", ")})\n")
    line("local _")
    if (spill) line("local L = {}")
    if (resultType == VoidType) {
      stat(body)
      if (returnThis) line("return this")
    } else {
      emitInto(body, ReturnTo(resultType))
    }
    out.append("end\n")
    out.toString
  }

  // ---------------------------------------------------------------------------
  // Statements

  private def stat(tree: Tree): Unit = emitInto(tree, Discard)

  private def emitInto(tree: Tree, target: Target): Unit = {
    tree match {
      case Block(stats) =>
        stats.init.foreach(stat)
        emitInto(stats.last, target)

      case Skip() =>
        target match {
          case Discard => ()
          case ReturnTo(_) => line("do return end")
          case AssignTo(v, _) => line(s"$v = nil")
        }

      case VarDef(name, _, vtpe, _, rhs) =>
        val ident = localIdent(name.name)
        if (isSimple(rhs) || rhs.tpe == NothingType) {
          val e = adapt(expr(rhs), rhs.tpe, vtpe)
          declare(ident, Some(e))
        } else {
          declare(ident, None)
          emitInto(rhs, AssignTo(ident, vtpe))
        }

      case Assign(lhs, rhs) =>
        val lhsCode = lhs match {
          case VarRef(name) =>
            localIdent(name)
          case Select(qual, field) =>
            s"${prefix(exprMaybeTemp(qual, !isSimple(rhs)))}.${Naming.fieldName(field.name)}"
          case SelectStatic(field) =>
            s"C.${Naming.className(field.name.className)}.${Naming.staticFieldName(field.name)}"
          case ArraySelect(array, index) =>
            val List(a, i) = exprsBefore(List(array, index), rhs)
            s"${prefixCode(a)}[$i]"
          case JSSelect(qual, item0) =>
            val item = item0 match {
              case StringLiteral(ArrayIndex(i)) => IntLiteral(i)(using item0.pos)
              case _ => item0
            }
            val List(q, k) = exprsBefore(List(qual, item), rhs)
            jsMemberLhs(q, item, k)
          case RecordSelect(_, _) =>
            throw new UnsupportedOperationException("RecordSelect (the optimizer must be off)")
          case JSPrivateSelect(_, _) | JSSuperSelect(_, _, _) | JSGlobalRef(_) =>
            unsupportedStat(lhs)
            return
        }
        emitInto(rhs, AssignTo(lhsCode, lhs.tpe))

      case Return(expr, label) =>
        emitReturn(expr, label)

      case Labeled(label, tpe, body) =>
        target match {
          case ReturnTo(resultType) =>
            withLabel(label, new LabelInfo("", target, resultType, fnDepth)) {
              emitInto(body, target)
            }
          case _ =>
            labelCounter += 1
            val luaLabel = s"_L$labelCounter"
            line("do")
            indented {
              withLabel(label, new LabelInfo(luaLabel, target, tpe, fnDepth)) {
                emitInto(body, target)
              }
            }
            line("end")
            line(s"::$luaLabel::")
        }

      case If(cond, thenp, elsep) if !isSimple(tree) || tree.tpe == VoidType =>
        val c = expr(cond).code
        line(s"if $c then")
        indented(emitInto(thenp, target))
        emitElse(elsep, target)
        line("end")

      case While(cond, body) =>
        if (isSimple(cond)) {
          line(s"while ${expr(cond).code} do")
          indented(stat(body))
          line("end")
        } else {
          line("while true do")
          indented {
            val c = expr(cond).code
            line(s"if not $c then break end")
            stat(body)
          }
          line("end")
        }
        finishVoid(target)

      case ForIn(obj, keyVar, _, body) =>
        val o = expr(obj).code
        val k = localIdent(keyVar.name)
        if (spill) {
          line(s"for _k in pairs($o) do")
          indented { line(s"$k = _k"); stat(body) }
        } else {
          line(s"for $k in pairs($o) do")
          indented(stat(body))
        }
        line("end")

      case TryCatch(block, errVar, _, handler) =>
        val resultVar = valueVarFor(tree, target)
        val res       = emitPcall(block, resultVar.map(v => AssignTo(v, tree.tpe)))
        line(s"if not ${res.ok} then")
        indented {
          declare(localIdent(errVar.name), Some(res.err))
          emitInto(handler, target)
        }
        line("else")
        indented {
          emitJumpDispatch(res)
          resultVar.foreach(v => finish(LExp(v, Prefix), tree.tpe, target))
        }
        line("end")

      case TryFinally(block, finalizer) =>
        val resultVar = valueVarFor(tree, target)
        val res       = emitPcall(block, resultVar.map(v => AssignTo(v, tree.tpe)))
        stat(finalizer)
        line(s"if not ${res.ok} then error(${res.err}, 0) end")
        emitJumpDispatch(res)
        resultVar.foreach(v => finish(LExp(v, Prefix), tree.tpe, target))

      case Match(selector, cases, default) =>
        val s     = exprToTemp(selector)
        var first = true
        for ((alts, body) <- cases) {
          val conds = alts.map(lit => s"$s == ${literal(lit).code}").mkString(" or ")
          line(s"${if (first) "if" else "elseif"} $conds then")
          indented(emitInto(body, target))
          first = false
        }
        if (first) {
          emitInto(default, target)
        } else {
          line("else")
          indented(emitInto(default, target))
          line("end")
        }

      case StoreModule() =>
        line(s"MOD.${Naming.className(enclosingClass)} = this")
        finishVoid(target)

      case JSDelete(qual, item) =>
        val List(q, k) = exprs(List(qual, item))
        line(s"${prefixCode(q)}[$k] = nil")

      case Debugger() =>
        ()

      case _ =>
        finish(expr(tree), tree.tpe, target, Some(tree))
    }
  }

  private def emitElse(elsep: Tree, target: Target): Unit = elsep match {
    case Skip() if target == Discard =>
      ()
    case If(c2, t2, e2) if isSimple(c2) && (!isSimple(elsep) || elsep.tpe == VoidType) =>
      line(s"elseif ${expr(c2).code} then")
      indented(emitInto(t2, target))
      emitElse(e2, target)
    case _ =>
      line("else")
      indented(emitInto(elsep, target))
  }

  private def finishVoid(target: Target): Unit = target match {
    case Discard => ()
    case ReturnTo(_) => line("do return end")
    case AssignTo(v, _) => line(s"$v = nil")
  }

  /** Pushes the value of an expression into a target. */
  private def finish(e: LExp, tpe: Type, target: Target, tree: Option[Tree] = None): Unit =
    target match {
      case Discard =>
        if (e.kind == Call) line(e.code)
        else if (tree.exists(isPureForDiscard)) ()
        else line(s"_ = ${e.code}")
      case ReturnTo(resultType) =>
        line(s"return ${adapt(e, tpe, resultType)}")
      case AssignTo(v, vtpe) =>
        line(s"$v = ${adapt(e, tpe, vtpe)}")
    }

  private def isPureForDiscard(tree: Tree): Boolean = tree match {
    case _: Literal | _: VarRef | _: Closure | _: SelectStatic => true
    case Select(q, _) => isPureForDiscard(q)
    case _ => false
  }

  private def withLabel[A](label: LabelName, info: LabelInfo)(f: => A): A = {
    val saved = labels
    labels += label -> info
    try f
    finally labels = saved
  }

  /** Inside a pcall'ed function: return a jump marker to the enclosing try. */
  private def jumpOut(label: LabelName, v: String): Unit = {
    val ctx = tryStack.head
    val k   = ctx.escapes.getOrElseUpdate(label, ctx.escapes.size + 1)
    line(s"do return R.JMP, $k, $v end")
  }

  /** Return of an already evaluated (and adapted) value to a label. */
  private def emitReturnCode(v: String, label: LabelName): Unit = {
    val info = labels(label)
    if (info.fnDepth != fnDepth) jumpOut(label, v)
    else
      info.target match {
        case ReturnTo(_) =>
          line(s"do return $v end")
        case AssignTo(lhs, _) =>
          line(s"$lhs = $v")
          line(s"goto ${info.luaLabel}")
        case Discard =>
          line(s"goto ${info.luaLabel}")
      }
  }

  private def emitReturn(value: Tree, label: LabelName): Unit = {
    val info = labels(label)
    if (info.fnDepth != fnDepth) {
      val v = if (value.tpe == VoidType) { stat(value); "nil" }
      else adapt(expr(value), value.tpe, info.tpe)
      jumpOut(label, v)
    } else {
      info.target match {
        case ReturnTo(_) =>
          if (isSimple(value) && value.tpe != VoidType) {
            line(s"do return ${adapt(expr(value), value.tpe, info.tpe)} end")
          } else {
            line("do")
            indented(emitInto(value, info.target))
            line("end")
          }
        case t @ AssignTo(_, _) =>
          emitInto(value, t)
          line(s"goto ${info.luaLabel}")
        case Discard =>
          stat(value)
          line(s"goto ${info.luaLabel}")
      }
    }
  }

  private final class PcallResult(
      val ok: String,
      val err: String,
      val k: String,
      val v: String,
      val escapes: List[(LabelName, Int)]
  )

  /** Emits `local ok, err, k, v = pcall(function() <block> end)`. */
  private def emitPcall(block: Tree, blockTarget: Option[Target]): PcallResult = {
    val ok  = newTemp()
    val err = newTemp()
    val k   = newTemp()
    val v   = newTemp()
    val ctx = new TryContext(fnDepth + 1)
    line(s"${if (spill) "" else "local "}$ok, $err, $k, $v = pcall(function()")
    val savedLocalCount = localCount
    localCount = 0
    indented {
      fnDepth += 1
      tryStack ::= ctx
      try {
        line("local _")
        emitInto(block, blockTarget.getOrElse(Discard))
      } finally {
        tryStack = tryStack.tail
        fnDepth -= 1
        localCount = savedLocalCount
      }
    }
    line("end)")
    new PcallResult(ok, err, k, v, ctx.escapes.toList)
  }

  /** After a pcall: re-issues the jumps to outer labels requested by the try body. */
  private def emitJumpDispatch(res: PcallResult): Unit =
    if (res.escapes.nonEmpty) {
      line(s"if ${res.err} == R.JMP then")
      indented {
        var first = true
        for ((label, k) <- res.escapes) {
          line(s"${if (first) "if" else "elseif"} ${res.k} == $k then")
          indented(emitReturnCode(res.v, label))
          first = false
        }
        line("end")
      }
      line("end")
    }

  private def valueVarFor(tree: Tree, target: Target): Option[String] =
    if (target == Discard || tree.tpe == VoidType || tree.tpe == NothingType) None
    else {
      val t = newTemp()
      declare(t, None)
      Some(t)
    }

  private def unsupportedStat(tree: Tree): Unit =
    line(s"""R.unsupported("${tree.getClass.getSimpleName}")""")

  // ---------------------------------------------------------------------------
  // Expressions

  /** Is `expr(tree)` guaranteed not to emit any statement? */
  private def isSimple(tree: Tree): Boolean = tree match {
    case _: Literal | _: VarRef | _: LoadModule | _: SelectStatic | _: JSGlobalRef |
        _: LoadJSModule | _: LoadJSConstructor | _: SelectJSNativeMember | _: JSTypeOfGlobalRef =>
      true
    case Select(q, _) => isSimple(q)
    case ArraySelect(a, i) => isSimple(a) && isSimple(i)
    case Apply(_, r, _, args) => isSimple(r) && args.forall(isSimple)
    case ApplyStatically(_, r, _, _, args) => isSimple(r) && args.forall(isSimple)
    case ApplyStatic(_, _, _, args) => args.forall(isSimple)
    case ApplyTypedClosure(_, f, args) => isSimple(f) && args.forall(isSimple)
    case New(_, _, args) => args.forall(isSimple)
    case UnaryOp(_, e) => isSimple(e)
    case BinaryOp(_, a, b) => isSimple(a) && isSimple(b)
    case IsInstanceOf(e, _) => isSimple(e)
    case AsInstanceOf(e, _) => isSimple(e)
    case NewArray(_, l) => isSimple(l)
    case ArrayValue(_, elems) => elems.forall(isSimple)
    case Closure(_, _, _, _, _, _, captures) => captures.forall(isSimple)
    case JSNew(c, args) => isSimple(c) && args.forall(isSimpleOrSpread)
    case JSSelect(q, i) => isSimple(q) && isSimple(i)
    case JSFunctionApply(f, args) => isSimple(f) && args.forall(isSimpleOrSpread)
    case JSMethodApply(r, m, args) => isSimple(r) && isSimple(m) && args.forall(isSimpleOrSpread)
    case JSUnaryOp(_, e) => isSimple(e)
    case JSBinaryOp(_, a, b) => isSimple(a) && isSimple(b)
    case JSArrayConstr(items) => items.forall(isSimpleOrSpread)
    case JSObjectConstr(fields) => fields.forall((k, v) => isSimple(k) && isSimple(v))
    case If(c, a, BooleanLiteral(false)) => isSimple(c) && isSimple(a)
    case If(c, BooleanLiteral(true), b) => isSimple(c) && isSimple(b)
    case If(c, a, b) if neverFalsy(tree.tpe) => isSimple(c) && isSimple(a) && isSimple(b)
    case _ => false
  }

  private def isSimpleOrSpread(t: TreeOrJSSpread): Boolean = t match {
    case JSSpread(items) => isSimple(items)
    case t: Tree => isSimple(t)
  }

  /** Values of this type are never `nil` or `false` in Lua. */
  private def neverFalsy(tpe: Type): Boolean = tpe match {
    case CharType | ByteType | ShortType | IntType | LongType | FloatType | DoubleType |
        StringType =>
      true
    case ArrayType(_, false, _) => true
    case ClassType(_, false, _) if !g.mayBeHijacked(tpe) => true
    case _ => false
  }

  private def exprToTemp(tree: Tree): String = {
    val e = expr(tree)
    if (e.kind == Prefix && Naming.isValidLuaIdent(e.code)) e.code
    else {
      val t = newTemp()
      declare(t, Some(e.code))
      t
    }
  }

  private def exprMaybeTemp(tree: Tree, needTemp: Boolean): LExp =
    if (needTemp && !isConstant(tree)) LExp(exprToTemp(tree), Prefix) else expr(tree)

  private def isConstant(tree: Tree): Boolean = tree match {
    case _: Literal => true
    case _ => false
  }

  /** Evaluates `trees` in order, preserving the evaluation order even if later trees emit
    * statements.
    */
  private def exprs(trees: List[Tree]): List[String] = {
    val lastComplex = trees.lastIndexWhere(t => !isSimple(t))
    trees.zipWithIndex.map { (t, i) =>
      exprMaybeTemp(t, i < lastComplex).code
    }
  }

  /** Like `exprs`, but also forces temps if `after` (evaluated later) emits statements. */
  private def exprsBefore(trees: List[Tree], after: Tree): List[String] =
    if (isSimple(after)) exprs(trees)
    else trees.map(t => exprMaybeTemp(t, true).code)

  /** Like `exprs`, with expected types for boxing. */
  private def args(trees: List[Tree], expected: List[Type]): List[String] = {
    val codes = exprs(trees)
    codes.lazyZip(trees).lazyZip(expected).map((c, t, exp) => adapt(LExp(c, Prefix), t.tpe, exp))
  }

  private def prefix(e: LExp): String =
    if (e.kind == Other) s"(${e.code})" else e.code

  private def prefixCode(code: String): String =
    if (DottedIdent.matches(code) && !Naming.LuaKeywords(code.takeWhile(_ != '.'))) code
    else s"($code)"

  /** Boxes chars that flow into non-char slots. */
  private def adapt(e: LExp, from: Type, to: Type): String =
    if (from == CharType && to != CharType && to != VoidType && to != NothingType)
      s"R.boxC(${e.code})"
    else e.code

  private def typeOfRef(ref: TypeRef): Type = ref match {
    case ref: PrimRef => ref.tpe
    case ClassRef(cls) => ClassType(cls, nullable = true, exact = false)
    case ref: ArrayTypeRef => ArrayType(ref, nullable = true, exact = false)
    case ref: TransientTypeRef => ref.tpe
  }

  private def paramTypes(method: MethodName): List[Type] = method.paramTypeRefs.map(typeOfRef)

  def expr(tree: Tree): LExp = tree match {
    case tree: Literal =>
      literal(tree)

    case VarRef(name) =>
      LExp(localIdent(name), Prefix)

    case Select(qual, field) =>
      LExp(s"${prefix(expr(qual))}.${Naming.fieldName(field.name)}", Prefix)

    case SelectStatic(field) =>
      LExp(
        s"C.${Naming.className(field.name.className)}.${Naming.staticFieldName(field.name)}",
        Prefix
      )

    case ArraySelect(array, index) =>
      val List(a, i) = exprs(List(array, index))
      LExp(s"${prefixCode(a)}[$i]", Prefix)

    case Apply(_, receiver, method, args0) =>
      val methodIdent = Naming.methodName(MemberNamespace.Public, method.name)
      val recvAndArgs = exprs(receiver :: args0)
      val r           = recvAndArgs.head
      val as          = recvAndArgs.tail
        .lazyZip(args0)
        .lazyZip(paramTypes(method.name))
        .map((c, t, exp) => adapt(LExp(c, Prefix), t.tpe, exp))
      g.hijackedClassOf(receiver.tpe) match {
        case Some(hijacked) if g.hasMethod(hijacked, MemberNamespace.Public, method.name) =>
          // Hijacked methods receive `this` as a primitive (only matters for chars)
          val primThis = receiver.tpe match {
            case ClassType(BoxedCharacterClass, _, _) => s"R.unboxC($r)"
            case _ => r
          }
          call(s"C.${Naming.className(hijacked)}.$methodIdent", primThis :: as)
        case _ if g.mayBeHijacked(receiver.tpe) =>
          val boxedThis = adapt(LExp(r, Prefix), receiver.tpe, AnyType)
          call("R.dp", boxedThis :: s""""$methodIdent"""" :: as)
        case _ =>
          call(s"${prefixCode(r)}:$methodIdent", as)
      }

    case ApplyStatically(flags, receiver, className, method, args0) =>
      val namespace =
        if (flags.isConstructor) MemberNamespace.Constructor
        else if (flags.isPrivate) MemberNamespace.Private
        else MemberNamespace.Public
      val all = args(receiver :: args0, receiver.tpe :: paramTypes(method.name))
      call(s"C.${Naming.className(className)}.${Naming.methodName(namespace, method.name)}", all)

    case ApplyStatic(flags, className, method, args0) =>
      val namespace =
        if (flags.isPrivate) MemberNamespace.PrivateStatic else MemberNamespace.PublicStatic
      call(
        s"C.${Naming.className(className)}.${Naming.methodName(namespace, method.name)}",
        args(args0, paramTypes(method.name))
      )

    case ApplyTypedClosure(_, fun, args0) =>
      // A Scala lambda is a table {liftedFunction, captures...}: call liftedFunction(lambda, args...)
      val funExp = expr(fun)
      val f      =
        if (DottedIdent.matches(funExp.code) && args0.forall(isSimple)) funExp.code
        else { val t = newTemp(); declare(t, Some(funExp.code)); t }
      val expected = fun.tpe match {
        case ClosureType(paramTypes, _, _) => paramTypes
        case _ => args0.map(_ => AnyType)
      }
      val as = args(args0, expected)
      call(s"$f[1]", f :: as)

    case New(className, ctor, args0) =>
      val cls = s"C.${Naming.className(className)}"
      val as  = args(args0, paramTypes(ctor.name))
      call(
        s"$cls.${Naming.methodName(MemberNamespace.Constructor, ctor.name)}",
        s"$cls.__new()" :: as
      )

    case LoadModule(className) =>
      val c = Naming.className(className)
      LExp(s"(MOD.$c or LOAD.$c())", Prefix)

    case UnaryOp(op, lhs) =>
      unaryOp(op, lhs)

    case BinaryOp(op, lhs, rhs) =>
      binaryOp(op, lhs, rhs)

    case NewArray(typeRef, length) =>
      call("R.newArray", List(s"AT[${g.arrayTypeIndex(typeRef)}]", expr(length).code))

    case ArrayValue(typeRef, elems) =>
      val elemType = typeRef match {
        case ArrayTypeRef(base, 1) => typeOfRef(base)
        case ArrayTypeRef(base, d) =>
          ArrayType(ArrayTypeRef(base, d - 1), nullable = true, exact = false)
      }
      val es  = args(elems, elems.map(_ => elemType))
      val tbl =
        es.zipWithIndex.map((e, i) => if (i == 0) s"[0] = $e" else e).mkString("{", ", ", "}")
      call("R.arrayOf", List(s"AT[${g.arrayTypeIndex(typeRef)}]", tbl, elems.size.toString))

    case IsInstanceOf(e, testType) =>
      isInstanceOf(expr(e), testType)

    case MultipleReturns(jsCall, arity) =>
      // Pack all the values returned by the Lua function into a TupleN
      val tuple = MultipleReturns.tupleClass(arity)
      val ctor  =
        Naming.methodName(MemberNamespace.Constructor, MultipleReturns.tupleConstructor(arity))
      call("R.tuple", List(s"C.${Naming.className(tuple)}", s""""$ctor"""", expr(jsCall).code))

    case AsInstanceOf(e, tpe) if e.tpe == tpe =>
      expr(e)

    case AsInstanceOf(e, tpe) =>
      val v = expr(e)
      tpe match {
        case BooleanType => LExp(s"(${v.code} == true)", Prefix)
        case CharType => call("R.unboxC", List(v.code))
        case ByteType | ShortType | IntType => LExp(s"(${v.code} or 0)", Prefix)
        case FloatType | DoubleType => LExp(s"(${v.code} or 0)", Prefix)
        case LongType => LExp(s"(${v.code} or 0LL)", Prefix)
        case StringType => LExp(s"""(${v.code} or "")""", Prefix)
        case UndefType => call("R.void", List(v.code))
        case _ => v
      }

    case Closure(flags, captureParams, params, restParam, resultType, body, captureValues) =>
      closure(flags, captureParams, params, restParam, resultType, body, captureValues)

    case JSNew(ctor, args0) =>
      val all = jsArgs(ctor :: args0)
      call("R.jnew", all)

    case JSSelect(qual, StringLiteral(ArrayIndex(i))) =>
      // In JS, a["0"] is a[0]
      expr(JSSelect(qual, IntLiteral(i)(using tree.pos))(using tree.pos))

    case JSSelect(qual, item) =>
      val List(q, k) = exprs(List(qual, item))
      if (isNamespace(qual)) LExp(jsMemberLhs(q, item, k), Prefix)
      else call("R.jget", List(q, k))

    case JSFunctionApply(fun, args0) =>
      val all = jsArgs(fun :: args0)
      call(prefixCode(all.head), all.tail)

    case JSMethodApply(receiver, method, args0) =>
      val all          = jsArgs(receiver :: method :: args0)
      val r :: m :: as = all: @unchecked
      method match {
        case StringLiteral("call") =>
          call(prefixCode(r), as)
        case StringLiteral(name) if isNamespace(receiver) && Naming.isValidLuaIdent(name) =>
          call(s"${prefixCode(r)}.$name", as)
        case _ if isNamespace(receiver) =>
          call(s"${prefixCode(r)}[$m]", as)
        case _ =>
          call("R.jcall", r :: m :: as)
      }

    case JSUnaryOp(op, lhs) =>
      val e = expr(lhs).code
      op match {
        case JSUnaryOp.+ => call("tonumber", List(e))
        case JSUnaryOp.- => LExp(s"(- $e)", Prefix)
        case JSUnaryOp.~ => call("bnot", List(e))
        case JSUnaryOp.! => LExp(s"(not R.truthy($e))", Prefix)
        case JSUnaryOp.typeof => call("R.typeof", List(e))
      }

    case JSBinaryOp(op, lhs, rhs) =>
      jsBinaryOp(op, lhs, rhs)

    case JSArrayConstr(items) if items.forall(_.isInstanceOf[Tree]) =>
      val elems = jsArgs(items).zipWithIndex.map((e, i) => if (i == 0) s"[0] = $e" else e)
      call("R.jarr", List(elems.mkString("{", ", ", "}"), items.size.toString))

    case JSObjectConstr(fields) =>
      val codes   = exprs(fields.flatMap((k, v) => List(k, v))).toIndexedSeq
      val entries = fields.zipWithIndex.map { case ((k, _), i) =>
        val vc = codes(2 * i + 1)
        k match {
          case StringLiteral(name) if Naming.isValidLuaIdent(name) => s"$name = $vc"
          case _ => s"[${codes(2 * i)}] = $vc"
        }
      }
      LExp(entries.mkString("{", ", ", "}"), Other)

    case JSGlobalRef(name) =>
      LExp(jsGlobal(name), Prefix)

    case JSTypeOfGlobalRef(JSGlobalRef(name)) =>
      if (JSBuiltins(name)) call("R.typeof", List(s"""rawget(R.js, "$name")"""))
      else call("R.typeof", List(name))

    case LoadJSModule(className) =>
      LExp(loadSpec(g.jsNativeLoadSpec(className), className.nameString), Prefix)

    case LoadJSConstructor(className) =>
      LExp(loadSpec(g.jsNativeLoadSpec(className), className.nameString), Prefix)

    case SelectJSNativeMember(className, member) =>
      LExp(
        loadSpec(g.jsNativeMemberLoadSpec(className, member.name), member.name.nameString),
        Prefix
      )

    case _: Block | _: If | _: Labeled | _: TryCatch | _: TryFinally | _: Match | _: VarDef |
        _: Assign | _: While | _: Return | _: StoreModule | _: Skip | _: ForIn | _: JSDelete |
        _: Debugger =>
      tree match {
        case If(c, a, BooleanLiteral(false)) if isSimple(tree) =>
          LExp(s"(${expr(c).code} and ${expr(a).code})", Prefix)
        case If(c, BooleanLiteral(true), b) if isSimple(tree) =>
          LExp(s"(${expr(c).code} or ${expr(b).code})", Prefix)
        case If(c, a, b) if isSimple(tree) =>
          LExp(
            s"(${expr(c).code} and ${adapt(expr(a), a.tpe, tree.tpe)} or ${adapt(expr(b), b.tpe, tree.tpe)})",
            Prefix
          )
        case _ if tree.tpe == VoidType || tree.tpe == NothingType =>
          stat(tree)
          LExp("nil", Other)
        case _ =>
          val t = newTemp()
          declare(t, None)
          emitInto(tree, AssignTo(t, tree.tpe))
          LExp(t, Prefix)
      }

    case _ =>
      call("R.unsupported", List(s""""${tree.getClass.getSimpleName}""""))
  }

  private def call(fun: String, args: List[String]): LExp =
    LExp(s"$fun(${args.mkString(", ")})", Call)

  private def jsArgs(args: List[TreeOrJSSpread]): List[String] = {
    val trees = args.map {
      case JSSpread(items) => items
      case t: Tree => t
    }
    val codes = exprs(trees)
    args.zip(codes).zip(trees).map {
      case ((JSSpread(_), c), _) => s"R.unpackArr($c)"
      case ((_, c), t) => adapt(LExp(c, Prefix), t.tpe, AnyType)
    }
  }

  /** Native JS module/global references, which we treat as namespaces (dot calls, no `self`). */
  private def isNamespace(tree: Tree): Boolean = tree match {
    case _: JSGlobalRef | _: LoadJSModule | _: SelectJSNativeMember | _: LoadJSConstructor => true
    case JSSelect(q, StringLiteral(_)) => isNamespace(q)
    case _ => false
  }

  private def jsMemberLhs(q: String, item: Tree, k: String): String = item match {
    case StringLiteral(name) if Naming.isValidLuaIdent(name) => s"${prefixCode(q)}.$name"
    case _ => s"${prefixCode(q)}[$k]"
  }

  private def jsGlobal(name: String): String =
    if (JSBuiltins(name)) s"R.js.$name" else name

  private def loadSpec(spec: Option[JSNativeLoadSpec], what: String): String = spec match {
    case Some(JSNativeLoadSpec.Global(ref, path)) =>
      (jsGlobal(ref) :: path).mkString(".")
    case Some(JSNativeLoadSpec.Import(module, path)) =>
      // @JSImport("mod", JSImport.Namespace) or JSImport.Default is the module itself;
      // @JSImport("mod", "x") is require("mod").x
      val members = path match {
        case "default" :: rest => rest
        case _ => path
      }
      (s"require(${luaString(module)})" :: members).mkString(".")
    case Some(JSNativeLoadSpec.ImportWithGlobalFallback(importSpec, _)) =>
      loadSpec(Some(importSpec), what)
    case _ =>
      s"""R.unsupported("JS module import for $what")"""
  }

  private def closure(
      flags: ClosureFlags,
      captureParams: List[ParamDef],
      params: List[ParamDef],
      restParam: Option[ParamDef],
      resultType: Type,
      body: Tree,
      captureValues: List[Tree]
  ): LExp = {
    val captureCodes = args(captureValues, captureParams.map(_.ptpe))

    val savedOut        = out
    val savedIndent     = indentLevel
    val savedLabels     = labels
    val savedTry        = tryStack
    val savedLocalCount = localCount
    out = new StringBuilder
    indentLevel = 1
    labels = Map.empty
    tryStack = Nil
    localCount = 0
    fnDepth += 1
    val code =
      try {
        val paramIdents   = params.map(p => paramIdent(p.name.name)) ++ restParam.map(_ => "...")
        val captureIdents = captureParams.map(p => paramIdent(p.name.name))
        if (flags.typed && captureIdents.nonEmpty) {
          val loads = captureIdents.indices.map(i => s"cl[${i + 2}]")
          line(s"local ${captureIdents.mkString(", ")} = ${loads.mkString(", ")}")
        }
        line("local _")
        if (spill) line("local L = {}")
        if (!flags.arrow && !flags.typed) line("local this = nil")
        restParam.foreach(p => line(s"local ${paramIdent(p.name.name)} = R.restArgs(...)"))
        if (resultType == VoidType) stat(body)
        else emitInto(body, ReturnTo(resultType))
        val bodyCode = out.toString
        if (flags.typed) {
          // Lambda lifting: LuaJIT cannot compile closure creation (FNEW), but table creation is fine
          val key = s"${liftPrefix}_${liftedFunctions.size + 1}"
          liftedFunctions += (
            (
              key,
              s"function(${("cl" :: paramIdents).mkString(", ")})\n${bodyCode}end",
              captureParams.nonEmpty
            )
          )
          if (captureParams.isEmpty) s"FC.$key"
          else (s"F.$key" :: captureCodes).mkString("{", ", ", "}")
        } else {
          val fn = s"function(${paramIdents.mkString(", ")})\n$bodyCode${"  " * savedIndent}end"
          if (captureParams.isEmpty) fn
          else
            s"(function(${captureIdents.mkString(", ")}) return $fn end)(${captureCodes.mkString(", ")})"
        }
      } finally {
        out = savedOut
        indentLevel = savedIndent
        labels = savedLabels
        tryStack = savedTry
        localCount = savedLocalCount
        fnDepth -= 1
      }
    val kind =
      if (flags.typed) if (captureParams.isEmpty) Prefix else Other
      else if (captureParams.isEmpty) Other
      else Call
    LExp(code, kind)
  }

  // ---------------------------------------------------------------------------
  // Literals

  def literal(tree: Literal): LExp = tree match {
    case Null() | Undefined() => LExp("nil", Other)
    case BooleanLiteral(v) => LExp(v.toString, Other)
    case CharLiteral(v) => LExp(v.toInt.toString, Other)
    case ByteLiteral(v) => intLit(v.toInt)
    case ShortLiteral(v) => intLit(v.toInt)
    case IntLiteral(v) => intLit(v)
    case LongLiteral(v) =>
      if (v == Long.MinValue) LExp("(-9223372036854775807LL - 1LL)", Prefix)
      else if (v < 0) LExp(s"(${v}LL)", Prefix)
      else LExp(s"${v}LL", Other)
    case FloatLiteral(v) => doubleLit(v.toDouble)
    case DoubleLiteral(v) => doubleLit(v)
    case StringLiteral(v) => LExp(luaString(v), Other)
    case ClassOf(typeRef) => call("R.classOf", List(luaString(classDescriptor(typeRef))))
  }

  private def intLit(v: Int): LExp =
    if (v < 0) LExp(s"($v)", Prefix) else LExp(v.toString, Other)

  private def doubleLit(v: Double): LExp =
    if (v.isNaN) LExp("(0/0)", Prefix)
    else if (v == Double.PositiveInfinity) LExp("math.huge", Prefix)
    else if (v == Double.NegativeInfinity) LExp("(-math.huge)", Prefix)
    else if (v == 0.0 && 1.0 / v < 0) LExp("(-0.0)", Prefix)
    else if (v == v.toLong.toDouble && Math.abs(v) < 1e15) {
      val s = v.toLong.toString
      if (v < 0) LExp(s"($s)", Prefix) else LExp(s, Other)
    } else {
      val s = java.lang.Double.toString(v)
      if (v < 0) LExp(s"($s)", Prefix) else LExp(s, Other)
    }

  private def classDescriptor(typeRef: TypeRef): String = typeRef match {
    case ref: PrimRef => ref.charCode.toString
    case ClassRef(cls) => cls.nameString
    case ArrayTypeRef(base, dims) =>
      "[" * dims + (base match {
        case ref: PrimRef => ref.charCode.toString
        case ClassRef(cls) => s"L${cls.nameString};"
      })
    case other => other.displayName
  }

  // ---------------------------------------------------------------------------
  // Type tests

  private def isInstanceOf(e: LExp, testType: Type): LExp = {
    val v = e.code
    testType match {
      case ClassType(ObjectClass, _, _) | AnyNotNullType =>
        LExp(s"($v ~= nil)", Prefix)
      case ClassType(cls, _, _) if HijackedClasses(cls) =>
        isInstanceOfPrim(v, BoxedClassToPrimType(cls))
      case StringType | BooleanType | CharType | ByteType | ShortType | IntType | LongType |
          FloatType | DoubleType | UndefType =>
        isInstanceOfPrim(v, testType)
      case ClassType(cls, _, _) =>
        call("R.isInstance", List(v, luaString(cls.nameString)))
      case ArrayType(ArrayTypeRef(base, dims), _, _) =>
        val b = base match {
          case ref: PrimRef => ref.charCode.toString
          case ClassRef(cls) => cls.nameString
        }
        call("R.isArrayOf", List(v, luaString(b), dims.toString))
      case _ =>
        call("R.unsupported", List(luaString(s"isInstanceOf $testType")))
    }
  }

  private def isInstanceOfPrim(v: String, tpe: Type): LExp = tpe match {
    case StringType => LExp(s"""(type($v) == "string")""", Prefix)
    case DoubleType => LExp(s"""(type($v) == "number")""", Prefix)
    case BooleanType => LExp(s"""(type($v) == "boolean")""", Prefix)
    case UndefType => LExp(s"($v == nil)", Prefix)
    case IntType => call("R.isInt", List(v))
    case ByteType => call("R.isByte", List(v))
    case ShortType => call("R.isShort", List(v))
    case FloatType => call("R.isFloat", List(v))
    case LongType => call("R.isLong", List(v))
    case CharType => call("R.isChar", List(v))
    case _ => LExp("false", Other)
  }

  // ---------------------------------------------------------------------------
  // Operators

  private def unaryOp(op: UnaryOp.Code, lhs: Tree): LExp = {
    import UnaryOp.*
    val e = expr(lhs)
    val v = e.code
    (op: @annotation.switch) match {
      case Boolean_! => LExp(s"(not $v)", Prefix)
      case CharToInt | ByteToInt | ShortToInt | IntToDouble | FloatToDouble | CheckNotNull => e
      case IntToLong => call("R.int64", List(v))
      case IntToChar => call("band", List(v, "0xffff"))
      case IntToByte => call("arshift", List(s"lshift($v, 24)", "24"))
      case IntToShort => call("arshift", List(s"lshift($v, 16)", "16"))
      case LongToInt => call("R.l2i", List(v))
      case DoubleToInt => call("R.d2i", List(v))
      case DoubleToFloat => call("R.fround", List(v))
      case LongToDouble => call("tonumber", List(v))
      case DoubleToLong => call("R.d2l", List(v))
      case LongToFloat => call("R.fround", List(s"tonumber($v)"))
      case String_length => LExp(s"(#${prefix(e)})", Prefix)
      case Class_name => LExp(s"${prefix(e)}.name", Prefix)
      case Class_isPrimitive => LExp(s"${prefix(e)}.isPrimitive", Prefix)
      case Class_isInterface => LExp(s"${prefix(e)}.isInterface", Prefix)
      case Class_isArray => LExp(s"${prefix(e)}.isArrayClass", Prefix)
      case Class_componentType => call("R.classComponentType", List(v))
      case Class_superClass => call("R.unsupported", List("\"Class.getSuperclass\""))
      case Array_length => LExp(s"${prefix(e)}.length", Prefix)
      case GetClass => call("R.getClass", List(v))
      case Clone => call("R.clone", List(v))
      case IdentityHashCode => call("R.ihc", List(v))
      case WrapAsThrowable => call("R.wrapT", List(v))
      case UnwrapFromThrowable => call("R.unwrapT", List(v))
      case Throw => call("error", List(v, "0"))
      case Float_toBits => call("R.floatToBits", List(v))
      case Float_fromBits => call("R.floatFromBits", List(v))
      case Double_toBits => call("R.doubleToBits", List(v))
      case Double_fromBits => call("R.doubleFromBits", List(v))
      case Int_clz => call("R.clz", List(v))
      case Long_clz => call("R.lclz", List(v))
      case UnsignedIntToLong => call("R.int64", List(s"R.toUnsigned($v)"))
      case BoolToInt => LExp(s"($v and 1 or 0)", Prefix)
    }
  }

  private def isSmallIntLiteral(t: Tree): Boolean = t match {
    case IntLiteral(v) => Math.abs(v.toLong) <= (1L << 20)
    case _ => false
  }

  private def binaryOp(op: BinaryOp.Code, lhs: Tree, rhs: Tree): LExp = {
    import BinaryOp.*
    val List(a, b)       = exprs(List(lhs, rhs))
    def infix(o: String) = LExp(s"($a $o $b)", Prefix)
    def fn(f: String)    = call(f, List(a, b))
    (op: @annotation.switch) match {
      case === | Boolean_== | Int_== | Long_== | Double_== => infix("==")
      case !== | Boolean_!= | Int_!= | Long_!= | Double_!= => infix("~=")
      case String_+ =>
        LExp(s"(${toLuaString(a, lhs.tpe)} .. ${toLuaString(b, rhs.tpe)})", Prefix)
      case Boolean_| =>
        if (isSimple(rhs) && isPureForDiscard(rhs)) infix("or") else fn("R.zor")
      case Boolean_& =>
        if (isSimple(rhs) && isPureForDiscard(rhs)) infix("and") else fn("R.zand")
      case Int_+ => call("tobit", List(s"$a + $b"))
      case Int_- => call("tobit", List(s"$a - $b"))
      case Int_* =>
        if (isSmallIntLiteral(lhs) || isSmallIntLiteral(rhs)) call("tobit", List(s"$a * $b"))
        else fn("R.imul")
      case Int_/ => fn("R.idiv")
      case Int_% => fn("R.irem")
      case Int_| => fn("bor")
      case Int_& => fn("band")
      case Int_^ => fn("bxor")
      case Int_<< => fn("lshift")
      case Int_>>> => fn("rshift")
      case Int_>> => fn("arshift")
      case Int_< | Long_< | Double_< => infix("<")
      case Int_<= | Long_<= | Double_<= => infix("<=")
      case Int_> | Long_> | Double_> => infix(">")
      case Int_>= | Long_>= | Double_>= => infix(">=")
      case Long_+ => infix("+")
      case Long_- => infix("-")
      case Long_* => infix("*")
      case Long_/ => fn("R.ldiv")
      case Long_% => fn("R.lrem")
      case Long_| => fn("bor")
      case Long_& => fn("band")
      case Long_^ => fn("bxor")
      case Long_<< => fn("lshift")
      case Long_>>> => fn("rshift")
      case Long_>> => fn("arshift")
      case Float_+ => call("R.fround", List(s"$a + $b"))
      case Float_- => call("R.fround", List(s"$a - $b"))
      case Float_* => call("R.fround", List(s"$a * $b"))
      case Float_/ => call("R.fround", List(s"$a / $b"))
      case Float_% => call("R.fround", List(s"math.fmod($a, $b)"))
      case Double_+ => infix("+")
      case Double_- => infix("-")
      case Double_* => infix("*")
      case Double_/ => infix("/")
      case Double_% => fn("math.fmod")
      case String_charAt => LExp(s"${prefixCode(a)}:byte($b + 1)", Call)
      case Class_isInstance => fn("R.classIsInstance")
      case Class_isAssignableFrom => fn("R.classIsAssignableFrom")
      case Class_cast => LExp(b, Prefix)
      case Class_newArray => fn("R.classNewArray")
      case Int_unsigned_/ => fn("R.iudiv")
      case Int_unsigned_% => fn("R.iurem")
      case Long_unsigned_/ => fn("R.ludiv")
      case Long_unsigned_% => fn("R.lurem")
      case Int_unsigned_< => LExp(s"(R.toUnsigned($a) < R.toUnsigned($b))", Prefix)
      case Int_unsigned_<= => LExp(s"(R.toUnsigned($a) <= R.toUnsigned($b))", Prefix)
      case Int_unsigned_> => LExp(s"(R.toUnsigned($a) > R.toUnsigned($b))", Prefix)
      case Int_unsigned_>= => LExp(s"(R.toUnsigned($a) >= R.toUnsigned($b))", Prefix)
      case Long_unsigned_< => LExp(s"(R.lucmp($a, $b) < 0)", Prefix)
      case Long_unsigned_<= => LExp(s"(R.lucmp($a, $b) <= 0)", Prefix)
      case Long_unsigned_> => LExp(s"(R.lucmp($a, $b) > 0)", Prefix)
      case Long_unsigned_>= => LExp(s"(R.lucmp($a, $b) >= 0)", Prefix)
    }
  }

  private def toLuaString(code: String, tpe: Type): String = tpe match {
    case StringType => code
    case IntType | ByteType | ShortType => s"tostring($code)"
    case BooleanType => s"tostring($code)"
    case DoubleType | FloatType => s"R.numToStr($code)"
    case CharType => s"R.chr($code)"
    case LongType => s"R.ltos($code)"
    case UndefType => "\"undefined\""
    case _ => s"R.str($code)"
  }

  private def jsBinaryOp(op: JSBinaryOp.Code, lhs: Tree, rhs: Tree): LExp = {
    val List(a, b)       = exprs(List(lhs, rhs))
    def infix(o: String) = LExp(s"($a $o $b)", Prefix)
    def fn(f: String)    = call(f, List(a, b))
    def int32(x: String) = s"R.toInt32($x)"
    op match {
      case JSBinaryOp.=== => infix("==")
      case JSBinaryOp.!== => infix("~=")
      case JSBinaryOp.+ => fn("R.jadd")
      case JSBinaryOp.- => infix("-")
      case JSBinaryOp.* => infix("*")
      case JSBinaryOp./ => infix("/")
      case JSBinaryOp.% => fn("math.fmod")
      case JSBinaryOp.| => call("bor", List(int32(a), int32(b)))
      case JSBinaryOp.& => call("band", List(int32(a), int32(b)))
      case JSBinaryOp.^ => call("bxor", List(int32(a), int32(b)))
      case JSBinaryOp.<< => call("lshift", List(int32(a), b))
      case JSBinaryOp.>> => call("arshift", List(int32(a), b))
      case JSBinaryOp.>>> => call("R.toUnsigned", List(s"rshift(${int32(a)}, $b)"))
      case JSBinaryOp.< => infix("<")
      case JSBinaryOp.<= => infix("<=")
      case JSBinaryOp.> => infix(">")
      case JSBinaryOp.>= => infix(">=")
      case JSBinaryOp.&& => infix("and")
      case JSBinaryOp.|| => infix("or")
      case JSBinaryOp.in => fn("R.jin")
      case JSBinaryOp.instanceof => fn("R.jinstanceof")
      case JSBinaryOp.** => infix("^")
      case _ => call("R.unsupported", List(luaString(s"JS binary op $op")))
    }
  }
}

object FunctionEmitter {
  sealed trait Target
  case object Discard extends Target
  final case class ReturnTo(tpe: Type) extends Target
  final case class AssignTo(lhs: String, tpe: Type) extends Target

  sealed trait Kind

  /** A Lua prefixexp (name, index, parenthesized expression): can be called/indexed directly. */
  case object Prefix extends Kind

  /** A function call: a prefixexp that can also be used as a statement. */
  case object Call extends Kind

  /** Anything else (literals, function literals, table constructors). */
  case object Other extends Kind

  final case class LExp(code: String, kind: Kind)

  /** Strings that JS treats as array indices. */
  object ArrayIndex {
    def unapply(s: String): Option[Int] =
      if (s.nonEmpty && s.length <= 9 && s.forall(_.isDigit) && (s == "0" || s.head != '0'))
        Some(s.toInt)
      else None
  }

  private val DottedIdent = """[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z_][A-Za-z0-9_]*)*""".r

  /** JS globals that the Scala.js library may use, emulated by the runtime in `R.js`. */
  val JSBuiltins: Set[String] = Set(
    "Math",
    "Object",
    "Error",
    "Number",
    "String",
    "Symbol",
    "Array",
    "Date",
    "JSON",
    "isNaN",
    "isFinite",
    "parseInt",
    "parseFloat",
    "Infinity",
    "NaN",
    "undefined",
    "BigInt",
    "Reflect",
    "Proxy",
    "console",
    "performance",
    "globalThis",
    "RegExp",
    "TypeError",
    "RangeError",
    "Promise",
    "Map",
    "Set",
    "WeakMap",
    "Int8Array",
    "Int16Array",
    "Int32Array",
    "Uint8Array",
    "Uint16Array",
    "Float32Array",
    "Float64Array",
    "ArrayBuffer",
    "DataView",
    "BigInt64Array",
    "setTimeout",
    "clearTimeout",
    "setInterval",
    "clearInterval",
    "queueMicrotask"
  )

  /** Lua string literal. Non-ASCII chars are encoded in UTF-8. */
  def luaString(s: String): String = {
    val bytes = s.getBytes(java.nio.charset.StandardCharsets.UTF_8)
    val sb    = new StringBuilder("\"")
    for (b <- bytes) {
      val c = b & 0xff
      c match {
        case '"' => sb.append("\\\"")
        case '\\' => sb.append("\\\\")
        case '\n' => sb.append("\\n")
        case '\r' => sb.append("\\r")
        case '\t' => sb.append("\\t")
        case _ if c < 32 || c >= 127 =>
          sb.append(f"\\$c%03d")
        case _ => sb.append(c.toChar)
      }
    }
    sb.append('"').toString
  }
}
