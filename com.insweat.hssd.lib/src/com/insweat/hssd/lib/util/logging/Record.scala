package com.insweat.hssd.lib.util.logging

import scala.collection.immutable.HashMap
import java.util.Date

final case class Record(
        name: String,
        level: Int,
        msg: String,
        kwargs: HashMap[String, Any]) {
    val timestamp = new Date()
    val exception = kwargs.get("exception") match {
        case Some(e: Throwable) => 
            val stackTraceStr = e.getStackTrace().mkString("\n")
            Some(s"${e.getClass}: ${e.getMessage}\n${stackTraceStr}")
        case Some(other) => Some(other.toString)
        case None => None
    }
    // TODO location info
}
