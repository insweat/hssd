package com.insweat.hssd.lib.persistence

import com.insweat.hssd.lib.util.logging.Logger
import java.net.URI
import java.net.URL

trait BaseSL {
    val version: Int
    val uri: URI

    def checkPrerequisites() {
        if(uri == null) {
            throw new SetupError("uri cannot be null.")
        }
    }

    def resolveURI(relPath: String): URI = uri.resolve(relPath)
}

trait ResourceLoader[R] extends BaseSL {
    def load(optLog: Option[Logger]): Option[R]
}

trait ResourceSaver[R] extends BaseSL {
    def save(optLog: Option[Logger], resource: R)
}
