package com.insweat.hssd.lib.essence.thypes

import com.insweat.hssd.lib.essence.SimpleThypeLike
import com.insweat.hssd.lib.essence.Thype
import com.insweat.hssd.lib.essence.SchemaLike
import com.insweat.hssd.lib.constraints
import com.insweat.hssd.lib.essence.TraitThypeLike
import com.insweat.hssd.lib.util.Convert
import com.insweat.hssd.lib.util.ss
import com.insweat.hssd.lib.essence.Interpreted
import com.insweat.hssd.lib.tree.ValueNode
import com.insweat.hssd.lib.essence.ValueData
import scala.ref.WeakReference
import scala.collection.mutable.HashMap
import scala.collection.immutable.HashSet


class BoolThype(sch: SchemaLike) extends Thype(sch) with SimpleThypeLike {
    override val name = "Bool"
    override val description = "Represents booleans."

    override val constraint = Some(constraints.apply(
            "com.insweat.hssd.constraints.boolConstraint"))
    override def parse(s: String) = parseNullOrValue(s){
        trimed => trimed.toBoolean
    }
}

trait NumberThypeLike extends SimpleThypeLike {
    override val constraint = Some(constraints.apply(
            "com.insweat.hssd.constraints.numberConstraint"))
    override def parse(s: String) = parseNullOrValue(s){
        trimed => trimed.toDouble
    }
}

class IntThype(sch: SchemaLike) extends Thype(sch) with NumberThypeLike {
    override val name = "Int"
    override val description = "Represents integers."

    override def fixed(o: Any) = 
        if(o == null) null else Math.round(Convert.toDouble(o)).toInt
}

class LongThype(sch: SchemaLike) extends Thype(sch) with NumberThypeLike {
    override val name = "Long"
    override val description = "Represents long integers."

    override def fixed(o: Any) = 
        if(o == null) null else Math.round(Convert.toDouble(o))
}

class FloatThype(sch: SchemaLike) extends Thype(sch) with NumberThypeLike {
    override val name = "Float"
    override val description = "Represents floating points."

    override def fixed(o: Any) = 
        if(o == null) null else Convert.toDouble(o).toFloat
}

class DoubleThype(sch: SchemaLike) extends Thype(sch) with NumberThypeLike {
    override val name = "Double"
    override val description = "Represents double-precision floating points."
}

class RawStringThype(sch: SchemaLike) extends Thype(sch) with SimpleThypeLike {
    override val name = "RString"
    override val description = "Represents an untrimmed string."

    override def parse(s: String) = s
}

class StringThype(sch: SchemaLike) extends Thype(sch) with SimpleThypeLike {
    override val name = "String"
    override val description = "Represents a normal string, always trimmed."

    override def parse(s: String) = parseNullOrValue(s){ s => s.trim }
}

class RegexThype(sch: SchemaLike) extends StringThype(sch) {
    override val name = "Regex"
    override val description = "Represents a regular expression string."
}

class UrlThype(sch: SchemaLike) extends StringThype(sch) {
    override val name = "Url"
    override val description = "Represents an URL string."
}

class DateTimeThype(sch: SchemaLike) extends StringThype(sch) {
    override val name = "DateTime"
    override val description = "Represents an DateTime string " +
        "in format 'yyyy-MM-dd HH:mm:ss' or '+N'."
    
    override val constraint = Some(constraints.apply(
            "com.insweat.hssd.constraints.dateTimeConstraint"))
}

class LStringThype(sch: SchemaLike) extends LongThype(sch) with Interpreted {

    private var _lang: String = LStringThype.DEFAULT_LANG
    private val _strings: HashMap[Long, HashMap[String, String]] = HashMap()
    private var _allLangs: HashSet[String] = HashSet()

    override val name = "LString"
    override val description = "Represents a localized string."

    override def interpOut(ctx: Any, intVal: Any): Either[String, Any] = {
        if(intVal == null) {
            Right(null)
        }
        else {
            val stringID = fixed(intVal).asInstanceOf[Long]
            get(stringID) match {
                case None => Left(s"Invalid internal value: $intVal")
                case Some(text) => Right(text)
            }
        }
    }
    
    override def interpIn(ctx: Any, extVal: Any): Either[String, Any] = {
        if(extVal == null) {
            Right(null)
        }
        else {
            val vn = ctx.asInstanceOf[ValueNode]
            val vd = ValueData.of(vn)
            
            vd.valex match {
                case (valex, true) => Left(s"Cannot change default value: $ctx")
                case (valex, false) =>
                    val stringID = fixed(valex.get.value).asInstanceOf[Long]
                    set(stringID, ss(extVal, extVal.toString, ""))
                    Right(stringID)
            }            
        }
    }

    def lang: String = _lang

    def lang_=(l: String) { _lang = l }

    def allLangs: Array[String] = {
        var rv = _allLangs
        if(!rv.contains(LStringThype.DEFAULT_LANG)) {
            rv += LStringThype.DEFAULT_LANG
        }
        rv.toArray
    }

    def apply(stringID: Long): String = 
        get(stringID).getOrElse(LStringThype.UNLOCALIZED_STR)

    def apply(stringID: Long, lang: String): String =
        get(stringID, lang).getOrElse(LStringThype.UNLOCALIZED_STR)

    def get(stringID: Long): Option[String] = get(stringID, _lang)

    def get(stringID: Long, lang: String): Option[String] = {
        _strings.get(stringID) match {
            case Some(map) => map.get(lang)
            case None => None
        }
    }

    def update(stringID: Long, string: String) {
        set(stringID, string)
    }

    def update(stringID: Long, lang: String, string: String) {
        set(stringID, lang, string)
    }

    def set(stringID: Long, string: String) {
        set(stringID, _lang, string)
    }

    def set(stringID: Long, lang: String, string: String) {
        _strings.get(stringID) match {
            case Some(map) => map(lang) = string
            case None => _strings(stringID) = HashMap(lang -> string)
        }

        _allLangs += lang
    }

    def allStringIDs: Array[Long] = {
        /*
        // DEBUG
        if(_strings.isEmpty) {
            for(i <- 0 until 1000) {
                set(500000 + i, LStringThype.DEFAULT_LANG, s"Hello $i")
            }
        }
        */
        _strings.keys.toArray
    }
}

class RootThype(sch: SchemaLike) extends Thype(sch) with SimpleThypeLike {

    override val name = RootThype.name
    override val description = "The nominal thype used by root elements."
}

object RootThype {
    val name = "__root__"

    def get(sch: SchemaLike) = sch.get(name).get.asInstanceOf[RootThype]
}

object LStringThype {
    val DEFAULT_LANG = "zh"
    val UNLOCALIZED_STR = "<undefined>"
}
