package com.insweat.hssd.lib.util

trait Qualified {
    private var _qname: String = null

    def qname: String = {
        if(_qname == null) {
            _qname = computeQName
        }
        val s = _qname
        _qname
    }

    protected def resetQName() {
        _qname = null
    }

    protected def computeQName: String
}
