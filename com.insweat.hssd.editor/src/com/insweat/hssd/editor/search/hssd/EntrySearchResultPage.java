package com.insweat.hssd.editor.search.hssd;

import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.search.ui.IQueryListener;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.services.ISourceProviderService;

import com.insweat.hssd.editor.editors.hssd.HSSDEditor;
import com.insweat.hssd.editor.models.DecoratingLabelProvider;
import com.insweat.hssd.editor.models.hssd.EntryTreeLP;
import com.insweat.hssd.editor.search.AbstractSearchResultPage;
import com.insweat.hssd.editor.services.HSSDEditorSourceProvider;
import com.insweat.hssd.editor.util.Helper;
import com.insweat.hssd.lib.interop.Interop;
import com.insweat.hssd.lib.tree.EntryTree;
import com.insweat.hssd.lib.tree.structured.TreeNode;

public class EntrySearchResultPage extends AbstractSearchResultPage {

    private final IQueryListener queryListener = new IQueryListener(){

        @Override
        public void queryAdded(ISearchQuery query) {
        }

        @Override
        public void queryRemoved(ISearchQuery query) {
        }

        @Override
        public void queryStarting(ISearchQuery query) {
        }

        @Override
        public void queryFinished(ISearchQuery query) {
            final ISearchQuery q = query;
            final UIJob job = new UIJob("UpdateSearchResultView") {
                @Override
                public IStatus runInUIThread(IProgressMonitor monitor) {
                    setInput(q.getSearchResult(), null);
                    getViewPart().updateLabel();
                    return Status.OK_STATUS;
                }
            };

            job.setSystem(true);
            job.schedule();
        }
        
    };

    @Override
    public void dispose() {
        NewSearchUI.removeQueryListener(queryListener);
        super.dispose();
    }

    @Override
    public void setInput(ISearchResult search, Object uiState) {
        if(getViewer() != null) {
            getViewer().setInput(search);
        }
    }

    @Override
    public void createControl(Composite parent) {
        parent = new Composite(parent, SWT.NONE);

        final FillLayout layout = new FillLayout();
        parent.setLayout(layout);

        final TreeViewer tv = new TreeViewer(parent, SWT.FILL);
        setViewer(tv);

        tv.setContentProvider(new ContentProvider());
        tv.setLabelProvider(new DecoratingLabelProvider(new EntryTreeLP()));

        tv.setInput(getInput());

        Action doubleClickAction = new Action() {
            @Override
            public void run() {
                final IStructuredSelection selection = 
                        (IStructuredSelection)tv.getSelection();
                final HSSDEditor editor = getHSSDEditor();
                TreeNode en = (TreeNode)selection.getFirstElement();
                editor.openEntryEditor(en, null);
            }
        };

        tv.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(DoubleClickEvent event) {
                doubleClickAction.run();
            }
        });
        
        setControl(parent);
        
        NewSearchUI.addQueryListener(queryListener);
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
}

class ContentProvider 
        implements IStructuredContentProvider, ITreeContentProvider {

    private final static Object[] NO_CHILDREN = new Object[0];

    private EntrySearchResult searchResult;
    
    private final HashMap<TreeNode, HashSet<TreeNode>> children =
            new HashMap<>();

    @Override
    public void dispose() {
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        children.clear();

        if(newInput == null || !(newInput instanceof EntrySearchResult)) {
            return;
        }

        searchResult = (EntrySearchResult)newInput;
        for(TreeNode m : searchResult.iterMatches()) {
            for(TreeNode en = m, pen = null; en.hasParent(); en = pen) {
                pen = en.parent().get();
                HashSet<TreeNode> cs = children.get(pen);
                if(cs == null) {
                    cs = new HashSet<>();
                    children.put(pen, cs);
                }
                cs.add(en);
            }
        }
    }

    @Override
    public Object[] getChildren(Object parentElement) {
        if(parentElement.equals(searchResult)) {
            final HSSDEditor editor = searchResult.getHSSDEditor();
            if(editor != null && !children.isEmpty()) {
                return new Object[]{ editor.getEditorInput() };
            }
        }
        else if(parentElement instanceof IEditorInput) {
            if(searchResult == null) {
                return NO_CHILDREN;
            }

            final HSSDEditor editor = searchResult.getHSSDEditor();
            if(parentElement.equals(editor.getEditorInput())) {
                final EntryTree tree = editor.getMasterCP().getEntryTree();
                return children.get(tree.root().get()).toArray();
            }
        }
        else if(parentElement instanceof TreeNode) {
            final TreeNode en = (TreeNode)parentElement;
            final HashSet<TreeNode> cs = children.get(en);
            if(cs != null) {
                return cs.toArray();
            }
        }
        return NO_CHILDREN;
    }

    @Override
    public Object getParent(Object element) {
        if(element.equals(searchResult)) {
            return null;
        }
        else if(element instanceof IEditorInput) {
            if(searchResult == null) {
                return null;
            }
            final HSSDEditor editor = searchResult.getHSSDEditor();
            if(element.equals(editor.getEditorInput())) {
                return searchResult;
            }
        }
        else if(element instanceof TreeNode) {
            final TreeNode en = (TreeNode)element;
            return Interop.or(en.parent());
        }
        return null;
    }

    @Override
    public boolean hasChildren(Object element) {
        if(element.equals(searchResult)) {
            return searchResult.getHSSDEditor() != null && !children.isEmpty();
        }
        else if(element instanceof IEditorInput) {
            if(searchResult == null) {
                return false;
            }
            final HSSDEditor editor = searchResult.getHSSDEditor();
            if(element.equals(editor.getEditorInput())) {
                final EntryTree tree = editor.getMasterCP().getEntryTree();
                return children.get(tree.root().get()) != null;
            }
        }
        else if(element instanceof TreeNode) {
            return children.get((TreeNode)element) != null;
        }
        return false;
    }

    @Override
    public Object[] getElements(Object inputElement) {
        return getChildren(inputElement);
    }
    
}
