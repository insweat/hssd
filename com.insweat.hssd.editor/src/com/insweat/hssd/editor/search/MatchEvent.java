package com.insweat.hssd.editor.search;

import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.SearchResultEvent;

public class MatchEvent<T> extends SearchResultEvent {

    private static final long serialVersionUID = 2765338922851318978L;

    public static final int ADDED = 1;
    public static final int REMOVED = 2;

    private int kind;
    private T[] matches;

    public MatchEvent(ISearchResult searchResult) {
        super(searchResult);
    }

    @SuppressWarnings("unchecked")
    public MatchEvent<T> updated(int kind, T ... matches) {
        this.kind = kind;
        this.matches = matches;
        return this;
    }
    
    public int getKind() {
        return kind;
    }
    
    public T[] getMatches() {
        return matches;
    }
}
