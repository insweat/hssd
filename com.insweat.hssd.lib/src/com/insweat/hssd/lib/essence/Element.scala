package com.insweat.hssd.lib.essence

import com.insweat.hssd.lib.essence.thypes.RootThype
import com.insweat.hssd.lib
import com.insweat.hssd.lib.constraints
import com.insweat.hssd.lib.constraints.Constraint
import scala.collection.immutable.HashMap
import scala.collection.immutable.List
import scala.ref.WeakReference
import com.insweat.hssd.lib.tree.ValueTree
import com.insweat.hssd.lib.tree.TreePath

trait ElementLike {
    private var weakOuter: WeakReference[Thype] = null

    def outer: Thype = weakOuter.get match {
        case Some(t) => t
        case None => null
    }

    def caption: String = name

    def thype: Thype // There can be no circular reference between a thype 
                     // and an element, thus we do not need to weak ref a thype.

    def name: String
    
    def description: String

    def defaultValue: Option[Any] = None

    def constraints: List[Constraint] = Nil

    def attribs: Map[String, String] = Map.empty

    def compile(outer: Thype) {
        // we obtain outer here, so that it does not require the
        // construction of the outer to finish before the point the 
        // constructor is invoked.
        weakOuter = WeakReference(outer)
    }

    def compiled = weakOuter != null

    override def toString = s"${getClass.getSimpleName}($name)"
}

case class Element(
        override val caption: String,
        override val name: String,
        thypeStr: String,
        optDefValueStr: Option[String],
        override val description: String,
        override val attribs: Map[String, String]
    ) extends ElementLike {

    private var _thype: Thype = null
    private var _defaultValue: Option[Any] = None
    private var _constraints: List[Constraint] = Nil

    override def thype = _thype
    override def defaultValue = _defaultValue
    override def constraints = _constraints

    override def compile(outer: Thype) {
        super.compile(outer)

        outer.schema.get(thypeStr) match {
            case Some(t) => _thype = t
            case None =>
                throw new NoSuchElementException(s"No such thype: $thypeStr")
        }

        thype.compile

        optDefValueStr match {
            case Some(defValStr) => {
                if(thype.isInstanceOf[CollectionThypeLike]) {
                    val t = thype.asInstanceOf[CollectionThypeLike].elementThype
                    _defaultValue = Some(t.parse(defValStr))
                }
                else {
                    _defaultValue = Some(thype.parse(defValStr))
                }
            }
            case None => // pass
        }

        attribs.get("constraints") match {
            case Some(cons) => {
                cons.split(',').foreach{ name => 
                    _constraints ::= lib.constraints.apply(name.trim(), attribs)
                } 
            }
            case None => // pass
        }
    }

    override def toString = {
        val tag = if(outer != null) s"${outer.name}.Element" else "Element"

        optDefValueStr match {
            case Some(defValStr) => s"$tag($thypeStr $name = $defValStr)"
            case None => s"$tag($thypeStr $name)"
        }
    }
}

class CollectionElement(
        tree: ValueTree, path: => TreePath, _outer: CollectionThypeLike
        ) extends ElementLike {
    
    private val weakTree = WeakReference(tree)

    override def defaultValue: Option[Any] = parent.defaultValue
    override def constraints: List[Constraint] = parent.constraints

    override def outer: CollectionThypeLike = _outer 
    override def thype: SimpleThypeLike = outer.elementThype
    override def name = path.last
    override def description = outer.description
    
    private def parent: ElementLike = {
        val parentNode = weakTree.get.get.find(path.parent.get).get
        val parentVD = ValueData.of(parentNode)
        parentVD.element
    }
}

class ArrayElement(tree: ValueTree, path: => TreePath, _outer: CollectionThypeLike)
        extends CollectionElement(tree, path, _outer) {
}

class MapElement(tree: ValueTree, path: => TreePath, _outer: CollectionThypeLike)
        extends CollectionElement(tree, path, _outer) {
}

class RootNodeElement(val thype: RootThype, val name: String)
    extends ElementLike {
    override def description = name
}

class TraitNodeElement(val thype: TraitThypeLike) extends ElementLike {
    override def name = thype.name
    override def caption = thype.caption
    override def description = thype.description
}
