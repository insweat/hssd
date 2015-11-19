package com.insweat.hssd.lib.util

import scala.math.ScalaNumber

object Convert {
    def toInt(o: Any): Int = {
        val rv = Convert.toLong(o)
        if(LongValue.isInt(rv)) {
            rv.toInt
        }
        else {
            throw new ClassCastException(s"Cannot convert $o to an int value")
        }
    }

    def toLong(o: Any): Long = o match {
        case null => 0
        case value: Long => value
        case value: Int => value
        case value: Short => value
        case value: Byte => value
        case value: Char => value.toLong
        case value: Double => value.toLong
        case value: Float => value.toLong
        case value: ScalaNumber => value.longValue
        case value: String => value.toLong
        case _ =>
            throw new ClassCastException(s"Cannot convert $o to a long value")
    }

    def toFloat(o: Any): Float = {
        val rv = Convert.toDouble(o)
        if(DoubleValue.isFloat(rv)) {
            rv.toFloat
        }
        else {
            throw new ClassCastException(s"Cannot convert $o to a float value")
        }
    }
    
    def toDouble(o: Any): Double = o match {
        case null => 0.0
        case value: Double => value
        case value: Float => value
        case value: Long => value.toDouble
        case value: Int => value.toDouble
        case value: Short => value.toDouble
        case value: Byte => value.toDouble
        case value: Char => value.toDouble
        case value: ScalaNumber => value.doubleValue()
        case value: String => value.toDouble
        case _ =>
            throw new ClassCastException(s"Cannot convert $o to a double value")

    }
    
    def tryParseBoolean(s: String): Option[Boolean] = {
        try {
            Some(s.toBoolean)
        }
        catch {
            case e: Exception => None
        }
    }

    def tryParseByte(s: String): Option[Byte] = {
        try {
            Some(s.toByte)
        }
        catch {
            case e: Exception => None
        }
    }

    def tryParseShort(s: String): Option[Short] = {
        try {
            Some(s.toShort)
        }
        catch {
            case e: Exception => None
        }
    }

    def tryParseInt(s: String): Option[Int] = {
        try {
            Some(s.toInt)
        }
        catch {
            case e: Exception => None
        }
    }

    def tryParseLong(s: String): Option[Long] = {
        try {
            Some(s.toLong)
        }
        catch {
            case e: Exception => None
        }
    }
    
    def tryParseFloat(s: String): Option[Float] = {
        try {
            Some(s.toFloat)
        }
        catch {
            case e: Exception => None
        }
    }
    
    def tryParseDouble(s: String): Option[Double] = {
        try {
            Some(s.toDouble)
        }
        catch {
            case e: Exception => None
        }
    }
}

object LongValue {
    def unapply(o: Any): Option[Long] = try {
        val l = Convert.toLong(o)
        Some(l)
    }
    catch {
        case (_: ClassCastException | _: NumberFormatException) => None
    }
    
    def isInt(l: Long) = l >= Int.MinValue && l <= Int.MaxValue
}

object DoubleValue {
    def unapply(o: Any): Option[Double] = try {
        val d = Convert.toDouble(o)
        Some(d)
    }
    catch {
        case _: ClassCastException => None
    }
    
    def isFloat(d: Double) = 
        !d.isNaN && d >= Float.MinValue && d <= Float.MaxValue
}
