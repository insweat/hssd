package com.insweat.hssd.lib.essence.thypes

import com.insweat.hssd.lib.essence.DynamicThypeCompanion
import com.insweat.hssd.lib.essence.DynamicThypeLike
import com.insweat.hssd.lib.essence.EntryData
import com.insweat.hssd.lib.essence.EnumLike
import com.insweat.hssd.lib.essence.EnumValue
import com.insweat.hssd.lib.essence.PatternedThypeCompanion
import com.insweat.hssd.lib.essence.SchemaLike
import com.insweat.hssd.lib.essence.TraitThypeLike
import com.insweat.hssd.lib.essence.ValueData
import com.insweat.hssd.lib.tree.EntryNode
import com.insweat.hssd.lib.tree.EntryTree
import com.insweat.hssd.lib.tree.ValueNode
import scala.collection.mutable.ArrayBuffer
import scala.ref.WeakReference
import scala.util.control.Breaks

class ReferenceThype(sch: SchemaLike, traitThype: TraitThypeLike) 
    extends LongThype(sch) 
    with DynamicThypeLike 
    with EnumLike {

    private val weakTraitThype = WeakReference(traitThype)

    def elementThype = weakTraitThype()

    override def values(context: Any): IndexedSeq[EnumValue] = {
        val node = context.asInstanceOf[ValueNode]
        val vd = ValueData.of(node)
        val entryTree = vd.entryNode.owner.asInstanceOf[EntryTree]
        val rv: ArrayBuffer[EnumValue] = ArrayBuffer()
        // TODO
        // Now we organize schema in a hierarchy, we cannot tell if a request
        // is valid simply from the equality of schemas.
        /*
        if(entryTree.schema != schema) {
            throw new IllegalArgumentException(
                    s"EntryTree $entryTree is incompatible")
        }
        */
        entryTree.foreach{
            en => {
                if(en.isLeaf) {
                    val ed = EntryData.of(en.asInstanceOf[EntryNode])
                    if(ed.hasTrait(traitThype)) {
                        rv += EnumValue(en.name,
                                s"A reference to ${en.name} ({$ed.entryID}).",
                                ed.entryID)
                    }
                }
            }
        }
        rv.sortWith{(x, y) =>
            x.name.compareToIgnoreCase(y.name) < 0
        }.toIndexedSeq
    }

    override def interpOut(ctx: Any, intVal: Any): Either[String, Any] = {
        if(intVal == null) {
            Right(null)
        }
        else {
            val node = ctx.asInstanceOf[ValueNode]
            val vd = ValueData.of(node)
            val entryTree = vd.entryNode.owner.asInstanceOf[EntryTree]
            val entryID = intVal match {
                case ev: EnumValue => fixed(ev.value).asInstanceOf[Long]
                case _ => fixed(intVal).asInstanceOf[Long]
            }

            entryTree.nodesByID.get(entryID) match {
                case Some(en) =>
                    if(EntryData.of(en).isPublic) {
                        Right(en.name)
                    }
                    else {
                        Left(s"${en.name} - but NOT public")
                    }
                case None =>
                    Left(s"Invalid internal value: $intVal")
            }
        }
    }

    override def compile {
        traitThype.compile
        super.compile
    }

    override val name = ReferenceThype.mkName(elementThype.name)
    override val description = ReferenceThype.mkDesc(elementThype.name)

    override def repr(o: Any): String = reprValue(o)
    
    private def forEachCandidate[U](entries: EntryTree)(handler: EnumValue=>U) =
        entries.foreach{
            en => {
                if(en.isLeaf) {
                    val ed = EntryData.of(en.asInstanceOf[EntryNode])
                    if(ed.hasTrait(traitThype)) {
                        val rv = EnumValue(en.name,
                                "A reference to ${en.name} ({$ed.entryID}).",
                                ed.entryID)
                        handler(rv)
                    }
                }
            }
        }
}

object ReferenceThype extends DynamicThypeCompanion
                      with PatternedThypeCompanion {
    val Pattern = """Ref\[(.+)\]""".r

    override def apply(sch: SchemaLike, name: String)
            : Option[ReferenceThype] = {
        parseElemThype(sch, name) match {
            case Some(elemThype) => if(elemThype.isInstanceOf[TraitThypeLike]) {
                Some(new ReferenceThype(sch, 
                        elemThype.asInstanceOf[TraitThypeLike]))
            }
            else {
                None
            }
            case None => None
        }
    }

    override def mkName(inner: String) = s"Ref[$inner]"
    override def mkDesc(inner: String) = 
        s"A reference to an instance of trait $inner."
}
