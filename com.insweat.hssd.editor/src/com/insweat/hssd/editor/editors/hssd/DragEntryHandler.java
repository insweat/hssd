package com.insweat.hssd.editor.editors.hssd;

import java.util.Iterator;
import java.util.StringJoiner;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;

import com.insweat.hssd.lib.essence.EntryData;
import com.insweat.hssd.lib.tree.structured.TreeNode;

public class DragEntryHandler implements DragSourceListener {

    private final Viewer viewer;
    
    public DragEntryHandler(Viewer viewer) {
        this.viewer = viewer;
    }
    
    @Override
    public void dragStart(DragSourceEvent event) {
        // pass
    }

    @Override
    public void dragSetData(DragSourceEvent event) {
        IStructuredSelection sel = (IStructuredSelection) viewer.getSelection();
        StringJoiner sj = new StringJoiner(",");
        for(Iterator<?> itr = sel.iterator(); itr.hasNext(); ) {
            TreeNode en = (TreeNode)itr.next();
            EntryData ed = EntryData.of(en);
            sj.add(String.valueOf(ed.entryID()));
        }
        event.data = sj.toString();
    }

    @Override
    public void dragFinished(DragSourceEvent event) {
        // pass        
    }

}
