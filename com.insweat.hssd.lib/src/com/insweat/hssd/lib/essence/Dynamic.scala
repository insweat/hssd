package com.insweat.hssd.lib.essence

import com.insweat.hssd.lib.essence.thypes.ArrayThype
import com.insweat.hssd.lib.essence.thypes.ReferenceThype
import com.insweat.hssd.lib.essence.thypes.MapThype
import scala.util.matching.Regex

/**
 * The trait for companion objects of types that are not defined in schema
 * files, but are dynamically created. Examples are References, Arrays, and
 * Maps.
 */
trait DynamicThypeCompanion {
    def apply(sch: SchemaLike, inner: String): Option[Thype]

    def mkName(inner: String): String

    def mkDesc(inner: String): String
}

/**
 * The trait for companion objects of types whose names match a given pattern.
 */
trait PatternedThypeCompanion {
    val Pattern: Regex

    def parseElemThype(sch: SchemaLike, name: String): Option[Thype] = {
        name match {
            case Pattern(elemThypeName) => sch.get(elemThypeName)
            case _ => None
        }   
    }
}
