package com.insweat.hssd.lib

import com.insweat.hssd.lib.util._
import scala.collection.immutable.HashMap
import com.insweat.hssd.lib.essence.ValueData
import java.text.SimpleDateFormat
import java.text.ParseException
import com.insweat.hssd.lib.essence.SimpleThypeLike

package object constraints {
    private val builtin = List(
            BoolConstraint, 
            NumberConstraint,
            DateTimeConstraint,
            NotNull,
            RangedInt,
            RangedLong,
            RangedFloat,
            RangedDouble,
            RangedDateTime,
            Regexed
            )

    private var _factories: Map[String, ConstraintFactory] = {
        HashMap((builtin map {e => e.name -> e}): _*)
    }

    private val dateTimeFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    dateTimeFmt.setLenient(false)

    private object NotNull extends Constraint with ConstraintFactory {
        val name = "com.insweat.hssd.constraints.notNull"
    
        override def apply(vd: ValueData, value: Any): Option[String] = {
            if(vd.entryNode.isLeaf 
                    && vd.element.thype.isInstanceOf[SimpleThypeLike]) {
                value match {
                    case null => Some("Value cannot be null.")
                    case _ => None
                }   
            }
            else None
        }
        
        override def apply(attribs: Map[String, String]): Constraint = this
    }

    private object BoolConstraint 
            extends Constraint
            with ConstraintFactory {
        val name = "com.insweat.hssd.constraints.boolConstraint"

        override def apply(vd: ValueData, value: Any): Option[String] = {
            if(value.isInstanceOf[Boolean]) None
            else if(value == None) None
            else Some(s"${ss(value)} is not a Boolean.")
        }

        override def apply(attribs: Map[String, String]): Constraint = this
    }

    private object NumberConstraint extends Constraint with ConstraintFactory {
        val name = "com.insweat.hssd.constraints.numberConstraint"
        override def apply(vd: ValueData, value: Any): Option[String] = {
            if(value == None) None
            else {
                val d = Convert.toDouble(value)
                if(!d.isNaN) None else Some(s"${ss(value)} is not a Number.")
            }
        }

        override def apply(attribs: Map[String, String]): Constraint = this
    }

    private object DateTimeConstraint extends Constraint with ConstraintFactory {
        val name = "com.insweat.hssd.constraints.dateTimeConstraint"

        override def apply(vd: ValueData, value: Any): Option[String] = {
            value match {
                case null => None
                case None => None
                case s: String => {
                    if(s.startsWith("+") &&
                            s.length() >= 2 &&
                            Character.isDigit(s.charAt(1))) {
                        try {
                            val i = Integer.parseInt(s.substring(1))
                            if(i < 0) {
                                mkError(value)
                            }
                            else {
                                None
                            }
                        }
                        catch {
                            case e: Exception => mkError(value)
                        }
                    }
                    else {
                        try {
                            toUnixTime(s)
                            None
                        }
                        catch {
                            case e : ParseException => mkError(value)
                        }
                    }
                }
            }
        }
        
        private def mkError(value: Any) = 
            Some(s"${ss(value)} is not a valid date-time string")

        override def apply(attribs: Map[String, String]): Constraint = this
    }

    def register(cf: ConstraintFactory) {
        if(_factories.contains(cf.name)) {
            throw new ConstraintSetupError(
                    s"A constraint named ${cf.name} already exists.")
        }
        _factories += cf.name -> cf
    }

    def apply(name: String): Constraint = apply(name, Map.empty)

    def apply(name: String, attribs: Map[String, String]) = {
        _factories.get(name) match {
            case Some(factory) => factory(attribs)
            case None => 
                throw new NoSuchElementException(s"No constraint named $name")
        }
    }

    def toUnixTime(s: String): Long = dateTimeFmt.parse(s).getTime / 1000
}
