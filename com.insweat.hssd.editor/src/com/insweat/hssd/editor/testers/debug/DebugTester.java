package com.insweat.hssd.editor.testers.debug;

import com.insweat.hssd.editor.testers.AbstractPropertyTester;

public class DebugTester extends AbstractPropertyTester {

    @Override
    public boolean test(Object receiver, String property, Object[] args,
            Object expectedValue) {
        log.noticef("DebugTester: receiver=%s, property=%s, expectedValue=%s",
                receiver, property, expectedValue);
        return true;
    }

}
