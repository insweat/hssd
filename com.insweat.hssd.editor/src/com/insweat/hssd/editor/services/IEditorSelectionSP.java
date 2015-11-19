package com.insweat.hssd.editor.services;

import java.lang.reflect.Array;
import java.util.Iterator;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISourceProvider;


public interface IEditorSelectionSP extends ISourceProvider {
    public IEditorPart getEditor();
    public ISelection getSelection();
    public Object getFirstSelected();
    public <T> T[] selToArray(Class<T> clazz, T[] array);
    public void updateSelection(IEditorPart editor, ISelection selection);

    public final class Helper {
        private Helper() {}

        @SuppressWarnings("unchecked")
        public static <T> T[] selToArray(
                IStructuredSelection sel, Class<T> clazz, T[] array) {
            if(sel == null) {
                return null;
            }

            final int n = sel.size();
            if(array == null || array.length < n) {
                final T[] arr = (T[])Array.newInstance(clazz, sel.size());
                array = arr;
            }

            @SuppressWarnings("rawtypes")
            final Iterator itr = sel.iterator();
            for(int i = 0; i < n; ++i) {
                itr.hasNext(); // play nice
                array[i] = (T)itr.next();
            }
            return array;
        }
    }
}
