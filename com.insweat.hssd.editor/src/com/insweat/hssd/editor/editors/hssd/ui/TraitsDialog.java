package com.insweat.hssd.editor.editors.hssd.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.insweat.hssd.editor.editors.hssd.HSSDEditor;
import com.insweat.hssd.lib.essence.TraitThypeLike;

public class TraitsDialog extends MessageDialog {

    private final HSSDEditor editor;
    private TableViewer viewer;
    private ISelection selection;
    
    private final List<TraitThypeLike> inheritedTraits = new ArrayList<>();

    public TraitsDialog(Shell parent, HSSDEditor editor) {
        super(parent, "Edit Traits", null, "Choose traits from below", NONE,
                new String[]{
                    IDialogConstants.OK_LABEL,
                    IDialogConstants.CANCEL_LABEL,
                }, 1);
        this.editor = editor;
    }

    @Override
    protected Control createCustomArea(Composite parent) {
        viewer = UIHelper.createTraitSelect(parent, editor, inheritedTraits);
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                selection = event.getSelection();
            }
        });
        viewer.setSelection(selection);
        return parent;
    }
    
    public List<TraitThypeLike> getSelection() {
        final IStructuredSelection sel = (IStructuredSelection)selection;
        final Object[] selObjs = sel.toArray();
        final List<TraitThypeLike> rv = new ArrayList<>();
        for(Object o : selObjs) {
            rv.add((TraitThypeLike)o);
        }
        return rv;
    }

    public void setSelection(Collection<TraitThypeLike> traits) {
        final List<TraitThypeLike> toSelect = new ArrayList<>();
        toSelect.addAll(traits);

        // NB At this point, createCustomArea may not have been called.
        //    We have to remember selection for now.
        selection = new StructuredSelection(toSelect);
        if(viewer != null) {
            viewer.setSelection(selection);
        }
    }

    public void setInheritedTraits(Collection<TraitThypeLike> traits) {
        inheritedTraits.clear();
        inheritedTraits.addAll(traits);
    }
}
