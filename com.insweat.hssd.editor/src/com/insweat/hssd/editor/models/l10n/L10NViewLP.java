package com.insweat.hssd.editor.models.l10n;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;

import com.insweat.hssd.editor.models.LPHelper;
import com.insweat.hssd.editor.util.Helper;
import com.insweat.hssd.editor.util.S;

public class L10NViewLP
        extends StyledCellLabelProvider implements ILabelProvider {
    
	private final Styler DIM_OUT_STYLER = LPHelper.DIM_OUT_STYLER;

    @Override
    public void update(ViewerCell cell) {
        final Object element = cell.getElement();
        final int columnIndex = cell.getColumnIndex();
        final Integer zero = 0;
        if(0 == columnIndex) {
            cell.setImage(getImage(element));
            final String content = getText(element);
            if(element instanceof Row && zero.equals(((Row)element).cols[1])) {
            	StyledString styled = new StyledString(content, DIM_OUT_STYLER);
            	cell.setStyleRanges(styled.getStyleRanges());
            }
            cell.setText(content);
        }
        else if (element instanceof Row){
            final Row row = (Row)element;
            final String content = String.valueOf(row.cols[columnIndex]);
            if(zero.equals(row.cols[1])) {
            	StyledString styled = new StyledString(content, DIM_OUT_STYLER);
            	cell.setStyleRanges(styled.getStyleRanges());
            }
            cell.setText(content);
        }
    }

    @Override
    public Image getImage(Object element) {
        String imageKey = null;
        if(element instanceof Chunk) {
            imageKey = ISharedImages.IMG_OBJ_FOLDER;
        }
        else if(element instanceof Row) {
            imageKey = ISharedImages.IMG_OBJ_FILE;
        }
        if(imageKey != null) {
            return Helper.getSharedImage(imageKey);
        }
        return null;
    }

    @Override
    public String getText(Object element) {
        if(element instanceof Chunk) {
            final Chunk chunk = (Chunk)element;
            return S.fmt("%s - %s", chunk.firstID, chunk.lastID);
        }
        else if(element instanceof Row) {
            final Row row = (Row)element;
            return String.valueOf(row.cols[0]);
        }
        return "";
    }

}
