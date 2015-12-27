package com.insweat.hssd.editor.handlers;

import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;

import com.insweat.hssd.editor.editors.hssd.HSSDEditor;
import com.insweat.hssd.editor.util.S;
import com.insweat.hssd.lib.essence.EntryData;

public class HSSDEditorCopyID extends AbstractCommandHandler implements
		IElementUpdater {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
	    return watchedExecute(()->{
	        final HSSDEditor editor = getActiveHSSDEditor();
	        if(editor == null) {
	            return null;
	        }

	        EntryData ed = getSelectedEntry();
	        String data = String.valueOf(ed.entryID());

	        intoClipboard(editor, data);
	        return null;
	    });
	}

	@Override
	@SuppressWarnings("rawtypes")
	public void updateElement(UIElement element, Map parameters) {
		EntryData ed = getSelectedEntry();
		String label = S.fmt("ID: %s", ed.entryID());
		element.setText(label);
	}
}
