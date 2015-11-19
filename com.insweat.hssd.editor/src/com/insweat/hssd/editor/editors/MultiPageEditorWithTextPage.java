package com.insweat.hssd.editor.editors;

import com.insweat.hssd.editor.models.DecoratingLabelProvider;
import com.insweat.hssd.editor.util.LogSupport;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;

public abstract class MultiPageEditorWithTextPage
        extends MultiPageEditorPart
        implements IResourceChangeListener {

    protected final LogSupport log = new LogSupport(
            getClass().getSimpleName());

    private TextEditor editor;
    private int pageIndex = -1;

    public MultiPageEditorWithTextPage() {
        super();
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
    }

    protected void createPrimaryPage(TextEditor editor) {
        try {
            this.editor = editor;
            pageIndex = addPage(editor, getEditorInput());
            setPageText(pageIndex, editor.getTitle());
            setPartName(editor.getTitle());
        } catch (PartInitException e) {
            handlePartInitException(
                    e,
                    "Part Initialization Failure",
                    "Failed to create primary page.");
        }
    }

    public int getPrimaryPageIndex() {
        if(-1 == pageIndex) {
            throw new IllegalStateException("Primary page is not created.");
        }
        return pageIndex;
    }

    public TextEditor getPrimaryEditor() {
        return editor;
    }

    @Override
    public void dispose() {
        editor.dispose();
    	ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
    	super.dispose();
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
    	getEditor(pageIndex).doSave(monitor);
    }

    @Override
    public void doSaveAs() {
    	IEditorPart editor = getEditor(pageIndex);
    	editor.doSaveAs();
    	setPageText(pageIndex, editor.getTitle());
    	setInput(editor.getEditorInput());
    }

    public void gotoMarker(IMarker marker) {
    	setActivePage(pageIndex);
    	IDE.gotoMarker(getEditor(pageIndex), marker);
    }



    @Override
    public boolean isSaveAsAllowed() {
    	return true;
    }

    @Override
    public void resourceChanged(final IResourceChangeEvent event) {
        /**
         * Closes all project files on project close.
         */
    	if(event.getType() != IResourceChangeEvent.PRE_CLOSE){
    	    return;
    	}
    	Display.getDefault().asyncExec(new Runnable(){
    		public void run(){
    			final IWorkbenchPage[] pages = 
    			        getSite().getWorkbenchWindow().getPages();
    			final FileEditorInput input = 
    			        (FileEditorInput)editor.getEditorInput();
    			final IProject proj = input.getFile().getProject();
    			for (int i = 0; i<pages.length; i++){
    				if(proj.equals(event.getResource())){
    					final IEditorPart editorPart = 
    					        pages[i].findEditor(editor.getEditorInput());
    					pages[i].closeEditor(editorPart, true);
    				}
    			}
    		}            
    	});
    }

    protected ILabelProvider wrapLabelProvider(ILabelProvider labelProvider) {
        if(labelProvider instanceof IStyledLabelProvider) {
            return new DecoratingLabelProvider(
                    (IStyledLabelProvider)labelProvider);
        }
        return labelProvider;
    }

    protected void handlePartInitException(
            PartInitException e,
            String title,
            String message) {
        ErrorDialog.openError(
                getSite().getShell(),
                title,
                message,
                e.getStatus());
    }
}
