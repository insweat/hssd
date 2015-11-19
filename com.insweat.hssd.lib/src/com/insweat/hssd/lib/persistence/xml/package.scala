package com.insweat.hssd.lib.persistence

import com.insweat.hssd.lib.util.Convert
import com.insweat.hssd.lib.util.logging.Logger
import scala.xml.Node
import scala.util.control.Breaks
import scala.collection.mutable.Stack
import scala.util.matching.Regex
import java.net.URI
import java.io.PrintWriter
import java.io.File
import java.nio.file.Files

package object xml {

    trait Versioned {
        final val version = 32
    }
    
    def checkFormat(
            root: Node,
            label: String,
            optVer: Option[Int],
            optNamePat: Option[Regex]) {
        if(root.label != label) {
            throw new FileFormatError(
                    s"Invalid root: expecting '$label', " +
                    s"got '${root.label}'")
        }
        if(optVer.isDefined) {
            checkVersion(root, optVer.get)
        }
        if(optNamePat.isDefined) {
            checkName(root, optNamePat.get)
        }
    }

    def attrib(n: Node, key: String, opt: Boolean = false): Option[String] = {
        n.attribute(key) match {
            case Some(attrib) => Some(attrib.head.text)
            case None => if(opt) {
                None
            }
            else {
                val msg = s"failed to get $key for ${n.label}"
                throw new SyntaxError(msg)
            }
        }
    }
    
    def longAttrib(node: Node, name: String): Long = {
        val attrStr = attrib(node, name).get
        Convert.tryParseLong(attrStr) match {
            case Some(value) => value
            case None =>
                val msg = s"Cannot parse $name: $attrStr into a long int."
                throw new SyntaxError(msg)
        }
    }

    def boolAttrib(node: Node, name: String): Boolean = {
        val attrStr = attrib(node, name).get
        Convert.tryParseBoolean(attrStr) match {
            case Some(value) => value
            case None =>
                val msg = s"Cannot parse $name: $attrStr into a bool."
                throw new SyntaxError(msg)
        }
    }


    def format(c: Stack[String]): String = {
        var rv = ""
        while(!c.isEmpty) {
            val comp = c.pop
            if(rv.length() > 0) {
                rv = comp + "(" + rv + ")"
            }
            else {
                rv = comp
            }
        }
        rv
    }

    def guarded[U](log: Logger)(fn: Stack[String]=>U) {
        val stack = Stack[String]()
        
        try {
            fn(stack)
        }
        catch {
            case e: SyntaxError =>
                log.error(s"failed to process ${format(stack)}", "exception"->e)
            case e: Throwable =>
                log.critical(s"failed to process ${format(stack)}", "exception"->e)
        }
    }
    
    def save(log: Logger, root: Node, uri: URI) {
        val sb = new StringBuilder()
        val pp = new PrettyPrinter(140, 4)

        sb ++= """<?xml version="1.0" encoding="UTF-8"?>"""
        sb += '\n'

        pp.format(root, sb)

        val f = new File(uri)
        Files.createDirectories(f.getParentFile().toPath())

        val out = new PrintWriter(f, "UTF-8")
        try{
            log.info(s"Writing $uri ...")
            out.write(sb.toString)                
        }
        finally {
            out.close()
        }
    }

    private def checkVersion(root: Node, version: Int) {
        try {
            val ver = attrib(root, "version")
            val optV = Convert.tryParseInt(ver.get)
            if(!optV.isDefined || optV.get > version) {
                throw new VersionError(version, ver.get)
            }
        }
        catch {
            case e: SyntaxError =>
                throw new FileFormatError(
                        "Invalid root element: missing version attribute")
        }                
    }

    private def checkName(root: Node, namePattern: Regex) {
        try {
            val name = attrib(root, "name").get
            name match {
                case namePattern() => // pass
                case _ =>
                    throw new FileFormatError(
                            s"Invalid name: name '$name' does not match " +
                            s"pattern '$namePattern'")
            }
        }
        catch {
            case e: SyntaxError =>
                throw new FileFormatError(
                        "Invalid root element: missing name attribute")
        }
    }
}
