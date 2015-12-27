package com.insweat.hssd.editor.handlers;

import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;

import com.insweat.hssd.editor.editors.hssd.HSSDEditor;
import com.insweat.hssd.editor.models.spreadsheet.SpreadSheetTable;
import com.insweat.hssd.editor.util.S;
import com.insweat.hssd.lib.essence.EntryData;

public class HSSDEditorCopyName extends AbstractCommandHandler implements
		IElementUpdater {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
	    return watchedExecute(()->{
	        final HSSDEditor editor = getActiveHSSDEditor();
	        if(editor == null) {
	            return null;
	        }

	        List<EntryData> sel = getSelectedEntries(); 
	        String data;
	        if(sel.size() == 1) {
	            data = sel.get(0).owner().name();
	        }
	        else {
	            SpreadSheetTable table = new SpreadSheetTable();
	            for(EntryData ed: sel) {
	                String id = String.valueOf(ed.entryID());
	                String name = ed.owner().name();
	                table.addRow(new String[]{id, name});
	            }
	            data = table.toString();
	        }

	        intoClipboard(editor, data);
	        return null;
	    });
	}

	@Override
	@SuppressWarnings("rawtypes")
	public void updateElement(UIElement element, Map parameters) {
		List<EntryData> eds = getSelectedEntries();
		if(eds.size() == 1) {
            EntryData ed = eds.get(0);
            String label = S.fmt("Name: %s", ed.owner().name());
            element.setText(label);
		}
		else {
            element.setText("Copy Names");
		}
	}
}
