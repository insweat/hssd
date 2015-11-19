package com.insweat.hssd.lib.essence

import com.insweat.hssd.lib.util._
import scala.util.control.Breaks

/**
 * @param name: The name of this EnumValue, which is usually displayed to users
 * @param description: Describing what this EnumValue is
 * @param value: The actual value of this EnumValue. This is used and stored
 *               internally. 
 */
case class EnumValue(name: String, description: String, value: Any) {
    def this(name: String, description: String) {
        this(name, description, name)
    }

    def this(name: String) {
        this(name, "", name)
    }

    override def toString = s"EnumValue($name,$description,$value)"

    def repr = ss(value, value.toString, "")
}

trait EnumLike extends Interpreted {
    def values(context: Any): IndexedSeq[EnumValue]

    def reprValue(o: Any): String = o match {
        case null => ""
        case ev: EnumValue => ev.repr
        case e => e.toString
    }

    override def interpOut(ctx: Any, intVal: Any): Either[String, Any] = {
        if(intVal == null) {
            Right(null)
        }
        else {
            val hay = values(ctx)
            val needle = if(intVal.isInstanceOf[EnumValue]) {
                intVal.asInstanceOf[EnumValue].value
            }
            else {
                intVal
            }
            hay.find(ev => ev.value == needle) match {
                case Some(ev) => Right(ev.name)
                case None => Left(s"Invalid internal value: $intVal")
            }
        }
    }
    
    override def interpIn(ctx: Any, extVal: Any): Either[String, Any] = {
        if(extVal == null) {
            Right(null)
        }
        else {
            val hay = values(ctx)
            val needle = if(extVal.isInstanceOf[String]) {
                extVal.asInstanceOf[String].trim()
            }
            else {
                extVal
            }
            hay.find(ev => ev.name == needle) match {
                case Some(ev) => Right(ev.value)
                case None => Left(s"Invalid external value: $extVal")
            }
        }
    }
}
