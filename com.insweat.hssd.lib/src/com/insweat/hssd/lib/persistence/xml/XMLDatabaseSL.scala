package com.insweat.hssd.lib.persistence.xml

import com.insweat.hssd.lib.util.logging.Logger
import com.insweat.hssd.lib.util.Convert
import com.insweat.hssd.lib.util.logging
import com.insweat.hssd.lib.essence.SchemaLike
import com.insweat.hssd.lib.essence.Database
import com.insweat.hssd.lib.tree.EntryTree
import com.insweat.hssd.lib.persistence._
import scala.collection.mutable.Buffer
import scala.collection.immutable.HashMap
import scala.xml.XML
import scala.xml.Node
import java.net.URI
import java.io.IOException
import java.io.File
import scala.xml.Elem
import java.io.PrintWriter
import scala.io.Source

class XMLDatabaseLoader(override val uri: URI)
        extends ResourceLoader[Database] with Versioned {
    def this(file: File) = this(file.toURI())
    def this(path: String) = this(new File(path).toURI())

    override def load(optLog: Option[Logger]): Option[Database] = {
        val log = optLog getOrElse logging.root
        try {
            checkPrerequisites()

            val root = XML.load(uri.toURL())
            
            checkFormat(root, "Database", Some(version), Some(patternID))

            val name = attrib(root, "name")

            val db = new Database(name.get, log)

            val schemas = loadSchemas(log, root)
            schemas.foreach { sch => db.insertSchema(sch) }
            db.compileSchemas()
            db.setActiveSchema(schemas.head.qname)

            db.entries = loadEntryTree(log, root, db.schemas)

            db.settings = loadSettings(log, root)
            
            return Some(db)
        }
        catch {
            case e: VersionError =>
                throw e
            case e @ (
                    _: SetupError |
                    _: SyntaxError |
                    _: FileFormatError |
                    _: IntegrityError |
                    _: IOException) =>
                        log.error("An exception occurred", "exception"->e)
            case e: Throwable =>
                log.critical("A critical error occurred", "exception"->e)
                throw e
        }
        None
    }

    private def loadSchemas(log: Logger, root: Node): List[SchemaLike] = {
        val schemas = root \ "schemas" \ "Schema"
        if(schemas.isEmpty) {
            throw new SyntaxError("No schema is defined.")
        }

        val rv: Buffer[SchemaLike] = Buffer()
        schemas.foreach { sch => {
            val uri = attrib(sch, "uri").get
            val loader = new XMLSchemaLoader(resolveURI(uri))
            loader.load(Some(log)) match {
                case Some(schema) => rv += schema
                case None => 
                    throw new IntegrityError(s"Failed to load schema $uri")
            }
        }}

        rv.toList
    }

    private def loadEntryTree(
            log: Logger,
            root: Node,
            knownSchemas: HashMap[String, SchemaLike]): EntryTree = {
        val entryTree = root \ "EntryTree"
        if(entryTree.isEmpty) {
            throw new SyntaxError("No entries is defined")
        }
        val uri = attrib(entryTree.head, "uri").get
        val sch = attrib(entryTree.head, "schema").get
        val schema = knownSchemas.get(sch)
        if(!schema.isDefined) {
            throw new SyntaxError(
                    s"Cannot load entry tree $uri: missing schema.")
        }
        val loader = new XMLEntryTreeLoader(resolveURI(uri), schema.get)
        loader.load(Some(log)) match {
            case Some(rv) => rv
            case None => throw new IntegrityError(
                    s"Failed to load entry tree $uri.")
        }
    }
    
    private def loadSettings(log: Logger, root: Node)
            : HashMap[String, String] = {
        var rv: HashMap[String, String] = HashMap()
        val settings = root \ "settings" \ "Setting"
        settings.foreach(s => {
            val name = attrib(s, "name").get
            val value = attrib(s, "value").get
            rv += name -> value
        })
        rv
    }
}

class XMLDatabaseSaver(override val uri: URI)
        extends ResourceSaver[Database] with Versioned {
    def this(file: File) = this(file.toURI())
    def this(path: String) = this(new File(path).toURI())

    override def save(optLog: Option[Logger], db: Database) {
        val log = optLog getOrElse logging.root
        try {
            val root = 
                <tns:Database
                    name="correctDB"
                    version={version.toString}
                    xmlns:tns="http://www.insweat.com/HSSDDef"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://www.insweat.com/HSSDDef ../../../../com.insweat.hssd.lib/res/xsd/HSSDDef.xsd "
                >
                {saveSchemas(log, db.activeSchema)}
                {saveEntryTree(log, db.entries)}
                {saveSettings(log, db.settings)}
                </tns:Database>

            xml.save(log, root, uri)
        }
        catch {
            case e @ (
                    _: SetupError |
                    _: SyntaxError |
                    _: FileFormatError |
                    _: IntegrityError |
                    _: IOException) =>
                        val msg = e.getMessage
                        log.error(msg)
            case e: Throwable =>
                log.critical(e.getMessage)
                throw e
        }
    }

    private def saveSchemas(log: Logger, schema: SchemaLike): Node = {
        val schemaRelPath = "schemas/schema.xml"
        log.warning("Right now, Schema Saving only saves localized strings.")
        val schemaSaver = new XMLSchemaSaver(resolveURI(schemaRelPath))
        schemaSaver.save(Some(log), schema)
        <tns:schemas>
            <tns:Schema
                uri={schemaRelPath}
            />
        </tns:schemas>
    }

    private def saveEntryTree(log: Logger, entries: EntryTree): Node = {
        val entryTreeRelPath = "entries/"
        val entryTreeSaver = new XMLEntryTreeSaver(resolveURI(entryTreeRelPath))
        entryTreeSaver.save(Some(log), entries)
        <tns:EntryTree
            uri={entryTreeRelPath}
            schema={entries.schema.qname}
        />
    }
    
    private def saveSettings(log: Logger, settings: HashMap[String, String]) = {
        <tns:settings>
		{settings.map{ e=>
		    val (name, value) = e
		    <tns:Setting name={name} value={value}/>
		}}
		</tns:settings>
    }
}
