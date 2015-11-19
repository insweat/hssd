package com.insweat.hssd.lib.constraints

import com.insweat.hssd.lib.essence.ValueData

trait Constraint {
    val name: String
    def apply(vd: ValueData, value: Any): Option[String]
}

trait ConstraintFactory {
    val name: String
    def apply(attribs: Map[String, String]): Constraint
}

class ConstraintSetupError(msg: String, cause: Throwable = null)
    extends Exception(msg, cause) {
}
