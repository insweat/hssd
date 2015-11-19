package com.insweat.hssd.lib.util

class OtherIfNull[T <: AnyRef](value: T) {
    def ??[U <: T](otherValue: => U): T = {
        if(value != null) value else otherValue
    }
}

class OtherIfNone[T <: Any](value: Option[T]) {
    def or[U <: T](otherValue: => Option[U]): Option[T] = {
        if(value.isDefined) value else otherValue
    }
}
