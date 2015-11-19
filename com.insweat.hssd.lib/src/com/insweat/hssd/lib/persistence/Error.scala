package com.insweat.hssd.lib.persistence

import scala.util.control.NoStackTrace

class SetupError(msg: String) extends Throwable(msg) with NoStackTrace
class FileFormatError(msg: String) extends Throwable(msg) with NoStackTrace
class IntegrityError(msg: String) extends Throwable(msg) with NoStackTrace
class SyntaxError(msg: String) extends Throwable(msg) with NoStackTrace
class SemanticError(msg: String) extends Throwable(msg) with NoStackTrace

class VersionError private(
        val supportedVersion:Int,
        val actualVersion: String,
        msg: String
        ) extends RuntimeException(msg) with NoStackTrace {
    def this(supportedVersion: Int, actualVersion: String) {
        this(supportedVersion, actualVersion,
                s"Invalid version: supporting up to '$supportedVersion', "
                + s"got '$actualVersion'")
    }
}
