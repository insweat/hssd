package com.insweat.hssd.editor.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.insweat.hssd.editor.search.hssd.EntrySearch;

public class HSSDEditorFindFWDRefs extends HSSDEditorFindRefCommandHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		return doExecute(event, EntrySearch.Objective.FWD_REF);
	}

}
