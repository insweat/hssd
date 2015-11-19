package com.insweat.hssd.lib.util

trait Func0[+R] {
    def apply(): R
}

trait Func1[-T1, +R] {
	def apply(a: T1): R
}

trait Func2[-T1, -T2, +R] {
    def apply(a: T1, b: T2): R
}

trait Func3[-T1, -T2, -T3, +R] {
    def apply(a: T1, b: T2, c: T3): R
}

trait Func4[-T1, -T2, -T3, -T4, +R] {
    def apply(a: T1, b: T2, c: T3, d: T4): R
}

object Func {
    def of[R](func: Func0[R]
            ): Function0[R] = func.apply
    def of[T1, R](func: Func1[T1, R]
            ): Function1[T1, R] = func.apply
    def of[T1, T2, R](func: Func2[T1, T2, R]
            ): Function2[T1, T2, R] = func.apply
    def of[T1, T2, T3, R](func: Func3[T1, T2, T3, R]
            ): Function3[T1, T2, T3, R] = func.apply
    def of[T1, T2, T3, T4, R](func: Func4[T1, T2, T3, T4, R]
            ): Function4[T1, T2, T3, T4, R] = func.apply
}
