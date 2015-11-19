package com.insweat.hssd.editor.handlers;

import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;

import com.insweat.hssd.editor.editors.entry.EditorInput;
import com.insweat.hssd.editor.editors.entry.EntryEditor;
import com.insweat.hssd.editor.editors.hssd.HSSDEditor;
import com.insweat.hssd.editor.util.S;
import com.insweat.hssd.lib.interop.Interop;
import com.insweat.hssd.lib.tree.structured.TreeNode;

public class HSSDEditorDelEntry extends AbstractCommandHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final HSSDEditor editor = getActiveHSSDEditor();
        if(editor == null) {
            return null;
        }
        
        ISelectionProvider sp = editor.getSite().getSelectionProvider();
        IStructuredSelection sel = (IStructuredSelection)sp.getSelection();
        
        final Shell shell = editor.getSite().getShell();
        
        final TreeNode[] nodes = Arrays.copyOf(
                sel.toArray(), sel.size(), TreeNode[].class);
        
        if(!warnDataLoss(shell, nodes)) {
            return null;
        }

        // Close any relevant entry editor
        final HashSet<TreeNode> nodeSet = new HashSet<>();
        for(TreeNode en: nodes) {
            nodeSet.add(en);
        }

        EntryEditor.multiApply((e) -> {
            final EditorInput input = (EditorInput)e.getEditorInput();
            TreeNode en = input.getEntryNode();
            while(en != null) {
                if(nodeSet.contains(en)) {
                    e.getSite().getPage().closeEditor(e, false);
                    break;
                }
				en = Interop.or(en.parent());
            }
            return false;
        }, null);

        // Removing entries
        for(TreeNode en: nodes) {
            en.owner().remove(en);
        }

        editor.markDirty();
        editor.refresh(null, false);
        
        // Make sure all references are updated
        refreshAllEntryEditors();
        
        return null;
    }

    private boolean warnDataLoss(Shell shell, TreeNode ... nodes) {
        final String s = S.join(", ", (n) -> n.name(), nodes);
        final S msg = new S();
        msg.addf("The following entries and their children will be removed:%n");
        msg.addf("%s%n%n", s);
        msg.addf("All data under the removed entries will be lost, ");
        msg.addf("and all entries referencing them will be broken.%n");
        msg.addf("%nDo you want to do so?");
        return MessageDialog.openConfirm(shell, "Data Loss", msg.toString());
    }
}
