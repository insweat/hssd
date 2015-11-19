package com.insweat.hssd.editor.models.hssd;

import java.util.Collection;
import java.util.HashSet;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;

import com.insweat.hssd.editor.models.LPHelper;
import com.insweat.hssd.lib.essence.Thype;
import com.insweat.hssd.lib.essence.TraitThypeLike;


public class TraitSelectLP
        extends StyledCellLabelProvider implements ILabelProvider {

    public static enum Column {
        CAPTION("Caption", 30, SWT.LEFT),
        DESC("Desc", 70, SWT.LEFT);

        public final String caption;
        public final int weight;
        public final int style;

        private Column(String caption, int weight, int style) {
            this.caption = caption;
            this.weight = weight;
            this.style = style;
        }
    }
    
    private final HashSet<TraitThypeLike> inherited = new HashSet<>();

    public void setInherited(Collection<TraitThypeLike> traits) {
        inherited.clear();
        inherited.addAll(traits);
    }


    @Override
    public void update(ViewerCell cell) {
        final Object element = cell.getElement();
        final int columnIndex = cell.getColumnIndex();

        StyledString styledText = new StyledString("");
        if(!(element instanceof TraitThypeLike)) {
            styledText.append("");
        }
        else {
            final String text;
            final TraitThypeLike trait = (TraitThypeLike)element;
            if(columnIndex == Column.CAPTION.ordinal()) {
                text = trait.caption();
            }
            else if(columnIndex == Column.DESC.ordinal()) {
                text = ((Thype)trait).description();
            }
            else {
                text = "";
            }
            
            if(inherited.contains(trait)) {
                styledText.append(text, LPHelper.DIM_OUT_STYLER);
            }
            else {
                styledText.append(text);
            }
        }

        cell.setText(styledText.getString());
        cell.setStyleRanges(styledText.getStyleRanges());
    }


    @Override
    public Image getImage(Object element) {
        return null;
    }


    // NB This is required by ViewerComparator.
    @Override
    public String getText(Object element) {
        if(element instanceof TraitThypeLike) {
            return ((TraitThypeLike)element).caption();
        }
        return String.valueOf(element);
    }
}
