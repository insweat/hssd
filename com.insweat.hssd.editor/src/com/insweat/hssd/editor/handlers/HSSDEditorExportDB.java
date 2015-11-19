package com.insweat.hssd.editor.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;

import com.insweat.hssd.editor.editors.hssd.HSSDEditor;
import com.insweat.hssd.editor.util.Helper;

class StopIterationException extends RuntimeException {

    private static final long serialVersionUID = -54629474568313326L;
    
}

public class HSSDEditorExportDB extends AbstractCommandHandler {
    
	public Object execute(ExecutionEvent event) {
        final HSSDEditor editor = getActiveHSSDEditor();
        if(editor == null) {
            return null;
        }

        if(!(editor.getEditorInput() instanceof IFileEditorInput)) {
        	return null;
        }
        IFileEditorInput input = (IFileEditorInput)editor.getEditorInput();
		final Object res = input.getFile();

        final IWorkbenchPage activePage = Helper.getActiveWBPage();
        if(activePage.saveAllEditors(true)) {
            doExportDB((IFile)res);
        }

	    return null;
	}

    private void doExportDB(IFile file) {
    	throw new UnsupportedOperationException();
    }
}
