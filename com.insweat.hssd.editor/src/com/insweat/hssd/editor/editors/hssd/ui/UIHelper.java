package com.insweat.hssd.editor.editors.hssd.ui;

import java.util.Collection;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Table;

import com.insweat.hssd.editor.editors.hssd.HSSDEditor;
import com.insweat.hssd.editor.models.hssd.EntryTreeCP;
import com.insweat.hssd.editor.models.hssd.TraitSelectCP;
import com.insweat.hssd.editor.models.hssd.TraitSelectLP;
import com.insweat.hssd.editor.models.hssd.TraitSelectLP.Column;
import com.insweat.hssd.editor.util.S;
import com.insweat.hssd.lib.essence.TraitThypeLike;
import com.insweat.hssd.lib.tree.EntryTree;
import com.insweat.hssd.lib.tree.structured.TreeNode;

public final class UIHelper {
    private UIHelper() {}
    
    public static TableViewer createTraitSelect(
            Composite parent,
            HSSDEditor editor,
            Collection<TraitThypeLike> inheritedTraits) {
        final Layout layout = new FillLayout();
        parent.setLayout(layout);
        final EntryTreeCP cp = editor.getMasterCP();
        final TableViewer rv = new TableViewer(parent,
                SWT.MULTI |
                SWT.FULL_SELECTION |
                SWT.V_SCROLL);
        final TraitSelectLP lp = new TraitSelectLP();
        lp.setInherited(inheritedTraits);
        
        final Column[] columns = Column.values();
        
        int totalWeight = 0;
        for(int i = 0; i < columns.length; ++i) {
            totalWeight += Math.max(0, columns[i].weight);
        }

        final Table table = rv.getTable();
        final TableLayout tableLayout = new TableLayout();
        for(int i = 0; i < columns.length; ++i) {
            final Column column = columns[i];
            final TableViewerColumn col = 
                    new TableViewerColumn(rv, column.style);

            col.setLabelProvider(new StyledCellLabelProvider() {
                @Override
                public void update(ViewerCell cell) {
                    lp.update(cell);
                    super.update(cell);
                }
            });

            final ColumnWeightData weight = new ColumnWeightData(
                    column.weight * 100 / totalWeight);
            tableLayout.addColumnData(weight);
            
            table.getColumn(i).setText(column.caption);
        }
        table.setLayout(tableLayout);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        rv.setContentProvider(new TraitSelectCP());
        rv.setLabelProvider(lp); // This is required by ViewerComparator.
        rv.setComparator(new ViewerComparator());
        rv.setInput(cp.getActiveSchema());
        return rv;
    }
    
    public static class EntryNameValidator implements IInputValidator {
        private final static Pattern BRANCH_PATTERN = 
                Pattern.compile("[_A-Z][_a-zA-Z0-9]*");
        private final static String BRANCH_NAME_EXAMPLE = "Name_likeThis";

        private final static Pattern LEAF_PATTERN = 
                Pattern.compile("[_a-z][_a-z0-9]*");
        private final static String LEAF_NAME_EXAMPLE = "name_like_this";
 
        private final static String PATTERN_ERROR = 
                "'%s' does not match '%s'. A valid name looks like '%s'.";

        private final EntryTree tree;
        private final TreeNode parent;
        private final boolean isLeaf;
        
        public EntryNameValidator(TreeNode parent, boolean isLeaf) {
            if(parent == null) {
                throw new NullPointerException("Cannot be applied on Root");
            }
            this.tree = (EntryTree)parent.owner();
            this.parent = parent;
            this.isLeaf = isLeaf;
        }

        @Override
        public String isValid(String newText) {
            if(isLeaf) {
                if(!LEAF_PATTERN.matcher(newText).matches()) {
                    return S.fmt(
                            PATTERN_ERROR,
                            newText,
                            LEAF_PATTERN,
                            LEAF_NAME_EXAMPLE);
                }
                
                if(tree.findByName(newText).isDefined()) {
                    return S.fmt(
                            "An entry in the entry tree already took name %s.",
                            newText
                            );
                }
            }
            else {
                if(!BRANCH_PATTERN.matcher(newText).matches()) {
                    return S.fmt(
                            PATTERN_ERROR,
                            newText,
                            BRANCH_PATTERN,
                            BRANCH_NAME_EXAMPLE);
                }

                if(parent != null) {
                    if(parent.findChild(newText).isDefined()) {
                        return S.fmt(
                                "An entry under %s already took name %s.",
                                parent.name(), newText
                                );
                    }
                }
            }
            return null;
        }
        
    }
    
    public static String mkName(
    		TreeNode parent, String pattern, String baseName, boolean isLeaf) {
		EntryNameValidator validator = new EntryNameValidator(parent, isLeaf);

		String name = baseName;
		for(int i = 0; i < 10000; ++i) {
			name = S.fmt(pattern, baseName, i);
			if(null == validator.isValid(name)) {
				return name;
			}
		}
		return null;
    }
}

