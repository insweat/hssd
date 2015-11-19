package com.insweat.hssd.editor.handlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.insweat.hssd.editor.editors.entry.EntryEditor;
import com.insweat.hssd.editor.util.LogSupport;
import com.insweat.hssd.editor.util.S;
import com.insweat.hssd.lib.essence.CollectionThypeLike;
import com.insweat.hssd.lib.essence.Thype;
import com.insweat.hssd.lib.essence.ValueData;
import com.insweat.hssd.lib.essence.thypes.ArrayThype;
import com.insweat.hssd.lib.essence.thypes.MapThype;
import com.insweat.hssd.lib.interop.Interop;
import com.insweat.hssd.lib.tree.TreePath;
import com.insweat.hssd.lib.tree.ValueTree;
import com.insweat.hssd.lib.tree.flat.Tree;
import com.insweat.hssd.lib.tree.flat.TreeNode;

public final class ElementHelper {
    private ElementHelper() {}

    public static TreeNode copyOnNeed(TreeNode parent, EntryEditor editor) {
        ValueData parentVD = ValueData.of(parent);
        final ValueTree tree = (ValueTree)parent.owner();

        if(!tree.isOverridden(parent.path())) {
            final Shell shell = editor.getSite().getShell();
            final String title = "Deep Copy";
            final S msg = new S();
            msg.addf("Add / Remove collection element will cause ");
            msg.addf("a deep copy of the collection node ([%s] and all its ",
                    parent.name());
            msg.addf("children) to be made. They will stop inheriting ");
            msg.addf("values from their base nodes.%n");
            msg.addf("%nDo you want to do so?");
            final boolean result = 
                    MessageDialog.openConfirm(shell, title, msg.toString());
            if(!result) {
                return null;
            }
            parentVD.ensureValueNodeOverridden(true);
            parent = parentVD.valueNode();
        }

        return parent;
    }
    
    public static boolean warnRemoveElements(TreeNode parent, EntryEditor editor) {
        final Shell shell = editor.getSite().getShell();
        final String title = "Remove Elemment(s)";
        final S msg = new S();
        msg.addf("You are going to remove element(s) from %s", parent.name());
        msg.addf("%nDo you want to do so?");
        return MessageDialog.openConfirm(shell, title, msg.toString());
    }

    public static void removeChildren(TreeNode parent) {
    	removeChildren(parent, false);
    }
    
    public static void removeChildren(TreeNode parent, boolean andParent) {
    	removeChildren(parent, Collections.emptySet(), andParent);
    }

    public static void removeChildren(TreeNode parent, Set<String> exempt) {
    	removeChildren(parent, exempt, false);
    }

    public static void removeChildren(TreeNode parent, Set<String> exempt, boolean andParent) {
        final List<TreeNode> children = new ArrayList<>();
        Interop.foreach(parent.children(), vn -> {
        	if(exempt.contains(vn.name())) {
        		return;
        	}
            children.add(vn);
        });

        Tree tree = parent.owner();
        for(TreeNode vn: children) {
            tree.remove(vn);
        }
        
        if(andParent) {
        	tree.remove(parent);
        }
    }
    
    public static String mkName(TreeNode parent, String name) {
        ValueData parentVD = ValueData.of(parent);
        final CollectionThypeLike parentThype = 
                (CollectionThypeLike)parentVD.element().thype();
        if(parentThype instanceof ArrayThype) {
            name = String.valueOf(parent.childCount());
        }
        else if(parentThype instanceof MapThype){
            final HashSet<String> siblingNames = new HashSet<String>();
            Interop.foreach(parent.children(), vn -> {
                siblingNames.add(vn.name());
            });
            if(name == null) {
                String newName = "";
                for(int i = 0; i < siblingNames.size() + 1; ++i) {
                    newName = String.format("NewNode%s", i);
                    if(!siblingNames.contains(newName)) {
                        break;
                    }
                }
                name = newName;
            }
            else if(name.isEmpty()) {
                throw new IllegalArgumentException(S.fmt(
                        "Invalid name: %s", name));
            }
            else if(siblingNames.contains(name)) {
                throw new IllegalArgumentException(S.fmt(
                        "Name already taken: %s", name));
            }
        }
        else {
            unsupportedThype((Thype)parentThype);
        }
        return name;
    }

    public static TreeNode addElement(TreeNode parent, String name) {
        ValueData parentVD = ValueData.of(parent);
        final CollectionThypeLike parentThype = 
                (CollectionThypeLike)parentVD.element().thype();
        final ValueTree tree = (ValueTree)parent.owner();
        TreeNode node = tree.insert(Interop.opt(parent), name, true);
        TreePath path = node.path();
        ValueData vd = new ValueData(
                tree, path, parentThype.makeElement(node));
        node.data_$eq(vd);
        return node;
    }
    
    public static void unsupportedThype(Thype thype) {
        throw new RuntimeException(S.fmt("Unsupported thype: %s", thype));
    }
    
    public static void panic(LogSupport log, String context, Exception e) {
    	log.errorf("An error occurred while %s: %s", context, e);
    }
}