package com.insweat.hssd.editor.handlers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.Policy;
import org.eclipse.swt.widgets.Shell;

import scala.Option;
import scala.collection.JavaConversions;

import com.insweat.hssd.editor.editors.hssd.HSSDEditor;
import com.insweat.hssd.editor.util.S;
import com.insweat.hssd.lib.essence.EntryData;
import com.insweat.hssd.lib.essence.Thype;
import com.insweat.hssd.lib.essence.TraitThypeLike;
import com.insweat.hssd.lib.interop.Interop;
import com.insweat.hssd.lib.tree.EntryTree;
import com.insweat.hssd.lib.tree.structured.TreeNode;

public class HSSDEditorMoveEntry extends AbstractCommandHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        return watchedExecute(()->{
            String source = event.getParameter("source");
            String target = event.getParameter("target");

            HSSDEditor editor = getActiveHSSDEditor();
            if(editor == null) {
                return null;
            }

            EntryTree tree = editor.getMasterCP().getEntryTree();

            final TreeNode tgt;
            try {
                int entryId = Integer.parseInt(target);
                tgt = tree.nodesByID().get(entryId).get();
            }
            catch (Exception e) {
                log.errorf("Invalid target: %s", target);
                return null;
            }

            boolean movingLeaf = false;
            TreeNode srcParent = null;
            String[] srcEntryIDs = source.split(",");
            final TreeNode[] srcs = new TreeNode[srcEntryIDs.length];
            for(int i = 0; i < srcEntryIDs.length; ++i) {
                try {
                    int entryId = Integer.parseInt(srcEntryIDs[i]);
                    srcs[i] = tree.nodesByID().get(entryId).get();
                }
                catch (Exception e) {
                    log.errorf("Invalid source: %s", srcEntryIDs[i]);
                    return null;
                }

                if(srcs[i] == tgt) {
                    log.errorf("Cannot move %s into itself", srcs[i]);
                    return null;
                }

                if(srcs[i].owner() != tgt.owner()) {
                    log.errorf("%s and %s do not belong to the same entry tree",
                            srcs[i], tgt);
                    return null;
                }

                if(tgt.isLeaf()) {
                    log.errorf("Cannot move %s into a leaf %s", srcs[i], tgt);
                    return null;
                }
                
                if(tgt.childNodes().get(srcs[i].name()).isDefined()) {
                    log.errorf("A child named %s already exists under %s",
                            srcs[i].name(), tgt);
                    return null;
                }

                for(Option<TreeNode> node = tgt.parent();
                        node.isDefined();
                        node = node.get().parent()) {
                    if(node.get() == srcs[i]) {
                        log.errorf("Cannot move %s into a descendant %s", srcs[i], tgt);
                        return null;
                    }
                }

                // srcs[i].parent must have been isDefined because it != root,
                // because tgt is a descendant of root.
                TreeNode sp = srcs[i].parent().get();
                if(srcParent == null) {
                    srcParent = sp;
                }
                else if(srcParent != sp) {
                    log.errorf("Cannot move nodes under different parents.");
                    return null;
                }
                
                if(srcs[i].isLeaf()) {
                    movingLeaf = true;
                }
            }
            
            if(srcs.length == 0) {
                log.errorf("Nothing to move.");
                return null;
            }
            
            final Shell shell = editor.getSite().getShell();
            if(!warnMoveEntry(shell, source, tgt)) {
                return null;
            }


            Set<String> addingTraits = new HashSet<>();
            Set<TraitThypeLike> desiredTraits = new HashSet<>();
            EntryData tgtEd = EntryData.of(tgt);
            Interop.foreach(tgtEd.traits(), tr -> {
                desiredTraits.add(tr); 
            });

            EntryData srcEd = EntryData.of(srcs[0]);
            Interop.foreach(srcEd.traits(), tr -> {
                if(desiredTraits.add(tr)) {
                    Thype t = (Thype)tr;
                    addingTraits.add(t.name());
                }
            });

            if(addingTraits.isEmpty()) {
                desiredTraits.clear();
            }
            else if(movingLeaf && !warnAddingTraits(shell, tgt, addingTraits)) {
                return null;
            }
            
            move(tree, srcs, tgt, movingLeaf, desiredTraits);

            if(!srcParent.hasParent()) {
                srcParent = null;
            }
            editor.markDirty();
            editor.refresh(srcParent, false);
            editor.refresh(tgt, true);

            refreshAllEntryEditors();

            return null;
        });
    }
    
    private void move(EntryTree tree,
    		TreeNode[] srcs,
    		TreeNode tgt,
    		boolean movingLeaf,
    		Set<TraitThypeLike> desiredTraits) {

    	if(movingLeaf && !desiredTraits.isEmpty()) {
    		EntryData tgtEd = EntryData.of(tgt);
            tgtEd.insertTraits(JavaConversions.iterableAsScalaIterable(
                    desiredTraits));
    	}

    	for(TreeNode src: srcs) {
    		tree.move(src, tgt);
            EntryData srcEd = EntryData.of(src);
    		if(!movingLeaf && !desiredTraits.isEmpty()) {
                srcEd.insertTraits(JavaConversions.iterableAsScalaIterable(
                        desiredTraits));
    		}
    		srcEd.markDirty();
    	}
    }

    private boolean warnMoveEntry(Shell shell, String source, TreeNode tgt) {
        final String title = "Moving entry";
        final S msg = new S();
        msg.addf("Do you want to move%n%s%n", source);
        msg.addf("%ninto%n%s ?", tgt.path());
        return MessageDialog.openConfirm(shell, title, msg.toString());
    }
    
    private boolean warnAddingTraits(
            Shell shell, TreeNode tgt, Set<String> addingTraits) {
        final String title = "Adding traits";
        final S msg = new S();
        msg.addf("The source entry has more traits than the target one. ");
        msg.addf("To proceed, the following traits:%n");
    
        List<String> traitList = new ArrayList<>(addingTraits);
        traitList.sort(Policy.getComparator());

        for(String tr: addingTraits) {
            msg.addf("%s%n", tr);
        }
        
        msg.addf("%nwill be added to the target%n");
        msg.addf("%s .%n", tgt.path());

        msg.addf("%nDo you want to do so?");
        return MessageDialog.openConfirm(shell, title, msg.toString());
    }
    
}
