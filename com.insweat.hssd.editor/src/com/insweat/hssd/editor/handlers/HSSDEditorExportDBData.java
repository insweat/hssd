package com.insweat.hssd.editor.handlers;

import java.io.File;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;

import com.insweat.hssd.editor.editors.hssd.HSSDEditor;
import com.insweat.hssd.editor.util.Helper;
import com.insweat.hssd.export.Exporter;
import com.insweat.hssd.lib.essence.Database;

public class HSSDEditorExportDBData extends AbstractCommandHandler {
    
	public Object execute(ExecutionEvent event) {
	    return watchedExecute(() -> {
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
                File loc = ((IFile)res).getLocation().toFile();
                doExportDBData(editor.getMasterCP().getDB(), loc.getParentFile());
            }

            return null;
	    });
	}

    private void doExportDBData(Database db, File parentLocation) {
        Exporter exporter = new Exporter();
        exporter.exportDBData(db, parentLocation);
    }
}
