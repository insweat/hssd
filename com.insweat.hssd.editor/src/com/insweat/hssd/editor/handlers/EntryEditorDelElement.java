package com.insweat.hssd.editor.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.insweat.hssd.editor.editors.entry.EntryEditor;
import com.insweat.hssd.editor.models.IntAwareCmp;
import com.insweat.hssd.lib.essence.CollectionThypeLike;
import com.insweat.hssd.lib.essence.ValueData;
import com.insweat.hssd.lib.essence.thypes.ArrayThype;
import com.insweat.hssd.lib.interop.Interop;
import com.insweat.hssd.lib.tree.TreePath;
import com.insweat.hssd.lib.tree.ValueTree;
import com.insweat.hssd.lib.tree.flat.TreeNode;

public class EntryEditorDelElement extends AbstractCommandHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final EntryEditor editor = getActiveEntryEditor();
        ISelectionProvider sp = editor.getSite().getSelectionProvider();
        IStructuredSelection sel = (IStructuredSelection)sp.getSelection();

        TreeNode node = (TreeNode)sel.getFirstElement();
        final List<TreePath> paths = new ArrayList<>(sel.size());
        for(Iterator<?> itr = sel.iterator(); itr.hasNext();){
            TreeNode vn = (TreeNode)itr.next();
            paths.add(vn.path());
        }

        if(!ElementHelper.warnRemoveElements(node.parent().get(), editor)) {
        	return null;
        }

        final TreeNode parent = ElementHelper.copyOnNeed(
                node.parent().get(), editor);
        if(parent == null) {
            return null;
        }

        final ValueData parentVD = ValueData.of(parent);
        final ValueTree tree = (ValueTree)parent.owner();

        final CollectionThypeLike parentThype = 
                (CollectionThypeLike)parentVD.element().thype();

        for(TreePath path: paths) {
            node = tree.find(path).get();
            tree.remove(node);
        }

        // NB TreeViewer internally uses a hash table to sync up UI widgets and
        //    content elements. If we process renaming first, then the process
        //    will be confused because the old UI widget which references the
        //    old node has the same path as the one renamed to the old name.
        editor.refresh(parent, true);

        if(parentThype instanceof ArrayThype) {
            final TreeNode[] siblings = (TreeNode[])Interop.toArray(
                    parent.children(), TreeNode.class);
            Arrays.sort(siblings, new Comparator<TreeNode>() {
            	private final Comparator<String> cmp = new IntAwareCmp();

                @Override
                public int compare(TreeNode o1, TreeNode o2) {
                	return cmp.compare(o1.name(), o2.name());
                }
            });

            for(int i = 0; i < siblings.length; ++i) {
                final String index = String.valueOf(i);
                if(!siblings[i].name().equals(index)) {
                    tree.rename(siblings[i], index);

                    // Since we already handled structural changes above, we
                    // need to update the label when one changes.
                    editor.update(siblings[i]);
                }
            }
        }

        editor.markDirty();
        return null;
    }
}
