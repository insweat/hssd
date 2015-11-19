package com.insweat.hssd.lib.util.logging

import scala.collection.Seq
import scala.collection.immutable
import scala.collection.mutable
import com.insweat.hssd.lib.util._

class Logger(
        val name: String,
        private var _level: Int,
        val parent: Option[Logger],
        val propagate: Boolean) extends FilteredObject with Qualified {

    private val handlers: mutable.Buffer[Handler] = mutable.Buffer()
    private val children: mutable.HashMap[String, Logger] = mutable.HashMap()

    def level: Int = _level
    def level_=(l: Int): Unit = _level = l

    def getChild(
            name: String,
            level: Option[Int],
            propagate: Boolean = true): Logger = {
        children.get(name) match {
            case Some(logger) => logger
            case None => {
                val logger = new Logger(
                        name,
                        level getOrElse this.level,
                        Some(this),
                        propagate)
                children(name) = logger
                logger
            }
        }
    }

    def _log(level: Int, msg: String, kwargs: Seq[(String, Any)]) = {
        // TODO make this function thread safe.
        val s = qname
        val kws = immutable.HashMap(kwargs:_*)
        val record = new Record(qname, level, msg, kws)
        handle(record)
        kws
    }

    def log(level: Int, msg: String): Unit = {
        _log(level, msg, Seq());
    }

    def log(level: Int, msg: String, kwargs: Tuple2[String, Any]*): Unit = {
        _log(level, msg, kwargs)
    }

    def debug(msg: String, kwargs: Tuple2[String, Any]*): Unit = {
        _log(LEVEL_DEBUG, msg, kwargs)
    }

    def info(msg: String, kwargs: Tuple2[String, Any]*): Unit = {
        _log(LEVEL_INFO, msg, kwargs)
    }

    def notice(msg: String, kwargs: Tuple2[String, Any]*): Unit = {
        _log(LEVEL_NOTICE, msg, kwargs)
    }

    def warning(msg: String, kwargs: Tuple2[String, Any]*): Unit = {
        _log(LEVEL_WARNING, msg, kwargs)
    }

    def error(msg: String, kwargs: Tuple2[String, Any]*): Unit = {
        _log(LEVEL_ERROR, msg, kwargs)
    }

    def critical(msg: String, kwargs: Tuple2[String, Any]*): Unit = {
        val kws = _log(LEVEL_CRITICAL, msg, kwargs)
        kws.get("exception") match {
            case Some(t: Throwable) =>
                throw new RuntimeException(s"Critical error: $msg", t)
            case _ => 
                throw new RuntimeException(s"Critical error: $msg")
        }
    }

    def addHandler(handler: Handler): Unit = {
        handlers += handler
    }

    def removeHandler(handler: Handler): Unit = {
        handlers -= handler
    }

    def handlerCount: Int = handlers.size

    def getHandler(index: Int): Handler = handlers(index)

    protected def handle(record: Record): Unit = {
        if(0 == handlerCount && (!propagate || !parent.isDefined)) {
            Console.err.println(s"Logger $qname has no handler installed.")
        }
        if(filter(record)) {
            handlers.foreach{
                h => h.handle(record)
            }
        }
        if(propagate && parent.isDefined) {
            parent.get.handle(record)
        }
    }

    override protected def filter(record: Record): Boolean = {
        this.level <= record.level && super.filter(record)
    }

    override protected def computeQName: String = {
        ss(parent, s"${parent.get.qname}.${name}", name)
    }
}
