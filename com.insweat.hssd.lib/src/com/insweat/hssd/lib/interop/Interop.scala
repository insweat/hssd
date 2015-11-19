package com.insweat.hssd.lib.interop

import com.insweat.hssd.lib.util.nullStr
import com.insweat.hssd.lib.util

import java.util.function.Supplier
import java.util.function.BiFunction
import java.util.function.Consumer
import scala.reflect.ClassTag

object Interop {
    def none[T](): Option[T] = None
    
    def opt[T](value: T): Option[T] = Option(value)

    def tuple[A, B](a: A, b: B): (A, B) = (a, b)
    def tuple[A, B, C](a: A, b: B, c: C): (A, B, C) = (a, b, c)
    def tuple[A, B, C, D](a: A, b: B, c: C, d: D): (A, B, C, D) = (a, b, c, d)

    def fn[T](f: Supplier[T]): Function0[T] = f.get

    def fn[T, R](f: java.util.function.Function[T, R]): Function1[T, R] = f.apply
    def fn[T1, T2, R](f: BiFunction[T1, T2, R]): Function2[T1, T2, R] = f.apply

    def or[T](opt: Option[T], default: T): T = opt.getOrElse(default)
    def or[T >: AnyRef](opt: Option[T]): T = or(opt, null)

    def literal(o: Any): String = util.literal(o)
    
    def literal(o: Any, default: String): String = util.literal(o, default)

    def foreach[T](tra: scala.Traversable[T], f: Consumer[T]) {
        tra.foreach{ f.accept }
    }

    def toArray[T](tra: scala.Traversable[T]): Array[Any] = tra.toArray
    
    def toArray[T, R >: T](
            tra: scala.Traversable[T], clazz: Class[R]): Array[R] = {
        tra.toArray(ClassTag(clazz))
    }
}
