package com.insweat.hssd.editor.editors;

import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class SelectCellEditor extends ComboBoxCellEditor {
    // This is to workaround a bug in ComboBoxellEditor, that the user can
    // click at a blank line to null-out the selection.
    private int lastSelection = -1;

    public SelectCellEditor(Composite parent) {
        super(parent, new String[0], SWT.READ_ONLY);
        setActivationStyle(
                DROP_DOWN_ON_KEY_ACTIVATION |
                DROP_DOWN_ON_MOUSE_ACTIVATION |
                DROP_DOWN_ON_PROGRAMMATIC_ACTIVATION |
                DROP_DOWN_ON_TRAVERSE_ACTIVATION);
    }
    
    @Override
    protected Object doGetValue() {
        final String[] items = getItems();
        final Object value = super.doGetValue();
        if(value == null) {
            return "";
        }
        Integer index = (Integer)value;
        if(index < 0 || index >= items.length) {
            if(lastSelection < 0 || lastSelection >= items.length) {
                return "";
            }
            index = lastSelection;
        }
        return items[index];
    }

    @Override
    protected void doSetValue(Object value) {
        final String[] items = getItems();
        int sel = 0;
        for(int i = 0; i < items.length; ++i) {
            if(items[i].equals(value)) {
                sel = i;
                break;
            }
        }
        super.doSetValue(sel);
        lastSelection = sel;
    }
}
