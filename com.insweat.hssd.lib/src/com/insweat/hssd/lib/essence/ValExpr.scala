package com.insweat.hssd.lib.essence

import com.insweat.hssd.lib.util._
import scala.math.ScalaNumericConversions
import javax.script.ScriptEngineFactory
import javax.script.ScriptEngineManager
import javax.script.Compilable
import javax.script.CompiledScript
import javax.script.Bindings
import javax.script.ScriptException
import com.insweat.hssd.lib.constraints.Constraint
import com.insweat.hssd.lib.tree.TreePath
import javax.script.SimpleBindings

private class ValExprInitException(err: String) extends Exception(err)

sealed abstract class ValExpr {
    def apply(src: ValExpr)(implicit vd: ValueData): ValExpr
    def isError: Boolean
    def isAbsolute: Boolean
    def isSharp: Boolean

    val thype: Thype
    val sym: String
    def value: Any

    def validated(vd: ValueData, optConstraint: Option[Constraint]) = {
        if(!isError) {
            optConstraint match {
                case Some(constraint) => constraint(vd, value) match {
                    case Some(error) => ErrorExpr(error)
                    case None => this
                }
                case None => this
            }
        }
        else this
    }

    override def toString = s"${sym} ${ss(value)}"

    def repr: String = {
        val r2 = repr2
        s"${r2._1}${r2._2}"
    }

    def repr2: (String, String) = {
        if(thype != null) {
            (sym, thype.repr(value))
        }
        else {
            (sym, ss(value, value.toString, ""))
        }
    }
}

final case class ErrorExpr(value: String) extends ValExpr {
    require(value != null)

    override val sym = ValExpr.errSym
    override val thype = null
    override def apply(src: ValExpr)(implicit vd: ValueData) =
        ValExpr.applyingError
    override def isError = true
    override def isAbsolute = false
    override def isSharp = false
}

final case class AbsoluteExpr(thype: Thype, value: Any) extends ValExpr {
    override val sym = ValExpr.absSym
    override def apply(src: ValExpr)(implicit vd: ValueData): ValExpr =
        AbsoluteExpr.this
    override def isError = false
    override def isAbsolute = true
    override def isSharp = false
}

final case class LambdaExpr(thype: Thype, value: String) extends ValExpr {
    private var compilerError: Option[ErrorExpr] = null
    private var compiledCode: CompiledScript = null
    private var bindings: Bindings = new SimpleBindings()

    override val sym = ValExpr.lamSym
    override def apply(src: ValExpr)(implicit vd: ValueData): ValExpr = {
        if(compilerError == null) {
            try {
                compiledCode = ValExpr.compiler.compile(value)
                compilerError = None
            }
            catch {
                case e: Throwable =>
                    compilerError = Some(ErrorExpr(e.getMessage()))
            }
        }
        compilerError match {
            case Some(errorExpr) => errorExpr
            case None => {
                if(!src.isAbsolute) {
                    ErrorExpr(s"Cannot apply $this to $src")
                }
                else {
                    try {
                        updateBindings(src.value, vd)
                        val rv = compiledCode.eval(bindings)
                        AbsoluteExpr(thype, rv)
                    }
                    catch {
                        case e@(_: ScriptException | _: ValExprInitException) =>
                            ErrorExpr(e.getMessage)
                    }
                }
            }
        }
    }
    
    private def updateBindings(x: Any, vd: ValueData) {
        bindings.clear();
        bindings.put("x", x);

        val prefix = "com.insweat.hssd.bindings."
        vd.element.attribs.foreach{e =>
            val (k, v) = e
            if(k.startsWith(prefix)) {
                val name = k.substring(prefix.length)
                
                if(bindings.containsKey(name)) {
                    val err = s"A binding named $name already exists."
                    throw new ValExprInitException(err)
                }
                
                val vspec = v.split("#")
                var ed = EntryData.of(vd.entryNode)
                for(i <- 0 until vspec.length - 1) {
                    val path = mkPath(vspec(i))
                    ed = ed.searchValueDataAt(path) match {
                        case Some(vd) if vd.path.eqLen(path) &&
                                vd.asRef.isDefined =>
                            vd.asRef.get
                        case _ =>
                            val err = s"Invalid vspec (X#...) for binding $name"
                            throw new ValExprInitException(err)
                    } 
                }

                val path = mkPath(vspec.last)
                ed.searchValueDataAt(path) match {
                    case Some(vd) if vd.path.eqLen(path) =>
                        if(vd.value.isError) {
                            val err = s"The value of binding $name is an error"
                            throw new ValExprInitException(err)
                        }
                        bindings.put(name, vd.value.value)
                    case _ =>
                        val err = s"Invalid vspec (...#X) for binding $name"
                        throw new ValExprInitException(err)
                }
            }
        }
    }
    
    private def mkPath(s: String): TreePath = {
        var path = TreePath.fromStr(s);
        if(path(0) != "Root" && path(0) != "*") {
            path = TreePath.fromStr("*.*." + path)
        }
        path
    }

    override def isError = false
    override def isAbsolute = false
    override def isSharp = false
}

final case class SharpExpr(thype: Thype, value: Any) extends ValExpr {
    override val sym = ValExpr.absSym
    override def apply(src: ValExpr)(implicit vd: ValueData): ValExpr = 
        ErrorExpr(s"Cannot apply $this.")

    override def isError = false
    override def isAbsolute = false
    override def isSharp = true
}


object ValExpr extends Enumeration {
    private val Pattern = """(?s)(\!\!|:=|=\>|#=)(.*)""".r

    val engine = new ScriptEngineManager().getEngineByName("javascript")
    def compiler = engine.asInstanceOf[Compilable]

    val unknownError = ErrorExpr("Unknown error");
    val applyingError = ErrorExpr("Applying an error expression.")

    val Error, Absolute, Lambda, Sharp = Value

    val errSym  = "!!"
    val absSym  = ":="
    val lamSym = "=>"
    val shpSym  = "#="

    def fmt(sym: String, value: String): String = s"$sym$value"

    def fmtErr(value: String) = fmt(errSym, value)

    def make(e: Value, thype: Thype, value: Any): ValExpr = e match {
        case Error => ErrorExpr(ss(value))
        case Absolute => AbsoluteExpr(thype, value)
        case Lambda => LambdaExpr(thype, value.asInstanceOf[String])
        case Sharp => SharpExpr(thype, value)
    }

    def make(e: String, thype: Thype, value: Any): ValExpr = e match {
        case `errSym` => ErrorExpr(ss(value))
        case `absSym` => AbsoluteExpr(thype, value)
        case `lamSym` => LambdaExpr(thype, value.asInstanceOf[String])
        case `shpSym` => SharpExpr(thype, value)
    }

    def preparse(s: String): (String, String) = s match {
        case Pattern(sym, value) => (sym, value)
        case _ => (absSym, s)
    }

    def parse(thype: Thype, s: String): ValExpr = {
        val sv = preparse(s)
        parse(thype, sv._1, sv._2)
    }

    def parse(thype: Thype, sym: String, value: String) = try {
        sym match {
            case `absSym` => make(Absolute, thype, thype.parse(value))
            case _ => make(sym, thype, value)
        }
    }
    catch {
        case e: NumberFormatException => 
            ErrorExpr(s"'$value' cannot be parsed into a proper number.")
        case e: Exception => ErrorExpr(e.toString)
    }
}
