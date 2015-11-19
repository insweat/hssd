package com.insweat.hssd.editor.models.hssd;

import java.util.HashSet;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import scala.collection.JavaConversions;

import com.insweat.hssd.editor.util.Helper;
import com.insweat.hssd.lib.essence.SchemaLike;
import com.insweat.hssd.lib.essence.Thype;
import com.insweat.hssd.lib.essence.TraitThypeLike;

public class TraitSelectCP implements IStructuredContentProvider {

    private final static Object[] NO_ELEMENTS = new Object[0];
    private Object input;
    private final HashSet<TraitThypeLike> traits = new HashSet<>(100);

    @Override
    public void dispose() {
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        if(Helper.objeq(oldInput, newInput)) {
            return;
        }

        input = newInput;
        traits.clear();

        if(!(newInput instanceof SchemaLike)) {
            return;
        }

        final SchemaLike sch = (SchemaLike)newInput;
        gatherTraits(sch);
        gatherTraits(sch.builtin());
    }
    
    private void gatherTraits(SchemaLike sch) {
        final Iterable<Thype> thypes = 
                JavaConversions.asJavaIterable(sch.thypes().values());

        for(Thype t: thypes) {
            if(t instanceof TraitThypeLike) {
                traits.add((TraitThypeLike)t);
            }
        }
        
        if(sch.parent().isDefined()) {
            gatherTraits(sch.parent().get());
        }
    }

    @Override
    public Object[] getElements(Object inputElement) {
        if(input != null && input.equals(inputElement)) {
            return traits.toArray();
        }

        return NO_ELEMENTS;
    }
}
