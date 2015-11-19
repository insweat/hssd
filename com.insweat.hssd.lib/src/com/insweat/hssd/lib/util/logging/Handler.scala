package com.insweat.hssd.lib.util.logging

abstract class Handler(protected var _level: Int) extends FilteredObject {
    def level: Int = _level
    def level_=(l: Int): Unit = _level = l

    def handle(record: Record): Boolean = {
        val rv = filter(record)
        if(rv) {
            emit(record)
        }
        rv
    }
    
    protected def emit(record: Record): Unit
    
    protected def format(record: Record): String = {
        val levelName = getLevelName(record.level)
        record.exception match {
            case Some(s) =>
                s"${record.timestamp} [${levelName}] ${record.msg} ${s}"
            case None =>
                s"${record.timestamp} [${levelName}] ${record.msg}"
        }
    }

    override protected def filter(record: Record): Boolean = {
        this.level <= record.level && super.filter(record)
    }
}

class ConsoleHandler(_level: Int) extends Handler(_level) {
    override protected def emit(record: Record): Unit = {
        if(record.level >= LEVEL_WARNING) {
            Console.err.println(format(record)) 
        }
        else {
            Console.out.println(format(record))
        }
    }
}
