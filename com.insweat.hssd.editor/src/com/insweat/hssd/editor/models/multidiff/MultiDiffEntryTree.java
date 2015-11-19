package com.insweat.hssd.editor.models.multidiff;

import java.util.List;

import com.insweat.hssd.editor.views.multidiff.MultiDiffInput;
import com.insweat.hssd.lib.tree.EntryTree;
import com.insweat.hssd.lib.tree.structured.TreeNode;


public class MultiDiffEntryTree extends EntryTree {
    public final List<TreeNode> entries;

    public MultiDiffEntryTree(MultiDiffInput input) {
        super(input.schema);
        entries = input.entries;
    }
}
