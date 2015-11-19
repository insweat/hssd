package com.insweat.hssd.editor.handlers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;

import com.insweat.hssd.editor.editors.hssd.HSSDEditor;
import com.insweat.hssd.editor.util.Helper;
import com.insweat.hssd.editor.views.multidiff.MultiDiffInput;
import com.insweat.hssd.editor.views.multidiff.MultiDiffView;
import com.insweat.hssd.lib.essence.EntryData;
import com.insweat.hssd.lib.essence.SchemaLike;
import com.insweat.hssd.lib.essence.TraitThypeLike;
import com.insweat.hssd.lib.interop.Interop;
import com.insweat.hssd.lib.tree.structured.TreeNode;

public class HSSDEditorMultiDiff extends AbstractCommandHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        HSSDEditor editor = getActiveHSSDEditor();
        if(editor == null) {
            return null;
        }

        ISelectionProvider sp = editor.getSite().getSelectionProvider();
        IStructuredSelection sel = (IStructuredSelection)sp.getSelection();

        List<TreeNode> entries = new ArrayList<>();
        Set<TraitThypeLike> traits = new HashSet<>();

        for(Iterator<?> itr = sel.iterator(); itr.hasNext();) {
            TreeNode en = (TreeNode)itr.next();
            entries.add(en);

            EntryData ed = EntryData.of(en);
            Interop.foreach(ed.traits(), tr -> {
                traits.add(tr);
            });
        }
        
        SchemaLike sch = editor.getMasterCP().getActiveSchema();
        MultiDiffInput input = new MultiDiffInput(sch, entries, traits);

        try {
            IViewPart v = Helper.getActiveWBPage().showView(MultiDiffView.ID);
            MultiDiffView view = (MultiDiffView)v;
            view.setInput(input);
        } catch (PartInitException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

}
