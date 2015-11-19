package com.insweat.hssd.editor.views.l10n;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.part.ViewPart;

import com.insweat.hssd.editor.editors.hssd.HSSDEditor;
import com.insweat.hssd.editor.models.IntAwareCmp;
import com.insweat.hssd.editor.models.LPHelper;
import com.insweat.hssd.editor.models.hssd.EntryTreeCP;
import com.insweat.hssd.editor.models.l10n.L10NViewCP;
import com.insweat.hssd.editor.models.l10n.L10NViewLP;
import com.insweat.hssd.editor.util.Helper;
import com.insweat.hssd.lib.tree.EntryTree;

public class L10NView extends ViewPart {

	public final static String ID = "com.insweat.hssd.editor.views.L10NView";

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
        // parent.setLayout(new FillLayout());
        
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

        tv.setContentProvider(new L10NViewCP());
        tv.setLabelProvider(new L10NViewLP());
        tv.setComparator(new ViewerComparator(new IntAwareCmp()));

        getSite().setSelectionProvider(viewer);

        final IWorkbenchWindow window = getSite().getWorkbenchWindow();
        final IPartService svc = window.getPartService();

        partListener = new IPartListener() {

            @Override
            public void partActivated(IWorkbenchPart part) {
                obtainInput(Helper.getActiveHSSDEditor());
            }

            @Override
            public void partBroughtToTop(IWorkbenchPart part) {
            }

            @Override
            public void partClosed(IWorkbenchPart part) {
            }

            @Override
            public void partDeactivated(IWorkbenchPart part) {
            }

            @Override
            public void partOpened(IWorkbenchPart part) {
            }
            
        };
        svc.addPartListener(partListener);

        obtainInput(Helper.getActiveHSSDEditor());

        hookContextMenu();
        
        IContextService contextSvc = 
        		(IContextService)getSite().getService(IContextService.class);

        contextSvc.activateContext(
        		"com.insweat.hssd.editor.contexts.inHSSDL10NView");
    }

    private void obtainInput(HSSDEditor hssdEditor) {
        if(hssdEditor != null) {
            final EntryTreeCP cp = hssdEditor.getMasterCP();
            EntryTree tree = null;
            if(cp != null) {
            	tree = cp.getEntryTree();
            	recreateColumns(tree);
            }
            viewer.setInput(tree);
        }
    }

    private void recreateColumns(EntryTree entryTree) {
        final L10NViewCP cp = (L10NViewCP)viewer.getContentProvider();
        final L10NViewLP baseLP = (L10NViewLP)viewer.getLabelProvider();
        final L10NViewCP.Column[] columns = cp.createColumns(entryTree);
        final TreeViewer tv = (TreeViewer)viewer;
        final Tree tree = tv.getTree();
        
        tree.setRedraw(false);
        tree.clearAll(true);

        while(tree.getColumnCount() > 0) {
            tree.getColumn(0).dispose();
        }

        float totalWeight = 0;
        for(int i = 0; i < columns.length; ++i) {
            final String name = columns[i].label;
            final int style = columns[i].style;
            final TreeViewerColumn col = new TreeViewerColumn(tv, style);

            col.getColumn().setText(name);

            col.setLabelProvider(LPHelper.wrap(baseLP));

            int weight = columns[i].weight;
            if(weight < 0) {
                weight = - weight;
            }
            else if(weight == 0) {
                weight = 1;
            }

            totalWeight += weight;
        }

        final TableLayout tableLayout = new TableLayout();
        for(int i = 0; i < columns.length; ++i) {
            final int weight = (int)(columns[i].weight * 100 / totalWeight);
            tableLayout.addColumnData(new ColumnWeightData(weight));
        }

        tree.setLayout(tableLayout);
        tree.layout(true);
        tree.setRedraw(true);
    }
    
    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
    }

    private void hookContextMenu() {
        final MenuManager manager = 
                new MenuManager("#PopupMenu", getSite().getId() + "#PopupMenu");
        manager.setRemoveAllWhenShown(true);
        final Menu menu = manager.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);
        getSite().registerContextMenu(manager.getId(), manager, viewer);
    }
}
