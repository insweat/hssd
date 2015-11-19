package com.insweat.hssd.editor.editors;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.services.ISourceProviderService;

import com.insweat.hssd.editor.services.IEditorSelectionSP;
import com.insweat.hssd.editor.util.Helper;

public class EditorSelectionSP implements SelectionListener {

    private final IEditorPart editor;
    private final ISelectionProvider selectionProvider;
    private final String var;

    public EditorSelectionSP(IEditorPart editor, ISelectionProvider sp, String var) {
        this.editor = editor;
        this.selectionProvider = sp;
        this.var = var;
        updateSelection(new StructuredSelection());
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
        updateSelection(e);
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
        updateSelection(e);
    }
    
    private void updateSelection(SelectionEvent e) {
        updateSelection(selectionProvider.getSelection());

    }
    
    private void updateSelection(ISelection sel) {
        final ISourceProviderService svc = Helper.getSPSvc();
        final IEditorSelectionSP sp = Helper.getSP(svc, var);
        sp.updateSelection(editor, sel);
    }
}
