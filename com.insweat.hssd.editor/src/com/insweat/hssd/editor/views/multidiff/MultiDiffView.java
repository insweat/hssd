package com.insweat.hssd.editor.views.multidiff;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.services.ISourceProviderService;

import scala.Option;

import com.insweat.hssd.editor.Activator;
import com.insweat.hssd.editor.editors.hssd.HSSDEditor;
import com.insweat.hssd.editor.models.IntAwareCmp;
import com.insweat.hssd.editor.models.LPHelper;
import com.insweat.hssd.editor.models.multidiff.MultiDiffCP;
import com.insweat.hssd.editor.models.multidiff.MultiDiffLP;
import com.insweat.hssd.editor.services.HSSDEditorSourceProvider;
import com.insweat.hssd.editor.util.Helper;
import com.insweat.hssd.editor.util.S;
import com.insweat.hssd.lib.essence.EntryData;
import com.insweat.hssd.lib.tree.EntryTree;
import com.insweat.hssd.lib.tree.TreeLike;
import com.insweat.hssd.lib.tree.TreeNodeLike;
import com.insweat.hssd.lib.tree.structured.TreeNode;

public class MultiDiffView extends ViewPart {
    public final static String ID =
            "com.insweat.hssd.editor.views.MultiDiffView";

    private final static String IMG_TOOL_REFRESH = 
            "icons/full/etool16/refresh_interpreter.gif";

    private final static String COL_CAPTION = "Caption";

    private final static int COL_CAPTION_WEIGHT = 1;
    private final static int COL_VALUE_WEIGHT = 1;

    private final static int COL_CAPTION_STYLE = SWT.LEFT;
    private final static int COL_VALUE_STYLE = SWT.LEFT;

    private StructuredViewer viewer;
    private IPartListener partListener;

    @Override
    public void dispose() {
        if(partListener != null) {
            final IWorkbenchWindow window = getSite().getWorkbenchWindow();
            final IPartService svc = window.getPartService();
            svc.removePartListener(partListener);
            partListener = null;
        }
        super.dispose();
    }

    @Override
    public void createPartControl(Composite parent) {
        final TreeViewer tv = new TreeViewer(parent,
                SWT.MULTI |
                SWT.H_SCROLL |
                SWT.V_SCROLL |
                SWT.FULL_SELECTION |
                SWT.VIRTUAL);
        viewer = tv;

        final Tree tree = tv.getTree();
        tree.setHeaderVisible(true);
        tree.setLinesVisible(true);

        tv.setContentProvider(new MultiDiffCP());
        tv.setLabelProvider(new MultiDiffLP());
        tv.setComparator(new ViewerComparator(new IntAwareCmp()));
        
        tree.addMouseListener(new MouseListener() {
            
            @Override
            public void mouseUp(MouseEvent e) {
            }
            
            @Override
            public void mouseDown(MouseEvent e) {
            }
            
            @Override
            public void mouseDoubleClick(MouseEvent e) {
                Object[] ev = locateEntryValue(e.x, e.y);
                if(ev == null) {
                    return;
                }
                HSSDEditor editor = getHSSDEditor();
                if(editor == null) {
                    return;
                }
                
                TreeNode en = (TreeNode)ev[0];
                com.insweat.hssd.lib.tree.flat.TreeNode vn = 
                        (com.insweat.hssd.lib.tree.flat.TreeNode)ev[1];
                editor.openEntryEditor(en, vn);
            }
        });
        
        contributeToActionBars();

        final IWorkbenchWindow window = getSite().getWorkbenchWindow();
        final IPartService svc = window.getPartService();

        partListener = new IPartListener() {

            @Override
            public void partActivated(IWorkbenchPart part) {
            }

            @Override
            public void partBroughtToTop(IWorkbenchPart part) {
            }

            @Override
            public void partClosed(IWorkbenchPart part) {
                if(part instanceof HSSDEditor) {
                    HSSDEditor editor = (HSSDEditor)part;
                    EntryTree et = editor.getMasterCP().getEntryTree();
                    MultiDiffInput input = (MultiDiffInput)viewer.getInput();
                    if(input != null && input.entries.get(0).owner() == et) {
                        setInput(null);
                    }
                }
            }

            @Override
            public void partDeactivated(IWorkbenchPart part) {
            }

            @Override
            public void partOpened(IWorkbenchPart part) {
            }
            
        };
        svc.addPartListener(partListener);
    }


    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
    }


    public void setInput(MultiDiffInput input) {
        recreateColumns(input);
        viewer.setInput(input);
    }
    
    private void recreateColumns(MultiDiffInput input) {
        final MultiDiffLP baseLP = (MultiDiffLP)viewer.getLabelProvider();
        final TreeViewer tv = (TreeViewer)viewer;
        final Tree tree = tv.getTree();
        final int numCols = input != null ? (input.entries.size() + 1) : 0;

        tree.setRedraw(false);
        tree.clearAll(true);

        while(tree.getColumnCount() > 0) {
            tree.getColumn(0).dispose();
        }

        float totalWeight = 0;
        for(int i = 0; i < numCols; ++i) {
            final String name;
            final int style;
            int weight;
            if(i == 0) {
                name = COL_CAPTION;
                style = COL_CAPTION_STYLE;
                weight = COL_CAPTION_WEIGHT;
            }
            else {
                TreeNode en = input.entries.get(i - 1);
                EntryData ed = EntryData.of(en);
                name = S.fmt("%s (%s)", en.name(), ed.entryID());
                style = COL_VALUE_STYLE;
                weight = COL_VALUE_WEIGHT;
            }

            final TreeViewerColumn col = new TreeViewerColumn(tv, style);

            col.getColumn().setText(name);

            col.setLabelProvider(LPHelper.wrap(baseLP));

            if(weight < 0) {
                weight = - weight;
            }
            else if(weight == 0) {
                weight = 1;
            }

            totalWeight += weight;
        }

        final TableLayout tableLayout = new TableLayout();
        for(int i = 0; i < numCols; ++i) {
            final int colWeight;
            if(i == 0) {
                colWeight = COL_CAPTION_WEIGHT;
            }
            else {
                colWeight = COL_VALUE_WEIGHT;
            }
            final int weight = (int)(colWeight * 100 / totalWeight);
            tableLayout.addColumnData(new ColumnWeightData(weight));
        }

        tree.setLayout(tableLayout);
        tree.layout(true);
        tree.setRedraw(true);
    }
    
    private void contributeToActionBars() {
        IActionBars bars = getViewSite().getActionBars();
        Action refresh = new Action(){
           @Override
            public void run() {
               Object currentInput = viewer.getInput();
               if(currentInput == null) {
                   return;
               }
               MultiDiffInput input = (MultiDiffInput)currentInput;
               MultiDiffInput newInput = refreshInput(input);
               setInput(newInput);
            }
        };
        refresh.setText("Refresh");
        refresh.setToolTipText("Refresh MultiDiff view");
        refresh.setImageDescriptor(
                Activator.getImageDescriptor(IMG_TOOL_REFRESH));
        bars.getToolBarManager().add(refresh);
    }
    
    private Object[] locateEntryValue(int x, int y) {
        final MultiDiffInput input = (MultiDiffInput)viewer.getInput();
        if(input == null) {
            return null;
        }

        final Tree tree = (Tree)viewer.getControl();
        final Point p = new Point(x, y);
        final TreeItem item = tree.getItem(p);
        if(item == null) {
            return null;
        }

        int columnIndex = -1;
        int numCols = tree.getColumnCount();
        for(int i = 0; i < numCols; ++i) {
            Rectangle rect = item.getBounds(i);
            if(rect.contains(p)) {
                columnIndex = i;
                break;
            }
        }
        
        if(columnIndex <= 0) {
            return null;
        }

        TreeNode en = input.entries.get(columnIndex - 1);
        TreeNodeLike vn = (TreeNodeLike)item.getData();

        EntryData ed = EntryData.of(en);
        vn = ed.valueTree().find(vn.path()).get();

        return new Object[]{en, vn};
    }
    
    private HSSDEditor getHSSDEditor() {
        final ISourceProviderService svc = Helper.getSPSvc();
        final HSSDEditorSourceProvider sp = Helper.getHSSDEditorSP(svc);
        final IEditorPart ep = sp.getEditor();
        if(!(ep instanceof HSSDEditor)) {
            return null;
        }

        return (HSSDEditor)ep;
    }

    private static MultiDiffInput refreshInput(MultiDiffInput input) {
        // See if any entry is removed.
        TreeLike et = input.entries.get(0).owner();
        List<TreeNode> entries = new ArrayList<>();
        for(TreeNode en: input.entries) {
            Option<TreeNodeLike> optEN = et.find(en.path());
            if(optEN.isDefined() && optEN.get() == en) {
                entries.add(en);
            }
        }

        if(entries.isEmpty()) {
            return null;
        }

        return new MultiDiffInput(input.schema, entries, input.traits);
    }
}
