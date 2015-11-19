package com.insweat.hssd.editor.util;

import java.util.Collection;
import java.util.Comparator;

import java.util.Arrays;

import com.insweat.hssd.lib.essence.TraitThypeLike;

public final class TraitsHelper {
    private TraitsHelper() {}
    
    public static TraitThypeLike[] sortedArray(Collection<TraitThypeLike> trs) {
        return sortedArray(trs, new Comparator<TraitThypeLike>(){

            @Override
            public int compare(TraitThypeLike o1, TraitThypeLike o2) {
                return o1.caption().compareTo(o2.caption());
            }

        });
    }
    
    public static TraitThypeLike[] sortedArray(
            Collection<TraitThypeLike> trs, Comparator<TraitThypeLike> cmp) {
        TraitThypeLike[] rv = new TraitThypeLike[trs.size()];
        rv = trs.toArray(rv);
        Arrays.sort(rv, cmp);
        return rv;
    }
}
