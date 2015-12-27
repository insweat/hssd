package com.insweat.hssd.editor.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;

import scala.Option;

import com.insweat.hssd.editor.editors.entry.EntryEditor;
import com.insweat.hssd.editor.editors.entry.EntryEditorEditingSupport;
import com.insweat.hssd.editor.models.spreadsheet.SpreadSheetTable;
import com.insweat.hssd.editor.util.LogSupport;
import com.insweat.hssd.editor.util.S;
import com.insweat.hssd.lib.essence.CollectionThypeLike;
import com.insweat.hssd.lib.essence.SimpleThypeLike;
import com.insweat.hssd.lib.essence.Thype;
import com.insweat.hssd.lib.essence.ValueData;
import com.insweat.hssd.lib.essence.thypes.ArrayThype;
import com.insweat.hssd.lib.essence.thypes.MapThype;
import com.insweat.hssd.lib.tree.TreeNodeLike;
import com.insweat.hssd.lib.tree.flat.TreeNode;

public class EntryEditorPasteContent extends AbstractCommandHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        return watchedExecute(()->{
            final EntryEditor editor = getActiveEntryEditor();
            if(editor == null) {
                return null;
            }

            String data = fromClipboard(editor);

            ISelectionProvider sp = editor.getSite().getSelectionProvider();
            IStructuredSelection sel = (IStructuredSelection)sp.getSelection();

            TreeNode selection = (TreeNode)sel.getFirstElement();
            ValueData selectionVD = ValueData.of(selection);
            Thype selThype = selectionVD.element().thype();
            
            try{
                if(selThype instanceof CollectionThypeLike) {
                    selection = pasteCollection(editor, selection, data);
                }
                else if(selThype instanceof SimpleThypeLike) {
                    selection = pasteSimple(editor, selection, data);
                }
                else {
                    ElementHelper.unsupportedThype(selThype);
                }
            }
            catch (Exception e) {
                ElementHelper.panic(log, "pasting content", e);
                throw e;
            }
            
            editor.refresh(selection, false);
            editor.markDirty();

            return null; 
        });
    }
    
    private TreeNode pasteSimple(EntryEditor editor, TreeNode sel, String data) {
        EntryEditorEditingSupport.writeValue(sel, data, log);
    	return sel;
    }
    
    private TreeNode pasteCollection(EntryEditor editor, TreeNode parent, String data) {
        Shell shell = editor.getSite().getShell();
    	Thype parentThype = ValueData.of(parent).element().thype();
        SpreadSheetTable table = new SpreadSheetTable(data);
        if(parentThype instanceof ArrayThype) {
            if(table.numCols() != 1) {
                String title = "Invalid content";
                S msg = new S();
                msg.addf("You can only paste a single column table" +
                        " onto an array node!");
                MessageDialog.openError(shell, title, msg.toString());
                return parent;
            }
            
            parent = ElementHelper.copyOnNeed(parent, editor);
            if(parent == null) {
                return null;
            }

            addArrayElements(parent, table, range(0, table.numRows()), log);
        }
        else if(parentThype instanceof MapThype) {
            if(table.numCols() != 2) {
                String title = "Invalid content";
                S msg = new S();
                msg.addf("You can only paste a two-column table" +
                        " onto a map node!");
                MessageDialog.openError(shell, title, msg.toString());
                return null;
            }

            parent = ElementHelper.copyOnNeed(parent, editor);
            if(parent == null) {
                return null;
            }
            
            addMapElements(parent, table, range(0, table.numRows()), log);
        }
        else {
            ElementHelper.unsupportedThype(parentThype);
            return null;
        }
    
        return parent;
    }

	private static List<Integer> range(int beg, int end) {
		List<Integer> rv = new ArrayList<>(end - beg);
		for(int i = beg; i < end; ++i) {
			rv.add(i);
		}
		return rv;
	}
	
	public static void addArrayElements(
			TreeNode parent, SpreadSheetTable table, List<Integer> rows,
			LogSupport log) {
        int valCol = table.numCols() - 1;
		Map<String, String> values = new HashMap<>();
        for(int i = 0; i < rows.size(); ++i) {
        	String name;
        	if(table.numCols() > 1) {
        		name = table.get(rows.get(i), 0);
                int index = name.lastIndexOf('.');
                if(index != -1) {
                    name = name.substring(index + 1);
                }
        	}
        	else {
        		name = S.fmt("%s", i);
        	}
        	values.put(name, table.get(rows.get(i), valCol));
        }

        for(Map.Entry<String, String> e: values.entrySet()) {
            String name = e.getKey();
            String value = e.getValue();
            
            Option<TreeNodeLike> optVN = parent.findChild(name);
            TreeNode vn;
            if(!optVN.isDefined()) {
                vn = ElementHelper.addElement(parent, name);
            }
            else {
            	vn = (TreeNode)optVN.get();
            }
            
            EntryEditorEditingSupport.writeValue(vn, value, log);
        }
        
        ElementHelper.removeChildren(parent, values.keySet());
	}

	public static void addMapElements(
			TreeNode parent, SpreadSheetTable table, List<Integer> rows,
			LogSupport log) {
		Map<String, String> values = new HashMap<>();
        for(int i = 0; i < rows.size(); ++i) {
        	String name = table.get(rows.get(i), 0);
        	int index = name.lastIndexOf('.');
        	if(index != -1) {
        		name = name.substring(index + 1);
        	}
            name = ElementHelper.mkName(parent, name);
            values.put(name, table.get(rows.get(i), 1));
        }

        for(Map.Entry<String, String> e: values.entrySet()) {
            String name = e.getKey();
            String value = e.getValue();
            TreeNode vn = ElementHelper.addElement(parent, name);
            EntryEditorEditingSupport.writeValue(vn, value, log);
        }
        
        ElementHelper.removeChildren(parent, values.keySet());
	}
}
