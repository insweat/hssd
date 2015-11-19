package com.insweat.hssd.lib.essence

import com.insweat.hssd.lib.util._
import com.insweat.hssd.lib.util.logging.Logger
import scala.collection.immutable.HashMap
import scala.annotation.tailrec
import com.insweat.hssd.lib.tree.EntryTree
import java.net.URI

class DBException(msg: String, cause: Throwable = null)
    extends Exception(msg, cause) {
}

class DBSetupError(msg: String, cause: Throwable = null)
    extends DBException(msg, cause) {
}

/**
 * This class contains API for manipulating A HSSD DB.
 */
class Database(val name: String, log: Logger) {
    private var _activeSchema: SchemaLike = new BuiltinSchema()
    private var _schemas: HashMap[String, SchemaLike] = HashMap()
    private var _entries: EntryTree = null
    private var _settings: HashMap[String, String] = HashMap()
    private var _changed = false

    /**
     * The schemas that are associated with this DB.
     */
    def schemas = _schemas

    /**
     * Makes a schema known to the DB.
     */
    def insertSchema(schema: SchemaLike) {
        _schemas += schema.qname -> schema
    }

    /**
     * Makes a schema unknown to the DB.
     */
    def removeSchema(qname: String) {
        _schemas -= qname
    }

    /**
     * Gets the current active schema.
     */
    def activeSchema = _activeSchema

    /**
     * Sets the current active schema.
     */
    def setActiveSchema(qname: String) {
        schemas.get(qname) match {
            case Some(schema) => _activeSchema = schema
            case None => throw new NoSuchElementException(
                    s"No such schema: $qname.")
        }
    }

    /**
     * Compiles all schemas in the DB.
     */
    def compileSchemas() {
        def compile(sch: SchemaLike) {
            if(sch.isPending) {
                if(sch.parent.isDefined) {
                    compile(sch.parent.get)
                }
                sch.compile(Some(schemas))
            }
        }
        schemas.values.foreach(compile)
    }

    /**
     * The entries that this DB holds.
     */
    def entries: EntryTree = {
        if(_entries == null) {
            _entries = new EntryTree(activeSchema)
        }
        _entries
    }

    /**
     * Sets entries in the DB.
     * If the entry tree, optEnts, is None, an empty one will be created.
     * If optEnts is not None, it must use a schema known to the DB.
     */
    def entries_=(ents: EntryTree) {
        if(ents != null)
        {
            if(ents.schema == null) {
                throw new DBSetupError(s"$this cannot setEntries: " +
                        "entry tree schema is null.")
            }
            else if(!schemas.contains(ents.schema.qname))
            {
                val sch = ents.schema
                throw new DBSetupError(s"$this cannot setEntries: " +
                        s"entry tree schema $sch is unknown to the DB.")
            }
        }
        _entries = ents

        val entsDesc = if(ents != null) {
            "a given entry tree"
        }
        else {
            "an empty entry tree"
        }
        log.info(s"$this is set with $entsDesc.")
    }

    /**
     * Settings for this DB
     */
    def settings = _settings

    /**
     * Putting settings for this DB
     */
    def settings_=(settings: HashMap[String, String]) {
        _settings = settings
    }

    /**
     * Indicates whether the DB has un-persisted changes.
     */
    def changed = _changed

    /**
     * Sets whether the DB has un-persisted changes.
     */
    def changed_=(value: Boolean):Unit = {
        if(value != _changed) {
            val status = if(value) "changed" else "unchanged"
            log.info(s"$this is marked as $status.")
            _changed = value
        }
    }

    override def toString() = s"HSSD($name)"
}
