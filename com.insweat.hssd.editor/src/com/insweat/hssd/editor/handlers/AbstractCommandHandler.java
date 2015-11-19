package com.insweat.hssd.editor.handlers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;

import com.insweat.hssd.editor.editors.entry.EntryEditor;
import com.insweat.hssd.editor.editors.hssd.HSSDEditor;
import com.insweat.hssd.editor.util.Helper;
import com.insweat.hssd.editor.util.LogSupport;
import com.insweat.hssd.editor.views.l10n.L10NView;
import com.insweat.hssd.lib.essence.EntryData;

abstract class AbstractCommandHandler extends AbstractHandler {
    protected final LogSupport log = new LogSupport(getClass().getSimpleName());

    protected void refreshAllEntryEditors() {
        EntryEditor.multiApply((e) -> {
            e.refresh(null, false, false);
            return false;
        }, null);
    }

    protected HSSDEditor getActiveHSSDEditor() {
        HSSDEditor editor = Helper.getActiveHSSDEditor();
        if(editor == null) {
            log.warnf("No active HSSDEditor.");
        }
        return editor;
    }

    protected EntryEditor getActiveEntryEditor() {
        EntryEditor editor = Helper.getActiveEntryEditor();
        if(editor == null) {
            log.warnf("No active EntryEditor.");
        }
        return editor;
    }
    
    protected L10NView getL10NView() {
    	L10NView view = Helper.getL10NView();
    	if(view == null) {
    		log.warnf("Localization view not found.");
    	}
    	return view;
    }
    
	protected EntryData getSelectedEntry() {
		final HSSDEditor editor = getActiveHSSDEditor();
        if(editor == null) {
            return null;
        }
        
        ISelectionProvider sp = editor.getSite().getSelectionProvider();
        IStructuredSelection sel = (IStructuredSelection)sp.getSelection();
        
        return EntryData.of((com.insweat.hssd.lib.tree.structured.TreeNode)
        		sel.getFirstElement());
	}
	
	protected List<EntryData> getSelectedEntries() {
		final HSSDEditor editor = getActiveHSSDEditor();
        if(editor == null) {
            return null;
        }
        
        ISelectionProvider sp = editor.getSite().getSelectionProvider();
        IStructuredSelection sel = (IStructuredSelection)sp.getSelection();
        
        List<EntryData> rv = new ArrayList<>(sel.size());
        for(Iterator<?> itr = sel.iterator(); itr.hasNext();) {
            rv.add(EntryData.of((com.insweat.hssd.lib.tree.structured.TreeNode)
                            itr.next()));
        }
        return rv;
	}
	
	protected String fromClipboard(IEditorPart editor) {
        Shell shell = editor.getSite().getShell();
        Display display = shell.getDisplay();
        Clipboard clip = new Clipboard(display);
        try {
            TextTransfer transfer = TextTransfer.getInstance();
            return (String)clip.getContents(transfer);
        }
        catch(Exception e) {
        	ElementHelper.panic(log, "from clipboard", e);
        	throw new RuntimeException(e);
        }
        finally {
            clip.dispose();
        }
	}
	
	protected void intoClipboard(IEditorPart editor, String data) {
        Display display = editor.getSite().getShell().getDisplay();
        Clipboard clip = new Clipboard(display);
        try {
            TextTransfer transfer = TextTransfer.getInstance();
            clip.setContents(new Object[]{data}, new Transfer[]{transfer});
        }
        catch(Exception e) {
        	ElementHelper.panic(log, "into clipboard", e);
        	throw new RuntimeException(e);
        }
        finally {
            clip.dispose();
        }
	}
}
