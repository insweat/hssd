package com.insweat.hssd.editor.editors.entry;

import java.util.EventObject;
import java.util.HashMap;
import java.util.function.Function;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.TreeViewerEditor;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.part.EditorPart;

import com.insweat.hssd.editor.models.IntAwareCmp;
import com.insweat.hssd.editor.models.LPHelper;
import com.insweat.hssd.editor.models.entry.ValueTreeCP;
import com.insweat.hssd.editor.models.entry.ValueTreeLP;
import com.insweat.hssd.editor.util.Helper;
import com.insweat.hssd.lib.essence.EntryData;
import com.insweat.hssd.lib.tree.TreePath;

public class EntryEditor extends EditorPart {

    public final static String ID = 
            "com.insweat.hssd.editor.editors.EntryEditor"; // $NON-NLS-1$

    private final HashMap<String, TreeViewerColumn> columns = new HashMap<>();
    private TreeViewer viewer;

    @Override
    public void doSave(IProgressMonitor monitor) {
        if(monitor != null) {
            final EditorInput input = (EditorInput)getEditorInput();
            input.getHSSDEditor().doSave(monitor);
        }
        else {
        	clearDirty();
        }
    }

    @Override
    public void doSaveAs() {
        String msg = "EntryEditor.doSaveAs is unsupported";
        throw new UnsupportedOperationException(msg);
    }

    @Override
    public void init(IEditorSite site, IEditorInput input)
            throws PartInitException {
        if (!(input instanceof EditorInput)) {
            String msg = "Invalid Input: Must be EditorInput, got %s";
            throw new PartInitException(String.format(
                    msg, input == null ? null : input.getClass()));
        }

        setSite(site);
        setInput(input);
    }

    @Override
    public boolean isDirty() {
    	EditorInput input = (EditorInput)getEditorInput();
    	if(input != null) {
            EntryData ed = EntryData.of(input.getEntryNode());
            return ed.isDirty();
    	}
        return false;
    }

    public void markDirty() {
    	EditorInput input = (EditorInput)getEditorInput();
    	if(input != null) {
            EntryData ed = EntryData.of(input.getEntryNode());
            ed.markDirty();
    	}
        firePropertyChange(PROP_DIRTY);
    }
    
    private void clearDirty() {
    	EditorInput input = (EditorInput)getEditorInput();
    	if(input != null) {
            EntryData ed = EntryData.of(input.getEntryNode());
            ed.clearDirty();
    	}
        firePropertyChange(PROP_DIRTY);
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void createPartControl(Composite parent) {
        final Layout layout = new FillLayout();
        parent.setLayout(layout);

        viewer = new TreeViewer(parent,
                SWT.MULTI |
                SWT.H_SCROLL |
                SWT.V_SCROLL |
                SWT.FULL_SELECTION |
                SWT.VIRTUAL);

        final Tree tree = viewer.getTree();

        tree.setHeaderVisible(true);
        tree.setLinesVisible(true);

        viewer.setContentProvider(new ValueTreeCP());
        viewer.setLabelProvider(new ValueTreeLP());
        viewer.setComparator(new ViewerComparator(new IntAwareCmp()));
        viewer.setInput(getEditorInput());
        
        final ValueTreeLP.Column[] columns = ValueTreeLP.Column.values();

        createColumns(columns);

        enableValueTreeEditing();

        ColumnViewerToolTipSupport.enableFor(viewer);
        
        setPartName(getEditorInput().getName());
        
        hookContextMenu();
        
        hookDragAndDrop();
        
        getSite().setSelectionProvider(viewer);
        
        IContextService svc = 
        		(IContextService)getSite().getService(IContextService.class);

        svc.activateContext("com.insweat.hssd.editor.contexts.inEntryEditor");
    }

    @Override
    public void setFocus() {
        viewer.getTree().setFocus();
    }

    public TreeViewer getEntryViewer() {
        return viewer;
    }
    
    public void initExpand() {
        final ValueTreeCP cp = getMasterCP();
        getEntryViewer().setExpandedElements(cp.getElements(getEditorInput()));
    }

    public void refresh(Object element, boolean ensureExpanded) {
        refresh(element, ensureExpanded, true);
    }
    
    public void refresh(
            Object element, boolean ensureExpanded, boolean handleSimilar) {
        internalRefresh(element, ensureExpanded);

        if(handleSimilar) {
            final ValueTreeCP cp = getMasterCP();
            final TreePath path = cp.getTreePath(element);
            multiApply((e) -> {
                e.internalRefresh(path, false);
                return false;
            }, this);
        }
    }

    private void internalRefresh(Object element, boolean ensureExpanded) {
        if(element != null) {
            viewer.refresh(element);
            if(ensureExpanded) {
                viewer.expandToLevel(element, 1);
            }
        }
        else {
            viewer.refresh();
            if(ensureExpanded) {
                viewer.expandToLevel(1);
            }
        }
    }

    private void internalRefresh(TreePath path, boolean ensureExpanded) {
        final ValueTreeCP cp = getMasterCP();
        final Object element = path == null ? null : cp.getTreeNode(getEditorInput(), path);
        internalRefresh(element, ensureExpanded);
    }

    public void update(Object element) {
        update(element, true);
    }
    
    public void update(Object element, boolean handleSimilar) {
        internalUpdate(element);

        if(handleSimilar) {
            final ValueTreeCP cp = getMasterCP();
            final TreePath path = cp.getTreePath(element);
            multiApply((e) -> {
                e.internalUpdate(path);
                return false;
            }, this);
        }
    }

    private void internalUpdate(Object element) {
        if(element == null) {
            return;
        }
        viewer.update(element, null);
    }

    private void internalUpdate(TreePath path) {
        final ValueTreeCP cp = getMasterCP();
        final Object element = cp.getTreeNode(getEditorInput(), path);
        internalUpdate(element);    
    }

    public void updateTitle() {
        setPartName(getEditorInput().getName());
        firePropertyChange(PROP_TITLE);
    }

    public static boolean multiApply(
            Function<EntryEditor, Boolean> func,
            IEditorPart exclude) {
        for(IWorkbenchPage page: Helper.iterWBPages()) {
            final IEditorReference[] editorRefs =
                    page.findEditors(null, ID, IWorkbenchPage.MATCH_ID);
            for(IEditorReference editorRef: editorRefs) {
                final EntryEditor editor = 
                        (EntryEditor) editorRef.getEditor(false);
                if(editor == exclude) {
                    continue;
                }

                if(func.apply(editor)) {
                    return true;
                }
            }
        }
        return false;
    }


	private void createColumns(ValueTreeLP.Column[] columns) {
	    if(columns == null) {
	        throw new NullPointerException("Invalid arguments.");
	    }

        final ValueTreeLP baseLP = (ValueTreeLP)viewer.getLabelProvider();

	    float totalWeight = 0;
	    for(int i = 0; i < columns.length; ++i) {
	        final String name = columns[i].label;
	        final int style = columns[i].style;
	        final TreeViewerColumn col = new TreeViewerColumn(viewer, style);
	        this.columns.put(name, col);

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
        viewer.getTree().setLayout(tableLayout);
	}

	public ValueTreeCP getMasterCP() {
	    return (ValueTreeCP)getEntryViewer().getContentProvider();
	}

	private void enableValueTreeEditing() {
        final TreeViewerColumn col = columns.get(
                ValueTreeLP.Column.VALEX.label);

        TreeViewerEditor.create(viewer,
        		new EditorActivationStrategy(viewer),
        		ColumnViewerEditor.DEFAULT);

        col.setEditingSupport(new EntryEditorEditingSupport(this));
	}

	private void hookContextMenu() {
	    final MenuManager manager = 
	    		new MenuManager("#PopupMenu", getSite().getId() + "#PopupMenu");
	    manager.setRemoveAllWhenShown(true);
	    /*
	    manager.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager) {
            	int a = 0;
            	if(a == 0) {
            		a++;
            	}
            }
        });
        */
	    final Menu menu = manager.createContextMenu(viewer.getControl());
	    viewer.getControl().setMenu(menu);
	    getSite().registerContextMenu(manager.getId(), manager, viewer);
	}
	
	private void hookDragAndDrop() {
	    int ops = DND.DROP_MOVE;
	    Transfer[] types = new Transfer[]{ TextTransfer.getInstance() };
	    viewer.addDropSupport(ops, types, new DropHandler(this));
	    viewer.addDragSupport(ops, types, new DragElementHandler(this));
	}

}


class EditorActivationStrategy extends ColumnViewerEditorActivationStrategy {
	
	public final static int EVENT_TYPE_OF_INTEREST = 
			ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION;

	public EditorActivationStrategy(TreeViewer treeViewer) {
		super(treeViewer);
	}
	
	@Override
    protected boolean isEditorActivationEvent(
    		ColumnViewerEditorActivationEvent event) {
        if (event.eventType != EVENT_TYPE_OF_INTEREST) {
        	return false;
        }

        EventObject source = event.sourceEvent;
        if (source instanceof MouseEvent && ((MouseEvent)source).button == 3) {
            return false;
        }

        return true;
    }
}
