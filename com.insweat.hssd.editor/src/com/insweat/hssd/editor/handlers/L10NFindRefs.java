package com.insweat.hssd.editor.handlers;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.search.ui.NewSearchUI;

import com.insweat.hssd.editor.models.l10n.Row;
import com.insweat.hssd.editor.search.hssd.EntrySearch;
import com.insweat.hssd.editor.search.hssd.EntrySearchQuery;
import com.insweat.hssd.editor.views.l10n.L10NView;

public class L10NFindRefs extends AbstractCommandHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		L10NView view = getL10NView();
		if(view == null) {
			return null;
		}
		
		ISelectionProvider sp = view.getSite().getSelectionProvider();
		IStructuredSelection sel = (IStructuredSelection)sp.getSelection();
		Row row = (Row)sel.getFirstElement();
		
		EntrySearch.Pattern pat = new EntrySearch.Pattern(
				String.valueOf(row.cols[0]), false, false);
		Set<EntrySearch.Objective> objs = new HashSet<>();
		objs.add(EntrySearch.Objective.LSTR_REF);
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
	}

}
