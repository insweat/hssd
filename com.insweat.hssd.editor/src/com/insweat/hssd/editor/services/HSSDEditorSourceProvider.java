package com.insweat.hssd.editor.services;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISources;

import com.insweat.hssd.lib.tree.structured.TreeNode;


public class HSSDEditorSourceProvider
        extends AbstractSourceProvider
        implements IEditorSelectionSP {
    public final static String VAR_ACTIVE_HSSD_EDITOR = 
            "com.insweat.hssd.editor.handlers.hssd.activeHSSDEditor";

    public final static String VAR_HSSD_SELECTION = 
            "com.insweat.hssd.editor.handlers.hssd.hssdSel";

    private IEditorPart editor;
    private ISelection selection;

    @Override
    public void dispose() {
    }

    @Override
    public Map<String, Object> getCurrentState() {
        final Map<String, Object> map = new HashMap<>();
        map.put(VAR_ACTIVE_HSSD_EDITOR, editor);
        map.put(VAR_HSSD_SELECTION, selection);
        return map;
    }

    @Override
    public String[] getProvidedSourceNames() {
        return new String[]{
                VAR_ACTIVE_HSSD_EDITOR,
                VAR_HSSD_SELECTION
        };
    }

    @Override
    public void updateSelection(IEditorPart editor, ISelection selection) {
        if(editor == this.editor && selection == this.selection) {
            return;
        }

        final Object oldEditor = this.editor;
        final Object oldSelection = this.selection;

        this.editor = editor;
        this.selection = selection;

        conditionalFireVarChanged(
                VAR_ACTIVE_HSSD_EDITOR, oldEditor, this.editor);
        conditionalFireVarChanged(
                VAR_HSSD_SELECTION, oldSelection, this.selection);
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
}
