package com.insweat.hssd.editor.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;

import com.insweat.hssd.editor.editors.entry.EditorInput;
import com.insweat.hssd.editor.editors.entry.EntryEditor;
import com.insweat.hssd.editor.editors.hssd.HSSDEditor;
import com.insweat.hssd.editor.editors.hssd.ui.UIHelper;
import com.insweat.hssd.lib.essence.EntryData;
import com.insweat.hssd.lib.interop.Interop;
import com.insweat.hssd.lib.tree.structured.TreeNode;

public class HSSDEditorRenameEntry extends AbstractCommandHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        HSSDEditor editor = getActiveHSSDEditor();
        if(editor == null) {
            return null;
        }
        
        ISelectionProvider sp = editor.getSite().getSelectionProvider();
        IStructuredSelection sel = (IStructuredSelection)sp.getSelection();
        TreeNode en = (TreeNode)sel.getFirstElement();
        
        final Shell shell = editor.getSite().getShell();
        final String newName = inputNewName(shell, en);
        if(newName != null) {
            en.owner().rename(en, newName);
            editor.update(en);
            editor.markDirty();
            EntryData.of(en).markDirty();
            
            // Make sure all references are updated.
            refreshAllEntryEditors();
            
            EntryEditor.multiApply((e) -> {
                final EditorInput input = (EditorInput)e.getEditorInput(); 
                if(en.equals(input.getEntryNode())){
                    e.updateTitle();
                }
                return false;
            }, null);
        }
        
        return null;
    }

    private String inputNewName(Shell shell, TreeNode node) {
        final InputDialog dialog = new InputDialog(
                shell, "Rename", "Input new name:", node.name(),
                new UIHelper.EntryNameValidator(
                        Interop.or(node.parent()),
                        node.isLeaf())
                );

        if(dialog.open() == InputDialog.OK) {
            return dialog.getValue();
        }
        return null;
    }
}
