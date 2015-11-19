package com.insweat.hssd.editor.testers.clip;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;

import com.insweat.hssd.editor.testers.AbstractPropertyTester;
import com.insweat.hssd.editor.util.Helper;

public class ClipboardTester extends AbstractPropertyTester {

    @Override
    public boolean test(Object receiver, String property, Object[] args,
            Object expectedValue) {
        if("hasTextContent".equals(property)) {
            IWorkbenchPart part = Helper.getActivePart();
            if(part == null) {
                return false;
            }
            
            Display display = part.getSite().getShell().getDisplay();
            Clipboard clip = new Clipboard(display);
            Object content = clip.getContents(TextTransfer.getInstance());
            clip.dispose();
            return expectedValue.equals(content != null);
        }
        return false;
    }

}
