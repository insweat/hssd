package com.insweat.hssd.editor.search;

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.ISearchResultPage;
import org.eclipse.search.ui.ISearchResultViewPart;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.part.Page;

public abstract class AbstractSearchResultPage
        extends Page
        implements ISearchResultPage {

    private StructuredViewer viewer;
    
    private Control control;
    private ISearchResultViewPart viewPart;
    private ISearchResult input;
    private String id;

    public ISearchResult getInput() {
        return input;
    }

    @Override
    public Object getUIState() {
        return viewer.getSelection();
    }

    protected StructuredViewer getViewer() {
        return viewer;
    }
    
    protected void setViewer(StructuredViewer viewer) {
        this.viewer = viewer;
    }

    @Override
    public void setViewPart(ISearchResultViewPart part) {
        viewPart = part;
    }

    protected ISearchResultViewPart getViewPart() {
        return viewPart;
    }

    @Override
    public void restoreState(IMemento memento) {
        // pass
    }

    @Override
    public void saveState(IMemento memento) {
        // pass
    }

    @Override
    public void setID(String id) {
        this.id = id;
    }

    @Override
    public String getID() {
        return id;
    }

    @Override
    public String getLabel() {
        final ISearchResult result = getInput();
        if(result != null) {
            return result.getLabel();
        }
        return "";
    }

    @Override
    public Control getControl() {
        return control;
    }

    protected void setControl(Control control) {
        this.control = control;
    }

    @Override
    public void setFocus() {
        if(viewer != null) {
            viewer.getControl().setFocus();    
        }
    }
}
