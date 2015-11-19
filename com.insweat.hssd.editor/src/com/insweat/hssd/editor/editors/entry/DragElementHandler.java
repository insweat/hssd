package com.insweat.hssd.editor.editors.entry;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;

import com.insweat.hssd.lib.tree.flat.TreeNode;

public class DragElementHandler implements DragSourceListener {

    private final EntryEditor editor;
    
    public DragElementHandler(EntryEditor editor) {
        this.editor = editor;
    }
    
    @Override
    public void dragStart(DragSourceEvent event) {
        // pass
    }

    @Override
    public void dragSetData(DragSourceEvent event) {
    	ISelectionProvider sp = editor.getSite().getSelectionProvider();
        IStructuredSelection sel = (IStructuredSelection) sp.getSelection();
        TreeNode en = (TreeNode)sel.getFirstElement();
        event.data = en.path().toString();
    }

    @Override
    public void dragFinished(DragSourceEvent event) {
        // pass        
    }

}
