package com.insweat.hssd.lib.util

import java.io.BufferedWriter
import java.io.OutputStreamWriter
import language.reflectiveCalls
import java.io.InputStreamReader
import java.io.BufferedReader
import java.io.File

class Subprocess(val proc: Process) {
  def communicate(input: Option[String] = None): (String, String) = {
    val outBuilder = new StringBuilder()
    val errBuilder = new StringBuilder()

    if(input.isDefined) {
      async {
        closing(proc.getOutputStream) { stream =>
          val writer = new BufferedWriter(new OutputStreamWriter(stream))
          writer.write(input.get)
        }
      }
    }
    
    async {
      closing(proc.getInputStream) { stream =>
        val reader = new BufferedReader(new InputStreamReader(stream))
        var line: String = null
        while(null != (line = reader.readLine())) {
          synchronized {
            outBuilder.append(line).append(String.format("%n"))
          }
        }
      }
    }

    async {
      closing(proc.getErrorStream) { stream =>
        val reader = new BufferedReader(new InputStreamReader(stream))
        var line: String = null
        while(null != (line = reader.readLine())) {
          synchronized {
            errBuilder.append(line).append(String.format("%n"))
          }
        }
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
}