package com.insweat.hssd.editor.testers.hssd;

import com.insweat.hssd.editor.testers.AbstractPropertyTester;
import com.insweat.hssd.lib.tree.structured.TreeNode;

public class EntryNodeTester extends AbstractPropertyTester {

    @Override
    public boolean test(Object receiver, String property, Object[] args,
            Object expectedValue) {
        TreeNode en = (TreeNode)receiver;
        if("isLeaf".equals(property)) {
            return Boolean.valueOf(en.isLeaf()).equals(expectedValue);
        }
        return false;
    }

}
