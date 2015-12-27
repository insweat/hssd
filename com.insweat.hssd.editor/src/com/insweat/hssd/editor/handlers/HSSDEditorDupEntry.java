package com.insweat.hssd.editor.handlers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;

import scala.Option;

import com.insweat.hssd.editor.editors.hssd.HSSDEditor;
import com.insweat.hssd.editor.editors.hssd.ui.UIHelper;
import com.insweat.hssd.editor.services.IDService;
import com.insweat.hssd.editor.util.Helper;
import com.insweat.hssd.editor.util.S;
import com.insweat.hssd.lib.essence.EntryData;
import com.insweat.hssd.lib.essence.Thype;
import com.insweat.hssd.lib.essence.ValueData;
import com.insweat.hssd.lib.essence.thypes.LStringThype;
import com.insweat.hssd.lib.interop.Interop;
import com.insweat.hssd.lib.tree.EntryTree;
import com.insweat.hssd.lib.tree.TreeNodeLike;
import com.insweat.hssd.lib.tree.structured.TreeNode;

public class HSSDEditorDupEntry extends AbstractCommandHandler {

	private final String NAME_PAT_LEAF = "%s_copy_%d";
	private final String NAME_PAT_BRANCH = "%sCopy%d";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
	    return watchedExecute(()->{
	        final HSSDEditor editor = getActiveHSSDEditor();
	        ISelectionProvider sp = editor.getSite().getSelectionProvider();
	        IStructuredSelection sel = (IStructuredSelection)sp.getSelection();

	        Set<TreeNode> entries = new HashSet<>();
	        for(Iterator<?> itr = sel.iterator(); itr.hasNext();) {
	            TreeNode en = (TreeNode)itr.next();
	            entries.add(en);
	        }

	        if(!verifySelection(editor, entries)) {
	            return null;
	        }

	        int numIDs = count(entries);
	        List<Long> idList;
	        try {
	            idList = allocIDs(numIDs);
	        }
	        catch (Exception e) {
	            log.errorf("Failed to allocate IDs: %s", e);
	            return null;
	        }

	        try {
	            Iterator<Long> idq = idList.iterator();
	            for(TreeNode en: entries) {
	                TreeNode parent = en.parent().get();
	                duplicate(en, parent, idq);
	            }
	        }
	        catch (Exception e) {
	            String s = "An error occurred while duplicating entries. "
	                    + "The current HSSD data in memory has corrupted. "
	                    + "YOU MUST DO A RELOAD WITHOUT SAVING.";
	            log.criticalf(s);
	            return null;
	        }

	        editor.markDirty();
	        editor.refresh(null, true);
	        return null;
	    });
	}

	private boolean verifySelection(HSSDEditor editor, Set<TreeNode> entries) {
        for(TreeNode en: entries) {
        	for(Option<TreeNode> ancestor = en.parent();
        			ancestor.isDefined();) {
        		if(entries.contains(ancestor.get())) {

                    final Shell shell = editor.getSite().getShell();
                    final String title = "Ambiguous Selection";
                    final S msg = new S();
                    msg.addf("Entry%n")
                    	.addf("%s%n%n", en.path())
                    	.addf("is a descendant of %n")
                    	.addf("%s.%n%n", ancestor.get().path())
                    	.addf("That causes ambiguation in duplication.");
                    MessageDialog.openError(shell, title, msg.toString());
        			
        			return false;
        		}
        		ancestor = ancestor.get().parent();
        	}
        }
        return true;
	}
	
	private List<Long> allocIDs(int numIDs) {
        IDService svc = Helper.getIDSvc();
        long[] ids = svc.multiAcquire(Helper.getActiveProject(),
                IDService.Namespace.ENTRY_ID, numIDs);
        List<Long> rv = new ArrayList<>(ids.length);
        for(long id: ids) {
        	rv.add(id);
        }
        return rv;
	}
	
	private int count(Set<TreeNode> entries) {
		int n = entries.size();
		for(TreeNode en: entries) {
			n += en.countDescendants();
		}
		return n;
	}

	private void duplicate(TreeNode orig, TreeNode parent, Iterator<Long> idq) {
		if(!idq.hasNext()) {
			throw new IllegalArgumentException();
		}

		String pat = orig.isLeaf() ? NAME_PAT_LEAF : NAME_PAT_BRANCH;

		String name = UIHelper.mkName(parent, pat, orig.name(), orig.isLeaf());

		long id = idq.next();
		EntryData origED = EntryData.of(orig);
		EntryTree tree = (EntryTree)orig.owner();
		TreeNode en = tree.insert(Interop.opt(parent), name, orig.isLeaf());
		EntryData ed = new EntryData(origED.schema(), en, id);
		en.data_$eq(ed);
		ed.traits_$eq(origED.traits());
		
		ed.restore(origED.backup(Interop.fn(
				(com.insweat.hssd.lib.tree.flat.TreeNode vn)-> {
			ValueData vd = ValueData.of(vn);
			Thype elemThype = vd.element().thype();
			if(elemThype instanceof LStringThype) {
				return false;
			}
			return true;
		})));
		ed.markDirty();
		
		Interop.foreach(orig.children(), (TreeNodeLike child) -> {
			duplicate((TreeNode)child, en, idq);
		});
	}
}
