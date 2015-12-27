package com.insweat.hssd.editor.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.insweat.hssd.editor.editors.entry.EntryEditor;
import com.insweat.hssd.lib.tree.flat.TreeNode;

public class EntryEditorAddElement extends AbstractCommandHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        return watchedExecute(() ->{
            final EntryEditor editor = getActiveEntryEditor();
            ISelectionProvider sp = editor.getSite().getSelectionProvider();
            IStructuredSelection sel = (IStructuredSelection)sp.getSelection();
            TreeNode parent = (TreeNode)sel.getFirstElement();

            String name = ElementHelper.mkName(parent, null);
            
            parent = ElementHelper.copyOnNeed(parent, editor);
            if(parent == null) {
                return null;
            }

            ElementHelper.addElement(parent, name);

            editor.refresh(parent, true);
            editor.markDirty();
            return null;
        });
    }

}
