package com.insweat.hssd.lib.essence

import com.insweat.hssd.lib.util._
import com.insweat.hssd.lib.constraints.Constraint
import com.insweat.hssd.lib.tree.EntryTree
import com.insweat.hssd.lib.tree.TreePath
import com.insweat.hssd.lib.tree.ValueTree
import com.insweat.hssd.lib.tree.ValueNode

import scala.collection.immutable.HashMap
import scala.ref.WeakReference


/**
 * Represents a type in the HSSD. It is named thype to avoid name conflict
 * with the type keyword in scala. You can pronounce it as T-[h]-ai-p.
 */
abstract class Thype(sch: SchemaLike) extends Qualified {
    private val weakSchema: WeakReference[SchemaLike] = WeakReference(sch)

    private var _rank: Long = 0
    private var isCompiled: Boolean = false

    schema.add(this)

    def name: String
    def description: String

    def schema: SchemaLike = weakSchema()

    def rank: Long = _rank

    /** parse(value_repr) -> value => [Core]
     */
    def parse(s: String): Any = throw new NotImplementedError(
            s"parse(String) is not implemented for ${getClass.getName}")

    /** repr(value) -> value_repr => [Persistency]
     */
    def repr(o: Any): String = ss(o, o.toString, "")

    /** interpOut(fixed(value_repr)) -> editor_value => [Editor]
     *  interpOut(value_repr) -> editor_valex_value => [Editor]
     *  parse(interpIn(editor_valex_value)) -> value => [Core]
     */
    def fixed(o: Any): Any = o

    def constraint: Option[Constraint] = None
 
    def print(out: String=>Unit) {
        out(toString)
    }

	def compile {
        if(!compiled) {
            this._rank = Thype.getNextRank()
            isCompiled = true
        }
    }

    def compiled: Boolean = isCompiled

    protected def parseNullOrValue(s: String)(doParse: String=>Any): Any = {
        if(s == null) null
        else {
            val trimed = s.trim
            if(trimed == "" || trimed == nullStr) null
            else doParse(trimed)
        }
    }

    override protected def computeQName: String = s"${schema.qname}.$name"

    override def toString = s"${getClass.getSimpleName}($name)"
}

trait SimpleThypeLike extends Thype {
    def editable = true
}

trait EnumThypeLike extends Thype with SimpleThypeLike with EnumLike {
}

trait ComplexThypeLike extends Thype {
	def elements: HashMap[String, Element]
}

trait DynamicThypeLike extends Thype {
}

trait CollectionThypeLike extends Thype with DynamicThypeLike {
    def elementThype: SimpleThypeLike

    /**
     * NB taking a ValueNode rather than (ValueTree, TreePath), because the
     *    path to vn is dynamic, we need to obtain vn.path from vn dynamically.
     */
    def makeElement(vn: ValueNode): ElementLike
}

trait TraitThypeLike extends Thype {
    val caption: String
}

object Thype {
    private var nextRank = 1L

    def getNextRank(): Long = {
        val rv = nextRank
        nextRank += 1
        rv
    }
}
