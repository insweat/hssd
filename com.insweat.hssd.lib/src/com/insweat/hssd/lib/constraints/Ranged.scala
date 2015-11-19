package com.insweat.hssd.lib.constraints

import com.insweat.hssd.lib.util._
import scala.math.Numeric
import com.insweat.hssd.lib.essence.ValueData
import java.text.SimpleDateFormat

private case class Range[T] (
        optMin: Option[T], optMax: Option[T]
        ) (implicit ev: Numeric[T]) {

    import ev._
    
    if(optMin.isDefined && optMax.isDefined && optMin.get > optMax.get) {
        throw new IllegalArgumentException(
            s"Invalid range: $this")
    }

    override def toString = {
        val minStr = optMin.map(_.toString).getOrElse("-inf")
        val maxStr = optMax.map(_.toString).getOrElse("+inf")
        s"[$minStr, $maxStr]"
    }

    def isWithinBoundaries(value: T) = 
        (optMin.isEmpty || value >= optMin.get) &&
        (optMax.isEmpty || value <= optMax.get)

    def formatError(value: Any) = s"$value is out of range $this"
}

private class RangedInt private(range: Range[Int]) extends Constraint {
    override val name = RangedInt.name
    override def apply(vd: ValueData, value: Any): Option[String] = {
        value match {
            case null => None
            case LongValue(l) => {
                if(LongValue.isInt(l) && range.isWithinBoundaries(l.toInt)) {
                    None
                }
                else {
                    Some(range formatError value)
                }
            }
            case _ =>
                Some(s"${ss(value)} cannot be converted to an Int")
        }    
    }
}

private class RangedLong private(range: Range[Long]) extends Constraint {
    override val name = RangedLong.name
    override def apply(vd: ValueData, value: Any): Option[String] = {
        value match {
            case null => None
            case LongValue(l) =>  {
                if(range.isWithinBoundaries(l)) {
                    None
                }
                else {
                    Some(range formatError value)
                }
            }
            case _ =>
                Some(s"${ss(value)} cannot be converted to a Long")
        }    
    }
}

private class RangedFloat private(range: Range[Float]) extends Constraint {
    override val name = RangedFloat.name
    override def apply(vd: ValueData, value: Any): Option[String] = {
        value match {
            case null => None
            case DoubleValue(d) => {
                if(DoubleValue.isFloat(d) &&
                        range.isWithinBoundaries(d.toFloat)) {
                    None
                }
                else {
                    Some(range formatError value)
                }
            }
            case _ =>
                Some(s"${ss(value)} cannot be converted to a Float")
        }   
    }
}

private class RangedDouble private(range: Range[Double]) extends Constraint {
    override val name = RangedDouble.name
    override def apply(vd: ValueData, value: Any): Option[String] = {
        value match {
            case null => None
            case DoubleValue(d) =>  {
                if(range.isWithinBoundaries(d)) {
                    None
                }
                else {
                    Some(range formatError value)
                }
            }
            case _ =>
                Some(s"${ss(value)} cannot be converted to a Double")
        }   
    }
}

private class RangedDateTime private(
        optMin: Option[String], optMax: Option[String]
    ) extends Constraint {
    override val name = RangedDateTime.name
    val range = Range(optMin.map(toUnixTime(_)), optMax.map(toUnixTime(_)))

    override def apply(vd: ValueData, value: Any): Option[String] = {
        value match {
            case null => None
            case LongValue(l) => validate(value, l)
            case s: String =>
                val l = toUnixTime(s)
                validate(value, l)
            case _ =>
                Some(s"${ss(value)} cannot be converted to a Long")
        }
    }
    
    private def validate(value: Any, l: Long): Option[String] = {
        if(range.isWithinBoundaries(l)) {
            None
        }
        else {
            val s = ss(value)
            val sMin = optMin.getOrElse("-inf")
            val sMax = optMax.getOrElse("+inf")
            Some(s"$s is out of range [$sMin, $sMax]")
        }
    }
}

private object RangedInt extends ConstraintFactory {
    val name = "com.insweat.hssd.constraints.rangedInt"

    def apply(attribs: Map[String, String]): Constraint = {
        val optMin = get(attribs, s"$name.min")
        val optMax = get(attribs, s"$name.max")
        new RangedInt(Range(optMin, optMax))
    }

    private def get(attribs: Map[String, String], key: String): Option[Int] = {
        attribs.get(key).map(Convert.toInt(_))
    }
}

private object RangedLong extends ConstraintFactory {
    val name = "com.insweat.hssd.constraints.rangedLong"

    def apply(attribs: Map[String, String]): Constraint = {
        val optMin = get(attribs, s"$name.min")
        val optMax = get(attribs, s"$name.max")
        new RangedLong(Range(optMin, optMax))
    }

    private def get(attribs: Map[String, String], key: String): Option[Long] = {
        attribs.get(key).map(Convert.toLong(_))
    }
}

private object RangedFloat extends ConstraintFactory {
    val name = "com.insweat.hssd.constraints.rangedFloat"

    def apply(attribs: Map[String, String]): Constraint = {
        val optMin = get(attribs, s"$name.min")
        val optMax = get(attribs, s"$name.max")
        new RangedFloat(Range(optMin, optMax))
    }

    private def get(
            attribs: Map[String, String], key: String): Option[Float] = {
        attribs.get(key).map(Convert.toFloat(_))
    }
}

private object RangedDouble extends ConstraintFactory {
    val name = "com.insweat.hssd.constraints.rangedDouble"

    def apply(attribs: Map[String, String]): Constraint = {
        val optMin = get(attribs, s"$name.min")
        val optMax = get(attribs, s"$name.max")
        new RangedDouble(Range(optMin, optMax))
    }

    private def get(
            attribs: Map[String, String], key: String): Option[Double] = {
        attribs.get(key).map(Convert.toDouble(_))
    }
}

private object RangedDateTime extends ConstraintFactory {
    val name = "com.insweat.hssd.constraints.rangedDateTime"

    def apply(attribs: Map[String, String]): Constraint = {
        val optMin = get(attribs, s"$name.min")
        val optMax = get(attribs, s"$name.max")
        new RangedDateTime(optMin, optMax)
    }

    private def get(
            attribs: Map[String, String], key: String): Option[String] = {
        attribs.get(key).map{ str =>
            toUnixTime(str)
            str
        }
    }
}