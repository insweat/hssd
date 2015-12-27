package com.insweat.hssd.editor.handlers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import scala.Tuple2;

import com.insweat.hssd.editor.editors.entry.EntryEditor;
import com.insweat.hssd.editor.editors.entry.EntryEditorEditingSupport;
import com.insweat.hssd.editor.models.IntAwareCmp;
import com.insweat.hssd.editor.models.spreadsheet.SpreadSheetTable;
import com.insweat.hssd.lib.essence.CollectionThypeLike;
import com.insweat.hssd.lib.essence.SimpleThypeLike;
import com.insweat.hssd.lib.essence.Thype;
import com.insweat.hssd.lib.essence.ValueData;
import com.insweat.hssd.lib.essence.thypes.ArrayThype;
import com.insweat.hssd.lib.essence.thypes.MapThype;
import com.insweat.hssd.lib.interop.Interop;
import com.insweat.hssd.lib.tree.flat.TreeNode;



public class EntryEditorCopyContent extends AbstractCommandHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        return watchedExecute(()-> {
            final EntryEditor editor = getActiveEntryEditor();
            if(editor == null) {
                return null;
            }
            
            ISelectionProvider sp = editor.getSite().getSelectionProvider();
            IStructuredSelection sel = (IStructuredSelection)sp.getSelection();
            
            TreeNode selection = (TreeNode)sel.getFirstElement();
            ValueData selectionVD = ValueData.of(selection);
            
            final Thype selThype = selectionVD.element().thype();
            if(selThype instanceof CollectionThypeLike) {
                String data = parseCollection(selection);
                intoClipboard(editor, data);
            }
            else if(selThype instanceof SimpleThypeLike) {
                String data = parseSimple(selection);
                intoClipboard(editor, data);
            }
            else {
                ElementHelper.unsupportedThype(selThype);
            }

            return null;
        });
    }
    
    private String parseSimple(TreeNode sel) {
    	return Interop.literal(EntryEditorEditingSupport.readValue(sel));
    }
    
    private String parseCollection(TreeNode parent) {
        SpreadSheetTable table = new SpreadSheetTable();
    	Thype parentThype = ValueData.of(parent).element().thype();
    	List<Tuple2<String, String>> values = new ArrayList<>();

        Interop.foreach(parent.children(), vn -> {
            String name = vn.name();
            String value = Interop.literal(
                    EntryEditorEditingSupport.readValue(vn)
            );
            values.add(new Tuple2<>(name, value));
        });

        values.sort(new Comparator<Tuple2<String, String>>(){
        	private final IntAwareCmp cmp = new IntAwareCmp();

        	@Override
        	public int compare(Tuple2<String, String> o1, Tuple2<String, String> o2) {
        		return cmp.compare(o1._1(), o2._1());
        	}	
        });

        for(Tuple2<String, String> v: values) {
            if(parentThype instanceof ArrayThype) {
                table.addRow(new String[]{ v._2() });
            }
            else if(parentThype instanceof MapThype) {
                table.addRow(new String[]{ v._1(), v._2() });
            }
            else {
                ElementHelper.unsupportedThype(parentThype);
            }
        }

        return table.toString();
    }

}
