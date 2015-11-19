package com.insweat.hssd.lib.essence.thypes

import com.insweat.hssd.lib.essence.SchemaLike
import com.insweat.hssd.lib.essence.SimpleThypeLike
import com.insweat.hssd.lib.essence.TraitThypeLike
import com.insweat.hssd.lib.essence.ComplexThypeLike
import com.insweat.hssd.lib.essence.Thype
import com.insweat.hssd.lib.essence.Element
import scala.collection.immutable.HashMap

class TagThype(sch: SchemaLike) extends ComplexThype(
        sch,
        "Tag",
        "Represents a tag, which is useful for Refs.",
        HashMap(),
        Element("Singleton", "singleton", "Bool", Some("false"),
                "If this tag denotes a singleton entry",
                HashMap())
        ) with TraitThypeLike {
    override val caption = "*Tag*"
}