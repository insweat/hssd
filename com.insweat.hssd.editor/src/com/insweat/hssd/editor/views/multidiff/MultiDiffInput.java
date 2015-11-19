package com.insweat.hssd.editor.views.multidiff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.insweat.hssd.lib.essence.SchemaLike;
import com.insweat.hssd.lib.essence.TraitThypeLike;
import com.insweat.hssd.lib.tree.structured.TreeNode;

public class MultiDiffInput {
    public final SchemaLike schema;
    public final List<TreeNode> entries;
    public final List<TraitThypeLike> traits;
    
    public MultiDiffInput(
            SchemaLike schema,
            Collection<TreeNode> entries,
            Collection<TraitThypeLike> traits
            ) {
        this.schema = schema;
        this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
        this.traits = Collections.unmodifiableList(new ArrayList<>(traits));
    }
}
