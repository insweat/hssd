package com.insweat.hssd.lib.essence.thypes

import com.insweat.hssd.lib.essence.TraitThypeLike
import com.insweat.hssd.lib.essence.Thype
import com.insweat.hssd.lib.essence.SchemaLike
import com.insweat.hssd.lib.essence.ComplexThypeLike
import com.insweat.hssd.lib.essence.Element
import com.insweat.hssd.lib.essence.SimpleThypeLike

import scala.collection.immutable.HashMap

class TraitThype(
        sch: SchemaLike, 
        override val name: String,
        _caption: Option[String],
        override val description: String,
        override val attributes: HashMap[String, String],
        elems: Element*) 
    extends ComplexThype(sch, name, description, attributes, elems: _*)
    with TraitThypeLike {

    override val caption = _caption getOrElse name
}
