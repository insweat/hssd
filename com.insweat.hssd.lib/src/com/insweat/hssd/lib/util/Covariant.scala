package com.insweat.hssd.lib.util

private final class Revision(var number: Long)

final class Covariant[T](private val revision: Revision, updater: => T) {
    private var revisionNumber: Long = 0
    private var cache: T = _
    
    def get: T = {
        update
        cache
    }
    
    def isClean: Boolean = revisionNumber == revision.number
    
    def update {
        if(!isClean) {
            cache = updater
            touch(false)
        }
    }

    def touch(makeDirty: Boolean = true) {
        if(makeDirty) {
            revision.number += 1
        }
        else {
            revisionNumber = revision.number
        }
    }
}

object Covariant {
    def apply[T](other: Option[Covariant[T]] = None)(updater: => T) = {
        other match {
            case Some(co) => new Covariant(co.revision, updater)
            case None => new Covariant(new Revision(1), updater)
        }
    }
}
