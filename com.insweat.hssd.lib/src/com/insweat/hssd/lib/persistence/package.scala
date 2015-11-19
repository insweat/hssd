package com.insweat.hssd.lib

import com.insweat.hssd.lib.util.logging
import com.insweat.hssd.lib.util.logging.Logger

import scala.util.control.Breaks
import scala.util.control.NoStackTrace

import java.net.URI
import java.io.File
import java.io.FileFilter

package object persistence {
    val patternID = """[_a-zA-Z][_a-zA-Z0-9]*""".r
    val patternIndependentName = """[A-Z][_a-zA-Z0-9]*""".r
    val patternDependentName = """[_a-z][_a-zA-Z0-9]*""".r
    
    def walk(loc: File) (fn: File=>Unit): Unit = {
        if(loc.isDirectory()) {
            loc.listFiles().foreach{
                f => walk(f)(fn)
            }
        }
        else {
            fn(loc)
        }
    }
}
