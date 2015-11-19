package com.insweat.hssd.editor.handlers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.IInputValidator;

import com.insweat.hssd.editor.editors.entry.EditorInput;
import com.insweat.hssd.editor.editors.entry.EntryEditor;
import com.insweat.hssd.editor.editors.hssd.HSSDEditor;
import com.insweat.hssd.editor.editors.hssd.ui.UIHelper;
import com.insweat.hssd.editor.models.spreadsheet.SpreadSheetTable;
import com.insweat.hssd.lib.essence.EntryData;
import com.insweat.hssd.lib.tree.EntryTree;
import com.insweat.hssd.lib.tree.structured.TreeNode;

public class HSSDEditorPasteNames extends AbstractCommandHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        HSSDEditor editor = getActiveHSSDEditor();
        if(editor == null) {
            return null;
        }
        
        String data = fromClipboard(editor);
        
        SpreadSheetTable table = new SpreadSheetTable(data);
        
        if(table.numCols() != 2) {
        	log.errorf("Can only paste a two column spreadsheet");
        	return null;
        }

        Set<TreeNode> updated = new HashSet<>();
        try {
            Map<Integer, String> newNames = new HashMap<>();
            for(int i = 0; i < table.numRows(); ++i) {
            	Integer id = Integer.valueOf(table.get(i, 0));
            	String name = table.get(i, 1);
            	newNames.put(id, name);
            }

            EntryTree tree = editor.getMasterCP().getEntryTree();
            for(Map.Entry<Integer, String> e: newNames.entrySet()) {
                int id = e.getKey();
                String name = e.getValue();
                TreeNode en = tree.nodesByID().get(id).get();
                if(en.name().equals(name)) {
                	continue;
                }

                TreeNode parentEN = en.parent().get();
                IInputValidator v = new UIHelper.EntryNameValidator(
                		parentEN, en.isLeaf());
                String error = v.isValid(name);
                if(error != null) {
                    log.errorf("Failed to paste names: %s", error);
                    return null;
                }

                tree.rename(en, name);
                updated.add(en);
            }

        }
        catch(Exception ex) {
        	log.errorf("Failed to paste names: %s", ex);
        	return null;
        }

        if(updated.isEmpty()) {
        	return null;
        }
        editor.markDirty();
        
        for(TreeNode en: updated) {
            editor.update(en);
            EntryData.of(en).markDirty();
        }

        // Make sure all references are updated.
        refreshAllEntryEditors();

        EntryEditor.multiApply((e) -> {
            final EditorInput input = (EditorInput)e.getEditorInput(); 
            if(updated.contains(input.getEntryNode())){
                e.updateTitle();
            }
            return false;
        }, null);
        
        return null;
    }

}
