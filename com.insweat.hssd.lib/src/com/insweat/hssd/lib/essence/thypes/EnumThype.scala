package com.insweat.hssd.lib.essence.thypes

import com.insweat.hssd.lib.essence.EnumThypeLike
import com.insweat.hssd.lib.essence.Thype
import com.insweat.hssd.lib.essence.SchemaLike
import com.insweat.hssd.lib.essence.EnumValue
import scala.collection.immutable.HashMap

class EnumThype(sch: SchemaLike, 
        val name: String,
        val description: String,
        val attributes: HashMap[String, String],
        elemVals: EnumValue*) 
    extends Thype(sch)
    with EnumThypeLike {

    override def parse(s: String): Any = parseNullOrValue(s){ trimed =>
        elemVals.find{ e => e.value == trimed } match {
            case Some(e) => e
            case None => throw new NoSuchElementException(
                    s"No such element '$trimed' in $this")
        }
    }

    override def repr(o: Any): String = reprValue(o)
    
    override def fixed(o: Any): Any = 
      if(o == null) null else o.asInstanceOf[EnumValue].value

    override def values(context: Any) = elemVals.sortWith{(x, y) => 
        x.name.compareToIgnoreCase(y.name) < 0
    }.toIndexedSeq
}
