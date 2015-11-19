package com.insweat.hssd.editor.testers.entry;


import com.insweat.hssd.editor.testers.AbstractPropertyTester;
import com.insweat.hssd.lib.essence.ValueData;
import com.insweat.hssd.lib.tree.flat.TreeNode;

public class ValueNodeTester extends AbstractPropertyTester {

    @Override
    public boolean test(Object receiver, String property, Object[] args,
            Object expectedValue) {
        TreeNode vn = (TreeNode)receiver;
        Class<?> thype;
        try {
            thype = Class.forName((String)expectedValue);
        } catch (ClassNotFoundException e) {
            log.errorf("No such class: '%s'", expectedValue);
            return false;
        }
        if("elementThype".equals(property)) {
            ValueData vd = ValueData.of(vn);
            return thype.isInstance(vd.element().thype());
        }
        else if("parentElementThype".equals(property)) {
            if(!vn.parent().isDefined()) {
                return false;
            }
            vn = vn.parent().get();
            ValueData vd = ValueData.of(vn);
            return thype.isInstance(vd.element().thype());
        }
        else {
            log.errorf("Unknown property: '%s'", property);
        }
        return false;
    }

}
