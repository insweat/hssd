package com.insweat.hssd.lib.util

package object logging {
    val LEVEL_MIN = 0
    val LEVEL_DEBUG = 100
    val LEVEL_INFO = 200
    val LEVEL_NOTICE = 300
    val LEVEL_WARNING = 400
    val LEVEL_ERROR = 500
    val LEVEL_CRITICAL = 600
    val LEVEL_MAX = 999

    def getLevelName(level: Int): String = {
        level match {
            case LEVEL_MIN => "<MIN>"
            case LEVEL_DEBUG => "DEBUG"
            case LEVEL_INFO => "INFO"
            case LEVEL_NOTICE => "NOTICE"
            case LEVEL_WARNING => "WARNING"
            case LEVEL_ERROR => "ERROR"
            case LEVEL_CRITICAL => "CRITICAL"
            case LEVEL_MAX => "<MAX>"
            case _ => s"<UNKNOWN: $level>"
        }
    }

    val root = new Logger("root", LEVEL_INFO, None, false)

    def newHandler(level:Int, _emit: Record=>Unit) = new Handler(level) {
        override protected def emit(record: Record):Unit = _emit(record)
    }
}
