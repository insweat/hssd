package com.insweat.hssd.editor.util;

import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.text.StrSubstitutor;


public final class S {
    public final StringBuffer underlying;

    public S() {
        underlying = new StringBuffer();
    }

    public S(int capacity) {
        underlying = new StringBuffer(capacity);
    }
    
    public S(String str) {
        underlying = new StringBuffer(str);
    }

    public S(CharSequence charSeq) {
        underlying = new StringBuffer(charSeq);
    }

    public S addf(String pattern, Object ... args) {
        underlying.append(fmt(pattern, args));
        return this;
    }
    
    @Override
    public String toString() {
        return underlying.toString();
    }

    public static String fmt(String pat, Object ... args) {
        return String.format(pat, args);
    }

    public static String sub(String template, Map<String, Object> map) {
        StrSubstitutor sub = new StrSubstitutor(map);
        return sub.replace(template);
    }

    @SafeVarargs
    public static <T> String join(
            String sep, Function<T, String> getter, T ... args) {
        final String[] fragments = new String[args.length];
        for(int i = 0; i < fragments.length; ++i) {
            fragments[i] = getter.apply(args[i]);
        }
        return String.join(sep, fragments);
    }
}
