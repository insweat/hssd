package com.insweat.hssd.editor.handlers;

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;

import scala.collection.JavaConversions;

import com.insweat.hssd.editor.editors.hssd.AddEntryDialog;
import com.insweat.hssd.editor.editors.hssd.HSSDEditor;
import com.insweat.hssd.editor.services.IDService;
import com.insweat.hssd.editor.util.Helper;
import com.insweat.hssd.lib.essence.EntryData;
import com.insweat.hssd.lib.essence.TraitThypeLike;
import com.insweat.hssd.lib.interop.Interop;
import com.insweat.hssd.lib.tree.EntryTree;
import com.insweat.hssd.lib.tree.structured.TreeNode;

public class HSSDEditorAddEntry extends AbstractCommandHandler {
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final IDService idSvc = Helper.getIDSvc();

        final HSSDEditor editor = getActiveHSSDEditor();
        if(editor == null) {
            return null;
        }
        
        ISelectionProvider sp = editor.getSite().getSelectionProvider();
        IStructuredSelection sel = (IStructuredSelection)sp.getSelection();
        TreeNode parent = (TreeNode)sel.getFirstElement();

        
        final Shell shell = editor.getSite().getShell();
        final long entryID = idSvc.acquire(Helper.getActiveProject(),
                IDService.Namespace.ENTRY_ID);
        
        final EntryTree tree = editor.getMasterCP().getEntryTree();
        if(parent == null) {
            if(!tree.root().isDefined()) {
                return null;
            }
            parent = tree.root().get();
        }
        final AddEntryDialog dialog = new AddEntryDialog(shell, editor, parent);
        if(AddEntryDialog.OK == dialog.open()) {
            parent = dialog.getParent();
            final EntryData parentED = EntryData.of(parent);
            final TreeNode en = parent.owner().insert(
                    Interop.opt(parent),
                    dialog.getName(),
                    dialog.isLeaf());
            final EntryData ed = new EntryData(parentED.schema(), en, entryID);
            en.data_$eq(ed);

            final List<TraitThypeLike> traits = dialog.getTraits();
            ed.insertTraits(JavaConversions.iterableAsScalaIterable(traits));

            ed.markDirty();
            editor.markDirty();
            if(parent == parent.owner().root().get()) {
                editor.refresh(null, true);
            }
            else {
                editor.refresh(parent, true);
            }
            
        }
        return null;
    }
}
