package com.insweat.hssd.editor.handlers;

import java.util.HashSet;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;

import scala.collection.JavaConversions;

import com.insweat.hssd.editor.editors.hssd.HSSDEditor;
import com.insweat.hssd.editor.editors.hssd.ui.TraitsDialog;
import com.insweat.hssd.editor.util.S;
import com.insweat.hssd.editor.util.TraitsHelper;
import com.insweat.hssd.lib.essence.EntryData;
import com.insweat.hssd.lib.essence.TraitThypeLike;
import com.insweat.hssd.lib.interop.Interop;
import com.insweat.hssd.lib.tree.structured.TreeNode;

public class HSSDEditorEditTraits extends AbstractCommandHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        HSSDEditor editor = getActiveHSSDEditor();
        if(editor == null) {
            return null;
        }
        
        ISelectionProvider sp = editor.getSite().getSelectionProvider();
        IStructuredSelection sel = (IStructuredSelection)sp.getSelection();
        TreeNode en = (TreeNode)sel.getFirstElement();
        EntryData ed = EntryData.of(en);

        Shell shell = editor.getSite().getShell();
        TraitsDialog dialog = new TraitsDialog(shell, editor);
        
        HashSet<TraitThypeLike> currentTraits = getCurrentTraits(ed);
        dialog.setSelection(currentTraits);
        dialog.setInheritedTraits(getInheritedTraits(ed));

        if(0 == dialog.open()) {
            final List<TraitThypeLike> traits = dialog.getSelection();

            final HashSet<TraitThypeLike> toRemove = currentTraits;
            for(TraitThypeLike tr: traits) {
                toRemove.remove(tr);
            }
            
            if(!warnDataLoss(shell, toRemove)) {
                return null;
            }

            ed.removeTraits(JavaConversions.iterableAsScalaIterable(toRemove));
            ed.insertTraits(JavaConversions.iterableAsScalaIterable(traits));
            ed.packValueTree();
            ed.markDirty();

            editor.markDirty();
            refreshAllEntryEditors();
        }

        return null;
    }

    private HashSet<TraitThypeLike> getCurrentTraits(EntryData ed) {
        final HashSet<TraitThypeLike> rv = new HashSet<>();
        Interop.foreach(ed.immediateTraits(), (TraitThypeLike tr) -> {
            rv.add(tr);
        });
        return rv;
    }
    
    private HashSet<TraitThypeLike> getInheritedTraits(EntryData ed) {
        final HashSet<TraitThypeLike> rv = new HashSet<>();
        Interop.foreach(ed.inheritedTraits(), (TraitThypeLike tr) -> {
            rv.add(tr);
        });
        return rv;
    }

    private boolean warnDataLoss(
            Shell shell, HashSet<TraitThypeLike> toRemove) {
        if(toRemove.isEmpty()) {
            return true;
        }
        final TraitThypeLike[] sa = TraitsHelper.sortedArray(toRemove);
        final String s = S.join(", ", (tr) -> tr.caption(), sa);
        final S msg = new S();
        msg.addf("Your operation will remove these traits from the node:%n");
        msg.addf("%s%n%n", s);
        msg.addf("All data under them will be lost. Do you want to do so?");
        return MessageDialog.openConfirm(shell, "Data Loss", msg.toString());
    }
}
