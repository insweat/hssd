package com.insweat.hssd.editor.search.hssd;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.search.ui.ISearchResult;

import scala.Option;

import com.insweat.hssd.editor.editors.hssd.HSSDEditor;
import com.insweat.hssd.editor.search.AbstractSearchQuery;
import com.insweat.hssd.editor.util.Helper;
import com.insweat.hssd.editor.util.S;
import com.insweat.hssd.lib.essence.EntryData;
import com.insweat.hssd.lib.essence.ValueData;
import com.insweat.hssd.lib.essence.ValueError;
import com.insweat.hssd.lib.essence.ValueText;
import com.insweat.hssd.lib.essence.thypes.LStringThype;
import com.insweat.hssd.lib.essence.thypes.ReferenceThype;
import com.insweat.hssd.lib.interop.EssenceHelper;
import com.insweat.hssd.lib.interop.Interop;
import com.insweat.hssd.lib.tree.EntryTree;
import com.insweat.hssd.lib.tree.structured.TreeNode;
import com.insweat.hssd.lib.util.StopIterationException;



public class EntrySearchQuery extends AbstractSearchQuery {

    private final Pattern pattern;
    private final Set<EntrySearch.Objective> objs;
    private final EntrySearch.Constraint constraint;

    public EntrySearchQuery(
    		EntrySearch.Pattern pat,
    		Collection<EntrySearch.Objective> objs,
    		EntrySearch.Constraint constraint) {
        super(pat.queryStr);
        this.pattern = pat.pattern;
        this.objs = new HashSet<>(objs);
        this.constraint = constraint;
    }

    @Override
    public IStatus run(IProgressMonitor monitor)
            throws OperationCanceledException {
        final HSSDEditor editor = Helper.getLastHSSDEditor();
        if(editor == null) {
            return Status.CANCEL_STATUS;
        }

        final EntryTree tree = editor.getMasterCP().getEntryTree();
        if(tree == null) {
            return Status.CANCEL_STATUS;
        }

        final EntrySearchResult r = new EntrySearchResult(editor, this);
        
        boolean searchId = objs.contains(EntrySearch.Objective.ID);
        boolean searchPath = objs.contains(EntrySearch.Objective.PATH);
        boolean searchName = objs.contains(EntrySearch.Objective.NAME);
        boolean searchCap = objs.contains(EntrySearch.Objective.CAPTION);
        boolean searchVal = objs.contains(EntrySearch.Objective.VALUE);
        boolean searchFWDRef = objs.contains(EntrySearch.Objective.FWD_REF);
        boolean searchBWDRef = objs.contains(EntrySearch.Objective.BWD_REF);
        boolean searchLStrRef = objs.contains(EntrySearch.Objective.LSTR_REF);

        if(searchId) {
        	TreeNode entry = trySearchAsID(tree, monitor);
        	if(entry != null) {
        		r.addMatch(entry);
        	}
        }

        if(searchFWDRef) {
        	// NB `trySearchAsID` honors `constraint.isLeaf`, so
        	//    `trySearchForFWDRefs` also honors that.
        	TreeNode entry = trySearchAsID(tree, monitor);
        	EntryData ed = EntryData.of(entry);
        	for(TreeNode ref: trySearchForFWDRefs(ed, monitor)) {
        		r.addMatch(ref);
        	}
        }

        TreeNode[] ref = {null};
        if(searchBWDRef) {
        	ref[0] = trySearchAsID(tree, monitor);
        }
        
        long[] sid = {0};
        if(searchLStrRef) {
        	sid[0] = Long.parseLong(getLabel());
        }

        if(searchPath 
        		|| searchName
        		|| searchCap
        		|| searchVal
        		|| searchBWDRef
        		|| searchLStrRef) {
	        try {
	        	EssenceHelper.foreach(tree, entry -> {
                    if(monitor.isCanceled()) {
                        throw new StopIterationException();
                    }

                    if(constraint.isLeaf && !entry.isLeaf()) {
                    	return;
                    }

                    EntryData ed = EntryData.of(entry);

                    if(searchPath && trySearchAsPath(ed, monitor)) {
                        r.addMatch(entry);
                    }

                    if(searchName && trySearchAsName(ed, monitor)) {
                        r.addMatch(entry);
                    }

                    if(searchCap && trySearchAsCaption(ed, monitor)) {
                        r.addMatch(entry);	
                    }

                    if(searchVal && trySearchAsValue(ed, monitor)) {
                        r.addMatch(entry);
                    }
                    
                    if(searchBWDRef &&
                    		trySearchForBWDRefs(ed, ref[0], monitor)) {
                    	r.addMatch(entry);
                    }
                    
                    if(searchLStrRef && trySearchAsLStrID(ed, sid[0], monitor)) {
                    	r.addMatch(entry);
                    }
                });
	        }
	        catch (StopIterationException e) {
	        	// pass
	        }
        }
        
        setSearchResult(r);
        return Status.OK_STATUS;
    }

    @Override
    protected ISearchResult getEmptyResult() {
        return new EntrySearchResult(null, this);
    }

    private TreeNode trySearchAsID(EntryTree tree, IProgressMonitor monitor) 
    {
    	monitor.subTask("as ID ...");
        final long id;
        try 
        {
            id = Long.parseLong(getLabel());
        }
        catch(Exception e)
        {
            return null;
        }

        TreeNode rv = Interop.or(tree.nodesByID().get(id));
        if(rv != null && constraint.isLeaf && !rv.isLeaf()) {
        	rv = null;
        }
        return rv;
    }
    
    private boolean trySearchAsName(EntryData ed, IProgressMonitor m)
    {
    	String name = ed.path().last();
    	m.subTask(S.fmt("as Name: %s(%s)", name, ed.entryID()));
    	return this.pattern.matcher(name).matches();
    }
    
    private boolean trySearchAsPath(EntryData ed, IProgressMonitor m) 
    {
    	m.subTask(S.fmt("as Path: %s(%s)", ed.path().last(), ed.entryID()));
        return this.pattern.matcher(ed.path().toString()).matches();
    }

    private boolean trySearchAsCaption(EntryData ed, IProgressMonitor m)
    {
    	m.subTask(S.fmt("as Caption: %s(%s)", ed.path().last(), ed.entryID()));
        return this.pattern.matcher(ed.caption()).matches();
    }
    
    private boolean trySearchAsValue(EntryData ed, IProgressMonitor m) {
    	m.subTask(S.fmt("as Value: %s(%s)", ed.path().last(), ed.entryID()));

    	if(constraint.valuePath != null) {
    		Option<ValueData> vd = ed.valueDataAt(constraint.valuePath);
    		if(vd.isDefined() && !vd.get().value().isError()) {
    			ValueText text = vd.get().valueText();
    			boolean isError = text instanceof ValueError;
	        	
	        	return !isError && this.pattern.matcher(text.value()).matches();
    		}
    		return false;
    	}
    	
    	try {
    		EssenceHelper.foreach(ed, vn -> {
    			ValueData vd = ValueData.of(vn);
    			ValueText text = vd.valueText();
    			boolean isError = text instanceof ValueError;
	        	
	        	if(!isError && this.pattern.matcher(text.value()).matches()) {
	        		throw new StopIterationException();	        		
	        	}
	        });
    	}
    	catch(StopIterationException e) {
    		return true;
    	}

    	return false;
    }

    private Set<TreeNode> trySearchForFWDRefs(
    		EntryData ed, IProgressMonitor m) {
    	String s = "for FWD refs: %s(%s)";
    	m.subTask(S.fmt(s, ed.path().last(), ed.entryID()));
    	EntryTree tree = (EntryTree)ed.owner().owner();
    	Set<TreeNode> rv = new HashSet<>();
		EssenceHelper.foreach(ed, vn -> {
			ValueData vd = ValueData.of(vn);
			if(vd.value().isError()) {
				return;
			}
			if(!(vd.element().thype() instanceof ReferenceThype)) {
				return;
			}
			ReferenceThype rt = (ReferenceThype)vd.element().thype();
			Long id = (Long)rt.fixed(vd.value().value());
			if(id == null) {
				return;
			}
			Option<TreeNode> ref = tree.nodesByID().get(id);
			if(!ref.isDefined()) {
				return;
			}
			rv.add(ref.get());
		});
		return rv;
    }

    private boolean trySearchForBWDRefs(
    		EntryData ed, TreeNode ref, IProgressMonitor m) {
    	String s = "for BWD refs: %s(%s)";
    	m.subTask(S.fmt(s, ed.path().last(), ed.entryID()));
    	Set<TreeNode> refs = trySearchForFWDRefs(ed, m);
    	return refs.contains(ref);
    }
    
    private boolean trySearchAsLStrID(
    		EntryData ed, long sid, IProgressMonitor m) {
    	String s = "as LStr ID: %s(%s)";
    	m.subTask(S.fmt(s, ed.path().last(), ed.entryID()));
    	try {
			EssenceHelper.foreach(ed, vn -> {
				ValueData vd = ValueData.of(vn);
				if(vd.value().isError()) {
					return;
				}

				if(!(vd.element().thype() instanceof LStringThype)) {
					return;
				}

				LStringThype lst = (LStringThype)vd.element().thype();
				Long id = (Long)lst.fixed(vd.value().value());
				if(id != null && id == sid) {
					throw new StopIterationException();
				}
	        });
    	}
    	catch (StopIterationException e) {
    		return true;
    	}

    	return false;
    }
}
