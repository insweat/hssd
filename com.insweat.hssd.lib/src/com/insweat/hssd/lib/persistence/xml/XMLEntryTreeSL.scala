package com.insweat.hssd.lib.persistence.xml

import com.insweat.hssd.lib.essence.Thype
import com.insweat.hssd.lib.essence.EntryData
import com.insweat.hssd.lib.essence.ValueData
import com.insweat.hssd.lib.essence.ValExpr
import com.insweat.hssd.lib.essence.SchemaLike
import com.insweat.hssd.lib.essence.TraitThypeLike
import com.insweat.hssd.lib.util.logging.Logger
import com.insweat.hssd.lib.util.logging
import com.insweat.hssd.lib.util.ss
import com.insweat.hssd.lib.tree.EntryNode
import com.insweat.hssd.lib.tree.EntryTree
import com.insweat.hssd.lib.tree.TreePath
import com.insweat.hssd.lib.tree.structured
import com.insweat.hssd.lib.tree.TreeDataLike
import com.insweat.hssd.lib.tree.structured.TreeNode
import com.insweat.hssd.lib.persistence._
import scala.collection.immutable.HashMap
import scala.collection.mutable.Buffer
import java.net.URI
import java.io.File
import scala.xml.XML
import com.insweat.hssd.lib.tree.ValueNode
import scala.annotation.tailrec
import com.insweat.hssd.lib.tree.ValueTree
import com.insweat.hssd.lib.essence.CollectionThypeLike
import java.nio.file.Files
import com.insweat.hssd.lib.essence.thypes.TupleThype

trait BaseXMLEntryTreeSL extends BaseSL {
    def loadPlainEntries(log: Logger): HashMap[Long, PlainEntry] = {
        var entries: HashMap[Long, PlainEntry] = HashMap()
        walk(new File(uri)) {
            f => {
                if(f.getName().toLowerCase().endsWith(".xml")){
                    val entryLoader = new XMLEntryLoader(f)
                    entryLoader.load(Some(log)) match {
                        case Some(entry) => entries += entry.id -> entry
                        case None => // pass
                    }
                }
            }
        }
        entries
    }
}

class XMLEntryTreeLoader(override val uri: URI, sch: SchemaLike)
        extends ResourceLoader[EntryTree]
        with Versioned
        with BaseXMLEntryTreeSL {
    def this(file: File, sch: SchemaLike) = this(file.toURI(), sch)
    def this(path: String, sch: SchemaLike) = this(new File(path).toURI(), sch)

    override def load(optLog: Option[Logger]): Option[EntryTree] = {
        val log = optLog getOrElse logging.root
        var rv: Option[EntryTree] = None
        try {
            checkPrerequisites()
            val (entries, roots, orphans) = loadEntries(log)
            handleRoots(log, roots)
            handleOrphans(log, orphans)
            roots.headOption match {
                case Some(root) => {
                    val entryTree = new EntryTree(sch)
                    grow(log, entryTree, None, root);
                    rv = Some(entryTree)
                }
                case None => // pass
            }
        }
        catch {
            case e @ (_: SetupError) =>
                log.error("An exception occurred", "exception" -> e)
        }
        rv
    }

    private def loadEntries(log: Logger): (
            HashMap[Long, PlainEntry],
            List[PlainEntry],
            List[PlainEntry]) = {
        val entries = loadPlainEntries(log)
        val roots: Buffer[PlainEntry] = Buffer()
        val orphans: Buffer[PlainEntry] = Buffer()
        entries.values.foreach{
            e => e.process(entries, roots, orphans)
        }
        (entries, roots.toList, orphans.toList)
    }

    private def handleRoots(log: Logger, roots: List[PlainEntry]) {
        if(roots.isEmpty) {
            log.warning(s"No root entry found.")
        }
        else if(roots.size > 1) {
            val rootsStr = roots.mkString(", ")
            log.warning(s"Too many roots: $rootsStr")
        }
    }

    private def handleOrphans(log: Logger, orphans: List[PlainEntry]) {
        orphans.foreach{ o => {
            log.warning(s"Orphan $o with parentID ${o.parent}")
        }}
    }

    private def grow(
            log: Logger,
            entryTree: EntryTree,
            optParent: Option[TreeNode],
            entry: PlainEntry) {
        val sch = entryTree.schema
        val node = entryTree.insert(optParent, entry.name, entry.leaf)
        val data = new EntryData(sch, node, entry.id)
        node.data = data
        var traits: List[TraitThypeLike] = Nil
        entry.traits.foreach{t => sch.get(t) match {
            case Some(tr) => {
                if(tr.isInstanceOf[TraitThypeLike]) {
                    traits ::= tr.asInstanceOf[TraitThypeLike]
                }
                else {
                    log.error(s"$entry: thype $t is not a trait.")
                }
            }
            case None => {
                log.error(s"$entry: no such thype: $t.")
            }
        }}
        data.traits = traits
        entry.properties.foreach{p => {
            val (pathStr, sym, value) = p
            val path = TreePath.fromStr(s"*.*.$pathStr")
            
            val handled = grow(data.valueTree, path, sym, value)
            if(handled.isEmpty) {
                log.error(s"$entry: invalid value path $pathStr")
            }
        }}
        entry.children.values.foreach{e => {
            grow(log, entryTree, Some(node), e)
        }}
    }
    
    private def grow(
            valueTree: ValueTree,
            path: TreePath,
            sym: String,
            value: String): Option[TreePath] = {
        var handled: Option[TreePath] = None
        valueTree.search(path) match {
            case Some(vn) => {
                if(vn.path.length == path.length) {
                    initVN(vn, sym, value)
                    handled = Some(vn.path)
                }
                else if(vn.path.length + 1 == path.length) {
                    val vd = ValueData.of(vn)
                    val vt = vd.element.thype
                    if(vt.isInstanceOf[CollectionThypeLike]) {
                        // NB Keep in mind that path is *.*.$pathStr, rather
                        // than an actual path. So we construct the actual
                        // one inside initCollElemVN.
                        initCollElemVN(vn, path(vn.path.length), sym, value)
                        handled = Some(vn.path)
                    }
                }
            }
            case None =>
        }
        
        handled match {
            case Some(p) =>
                if(p.length < path.length) {
                    grow(valueTree, path, sym, value)
                } else {
                    handled
                }
            case None => None
        }
    }

    private def initVN(vn: ValueNode, sym: String, value: String) {
        val vd = ValueData.of(vn)
        vd.valex = makeValex(vd.element.thype, sym, value)
    }

    private def initCollElemVN(
            parentVN: ValueNode,
            name: String,
            sym: String,
            value: String) {
        val valueTree = parentVN.owner.asInstanceOf[ValueTree]
        val parentVD = ValueData.of(parentVN)
        val ct = parentVD.element.thype.asInstanceOf[CollectionThypeLike]
        val vn = valueTree.insert(Some(parentVN), name, true)
        val ve = makeValex(ct.elementThype, sym, value)
        val path = parentVN.path.append(name)
        vn.data = new ValueData(valueTree, path, ct.makeElement(vn), ve)
    }

    private def makeValex(thype: Thype, sym: String, value: String) = {
        if(sym == "") {
            None
        }
        else {
            Some(ValExpr.parse(thype, sym, value))
        }
    }
}

class XMLEntryTreeSaver(override val uri: URI)
        extends ResourceSaver[EntryTree]
        with Versioned
        with BaseXMLEntryTreeSL {

    private def mkEntryURI(entryID: Long): URI = {
        val p0: Long = entryID / 10000
        val p1: Long = (entryID / 100) % 100
        val l0: String = f"$p0%02d"
        val l1: String = f"$p1%02d"
        val relPath = List(l0, l1, s"${entryID}.xml").mkString("/")
        resolveURI(relPath)
    }

    override def save(optLog: Option[Logger], entryTree: EntryTree) {
        val log = optLog getOrElse logging.root
        var toDelete = loadPlainEntries(log)
        entryTree.foreach { node => guarded(log) { c =>
            val en = node.asInstanceOf[EntryNode]
            val ed = EntryData.of(en)
            
            toDelete -= ed.entryID

            if(ed.isDirty) {
                c.push(ed.entryID.toString)

                val parentID = en.parent match {
                    case Some(parent) =>
                        val parentED = EntryData.of(parent)
                        parentED.entryID
                    case None => 0
                }
                val traits = ed.immediateTraits.map{tr => tr.name}
                var props: List[(String, String, String)] = Nil
                ed.valueTree.overriddenNodes().foreach{ vn =>
                    val vnpath = vn.path

                    if(vnpath.length > 2) {
                        c.push(vnpath.toString)
                        
                        val pathStr = vnpath.toString(".", 2)
                        val vd = ValueData.of(vn)
                        if(vn.isLeaf) {
                            vd.valex match {
                                case (Some(valex), false) =>
                                    val valueStr = if(valex.isAbsolute) {
                                        valex.thype.repr(valex.value)
                                    }
                                    else {
                                        ss(valex.value, valex.value.toString, "")
                                    }
                                    val prop = (pathStr, valex.sym, valueStr)
                                    props ::= prop
                                case _ =>
                                    val prop = (pathStr, "", "")
                                    props ::= prop
                            }
                        }
                        else {
                            val prop = (pathStr, "", "")
                            props ::= prop
                        }

                        c.pop()
                    }
                }
                val entry = new PlainEntry(
                        en.name,
                        ed.entryID,
                        parentID,
                        en.isLeaf,
                        traits.toList.sorted,
                        props.sorted
                    )
                val entryURI = mkEntryURI(ed.entryID)
                val entrySaver = new XMLEntrySaver(entryURI)
                entrySaver.save(Some(log), entry)
                
                c.pop()
                ed.clearDirty()
            }
        }}
        
        toDelete.foreach { e =>
            val (id, _) = e
            val entryURI = mkEntryURI(id)
            try {
                val f = new File(entryURI)
                log.info(s"Deleting $entryURI ...")
                Files.delete(f.toPath())
            }
            catch {
                case e: Throwable =>
                    log.critical(s"Unable to delete $entryURI: $e")
                    throw e
            }
        }
    }
}
