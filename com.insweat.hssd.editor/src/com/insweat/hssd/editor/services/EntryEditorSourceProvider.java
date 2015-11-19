package com.insweat.hssd.editor.services;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISources;

import com.insweat.hssd.lib.essence.Thype;
import com.insweat.hssd.lib.essence.ValueData;
import com.insweat.hssd.lib.tree.flat.TreeNode;

public class EntryEditorSourceProvider
        extends AbstractSourceProvider
        implements IEditorSelectionSP {

    public final static String VAR_ACTIVE_ENTRY_EDITOR = 
            "com.insweat.hssd.editor.handlers.entry.activeEntryEditor";
    
    public final static String VAR_ENTRY_SELECTION = 
            "com.insweat.hssd.editor.handlers.entry.entrySel";
    
    public final static String VAR_ENTRY_SELECTION_ELEMENT_THYPE = 
            "com.insweat.hssd.editor.handlers.entry.selElemThype";

    public final static String VAR_ENTRY_SELECTION_PARENT_ELEMENT_THYPE = 
            "com.insweat.hssd.editor.handlers.entry.selParentElemThype";

    private IEditorPart editor;
    private ISelection selection;

    @Override
    public void dispose() {
    }

    @Override
    public Map<String, Object> getCurrentState() {
        final Map<String, Object> map = new HashMap<>();
        map.put(VAR_ACTIVE_ENTRY_EDITOR, editor);
        map.put(VAR_ENTRY_SELECTION, editor);
        map.put(
                VAR_ENTRY_SELECTION_ELEMENT_THYPE,
                getEntrySelectionElementThype()
        );
        map.put(
                VAR_ENTRY_SELECTION_PARENT_ELEMENT_THYPE,
                getEntrySelectionParentElementThype()
        );
        return map;
    }

    @Override
    public String[] getProvidedSourceNames() {
        return new String[] {
                VAR_ACTIVE_ENTRY_EDITOR,
                VAR_ENTRY_SELECTION,
                VAR_ENTRY_SELECTION_ELEMENT_THYPE,
                VAR_ENTRY_SELECTION_PARENT_ELEMENT_THYPE,
        };
    }

    @Override
    public void updateSelection(IEditorPart editor, ISelection selection) {
        if(editor == this.editor && selection == this.selection) {
            return;
        }

        final Object oldEditor = this.editor;
        final Object oldSelection = this.selection;
        
        final Thype oldSET = getEntrySelectionElementThype();
        final Thype oldSEPT = getEntrySelectionParentElementThype();

        this.editor = editor;
        this.selection = selection;
        
        final Thype newSET = getEntrySelectionElementThype();
        final Thype newSEPT = getEntrySelectionParentElementThype();
 
        conditionalFireVarChanged(
                VAR_ACTIVE_ENTRY_EDITOR, oldEditor, this.editor);
        
        conditionalFireVarChanged(
                VAR_ENTRY_SELECTION, oldSelection, this.selection);
        
        conditionalFireVarChanged(
                VAR_ENTRY_SELECTION_ELEMENT_THYPE, oldSET, newSET);
        conditionalFireVarChanged(
                VAR_ENTRY_SELECTION_PARENT_ELEMENT_THYPE, oldSEPT, newSEPT);
    }
    
    @Override
    public IEditorPart getEditor() {
        return editor;
    }

    @Override
    public IStructuredSelection getSelection() {
        if(selection instanceof IStructuredSelection) {
            return (IStructuredSelection)selection;
        }
        return null;
    }

    @Override
    public TreeNode getFirstSelected() {
        if(selection instanceof IStructuredSelection) {
            final IStructuredSelection sel = (IStructuredSelection)selection;
            if(sel.getFirstElement() instanceof TreeNode) {
                return (TreeNode)sel.getFirstElement();
            }
        }
        return null;
    }

    @Override
    public <T> T[] selToArray(Class<T> clazz, T[] array) {
        final IStructuredSelection sel = getSelection();
        return IEditorSelectionSP.Helper.selToArray(sel, clazz, array);
    }

    private void conditionalFireVarChanged(
            String var, Object oldValue, Object newValue) {
        if(oldValue != newValue) {
            fireSourceChanged(ISources.WORKBENCH, var, newValue);
        }
    }

    private Thype getEntrySelectionElementThype() {
        final TreeNode vn = getFirstSelected();
        if(vn == null || getSelection().size() != 1) {
            return null;
        }

        final ValueData vd = ValueData.of(vn);
        return vd.element().thype();
    }
    
    private Thype getEntrySelectionParentElementThype() {
        final TreeNode vn = getFirstSelected();
        if(vn == null) {
            return null;
        }

        if(!vn.hasParent()) {
            return null;
        }

        final TreeNode parent = vn.parent().get();

        @SuppressWarnings("rawtypes")
        final Iterator itr = getSelection().iterator();
        while(itr.hasNext()) {
            final TreeNode node = (TreeNode)itr.next();
            if(!node.hasParent()) {
                return null;
            }
            if(!node.parent().get().equals(parent)) {
                return null;
            }
        }
 
        final ValueData vd = ValueData.of(parent);
        return vd.element().thype();
    }
}
