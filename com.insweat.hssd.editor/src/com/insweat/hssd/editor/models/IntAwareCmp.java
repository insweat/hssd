package com.insweat.hssd.editor.models;

import java.util.Comparator;

import org.eclipse.jface.util.Policy;


public class IntAwareCmp implements Comparator<String> {

	public final static IntAwareCmp instance = new IntAwareCmp();

    private final Comparator<Object> defaultComparator = Policy.getComparator();
    
    @Override
    public int compare(String o1, String o2) {
        final Integer i1 = parseInt(o1);
        final Integer i2 = parseInt(o2);
        if(i1 != null && i2 != null) {
            return i1 - i2;
        }
        else if(i1 != null && i2 == null) {
            return -1;
        }
        else if(i1 == null && i2 != null) {
            return 1;
        }
        return defaultComparator.compare(o1, o2);
    }

    private Integer parseInt(String s) {
        try {
            return Integer.parseInt(s);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }
}
