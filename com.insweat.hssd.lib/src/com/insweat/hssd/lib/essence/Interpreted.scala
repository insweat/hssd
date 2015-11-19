package com.insweat.hssd.lib.essence

trait Interpreted {
    def interpOut(ctx: Any, intVal: Any): Either[String, Any]
    def interpIn(ctx: Any, extVal: Any): Either[String, Any]
}
