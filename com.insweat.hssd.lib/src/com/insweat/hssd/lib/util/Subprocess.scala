package com.insweat.hssd.lib.util

import java.io.BufferedWriter
import java.io.OutputStreamWriter
import language.reflectiveCalls
import java.io.InputStreamReader
import java.io.BufferedReader
import java.io.File
import scala.io.Source

class Subprocess(val proc: Process) {
  def communicate(input: Option[String] = None): (String, String) = {
    val outBuilder = new StringBuilder()
    val errBuilder = new StringBuilder()

    async {
      closing(proc.getInputStream) { stream =>
        val source = Source.fromInputStream(stream)
        for(line <- source.getLines()) {
          synchronized {
            outBuilder.append(line).append(System.lineSeparator())
          }
        }
      }
    }

    async {
      closing(proc.getErrorStream) { stream =>
        val source = Source.fromInputStream(stream)
        for(line <- source.getLines()) {
          synchronized {
            errBuilder.append(line).append(System.lineSeparator())
          }
        }
      }
    }

    if(input.isDefined) {
      closing(proc.getOutputStream) { stream =>
        val writer = new BufferedWriter(new OutputStreamWriter(stream))
        writer.write(input.get)
        writer.flush()
      }
    }
    
    proc.waitFor()
    
    (outBuilder.toString(), errBuilder.toString())
  }
  
  private def closing[T <: {def close(): Unit}, R](res: T)(code: (T) => R) = {
    try {
      code(res)
    } finally {
      res.close()
    }
  }
  
  private def async(code: => Unit): Thread = {
    val rv = new Thread(new Runnable() {
      override def run() {
        code
      }
    })
    rv.start
    rv
  }
}

object Subprocess {
  val EXEC_EXTS = List("", ".sh", ".py", ".exe", ".cmd", ".bat", ".demo")

  def findExecutable(parentLocation: File, basename: String): Option[File] = {
    EXEC_EXTS map { ext =>
      new File(parentLocation, basename + ext)
    } find { f => 
      f.exists() && f.canExecute()
    }       
  }
  
  def create(args: Array[String]): Subprocess = {
    new Subprocess(Runtime.getRuntime.exec(args))
  }
}