package com.insweat.hssd.lib.util.logging

import scala.collection.mutable.Buffer

trait Filter {
    def filter(record: Record): Boolean
}

class FilteredObject {
    private val filters: Buffer[Filter] = Buffer()

    def addFilter(f: Filter): Unit = {
        filters += f
    }

    def removeFilter(f: Filter): Unit = {
        filters -= f
    }

    protected def filter(record: Record): Boolean = {
        filters.forall{ f => f.filter(record) }
    }
}
