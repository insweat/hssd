package com.insweat.hssd.lib.essence.thypes

import com.insweat.hssd.lib.essence.Thype
import com.insweat.hssd.lib.essence.ComplexThypeLike
import com.insweat.hssd.lib.essence.Element
import com.insweat.hssd.lib.essence.SchemaLike
import scala.collection.immutable.HashMap
import com.insweat.hssd.lib.essence.SimpleThypeLike

class ComplexThype(
        sch: SchemaLike,
        override val name: String,
        override val description: String,
        val attributes: HashMap[String, String],
        elems: Element*
        ) extends Thype(sch) with ComplexThypeLike {

    override val elements = HashMap(elems map { e => e.name -> e}: _*)

    override def compile {
        if(!compiled) {
            elements.foreach{
                e => e._2.compile(this)
            }
            super.compile
        }
    }
}

