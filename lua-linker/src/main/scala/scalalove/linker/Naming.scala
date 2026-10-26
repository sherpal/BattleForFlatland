package scalalove.linker

import scala.collection.mutable

import org.scalajs.ir.Names.*
import org.scalajs.ir.Trees.MemberNamespace
import org.scalajs.ir.Types.*
import org.scalajs.ir.WellKnownNames.*

/** Maps IR names to Lua identifiers. */
object Naming {
  val LuaKeywords: Set[String] = Set(
    "and",
    "break",
    "do",
    "else",
    "elseif",
    "end",
    "false",
    "for",
    "function",
    "goto",
    "if",
    "in",
    "local",
    "nil",
    "not",
    "or",
    "repeat",
    "return",
    "then",
    "true",
    "until",
    "while"
  )

  /** Identifiers used by the generated code itself; locals must avoid them. */
  val Reserved: Set[String] = Set(
    "R",
    "C",
    "MOD",
    "LOAD",
    "AT",
    "L",
    "F",
    "FC",
    "cl",
    "bit",
    "ffi",
    "tobit",
    "band",
    "bor",
    "bxor",
    "bnot",
    "lshift",
    "rshift",
    "arshift",
    "self",
    "_"
  )

  /** Replaces characters that are not valid in a Lua identifier. */
  def sanitize(s: String): String = {
    val sb = new StringBuilder
    for (c <- s)
      if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_')
        sb += c
      else sb += '_'
    if (sb.isEmpty || sb.head.isDigit) sb.insert(0, '_')
    sb.toString
  }

  def isValidLuaIdent(s: String): Boolean =
    s.nonEmpty && !s.head.isDigit && !LuaKeywords(s) &&
      s.forall(c =>
        (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_'
      )

  private val classIdents     = mutable.Map.empty[ClassName, String]
  private val usedClassIdents = mutable.Set.empty[String]

  /** Key of the class in the `C` table (also used for `MOD` and `LOAD`). */
  def className(name: ClassName): String = classIdents.getOrElseUpdate(
    name, {
      val base  = sanitize(name.nameString)
      var ident = base
      var i     = 2
      while (usedClassIdents(ident)) { ident = s"${base}_$i"; i += 1 }
      usedClassIdents += ident
      ident
    }
  )

  private def typeRefCode(ref: TypeRef): String = ref match {
    case ref: PrimRef => ref.charCode.toString
    case ClassRef(ObjectClass) => "O"
    case ClassRef(BoxedStringClass) => "T"
    case ClassRef(cls) => sanitize(cls.nameString.substring(cls.nameString.lastIndexOf('.') + 1))
    case ArrayTypeRef(base, dims) => "A" * dims + typeRefCode(base)
    case other => sanitize(other.displayName)
  }

  private val methodIdents     = mutable.Map.empty[(MemberNamespace, MethodName), String]
  private val usedMemberIdents = mutable.Set.empty[String]

  /** Method idents always contain `__`; field idents never do, so they can share instance tables.
    */
  def methodName(namespace: MemberNamespace, name: MethodName): String =
    methodIdents.getOrElseUpdate(
      (namespace, name), {
        val prefix = namespace match {
          case MemberNamespace.Public => ""
          case MemberNamespace.Private => "p_"
          case MemberNamespace.PublicStatic => "s_"
          case MemberNamespace.PrivateStatic => "ps_"
          case MemberNamespace.Constructor => "ctor"
          case MemberNamespace.StaticConstructor => "clinit"
        }
        val simple =
          if (
            namespace == MemberNamespace.Constructor || namespace == MemberNamespace.StaticConstructor
          ) ""
          else {
            val s = sanitize(name.simpleName.nameString).replaceAll("_+", "_").stripPrefix("_")
            if (prefix.isEmpty && (s.isEmpty || s.head.isDigit)) "m" + s else s
          }
        val sig   = (name.paramTypeRefs :+ name.resultTypeRef).map(typeRefCode).mkString("_")
        val proxy = if (name.isReflectiveProxy) "_R" else ""
        val base  = s"$prefix${simple}__$sig$proxy"
        var ident = base
        var i     = 2
        while (usedMemberIdents(ident)) { ident = s"${base}_$i"; i += 1 }
        usedMemberIdents += ident
        ident
      }
    )

  private val fieldIdents = mutable.Map.empty[FieldName, String]

  /** Instance field ident. `takenInHierarchy` are the idents of the fields of the ancestors. */
  def assignFieldName(name: FieldName, takenInHierarchy: collection.Set[String]): String =
    fieldIdents.getOrElseUpdate(
      name, {
        var base = sanitize(name.simpleName.nameString)
          .replaceAll("_+", "_")
          .stripPrefix("_")
          .stripSuffix("_")
        if (base.isEmpty || base.head.isDigit) base = "f" + base
        if (LuaKeywords(base) || base == "length") base = base + "_f"
        var ident = base
        var i     = 2
        while (takenInHierarchy(ident)) { ident = s"${base}_$i"; i += 1 }
        ident
      }
    )

  def fieldName(name: FieldName): String =
    fieldIdents.getOrElse(
      name,
      // Fields of classes without instances (never written in practice); give them a stable name
      assignFieldName(name, Set.empty)
    )

  /** Static fields live in the class table, with an `sf__` prefix. */
  def staticFieldName(name: FieldName): String =
    "sf__" + sanitize(name.simpleName.nameString)
}
