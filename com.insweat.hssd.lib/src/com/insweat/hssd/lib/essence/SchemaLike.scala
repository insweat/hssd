package com.insweat.hssd.lib.essence

import scala.collection.immutable.HashMap

import com.insweat.hssd.lib.essence.thypes._
import com.insweat.hssd.lib.util._

class SchemaSetupError(msg: String, cause: Throwable = null)
    extends Exception(msg, cause) {
}

/**
 * Represents a namespace for thypes.
 * Schemas are managed in a hierarchy. They are searched from child to parent
 * for thypes. The first thype found will be returned.
 */
sealed abstract trait SchemaLike extends Qualified {

    private var _parent: Option[SchemaLike] = None
    private var _thypes: HashMap[String, Thype] = HashMap()
    private var pendingThypes: List[Thype] = Nil

    override protected def computeQName: String = parentQName match {
        case Some(parentSchemaQName) => s"${parentSchemaQName}.${name}"
        case None => name
    }
    
    def name: String
    
    def parentQName: Option[String]
    
    def builtin: SchemaLike

    def parent = _parent

    def thypes = _thypes

	def add(thype: Thype) {
        // NB Do not access thype.name here. Usually add is called in the 
        // constructor, and name may not be valid yet.
        pendingThypes ::= thype
    }

    def size = _thypes.size

    def isEmpty = _thypes.isEmpty

    def isPending = !pendingThypes.isEmpty

    def get(name: String): Option[Thype] = {
        hierarchyGet(name) or builtin.get(name) or dynamicGet(name)
    }

    def clear() {
        _thypes = HashMap()
        pendingThypes = Nil
    }

    def compile() {
        compile(None)
    }

    def compile(knownSchemas: Option[HashMap[String, SchemaLike]]) {
        if(parentQName.isDefined) {
            knownSchemas match {
                case Some(schMap) => schMap.get(parentQName.get) match {
                    case Some(psch) => _parent = Some(psch)
                    case None => raiseParentUnknownError()
                }
                case None => if(parent.isEmpty) {
                    raiseParentUnknownError()
                }
            }
        }

        pendingThypes.foreach{ t => register(t) }
        pendingThypes.foreach{ t => t.compile }
        pendingThypes = Nil
    }

    override def toString = s"Schema($qname)"

    private def raiseParentUnknownError() {
        throw new SchemaSetupError(
                s"$this: parent schema $parentQName is unknown.")
    }

    private def register(thype: Thype, allowDynamic: Boolean = false) {
        val name = thype.name
        if(_thypes contains name) {
            throw new SchemaSetupError(
                s"A thype named $name already exists.")
        }
        if(!allowDynamic && thype.isInstanceOf[DynamicThypeLike]) {
            throw new SchemaSetupError(
                s"Dynamic thype $name cannot be explicitly instantiated.")
        }
        _thypes += name -> thype
    }

    protected def localGet(name: String): Option[Thype] = _thypes.get(name)

    protected def hierarchyGet(name: String): Option[Thype] = {
        localGet(name) or { parent match {
            case Some(parentSchema) => parentSchema.hierarchyGet(name)
            case None => None
        }}
    }

    protected def dynamicGet(name: String): Option[Thype] = {
        var rv: Option[Thype] = None
        dynamicCompanions.find {
            d => {
                rv = d(this, name)
                rv.isDefined
            }
        }
        rv match {
            case Some(thype) => {
                register(thype, true)
                thype.compile
            }
            case None => // pass
        }
        rv
    }
}


final class BuiltinSchema extends SchemaLike {
    private var initialized = false

    override def name = "__builtin__"
    
    override def parentQName = None

    override def builtin = this

    override def thypes = {
        initialize()
        super.thypes
    }

    override def get(name: String): Option[Thype] = {
        initialize()
        localGet(name) or dynamicGet(name)
    }

    override def clear() {
        super.clear
        initialized = false
    }

    private def initialize() {
        if(!initialized) {
            initialized = true

            new BoolThype(this)
            new IntThype(this)
            new LongThype(this)
            new FloatThype(this)
            new DoubleThype(this)
            new RawStringThype(this)
            new StringThype(this)
            new RegexThype(this)
            new UrlThype(this)
            new LStringThype(this)
            new DateTimeThype(this)
            new TagThype(this)
            new RootThype(this)

            new EnumThype(this, "ETristate"
                , "Represents a three-state value."
                , HashMap()
                , EnumValue("Pending", "Pending", -1)
                , EnumValue("False", "False", 0)
                , EnumValue("True", "True", 1)
            )
    
            new TraitThype(this, "Entry", Some("*Entry*")
                , "The trait that any entry has."
                , HashMap()
                , Element("Caption"
                        , "caption", "LString", None
                        , "The literal name displayed to users."
                        , HashMap())
                , Element("Public"
                        , "isPublic", "Bool", Some("false")
                        , "Whether the entry is available to the public."
                        , HashMap())
                , Element("Tags"
                        , "tags", "Array[Ref[Tag]]", None
                        , "Tags assigned to the entry."
                        , HashMap())
            )

            compile()
        }
    }
}

class Schema(
        _name: String,
        _parentQName: Option[String]
    ) extends SchemaLike {

    override val name = _name
    
    override val parentQName = _parentQName
    
    override val builtin = new BuiltinSchema()    
}
