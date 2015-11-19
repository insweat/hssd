package com.insweat.hssd.lib.essence.thypes

import scala.ref.WeakReference
import com.insweat.hssd.lib.essence.CollectionThypeLike
import com.insweat.hssd.lib.essence.DynamicThypeCompanion
import com.insweat.hssd.lib.essence.PatternedThypeCompanion
import com.insweat.hssd.lib.essence.SchemaLike
import com.insweat.hssd.lib.essence.SimpleThypeLike
import com.insweat.hssd.lib.essence.Thype
import com.insweat.hssd.lib.essence.PatternedThypeCompanion
import com.insweat.hssd.lib.tree.TreePath
import com.insweat.hssd.lib.tree.ValueTree
import com.insweat.hssd.lib.essence.ArrayElement
import com.insweat.hssd.lib.essence.MapElement
import com.insweat.hssd.lib.essence.ElementLike
import com.insweat.hssd.lib.tree.flat.TreeNode
import com.insweat.hssd.lib.tree.ValueNode


class ArrayThype(schema: SchemaLike, elemThype: SimpleThypeLike)
        extends Thype(schema)
        with CollectionThypeLike {
    
    private val weakElemThype = WeakReference(elemThype)

    override def elementThype = weakElemThype()

    override def makeElement(vn: ValueNode) =
        new ArrayElement(vn.owner.asInstanceOf[ValueTree], vn.path, this)
    
    override val name = ArrayThype.mkName(elementThype.name)
    override val description = ArrayThype.mkDesc(elementThype.name)
}

class MapThype(schema: SchemaLike, elemThype: SimpleThypeLike)
        extends Thype(schema)
        with CollectionThypeLike {

    private val weakElemThype = WeakReference(elemThype)

    override def elementThype = weakElemThype()

    override def makeElement(vn: ValueNode) =
        new MapElement(vn.owner.asInstanceOf[ValueTree], vn.path, this)

    override val name = MapThype.mkName(elementThype.name)
    override val description = MapThype.mkDesc(elementThype.name)
}

object ArrayThype extends DynamicThypeCompanion with PatternedThypeCompanion {
    val Pattern = """Array\[(.+)\]""".r

    override def apply(sch: SchemaLike, inner: String): Option[ArrayThype] = {
        parseElemThype(sch, inner) match {
            case Some(simpleElemThype: SimpleThypeLike) => {
                // Only simple thypes are allowed.
                Some(new ArrayThype(sch, simpleElemThype))
            }
            case _ => None
        }
    }

    override def mkName(inner: String) = s"Array[${inner}]"
    override def mkDesc(inner: String) = s"An array of ${inner} elements."
}

object MapThype extends DynamicThypeCompanion with PatternedThypeCompanion {
    val Pattern = """Map\[(.+)\]""".r

    override def apply(sch: SchemaLike, inner: String): Option[MapThype] = {
        parseElemThype(sch, inner) match {
            case Some(simpleElemThype: SimpleThypeLike) => {
                // Only simple thypes are allowed.
                Some(new MapThype(sch, simpleElemThype))
            }
            case _ => None
        }
    }

    override def mkName(inner: String) = s"Map[${inner}]"
    override def mkDesc(inner: String) = s"A (String, ${inner}) map."
}
