package com.insweat.hssd.lib.persistence.xml

import com.insweat.hssd.lib.util.Convert
import com.insweat.hssd.lib.util.logging.Logger
import com.insweat.hssd.lib.util.logging
import com.insweat.hssd.lib.persistence._
import scala.collection.immutable.HashMap
import scala.collection.mutable.Buffer
import scala.xml.XML
import scala.xml.Node

import java.net.URI
import java.io.File

class PlainEntry(
        val name: String,
        val id: Long,
        val parent: Long,
        val leaf: Boolean,
        val traits: List[String],
        val properties: List[(String, String, String)]) {

    private var _children: HashMap[Long, PlainEntry] = HashMap()
    private var _processed: Option[Boolean] = None

    private def addChild(node: PlainEntry) = {
        if(node.parent != id) {
            throw new IllegalArgumentException(
                    s"Node ${node.name} (${node.id}) with parentID " +
                    s"${node.parent} is not a child of node $name ($id).")
        }
        _children += node.id -> node
    }

    def children = _children

    def processed = _processed

    def process(
            entries: HashMap[Long, PlainEntry],
            roots: Buffer[PlainEntry],
            orphans: Buffer[PlainEntry]): Unit = {
        processed match {
            case Some(b) => // pass
            case None => {
                _processed = Some(false)
                if(parent == 0) {
                    roots += this
                    _processed = Some(true)
                }
                else {
                    entries.get(parent) match {
                        case Some(parent) =>
                            parent.addChild(this)
                            parent.process(entries, roots, orphans)
                            _processed = parent.processed
                        case None =>
                            orphans += this
                    }
                }
            }
        }
    }

    override def toString = s"Entry($name, $id)"
}

class XMLEntryLoader(override val uri: URI)
        extends ResourceLoader[PlainEntry] with Versioned {
    def this(file: File) = this(file.toURI())
    def this(path: String) = this(new File(path).toURI())

    override def load(optLog: Option[Logger]): Option[PlainEntry] = {
        val log = optLog getOrElse logging.root
        var rv: Option[PlainEntry] = None
        try{
            checkPrerequisites()
            
            val root = XML.load(uri.toURL())
            checkFormat(root, "Entry", None, None)
            
            val name = attrib(root, "name").get
            val id = longAttrib(root, "id")
            val parent = longAttrib(root, "parent")
            val leaf = boolAttrib(root, "leaf")
            val traits = getTraits(root)
            val props = getProperties(root)
            
            rv = Some(new PlainEntry(name, id, parent, leaf, traits, props))
        }
        catch {
            case e @ (_: SetupError | _: FileFormatError | _: SyntaxError) =>
                log.error("An exception occurred", "exception"->e)
            case e: Throwable =>
                log.critical(s"Failed to load $uri")
                throw e
        }
        rv
    }

    private def getTraits(root: Node): List[String] = {
        val traitElems = root \ "traits" \ "Trait"
        val traits = traitElems map { tr => attrib(tr, "name").get }
        traits.toList
    }

    private def getProperties(root: Node): List[(String, String, String)] = {
        val propElems = root \ "properties" \ "Property"
        val props = propElems map { p => (
                attrib(p, "path").get,
                attrib(p, "sym").get,
                attrib(p, "value").get)
        }
        props.toList
    }
}

class XMLEntrySaver(override val uri: URI)
        extends ResourceSaver[PlainEntry] with Versioned {
    override def save(optLog: Option[Logger], entry: PlainEntry) {
        val log = optLog getOrElse logging.root
        try {
            checkPrerequisites()
            val root = mkRoot(log, entry)
            xml.save(log, root, uri)
        }
        catch {
            case e @ (_: SetupError | _: FileFormatError | _: SyntaxError) =>
                log.error(e.getMessage)
            case e: Throwable =>
                log.critical(s"Failed to load $uri")
                throw e
        }
    }

    private def mkRoot(log: Logger, entry: PlainEntry): Node = {
        <Entry
            name={entry.name}
            id={entry.id.toString}
            parent={entry.parent.toString}
            leaf={entry.leaf.toString}
        >
            { mkTraits(log, entry.traits) }
            { mkProps(log, entry.properties) }
        </Entry>
    }

    private def mkTraits(log: Logger, traits: List[String]): Node = {
        <traits>
            { traits.map{tr => mkTrait(log, tr)} }
        </traits>
    }

    private def mkTrait(log: Logger, tr: String): Node = {
        <Trait name={tr}/>
    }

    private def mkProps(
            log: Logger, props: List[(String, String, String)]): Node = {
        <properties>
            { props.map{prop => mkProp(log, prop)} }
        </properties>
    }

    private def mkProp(log: Logger, prop: (String, String, String)): Node = {
        <Property path={prop._1} sym={prop._2} value={prop._3} />
    }
}
