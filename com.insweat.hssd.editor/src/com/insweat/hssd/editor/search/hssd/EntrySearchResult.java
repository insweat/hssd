package com.insweat.hssd.editor.search.hssd;

import java.util.HashSet;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.search.ui.ISearchQuery;

import com.insweat.hssd.editor.editors.hssd.HSSDEditor;
import com.insweat.hssd.editor.search.AbstractSearchResult;
import com.insweat.hssd.editor.search.MatchEvent;
import com.insweat.hssd.editor.util.S;
import com.insweat.hssd.lib.tree.structured.TreeNode;

public class EntrySearchResult extends AbstractSearchResult {
    private final HSSDEditor editor;
    private final HashSet<TreeNode> matches = new HashSet<>();
    private final MatchEvent<TreeNode> event;

    public EntrySearchResult(HSSDEditor editor, ISearchQuery query) {
        super(query);
        this.editor = editor;
        this.event = new MatchEvent<>(this);
    }

    @Override
    public String getLabel() {
        return S.fmt(
                "'%s' - %s matches",
                getQuery().getLabel(),
                matches.size()
                );
    }

    @Override
    public String getTooltip() {
        return null;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return null;
    }

    protected void addMatch(TreeNode match) {
        if(match.hasParent()) {
            matches.add(match);
            fireChange(event.updated(MatchEvent.ADDED, match));
        }
    }
    
    public Iterable<TreeNode> iterMatches() {
        return matches;
    }
    
    public HSSDEditor getHSSDEditor() {
        return editor;
    }
}
