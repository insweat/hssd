package com.insweat.hssd.editor.search;

import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;

public abstract class AbstractSearchQuery implements ISearchQuery {

    private String label;
    private ISearchResult result;

    public AbstractSearchQuery() {
        setLabel("");
    }

    public AbstractSearchQuery(String label) {
        setLabel(label);
    }

    @Override
    public String getLabel() {
        return label;
    }
    
    protected void setLabel(String label) {
        this.label = label;
    }

    @Override
    public boolean canRerun() {
        return false;
    }

    @Override
    public boolean canRunInBackground() {
        return true;
    }

    @Override
    public ISearchResult getSearchResult() {
        if(result == null) {
            return getEmptyResult();
        }
        return result;
    }
    
    protected void setSearchResult(ISearchResult result) {
        this.result = result;
    }

    protected abstract ISearchResult getEmptyResult();
}
