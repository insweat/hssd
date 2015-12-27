package com.insweat.hssd.editor.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import scala.Option;

import com.insweat.hssd.editor.editors.entry.EntryEditor;
import com.insweat.hssd.editor.editors.hssd.HSSDEditor;
import com.insweat.hssd.lib.essence.EntryData;
import com.insweat.hssd.lib.essence.ValueData;
import com.insweat.hssd.lib.essence.thypes.ReferenceThype;
import com.insweat.hssd.lib.tree.flat.TreeNode;

public class EntryEditorGotoEntry extends AbstractCommandHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        return watchedExecute(() -> {
            final HSSDEditor hssdEditor = getActiveHSSDEditor();
            if(hssdEditor == null) {
                return null;
            }
            
            final EntryEditor editor = getActiveEntryEditor();
            if(editor == null) {
                return null;
            }
            
            ISelectionProvider sp = editor.getSite().getSelectionProvider();
            IStructuredSelection sel = (IStructuredSelection)sp.getSelection();
            
            TreeNode vn = (TreeNode)sel.getFirstElement();
            ValueData vd = ValueData.of(vn);
            
            Option<EntryData> ed = vd.asRef();

            if(!ed.isDefined()) {
                if(!(vd.element().thype() instanceof ReferenceThype)) {
                    log.warnf("Invalid element thype for selection: %s.", vn);
                    return null;
                }

                ReferenceThype thype = (ReferenceThype)vd.element().thype();

                Object value = vd.value().value();
                value = thype.fixed(value);
                if(value == null) {
                    log.warnf("Invalid reference value: %s", vd.value().value());
                    return null;
                }

                log.warnf("Entry not found: %s", value);
                return null;
            }
            
            hssdEditor.openEntryEditor(ed.get().owner(), null);

            return null;
        });
    }

}
