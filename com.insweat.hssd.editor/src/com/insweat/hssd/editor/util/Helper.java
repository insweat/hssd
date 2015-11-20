package com.insweat.hssd.editor.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISourceProvider;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.services.ISourceProviderService;

import com.insweat.hssd.editor.editors.entry.EditorInput;
import com.insweat.hssd.editor.editors.entry.EntryEditor;
import com.insweat.hssd.editor.editors.hssd.HSSDEditor;
import com.insweat.hssd.editor.services.EntryEditorSourceProvider;
import com.insweat.hssd.editor.services.HSSDEditorSourceProvider;
import com.insweat.hssd.editor.services.IDService;
import com.insweat.hssd.editor.views.l10n.L10NView;

public final class Helper {
    public final static IWorkbenchPage[] NO_PAGES = new IWorkbenchPage[0];

    private Helper() {
    }
    
    public static boolean objeq(Object a, Object b) {
        if(a == b) {
            return true;
        }

        if(a != null) {
            return a.equals(b);
        }
        
        return b.equals(a);
    }
    
    public static IDService getIDSvc() {
        final IWorkbench workbench = getWB();
        final Object svc = workbench.getService(IDService.class);
        return (IDService)svc;
    }
    
    public static ISourceProviderService getSPSvc(ExecutionEvent event) {
        final IWorkbenchWindow window = 
                HandlerUtil.getActiveWorkbenchWindow(event);
        final Object svc = window.getService(ISourceProviderService.class);
        return (ISourceProviderService)svc;
    }
    
    public static EntryEditorSourceProvider getEntryEditorSP(
            ISourceProviderService spSvc) {
        final String srcName = EntryEditorSourceProvider.VAR_ENTRY_SELECTION;
        final ISourceProvider sp = spSvc.getSourceProvider(srcName);
        return (EntryEditorSourceProvider)sp;
    }
    
    public static HSSDEditorSourceProvider getHSSDEditorSP(
            ISourceProviderService spSvc) {
        final String srcName = HSSDEditorSourceProvider.VAR_HSSD_SELECTION;
        final ISourceProvider sp = spSvc.getSourceProvider(srcName);
        return (HSSDEditorSourceProvider)sp;
    } 
    
    public static HSSDEditor getLastHSSDEditor() {
        final ISourceProviderService svc = getSPSvc();
        final HSSDEditorSourceProvider sp = getHSSDEditorSP(svc);
        final IEditorPart ep = sp.getEditor();
        if(!(ep instanceof HSSDEditor)) {
            return null;
        }

        return (HSSDEditor)ep;
    }
    
    public static IProject getActiveProject() {
        HSSDEditor hssdEditor = getActiveHSSDEditor();
        if(hssdEditor == null) {
            return null;
        }
        FileEditorInput fei = (FileEditorInput)hssdEditor.getEditorInput();
        return fei.getFile().getProject();
    }

    public static IWorkbench getWB() {
        return PlatformUI.getWorkbench();
    }

    public static IWorkbenchWindow getActiveWBWindow() {
        return getWB().getActiveWorkbenchWindow();
    }

    public static IWorkbenchPage getActiveWBPage() {
        return getActiveWBWindow().getActivePage();
    }

    public static IWorkbenchPart getActivePart() {
        return getActiveWBPage().getActivePart();
    }
    
    public static HSSDEditor getActiveHSSDEditor() {
        final IWorkbenchPart part = Helper.getActivePart();
        if(part instanceof HSSDEditor) {
            return (HSSDEditor)part;
        }
        if(part instanceof EntryEditor) {
            EntryEditor entryEditor = (EntryEditor)part;
            EditorInput input = (EditorInput)entryEditor.getEditorInput();
            return input.getHSSDEditor();
        }
        return null;
    }
    
    public static EntryEditor getActiveEntryEditor() {
        final IWorkbenchPart part = Helper.getActivePart();
        if(part instanceof EntryEditor) {
            return (EntryEditor)part;
        }
        return null;
    }
    
    public static L10NView getL10NView() {
    	IWorkbenchPage page = Helper.getActiveWBPage();
    	if(page == null) {
    		return null;
    	}

    	try {
			IViewPart part = page.showView(L10NView.ID);
			return (L10NView)part;
		} catch (PartInitException e) {
			throw new RuntimeException(e);
		}
    }
    
    public static Iterable<IWorkbenchPage> iterWBPages() {
        return new Iterable<IWorkbenchPage>(){
            @Override
            public Iterator<IWorkbenchPage> iterator() {
                return new Iterator<IWorkbenchPage>(){

                    private IWorkbenchWindow[] windows;
                    private IWorkbenchPage[] pages;
                    private int wi;
                    private int pi;
                    private IWorkbenchPage next;

                    @Override
                    public boolean hasNext() {
                        if(windows == null) {
                            windows = getWB().getWorkbenchWindows();
                            if(windows.length > 0) {
                                pages = windows[0].getPages();
                            }
                            else {
                                pages = NO_PAGES;
                            }
                            wi = pi = 0;
                        }

                        next = null;
                        while(wi < windows.length && pi == pages.length) {
                            pi = 0;
                            wi += 1;
                            if(wi < windows.length) {
                                pages = windows[wi].getPages();    
                            }
                            else {
                                pages = NO_PAGES;
                            }
                        }

                        if(wi < windows.length) {
                            next = pages[pi++];
                        }

                        return next != null;
                    }

                    @Override
                    public IWorkbenchPage next() {
                        if(next == null) {
                            throw new NoSuchElementException();
                        }
                        return next;
                    }
                    
                };
            }
            
        };
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T getSPSvc() {
        return (T)getWB().getService(ISourceProviderService.class);
    }
    
    @SuppressWarnings("unchecked")
    public static <T extends ISourceProvider> T getSP(
            ISourceProviderService svc,
            String var
            ) {
        return (T)svc.getSourceProvider(var);
    }
    
    public static CommonNavigator findCommonNavigator(String navigatorViewId)
    {
        IWorkbenchPage page = getActiveWBPage();
        if (page != null)
        {
            IViewPart view = page.findView(navigatorViewId);
            if (view != null && view instanceof CommonNavigator)
                return ((CommonNavigator) view);
            }
        return null;
    }
    
    public static Image getSharedImage(String imageKey) {
        return PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
    }
}
