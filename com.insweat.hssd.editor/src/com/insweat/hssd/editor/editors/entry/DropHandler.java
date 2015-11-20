package com.insweat.hssd.editor.editors.entry;

import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;

import scala.Option;

import com.insweat.hssd.editor.editors.hssd.HSSDEditor;
import com.insweat.hssd.editor.editors.hssd.ui.UIHelper;
import com.insweat.hssd.editor.services.IDService;
import com.insweat.hssd.editor.util.Helper;
import com.insweat.hssd.editor.util.LogSupport;
import com.insweat.hssd.lib.essence.EntryData;
import com.insweat.hssd.lib.essence.Thype;
import com.insweat.hssd.lib.essence.ValueData;
import com.insweat.hssd.lib.essence.thypes.ArrayThype;
import com.insweat.hssd.lib.essence.thypes.ReferenceThype;
import com.insweat.hssd.lib.interop.Interop;
import com.insweat.hssd.lib.tree.EntryTree;
import com.insweat.hssd.lib.tree.TreePath;
import com.insweat.hssd.lib.tree.ValueTree;
import com.insweat.hssd.lib.tree.flat.TreeNode;
import com.insweat.hssd.lib.util.StopIterationException;

public class DropHandler extends ViewerDropAdapter {
	private final LogSupport log = new LogSupport("dnd.entry");
	private final EntryEditor editor;
	private final static String LEAF_PATTERN = "%s_%s";

	protected DropHandler(EntryEditor editor) {
		super(editor.getEntryViewer());
		this.editor = editor;
	}

	@Override
	public boolean performDrop(Object data) {
		if(getCurrentLocation() == LOCATION_ON) {
            return dropRef(data);
		}
		else {
			return dropElement(data);
		}
	}
	
	private boolean dropRef(Object data) {
		TreeNode vn = (TreeNode)getCurrentTarget();
		ValueData vd = ValueData.of(vn);
		
		Integer entryID;
		try {
			entryID = Integer.valueOf(data.toString());
		}
		catch(Exception e) {
			return false;
		}

		EntryTree et = (EntryTree)vd.entryNode().owner();
		Option<com.insweat.hssd.lib.tree.structured.TreeNode> en =
				et.nodesByID().get(entryID);
		if(!en.isDefined()) {
			return false;
		}

		EntryData ed = EntryData.of(en.get());
		ReferenceThype rt = (ReferenceThype)vd.element().thype();
		if(!entryIsValidRef(ed, rt)) {
			return false;
		}

        if(!en.get().isLeaf()) {
        	HSSDEditor hssdEditor = Helper.getActiveHSSDEditor();
        	if(hssdEditor == null) {
        		log.warnf("No active HSSD Editor");
        		return false;
        	}
        	IDService idSvc = Helper.getIDSvc();
        	long id = idSvc.acquire(Helper.getActiveProject(),
        	        IDService.Namespace.ENTRY_ID);
        	
        	String name = camelToSnake(en.get().name()).toLowerCase();
        	name = UIHelper.mkName(en.get(), LEAF_PATTERN, name, true);
        	
    		EntryTree tree = (EntryTree)en.get().owner();
    		com.insweat.hssd.lib.tree.structured.TreeNode parent = en.get();
    		en = Interop.opt(tree.insert(Interop.opt(en.get()), name, true));
    		ed = new EntryData(ed.schema(), en.get(), id);
    		en.get().data_$eq(ed);
    		hssdEditor.refresh(parent, true);
        }

        EntryEditorEditingSupport.writeValue(vn, en.get().name(), log);
		
		editor.refresh(vn, true);
		editor.markDirty();
		
		return true;
	}
	
	private boolean dropElement(Object data) {
		TreePath srcPath = TreePath.fromStr(data.toString());
		TreeNode tgt = (TreeNode)getCurrentTarget();
		ValueTree tree = (ValueTree)tgt.owner();

		Option<TreeNode> optSrc = tree.find(srcPath);
		if(!optSrc.isDefined()) {
			// When dragging something from one value tree into another.
			return false;
		}

		TreeNode src = optSrc.get();
		if(src.equals(tgt) || !src.parent().equals(tgt.parent())) {
			return false;
		}
		
		int srcIndex = Integer.valueOf(src.name());
		int tgtIndex = Integer.valueOf(tgt.name());
	
		if(srcIndex < tgtIndex) {
			// Moving src from higher to lower
			if(getCurrentLocation() == LOCATION_BEFORE) {
				--tgtIndex;
			}
		}
		else {
			// Moving src from lower to higher
			if(getCurrentLocation() == LOCATION_AFTER) {
				++tgtIndex;
			}
		}

		TreePath parentPath = srcPath.parent().get();
		TreePath path = parentPath.append(String.valueOf(srcIndex));
		TreeNode node = tree.find(path).get();
		tree.rename(node, "tmp"); // preserve src node

		int step = srcIndex < tgtIndex ? 1 : -1;
		for(int i = srcIndex; i != tgtIndex; i += step) {
			TreePath p = parentPath.append(String.valueOf(i + step));
			Option<TreeNode> n = tree.find(p);
			if(n.isDefined()) {
				tree.rename(n.get(), String.valueOf(i));
			}
		}
		tree.rename(node, String.valueOf(tgtIndex));

		editor.refresh(node.parent().get(), false);
		editor.markDirty();
		return true;
	}

	@Override
	public boolean validateDrop(Object target, int operation,
			TransferData transferType) {
		Transfer transfer = TextTransfer.getInstance();
        if(!transfer.isSupportedType(transferType)) {
        	return false;
        }
        
		if(target instanceof TreeNode) {
			TreeNode vn = (TreeNode)target;
			ValueData vd = ValueData.of(vn);
			int location = getCurrentLocation();
			if(location == LOCATION_ON) {
				Thype thype = vd.element().thype();
				return thype instanceof ReferenceThype;
			}
			
			if(location == LOCATION_AFTER || location == LOCATION_BEFORE) {
				TreeNode parentVN = vn.parent().get();
				ValueData parentVD = ValueData.of(parentVN);
				Thype thype = parentVD.element().thype();
				return thype instanceof ArrayThype;
			}
		}
		return false;
	}
	
	private boolean entryIsValidRef(EntryData ed, ReferenceThype rt) {
		try {
			Interop.foreach(ed.traits(), e -> {
				if(rt.elementThype() == e) {
					throw new StopIterationException(); // Found
				}
			});
		}
		catch(StopIterationException e) {
			return true;
		}
		return false;
	}

	
	private static String camelToSnake(String s) {
		String pattern = String.format(
				"%s|%s|%s",
				"(?<=[A-Z])(?=[A-Z][a-z])",
				"(?<=[^A-Z])(?=[A-Z])",
				"(?<=[A-Za-z])(?=[^A-Za-z])"
		);
		return s.replaceAll(pattern, "_");
	}
}
