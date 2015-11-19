package com.insweat.hssd.lib.persistence.xml

import com.insweat.hssd.lib.essence.SchemaLike
import com.insweat.hssd.lib.essence.Schema
import com.insweat.hssd.lib.essence.thypes.EnumThype
import com.insweat.hssd.lib.essence.thypes.ComplexThype
import com.insweat.hssd.lib.essence.EnumValue
import com.insweat.hssd.lib.essence.thypes.TraitThype
import com.insweat.hssd.lib.essence.Element
import com.insweat.hssd.lib.util.Convert
import com.insweat.hssd.lib.util.logging.Logger
import com.insweat.hssd.lib.util.logging
import com.insweat.hssd.lib.persistence._
import scala.collection.immutable.HashMap
import scala.collection.mutable.Stack
import scala.collection.mutable
import scala.xml.Elem
import scala.xml.XML
import scala.xml.Node
import scala.xml.PCData
import java.net.URI
import java.io.IOException
import java.io.File
import com.insweat.hssd.lib.essence.thypes.LStringThype
import scala.collection.mutable.ArrayBuffer
import com.insweat.hssd.lib.essence.thypes.TupleThype
import scala.xml.parsing.ConstructingParser
import scala.io.Source

class XMLSchemaLoader(override val uri: URI)
        extends ResourceLoader[Schema] with Versioned {
    def this(file: File) = this(file.toURI())
    def this(path: String) = this(new File(path).toURI())

    /** ElementAggregationNamePattern
     */
    private val EANPattern = """([_a-zA-Z0-9]+)\{(\d+,)?(\d+)\}""".r
    
    /**
     * Loads a schema from the uri. The loaded schema is not compiled.
     */
    override def load(optLog: Option[Logger]): Option[Schema] = {
        val log = optLog getOrElse logging.root
        checkPrerequisites()
        try {
            val root = XML.load(uri.toURL())
            checkFormat(root, "Schema", Some(version), Some(patternID))

            val parent = attrib(root, "parent", true)
            val sch: Schema = new Schema(attrib(root, "name").get, parent)

            processEnumSeq(log, sch, root)
            processComplexSeq(log, sch, root)
            processTraitSeq(log, sch, root)

            loadL10NData(log, sch)
            return Some(sch)
        }
        catch {
            case e @ (
                    _: SetupError |
                    _: FileFormatError |
                    _: IOException) => log.error(e.getMessage, "exception" -> e)
        }
        None
    }

    private def processEnumSeq(log: Logger, sch: SchemaLike, root: Node) {
        val enums = root \ "enumerations" \ "Enum"
        enums.foreach{ loadEnum(log, sch, _) }
    }

    private def processComplexSeq(log: Logger, sch: SchemaLike, root: Node) {
        val complexes = root \ "complexes" \ "Complex"
        complexes.foreach{ loadComplex(log, sch, _) }

        val tuples = root \ "complexes" \ "Tuple"
        tuples.foreach{ loadTuples(log, sch, _) }
    }

    private def processTraitSeq(log: Logger, sch: SchemaLike, root: Node) {
        val traits = root \ "traits" \ "Trait"
        traits.foreach{ loadTrait(log, sch, _) }
    }

    private def loadEnum(log: Logger, sch: SchemaLike, node: Node) {
        guarded(log){ c =>
            c.push(node.label)

            val name = attrib(node, "name")
            
            c.push(name.get)

            val desc = getDesc(node)
            val attribs = getAttribMap(node)
            val enumVals = getValueSeq(node)
            new EnumThype(sch, name.get, desc, attribs, enumVals: _*)
            c.pop()
            c.pop()
        }
    }

    private def loadComplex(log: Logger, sch: SchemaLike, node: Node) {
        guarded(log){ c =>
            c.push(node.label)

            val name = attrib(node, "name")

            c.push(name.get)

            val desc = getDesc(node)
            val attribs = getAttribMap(node)
            val elems = getElemSeq(node)
            new ComplexThype(sch, name.get, desc, attribs, elems: _*)

            c.pop()
            c.pop()
        }
    }
    
    private def loadTuples(log: Logger, sch: SchemaLike, node: Node) {
        guarded(log){ c =>
            c.push(node.label)

            val name = attrib(node, "name")

            c.push(name.get)

            val desc = getDesc(node)
            val attribs = getAttribMap(node)
            val elems = getElemSeq(node)
            new TupleThype(sch, name.get, desc, attribs, elems: _*)

            c.pop()
            c.pop()
        }
    }

    private def loadTrait(log: Logger, sch: SchemaLike, node: Node) {
        guarded(log) { c =>
            c.push(node.label)

            val name = attrib(node, "name")

            c.push(name.get)

            val caption = attrib(node, "caption", true)
            val desc = getDesc(node)
            val attribs = getAttribMap(node)
            val elems = getElemSeq(node)
            new TraitThype(sch, name.get, caption, desc, attribs, elems: _*)

            c.pop()
            c.pop()
        }
    }

    private def loadL10NData(log: Logger, sch: SchemaLike) {
        val uri = resolveURI(s"l10n/${sch.qname}/")
        sch.get("LString") match {
            case Some(lst: LStringThype) =>
                walk(new File(uri)) { f =>
                    guarded(log) { c => 
                        if(f.getName().toLowerCase().endsWith(".xml")){
                            c.push(f.getName())
                            // NB The API is not 100% in reading the encoding
                            // from the XML. We need to create the source with
                            // explicit encoding to workaround the issue in
                            // CGI environment
                            val s = Source.fromFile(f, "UTF-8")
                            val parser = ConstructingParser.fromSource(s, true)
                            val root = parser.document().docElem
                            checkFormat(root, "strings", None, None)
                            
                            val lang = attrib(root, "lang").get
                            val strings = root \ "String"
                            strings.foreach { str =>
                                val strID = longAttrib(str, "id")
                                c.push(strID.toString)
                                val cd = str.child.find(_.isInstanceOf[PCData])
                                lst(strID, lang) = 
                                    cd.map(_.text).getOrElse(str.text)
                                c.pop()
                            }
                            c.pop()
                        }
                    }
                }
            case _ =>
                log.error(s"LString not defined in $sch. L10N data not loaded.")
        }
    }

    private def getDesc(node: Node): String = {
        val descs = node\"description"
        descs.headOption match {
            case Some(desc) => desc.text.trim()
            case None => ""
        }
    }

    private def getAttribMap(node: Node): HashMap[String, String] = {
        val attribs = node\"attributes"\"Attrib"
        val pairs = attribs map { getIndividualAttrib }
        HashMap[String, String](pairs: _*)
    }

    private def getValueSeq(node: Node): Seq[EnumValue] = {
        val values = node\"values"\"EnumValue"
        values map { getIndividualEnumValue }
    }

    private def getElemSeq(node: Node): Seq[Element] = {
        val elems = node\"elements"\"Element"
        elems flatMap { getIndividualElem }
    }

    private def getIndividualAttrib(aNode: Node): (String, String) = {
        val name = attrib(aNode, "name")
        val value = attrib(aNode, "value")
        (name.get, value.get)
    }

    private def getIndividualEnumValue(evNode: Node): EnumValue = {
        val name = attrib(evNode, "name")
        val desc = getDesc(evNode)
        attrib(evNode, "value") match {
            case Some(value) => new EnumValue(name.get, desc, value)
            case None => new EnumValue(name.get, desc)
        }
    }

    private def getIndividualElem(eNode: Node): Seq[Element] = {
        val caption = attrib(eNode, "caption").get
        val name = attrib(eNode, "name").get
        val thype = attrib(eNode, "thype").get
        val default = attrib(eNode, "defaultValue", true)
        val desc = getDesc(eNode)
        val attribs = getAttribMap(eNode)
        
        name match {
            case EANPattern(name, beg, end) =>
                val b = if(beg != null) {
                    Convert.toInt(beg.substring(0, beg.length - 1))
                }
                else 0
                val e = Convert.toInt(end)
                mkElemAggr(b, e, caption, name, thype, default, desc, attribs)
            case _ =>
                Seq(Element(caption, name, thype, default, desc, attribs))
        }
    }

    private def mkElemAggr(
            beg: Int,
            end: Int,
            caption: String,
            name: String,
            thype: String,
            default: Option[String],
            desc: String,
            attribs: Map[String, String]): Seq[Element] = {
        val padding = s"$end".length
        val namePat = s"${name}:%0${padding}d"
        Range(beg, end).map { i =>
            Element(
                caption.format(i),
                namePat.format(i),
                thype,
                default,
                desc,
                attribs)
        }
    }
}

class XMLSchemaSaver(override val uri: URI)
        extends ResourceSaver[SchemaLike] with Versioned {
    def this(file: File) = this(file.toURI())
    def this(path: String) = this(new File(path).toURI())
    
    val FILE_CAPACITY = 100
    
    override def save(optLog: Option[Logger], sch: SchemaLike) {
        // TODO right now, thypes are not saved through this interface
        val log = optLog getOrElse logging.root

        saveL10NData(log, sch)
    }
    
    private def saveL10NData(log: Logger, sch: SchemaLike) {
        sch.get("LString") match {
            case Some(lst: LStringThype) =>
                val langs = lst.allLangs
                val strIDs = lst.allStringIDs.sorted
                if(strIDs.length > 0) {
                    langs.foreach { lang =>
                        saveLang(log, sch, lst, strIDs, lang) 
                    }    
                }
            case _ =>
                log.error(s"LString not defined in $sch. L10N data not saved.")
        }
    }

    private def saveLang(
            log: Logger,
            sch: SchemaLike,
            lst: LStringThype,
            strIDs: Array[Long],
            lang: String) {
        val files = mutable.HashMap[Long, ArrayBuffer[(Long, String)]]()

        strIDs.foreach { strID =>
            val index = getIndex(strID)
            files.get(index) match {
                case Some(buf) => buf.append((strID, lst(strID, lang)))
                case None =>
                    val buf = ArrayBuffer[(Long, String)]()
                    files(index) = buf
                    buf.append((strID, lst(strID, lang)))
            }
        }

        files.foreach { e => guarded(log) { c=>
            val (index, strs) = e
            val uri = mkL10NURI(sch, lang, index)
 
            c.push(lang)
            c.push(index.toString)
            
            val root = mkStrings(log, lang, strs)
            xml.save(log, root, uri)

            c.pop()
            c.pop()
        }}
    }

    private def mkStrings(
            log: Logger, lang: String, strs: Iterable[(Long, String)]): Node = {
        <strings lang={lang}>
            { strs.map{s=>mkString(log, s._1, s._2)} }
        </strings>
    }
    
    private def mkString(log: Logger, strID: Long, content: String): Node = {
        <String id={strID.toString}>{PCData(content)}</String>
    }

    private def mkL10NURI(sch: SchemaLike, lang: String, index: Long): URI = {
        val p0: Long = index / 10000
        val p1: Long = (index /100) % 100
        val l0: String = f"$p0%02d"
        val l1: String = f"$p1%02d"
        val firstID = index * FILE_CAPACITY
        val lastID = firstID + FILE_CAPACITY - 1
        val pathComps = List("l10n", sch.qname, lang, l0, l1,
                f"${firstID}%08d-${lastID}%08d.xml")
        val relPath = pathComps.mkString("/")
        resolveURI(relPath)
    }
    
    private def getIndex(strID: Long): Long = strID / FILE_CAPACITY
}
