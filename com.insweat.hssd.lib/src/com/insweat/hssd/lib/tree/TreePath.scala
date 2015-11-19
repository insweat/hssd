package com.insweat.hssd.lib.tree

import scala.annotation.tailrec

final class TreePath private (private val comps: Array[String], val length: Int)
	extends Ordered[TreePath] {
    
    private var string : String = _;

    def append(following: String): TreePath = {
        val rvLength = length + 1
        val rv = new TreePath(Array.ofDim[String](rvLength), rvLength)
        Array.copy(comps, 0, rv.comps, 0, length)
        rv.comps(length) = following
        rv
    }

    def extend(following: String, trailing: String*): TreePath = {
        val rvLength = length + 1 + trailing.length
        val rv = new TreePath(Array.ofDim[String](rvLength), rvLength)
        Array.copy(comps, 0, rv.comps, 0, length)
        rv.comps(length) = following
        Array.copy(trailing, 0, rv.comps, length + 1, trailing.length)
        rv
    }
    
    def rebase(leading: TreePath): TreePath = {
        if(leading.length == length) {
            leading
        }
        else if(leading.length > length) {
            new TreePath(leading.comps, length)
        }
        else if (startsWith(leading)) {
            this
        }
        else {
            val comps = Array.ofDim[String](length)
            Array.copy(leading.comps, 0, comps, 0, leading.length)
            Array.copy(this.comps, leading.length, comps, leading.length, length - leading.length)
            new TreePath(comps, length)
        }
    }
    
    def rename(last: String): TreePath = {
        parent match {
            case Some(parentPath) => parentPath.append(last)
            case None => TreePath(last)
        }
    }

    def parent = if(length == 0) None else Some(new TreePath(comps, length - 1))

    def last = if (length == 0) null else comps(length - 1)
    
    def components = comps.clone()
    
    def apply(index: Int) = {
        if(index >= length) {
            throw new ArrayIndexOutOfBoundsException(s"$index out of $length")
        }
        comps(index)
    }
    
    def startsWith(leading: TreePath): Boolean = {
        @tailrec
        def test(index: Int): Boolean = {
            if(index == leading.length) {
                true
            }
            else if(comps(index) != leading.comps(index)) {
                false
            }
            else {
                test(index + 1)   
            }
        }
        length >= leading.length && test(0)
    }
    
    def eqLen(other: TreePath): Boolean = length == other.length
    
    override def equals(obj: Any): Boolean = obj match {
        case that: TreePath => {
            if(eq(that)) true
            else if(length != that.length) false
            else {
                for(i <- 0 until length) {
                    if(comps(i) != that.comps(i)) {
                        return false
                    }
                }
                true
            }
        }
        case _ => false
    }

    override def hashCode = toString().hashCode
    
    override def toString = {
        if(string == null) {
            string = toString(".", 0)
        }
        string;
    }
    
    def toString(sep: String): String = toString(sep, 0)

    def toString(sep: String, start: Int): String = if(length > 0) {
        comps.slice(start, length).mkString(sep)
    }
    else ""

    def compare(that: TreePath): Int = {
        if(equals(that)) {
            return 0;
        }
        if(string != null && that.string != null) {
            return string.compareTo(that.string);
        }
        val n = math.min(length, that.length);
        for(i <- 0 until n) {
            val r = comps(i).compareTo(that.comps(i));
            if(r != 0) {
                return r;
            }
        }
        if(length < that.length) {
            return -1;
        }
        return 1;
    }
}

object TreePath {
    def apply(pathComps: String*) = {
        val comps = pathComps.toArray
        new TreePath(comps, comps.length)
    }

    def fromComps(pathComps: Array[String]) ={
        val comps = pathComps.clone
        new TreePath(comps, comps.length)
    }

    def fromStr(path: String, sep: String): TreePath = {
        val escapedSep = java.util.regex.Pattern.quote(sep)
        val comps = path.split(escapedSep)
        new TreePath(comps, comps.length)
    }

    def fromStr(path: String): TreePath = fromStr(path, ".")
}
