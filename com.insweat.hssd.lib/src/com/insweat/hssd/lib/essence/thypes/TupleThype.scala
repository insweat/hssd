package com.insweat.hssd.lib.essence.thypes

import com.insweat.hssd.lib.essence.SchemaLike
import com.insweat.hssd.lib.essence.SimpleThypeLike
import com.insweat.hssd.lib.essence.Element
import scala.collection.immutable.HashMap
import com.insweat.hssd.lib.essence.Thype
import scala.xml.XML
import scala.xml.Utility
import com.insweat.hssd.lib.constraints.Constraint
import com.insweat.hssd.lib.essence.ValueData

class TupleThype(
        sch: SchemaLike,
        override val name: String,
        override val description: String,
        val attributes: HashMap[String, String],
        elems: Element*
        ) extends Thype(sch) with SimpleThypeLike {

    val elements = HashMap(elems map { e => e.name -> e}: _*)


    override val constraint: Option[Constraint] = Some(TupleConstraint)


    /**
     * Deserializes s into a value, from a string in the format:
     * 
     *     'key1="val1" key2="val2" ...'
     * 
     * where val1, val2, ... are XML-escaped.
     * 
     * If enclosed with any XML tag, this string makes a valid XML element.
     * For example, <Tuple key1="val1" key2="val2" ... /> is a valid XML
     * element.
     */
    override def parse(s: String): Any = parseNullOrValue(s) { trimmed =>
        val root = XML.loadString(s"<Tuple $trimmed />")
        elements.map { e =>
            val (name, elem) = e
            val valueStr = root.attribute(name) match {
                case Some(seq) => seq.headOption match {
                    case Some(attr) => attr.text
                    case None => null
                }
                case None => null
            }
            var value = elem.thype.parse(valueStr)
            if(value == null) {
                value = elem.defaultValue.getOrElse(null)
            }
            name -> value
        }
    }


    /**
     * Serializes o, the value, to a string in the format:
     * 
     *     'key1="val1" key2="val2" ...'
     *     
     * where val1, val2, ... are XML-escaped.
     *
     * This string is finally persisted.
     *
     * @See parse for why val1, val2, ... need to be escaped
     */
    override def repr(o: Any): String = if(o == null) "" else {
        val values = o.asInstanceOf[HashMap[String, Any]]
        val keys = elements.keySet.toArray.sorted
        val pairs = keys.map { key =>
            val elem = elements.get(key).get
            val thype = elem.thype
            val valueRepr = thype.repr(values.get(key).getOrElse(null))
            s"""$key="${Utility.escape(valueRepr)}""""
        }
        pairs.mkString(" ")
    }


    override def fixed(o: Any): Any = if(o == null) null else {
        val values = o.asInstanceOf[HashMap[String, Any]]
        elements.map { e =>
            val (name, elem) = e
            val thype = elem.thype
            name -> thype.fixed(values.get(name).getOrElse(null))
        }
    }


    override def compile {
        if(!compiled) {
            elements.foreach{ e =>
                val (name, elem) = e
                elem.compile(this)
                if(!elem.thype.isInstanceOf[SimpleThypeLike]) {
                    throw new IllegalArgumentException(
                            "A tuple can only contain simple thypes, " +
                            s"got ${elem.thype} in $this.")
                }
                if(elem.thype.isInstanceOf[TupleThype]) {
                    throw new IllegalArgumentException(
                            "A tuple cannot contain other tuples, " +
                            s"got ${elem.thype} in $this.")
                }
            }
            super.compile
        }
    }
}

private object TupleConstraint extends Constraint {
    override val name = "com.insweat.hssd.constraints.tupleConstraint"

    override def apply(vd: ValueData, value: Any): Option[String] = {
        if(value == null) None else {
            val thype = vd.element.thype.asInstanceOf[TupleThype]
            val values = value.asInstanceOf[HashMap[String, Any]]
    
            var errors: List[String] = Nil
            thype.elements.foreach { e =>
                val (name, elem) = e
                val value = values.get(name).getOrElse(null)
                elem.thype.constraint match {
                    case Some(c) => c.apply(vd, value) match {
                        case Some(error) => errors ::= error
                        case None => // pass
                    }
                    case None => // pass
                }
                elem.constraints.foreach{ c =>
                    c.apply(vd, value) match {
                        case Some(error) => errors ::= error
                        case None => // pass
                    }
                }
            }
    
            if(!errors.isEmpty) {
                Some(errors.mkString(","))
            }
            else {
                None
            }            
        }
    }
}
