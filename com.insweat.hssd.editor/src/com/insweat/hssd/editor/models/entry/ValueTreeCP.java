package com.insweat.hssd.editor.models.entry;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import scala.Option;

import com.insweat.hssd.editor.editors.entry.EditorInput;
import com.insweat.hssd.editor.util.LogSupport;
import com.insweat.hssd.lib.essence.EntryData;
import com.insweat.hssd.lib.essence.ValueData;
import com.insweat.hssd.lib.interop.Interop;
import com.insweat.hssd.lib.tree.TreePath;
import com.insweat.hssd.lib.tree.flat.TreeNode;

public class ValueTreeCP
        implements IStructuredContentProvider, ITreeContentProvider {

    private final LogSupport log = new LogSupport("ValueTree");
    private final static Object[] NO_CHILDREN = new Object[0];

    private final Map<TreeNode, TreeNode> subtrees = new HashMap<>();

    @Override
    public void dispose() {
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    	log.noticef("inputChanged: %s, %s, %s", viewer, oldInput, newInput);
    }

    @Override
    public Object[] getChildren(Object parentElement) {
    	if(parentElement instanceof EditorInput) {
    		final EditorInput input = (EditorInput)parentElement;
    		final EntryData ed = EntryData.of(input.getEntryNode());
    		if(ed.valueTree().hasRoot()) {
    			return Interop.toArray(ed.valueTree().root().get().children());
    		}
    	}
    	else if(parentElement instanceof TreeNode) {
    		TreeNode node = (TreeNode) parentElement;
			ValueData vd = ValueData.of(node);
			Option<EntryData> ed = vd.asRef();
			if(ed.isDefined()) {
				// NB we do not use values cached in subtrees here, because
				// it may not be up-to-date if node has been changed since it
				// was cached.
				TreeNode subtreeRoot = ed.get().valueTree().root().get();
				subtrees.put(node, subtreeRoot);
				node = subtreeRoot;
			}
    		return Interop.toArray(node.children());
    	}
        return NO_CHILDREN;
    }

    @Override
    public Object getParent(Object element) {
    	if(element instanceof TreeNode) {
    		final TreeNode node = (TreeNode) element;
    		if(node.hasParent()) {
    			TreeNode parent = node.parent().get();
    			if(parent.path().length() == 1) {
    				for(Map.Entry<TreeNode, TreeNode> e: subtrees.entrySet()) {
    					if(e.getValue() == parent) {
    						// NB This may not be 100% right, because there
    						// may be many references referring to the same
    						// subtree.
    						parent = e.getKey();
    						break;
    					}
    				}
    			}
    			return parent;
    		}
    	}
    	return null;
    } 
    
    @Override
    public boolean hasChildren(Object element) {
    	if(element instanceof TreeNode) {
    		final TreeNode node = (TreeNode) element;
    		ValueData vd = ValueData.of(node);
    		Option<EntryData> ed = vd.asRef();
    		return ed.isDefined() || node.childCount() > 0;
    	}
    	return false;
    }
    
    @Override
    public Object[] getElements(Object inputElement) {
    	if(inputElement instanceof EditorInput) {
    		return getChildren(inputElement);
    	}
    	return NO_CHILDREN;
    }
    
    public TreePath getTreePath(Object element) {
        if(element instanceof TreeNode) {
            final TreeNode node = (TreeNode)element;
            return node.path();
        }
        return null;
    }
    
    public TreeNode getTreeNode(Object inputElement, TreePath path) {
        if(inputElement instanceof EditorInput) {
            final EditorInput input = (EditorInput)inputElement;
            final EntryData ed = EntryData.of(input.getEntryNode());
            final Option<TreeNode> optNode = ed.valueTree().find(path);
            if(optNode.isDefined()) {
                return optNode.get();
            }
            else {
            	for(TreeNode root: subtrees.values()) {
            		Option<TreeNode> node = root.owner().find(path);
            		if(node.isDefined()) {
            			return node.get();
            		}
            	}
            }
        }
        return null;
    }
}

