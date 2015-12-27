package com.insweat.hssd.editor.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.insweat.hssd.editor.editors.entry.EntryEditor;
import com.insweat.hssd.lib.essence.ValueData;
import com.insweat.hssd.lib.tree.flat.TreeNode;

public class EntryEditorInheritElements extends AbstractCommandHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        return watchedExecute(()->{
            final EntryEditor editor = getActiveEntryEditor();
            ISelectionProvider sp = editor.getSite().getSelectionProvider();
            IStructuredSelection sel = (IStructuredSelection)sp.getSelection();
            TreeNode parent = (TreeNode)sel.getFirstElement();

            ValueData parentVD = ValueData.of(parent);
            if(!parentVD.valueTree().isOverridden(parent.path())) {
                return null;
            }

            if(parent.childCount() > 0 &&
                    !ElementHelper.warnRemoveElements(parent, editor)) {
                return null;
            }
            ElementHelper.removeChildren(parent, true);

            editor.refresh(parent, true);
            editor.markDirty();
            return null;
        });
    }

}
