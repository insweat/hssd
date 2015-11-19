package com.insweat.hssd.util;

import com.insweat.hssd.lib.util.*;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import scala.collection.Traversable;

import org.junit.Assert;

public final class AssertEx {
	/** 
	 * Asserts that runnable throws an exception of class exClass. <br/>
	 * If any exception other than exClass is thrown, it will be thrown
	 * to the outside. If no (exClass) exception is thrown, the assertion
	 * fails.
	 */
	public static Throwable raises(Class<? extends Throwable> exClass,
	        Runnable runnable) {
		try {
			runnable.run();
			Assert.fail(String.format("Expected exception %s is not thrown.", 
					exClass.getSimpleName()));
		}
		catch(Throwable e) {
			if(!exClass.isInstance(e)) {
				throw new RuntimeException(e);
			}
			return e;
		}
		throw new IllegalStateException();
	}

	@SuppressWarnings("unchecked")
    public static <T> void contentEqualsA(
            Object expected, Traversable<T> trav) {
	    final int n = Array.getLength(expected);
	    final Set<T> set = new HashSet<T>(n);
	    for(int i = 0; i < n; ++i) {
	        final Object o = Array.get(expected, i);
	        set.add((T)o);
	    }
	    contentEquals(set, trav);
	}

    public static <T> void contentEquals(
            List<T> expected, Traversable<T> trav) {
        contentEquals(new ArrayList<T>(expected), trav);
    }

    public static <T> void contentEquals(
            Set<T> expected, Traversable<T> trav) {
        final Set<T> content = new HashSet<T>(expected.size());
        trav.foreach(Func.of(new Func1<T, Void>(){
            @Override
            public Void apply(T a) {
                content.add(a);
                return null;
            }
        }));
        contentEquals(expected, content);
    }

    public static <T> void contentEquals(
            Collection<T> expected, Collection<T> actual) {
        final Set<T> expectedSet = new HashSet<T>(expected);
        final Set<T> actualSet = new HashSet<T>(actual);
        contentEquals(expectedSet, actualSet);
    }
    
    public static <T> void contentEquals(Set<T> expected, Set<T> actual) {
        if(!actual.equals(expected)) {
            final String msg = String.format(
                    "Content not equal, expected: %s, got: %s",
                    expected,
                    actual);
            Assert.fail(msg);
        }
    }
}
