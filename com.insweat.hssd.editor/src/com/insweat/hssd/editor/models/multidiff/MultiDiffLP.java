package com.insweat.hssd.editor.models.multidiff;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;

import scala.Option;

import com.insweat.hssd.editor.models.entry.ValueTreeLP;
import com.insweat.hssd.lib.essence.EntryData;
import com.insweat.hssd.lib.essence.ValueData;
import com.insweat.hssd.lib.tree.flat.TreeNode;

public class MultiDiffLP
        extends StyledCellLabelProvider implements ILabelProvider {

    private final ValueTreeLP imp = new ValueTreeLP();
    private final static int COL_VAL_INDEX = 1;

    
    @Override
    public void update(ViewerCell cell) {
        if(0 == cell.getColumnIndex()) {
            imp.update(cell);
            return;
        }

        if(!(cell.getElement() instanceof TreeNode)) {
            final String text = String.format(
                    "(%s, %s)", 
                    cell.getElement(),
                    cell.getColumnIndex()
            );
            cell.setText(text);
            super.update(cell);
            return;
        }

        TreeNode pseudoVN = (TreeNode)cell.getElement();
        ValueData pseudoVD = ValueData.of(pseudoVN);
        MultiDiffEntryTree et = 
                (MultiDiffEntryTree)pseudoVD.entryNode().owner();
        EntryData ed = EntryData.of(et.entries.get(cell.getColumnIndex() - 1));
        Option<TreeNode> optVN = ed.valueTree().find(pseudoVN.path());
        if(!optVN.isDefined()) {
            cell.setText("");
            super.update(cell);
            return;
        }

        StyledString ss = ValueTreeLP.getStyledText(optVN.get(), COL_VAL_INDEX);
        cell.setText(ss.toString());
        cell.setStyleRanges(ss.getStyleRanges());
        super.update(cell);
    }

    @Override
    public Image getImage(Object element) {
        return imp.getImage(element);
    }

    @Override
    public String getText(Object element) {
        return imp.getText(element);
    }

}
