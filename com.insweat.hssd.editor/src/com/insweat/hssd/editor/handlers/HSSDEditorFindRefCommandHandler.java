package com.insweat.hssd.editor.handlers;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.search.ui.NewSearchUI;

import com.insweat.hssd.editor.editors.hssd.HSSDEditor;
import com.insweat.hssd.editor.search.hssd.EntrySearch;
import com.insweat.hssd.editor.search.hssd.EntrySearchQuery;
import com.insweat.hssd.lib.essence.EntryData;
import com.insweat.hssd.lib.tree.structured.TreeNode;

public abstract class HSSDEditorFindRefCommandHandler
		extends AbstractCommandHandler {

	public Object doExecute(ExecutionEvent event, EntrySearch.Objective obj) {
	    return watchedExecute(()->{
	        final HSSDEditor editor = getActiveHSSDEditor();
	        if(editor == null) {
	            return null;
	        }

	        ISelectionProvider sp = editor.getSite().getSelectionProvider();
	        IStructuredSelection sel = (IStructuredSelection)sp.getSelection();
	        TreeNode ref = (TreeNode)sel.getFirstElement();
	        EntryData ed = EntryData.of(ref);

	        EntrySearch.Pattern pat = new EntrySearch.Pattern(
	                String.valueOf(ed.entryID()), false, false);
	        Set<EntrySearch.Objective> objs = new HashSet<>();
	        objs.add(obj);
	        EntrySearch.Constraint cons = new EntrySearch.Constraint(false, null);
	        EntrySearchQuery query = new EntrySearchQuery(pat, objs, cons);
	        try {
	            if(query.canRunInBackground()) {
	                NewSearchUI.runQueryInBackground(query);
	            }
	            else {
	                NewSearchUI.runQueryInForeground(null, query);
	            }
	        }
	        catch (Exception e) {
	            throw new RuntimeException(e);
	        }
	        return null;
	    });
	}
}
