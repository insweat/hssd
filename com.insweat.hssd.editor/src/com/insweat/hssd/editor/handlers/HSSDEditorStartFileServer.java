package com.insweat.hssd.editor.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.insweat.hssd.editor.editors.hssd.HSSDEditor;
import com.insweat.hssd.editor.util.Helper;


public class HSSDEditorStartFileServer extends AbstractCommandHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
	    return watchedExecute(()->{
	        HSSDEditor editor = Helper.getLastHSSDEditor();
	        if(editor == null) {
	            return null;
	        }
	        editor.flipFileServer(true);
	        return null;
	    });
	}

}
