package com.insweat.hssd.editor.models.multidiff;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import scala.Option;
import scala.collection.JavaConversions;

import com.insweat.hssd.editor.handlers.ElementHelper;
import com.insweat.hssd.editor.views.multidiff.MultiDiffInput;
import com.insweat.hssd.lib.essence.CollectionThypeLike;
import com.insweat.hssd.lib.essence.EntryData;
import com.insweat.hssd.lib.essence.ValueData;
import com.insweat.hssd.lib.interop.Interop;
import com.insweat.hssd.lib.tree.TreePath;
import com.insweat.hssd.lib.tree.ValueTree;
import com.insweat.hssd.lib.tree.flat.TreeNode;


public class MultiDiffCP
        implements IStructuredContentProvider, ITreeContentProvider {

    private final static Object[] NO_CHILDREN = new Object[0];

    private MultiDiffEntryTree pseudoEntryTree;

    @Override
    public void dispose() {
        pseudoEntryTree = null;
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        if(oldInput == newInput) {
            return;
        }

        pseudoEntryTree = null;
        if(!(newInput instanceof MultiDiffInput)) {
            return;
        }

        MultiDiffInput input = (MultiDiffInput)newInput;

        pseudoEntryTree = new MultiDiffEntryTree(input);
        com.insweat.hssd.lib.tree.structured.TreeNode root = 
                pseudoEntryTree.insert(Interop.none(), "Root", false);
        EntryData ed = new EntryData(input.schema, root, 0);
        root.data_$eq(ed);

        ed.traits_$eq(JavaConversions.iterableAsScalaIterable(input.traits));

        List<TreeNode> collNodes = 
                new ArrayList<>();
        ValueTree unionValueTree = ed.valueTree();
        Interop.foreach(unionValueTree.preorder(), tn -> {
            TreeNode vn = (TreeNode)tn;
            ValueData vd = ValueData.of(vn);
            if(vd.element().thype() instanceof CollectionThypeLike) {
                collNodes.add(vn);
            }
        });

        collNodes.forEach(vn -> {
            unionCollection(unionValueTree, vn.path(), input.entries);
        });
    }

    private static void unionCollection(
            ValueTree unionValueTree,
            TreePath path,
            List<com.insweat.hssd.lib.tree.structured.TreeNode> entries) {

        Set<String> names = new HashSet<>();
        entries.forEach(en -> {
            EntryData ed = EntryData.of(en);
            Option<TreeNode> optVN = ed.valueTree().find(path);
            if(!optVN.isDefined()) {
                return;
            }

            Interop.foreach(optVN.get().children(), vn -> {
                names.add(vn.name());    
            });
        });

        TreeNode parentVN = unionValueTree.find(path).get();
        names.forEach(name -> {
            ElementHelper.addElement(parentVN, name);
        });
    }

    @Override
    public Object[] getChildren(Object parentElement) {
        if(parentElement instanceof MultiDiffInput) {
            if(pseudoEntryTree != null && pseudoEntryTree.hasRoot()) {
                EntryData ed = EntryData.of(pseudoEntryTree.root().get());
                ValueTree unionValueTree = ed.valueTree();
                if(unionValueTree.hasRoot()) {
                    TreeNode root = unionValueTree.root().get();
                    return Interop.toArray(root.children());
                }
            }
        }
        else if(parentElement instanceof TreeNode) {
            TreeNode vn = (TreeNode)parentElement;
            return Interop.toArray(vn.children());
        }
        return NO_CHILDREN;
    }

    @Override
    public Object getParent(Object element) {
        if(element instanceof TreeNode) {
            TreeNode vn = (TreeNode)element;
            if(vn.parent().isDefined()) {
                return vn.parent().get();
            }
        }
        return null;
    }

    @Override
    public boolean hasChildren(Object element) {
        if(element instanceof TreeNode) {
            TreeNode vn = (TreeNode)element;
            return vn.childCount() > 0;
        }
        return false;
    }

    @Override
    public Object[] getElements(Object inputElement) {
        if(inputElement instanceof MultiDiffInput) {
            return getChildren(inputElement);
        }
        return NO_CHILDREN;
    }

}
