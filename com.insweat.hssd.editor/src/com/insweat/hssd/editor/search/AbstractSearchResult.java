package com.insweat.hssd.editor.search;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.ISearchResultListener;
import org.eclipse.search.ui.SearchResultEvent;


public abstract class AbstractSearchResult implements ISearchResult {

    private final List<ISearchResultListener> listeners = new ArrayList<>();
    private final ISearchQuery query;

    public AbstractSearchResult(ISearchQuery query) {
        this.query = query;
    }

    @Override
    public void addListener(ISearchResultListener l) {
        synchronized (listeners) {
            listeners.add(l);
        }
    }

    @Override
    public void removeListener(ISearchResultListener l) {
        synchronized (listeners) {
            listeners.remove(l);
        }
    }

    /**
     * Send the given <code>SearchResultEvent</code> to all registered search
     * result listeners.
     *
     * @param e the event to be sent
     *
     * @see ISearchResultListener
     */
    protected void fireChange(SearchResultEvent e) {
        HashSet<ISearchResultListener> copy = new HashSet<>();
        synchronized (listeners) {
            copy.addAll(listeners);
        }
        final Iterator<ISearchResultListener> listeners= copy.iterator();
        while (listeners.hasNext()) {
            ((ISearchResultListener) listeners.next()).searchResultChanged(e);
        }
    }

    @Override
    public ISearchQuery getQuery() {
        return query;
    }
}
