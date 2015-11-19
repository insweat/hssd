package com.insweat.hssd.lib.constraints

import com.insweat.hssd.lib.util._
import scala.math.Numeric
import com.insweat.hssd.lib.essence.ValueData
import scala.util.matching.Regex

private class Regexed private(pattern: Regex) extends Constraint {

	override val name = Regexed.name

	override def apply(vd: ValueData, value: Any): Option[String] = {
    value match {
      case null => None
      case s: String if pattern.unapplySeq(s).isDefined => None
      case _ =>
        Some(s"${ss(value)} does not match '${pattern.regex}'")
    }
	}
}

private object Regexed extends ConstraintFactory {
	val name = "com.insweat.hssd.constraints.regexed"

  def apply(attribs: Map[String, String]): Constraint = {
		val pattern = attribs.get(s"$name.pattern").getOrElse(".*")
		new Regexed(new Regex(pattern))
	}
}
