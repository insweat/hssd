package com.insweat.hssd.editor.handlers;

import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;

import com.insweat.hssd.editor.editors.hssd.HSSDEditor;
import com.insweat.hssd.editor.util.S;
import com.insweat.hssd.lib.essence.EntryData;

public class HSSDEditorCopyCaption extends AbstractCommandHandler implements
		IElementUpdater {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
	    return watchedExecute(()->{
	        final HSSDEditor editor = getActiveHSSDEditor();
	        if(editor == null) {
	            return null;
	        }

	        String data = String.valueOf(getSelectionCaption());
	        intoClipboard(editor, data);

	        return null;
	    });
	}

	@Override
	@SuppressWarnings("rawtypes")
	public void updateElement(UIElement element, Map parameters) {
		String label = S.fmt("Caption: %s", getSelectionCaption());
		element.setText(label);
	}

	private String getSelectionCaption() {
		try {
			EntryData ed = getSelectedEntry();
			return ed.caption();
		}
		catch(Exception e) {
			String s = "%s(%s)";
			return S.fmt(s, e.getClass().getSimpleName(), e.getMessage());
		}
	}
}
