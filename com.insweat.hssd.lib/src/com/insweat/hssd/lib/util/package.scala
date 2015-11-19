package com.insweat.hssd.lib

import scala.language.implicitConversions

package object util {
    def emptyTrav[T]() = new Traversable[T] {
        def foreach[U](f: T=> U){}
    }

    val nullStr = "<null>"

    def ss(o: Any): String = ss(o, o.toString)
    def ss(o: Any, str: => String): String = ss(o, str, nullStr)
    def ss(o: Any, str: => String, strIfNull: => String): String = {
        if(o != null) str
        else strIfNull
    }

    def ss[T](o: Option[T]): String = ss(o, o.get.toString)
    def ss[T](o: Option[T], str: => String): String = ss(o, str, nullStr)
    def ss[T](o: Option[T], str: => String, strIfNone: => String): String = {
        if(o.isDefined) str
        else strIfNone
    }
    
    def literal(o: Any, default: => String = nullStr): String = o match {
        case null => default
        case Some(value) => String.valueOf(value)
        case None => default
        case _ => String.valueOf(o)
    }

    implicit def anyRefToOtherIfNull[T <: AnyRef](value: T): OtherIfNull[T] = 
        new OtherIfNull(value)

    implicit def optToOtherIfNone[T](value: Option[T]): OtherIfNone[T] = {
        new OtherIfNone(value)
    }
}
