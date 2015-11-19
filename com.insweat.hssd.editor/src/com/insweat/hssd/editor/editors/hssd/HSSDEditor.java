package com.insweat.hssd.editor.editors.hssd;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.part.FileEditorInput;

import com.insweat.hssd.editor.editors.EditorSelectionSP;
import com.insweat.hssd.editor.editors.MultiPageEditorWithTextPage;
import com.insweat.hssd.editor.editors.entry.EditorInput;
import com.insweat.hssd.editor.editors.entry.EntryEditor;
import com.insweat.hssd.editor.editors.xml.XMLEditor;
import com.insweat.hssd.editor.httpd.StaticHttpServer;
import com.insweat.hssd.editor.models.hssd.EntryTreeCP;
import com.insweat.hssd.editor.models.hssd.EntryTreeLP;
import com.insweat.hssd.editor.services.HSSDEditorSourceProvider;
import com.insweat.hssd.editor.util.Helper;
import com.insweat.hssd.editor.util.S;
import com.insweat.hssd.lib.persistence.VersionError;
import com.insweat.hssd.lib.tree.structured.TreeNode;

public class HSSDEditor extends MultiPageEditorWithTextPage {

    public final static String ID = 
            "com.insweat.hssd.editor.editors.HSSDEditor";
    
	private TreeViewer keyViewer;
	// private DrillDownAdapter drillDownAdapter;

	private Action doubleClickAction;

	private StaticHttpServer fileServer;

    protected void createPage0() {
        Composite composite = new Composite(getContainer(), SWT.NONE);
    
        final Layout layout = new FillLayout();
        composite.setLayout(layout);
        
        keyViewer = new TreeViewer(composite,
                SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
    
        keyViewer.setContentProvider(new EntryTreeCP());
    
        keyViewer.setLabelProvider(
                wrapLabelProvider(new EntryTreeLP()));
    
        keyViewer.setComparator(new ViewerComparator());
    
        try {
        	keyViewer.setInput(getEditorInput());
        }
        catch(VersionError e) {
        	keyViewer.setInput(null);

            final Shell shell = getSite().getShell();
            final String title = "Version Error";
            final S msg = new S();
            msg.addf("This HSSD editor supports resource version up to '%s', ",
            		e.supportedVersion());
            msg.addf("but the resource reports version '%s'.%n%n",
            		e.actualVersion());
            msg.addf("You probably need to upgrade your HSSD editor!");
            MessageDialog.openError(shell, title, msg.toString());
        }
    
        // Create the help context id for the keyViewer's control
        // PlatformUI.getWorkbench().getHelpSystem().setHelp(keyViewer.getControl(), "com.insweat.hssd.editor.viewer");
        hookContextMenu();
        makeActions();
        hookDoubleClickAction();
        hookDragAndDrop();
        // contributeToActionBars();
        
        int index = addPage(composite);
        setPageText(index, "EntryTree");
        
        getSite().setSelectionProvider(keyViewer);
        
        keyViewer.getTree().addSelectionListener(new EditorSelectionSP(
                this,
                keyViewer,
                HSSDEditorSourceProvider.VAR_HSSD_SELECTION));
        
        IContextService svc = 
        		(IContextService)getSite().getService(IContextService.class);

        svc.activateContext("com.insweat.hssd.editor.contexts.inHSSDEditor");
    }        

    @Override
    public void init(IEditorSite site, IEditorInput editorInput)
            throws PartInitException {
    	if (!(editorInput instanceof IFileEditorInput)) {
    	    final String msg = "Invalid Input: Must be IFileEditorInput";
            throw new PartInitException(msg);
    	}
    	super.init(site, editorInput);
    }
    
    @Override
    public void dispose() {
    	try {
    		flipFileServer(false);
    	}
    	finally {
    		super.dispose();
    	}
    }
    
    public void refresh(Object element, boolean ensureExpanded) {
        if(element != null) {
            keyViewer.refresh(element);
            if(ensureExpanded) {
                keyViewer.expandToLevel(element, 1);
            }
        }
        else {
            keyViewer.refresh();
            if(ensureExpanded) {
                keyViewer.expandToLevel(1);
            }
        }
    }

    public void update(Object element) {
        keyViewer.update(element, null);
    }

	@Override
	protected void createPages() {
		createPage0();
		createPrimaryPage(new XMLEditor());
	}
	
	private void makeActions() {
	    doubleClickAction = new Action() {
	        @Override
	        public void run() {
	            final IStructuredSelection selection = 
	                    (IStructuredSelection)keyViewer.getSelection();
				Object obj = selection.getFirstElement();
                log.noticef("doubleClick: %s", obj);
				try {
				    openEntryEditor((TreeNode)obj);
				}
				catch(PartInitException e) {
				    final String title = "Part Initialization Failure";
				    final String msg = String.format(
                            "Failed to open entry editor for %s.", obj);
		            handlePartInitException(e, title, msg);
				}
	        }
	    };
	}

	public void openEntryEditor(
	        TreeNode en,
	        com.insweat.hssd.lib.tree.flat.TreeNode vn) {
	    try 
        {
	        EntryEditor ee = openEntryEditor(en);
	        keyViewer.setSelection(new StructuredSelection(en), true);
	        if(vn != null) {
	            ISelection sel = new StructuredSelection(vn);
	            ee.getEntryViewer().setSelection(sel, true);
	        }
        }
        catch(PartInitException e) {
            final String title = "Part Initialization Failure";
            final String msg = String.format(
                    "Failed to open entry editor for %s.", en);
            handlePartInitException(e, title, msg);
        }
	}
	
	private EntryEditor openEntryEditor(TreeNode en) throws PartInitException {
	    final EditorInput input = new EditorInput(this, en);
	    final IWorkbenchPage activePage = Helper.getActiveWBPage();
	    IEditorPart editor = activePage.findEditor(input);
	    if(editor != null && editor instanceof IReusableEditor) {
	        editor.setFocus();
	        return (EntryEditor)editor;
	    }
	    else {
	        editor = activePage.openEditor(input, EntryEditor.ID);
	        final IEditorPart entryEditor = editor;
	        editor.addPropertyListener(new IPropertyListener() {
                
                @Override
                public void propertyChanged(Object source, int propId) {
                    if(propId == EntryEditor.PROP_DIRTY) {
                        if(entryEditor.isDirty()) {
                            markDirty();
                        }
                        firePropertyChange(PROP_DIRTY);
                    }
                }
            });

	        if(editor instanceof EntryEditor) {
	            ((EntryEditor)editor).initExpand();
	        }

	        return (EntryEditor)editor;
	    }
	}

	private void hookDoubleClickAction() {
	    keyViewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(DoubleClickEvent event) {
                doubleClickAction.run();
            }
        });
	}

	private void hookDragAndDrop() {
	    int ops = DND.DROP_MOVE;
	    Transfer[] types = new Transfer[]{ TextTransfer.getInstance() };
	    
	    keyViewer.addDragSupport(ops, types, new DragEntryHandler(keyViewer));
	    keyViewer.addDropSupport(ops, types, new DropEntryHandler(keyViewer));
	}
	
	public boolean isFileServerRunning() {
		return fileServer != null && fileServer.running();
	}
	
	public void flipFileServer(boolean toStart) {
		if(fileServer != null) {
			fileServer.stop();
			fileServer = null;
		}

		if(toStart) {
			EntryTreeCP cp = getMasterCP();
			String sPort = cp.getSetting("file.server.port");
			String sDocRoot = cp.getSetting("file.server.doc_root");
			
			int port = 8099;
			if(sPort != null) {
				try {
					port = Integer.parseInt(sPort);
				}
				catch (Exception e) {
					log.errorf("Unable to start file server on port %s: %s",
							sPort, e);
					return;
				}
			}
			
			if(sDocRoot == null || !sDocRoot.startsWith("/")) {
				FileEditorInput input = (FileEditorInput)getEditorInput();
				IProject project = input.getFile().getProject();
				if(sDocRoot == null) {
					sDocRoot = project.getLocation().toFile().getAbsolutePath();
				}
				else {
					IFile file = project.getFile(sDocRoot);
					sDocRoot = file.getLocation().toFile().getAbsolutePath();
				}
			}
			
			File docRoot = new File(sDocRoot);
			if(!docRoot.isDirectory()) {
				log.errorf("Unable to start file server at %s", docRoot);
				return;
			}
			
			fileServer = new StaticHttpServer(port, docRoot);
			fileServer.start();
		}
	}
	
	@Override
	public boolean isDirty() {
	    final EntryTreeCP cp = getMasterCP();
	    return (cp != null && cp.isDBChanged()) || super.isDirty();
	}

	public void markDirty() {
	    getMasterCP().markDBChanged();
	    firePropertyChange(PROP_DIRTY);
	}
	
	@Override
	public void doSave(IProgressMonitor monitor) {
	    super.doSave(monitor);
	    if(isDirty()) {
            final EntryTreeCP cp = getMasterCP();
            cp.save(getEditorInput());
            EntryEditor.multiApply((e) -> {
                e.doSave(null);
                return false;
            }, null);
	    }
        firePropertyChange(PROP_DIRTY);
	}
	
	public EntryTreeCP getMasterCP() {
	    return (EntryTreeCP)keyViewer.getContentProvider();
	}
	
	public TreeViewer getKeyViewer() {
	    return keyViewer;
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
        final Menu menu = manager.createContextMenu(keyViewer.getControl());
        keyViewer.getControl().setMenu(menu);
        getSite().registerContextMenu(manager.getId(), manager, keyViewer);
    }
}

