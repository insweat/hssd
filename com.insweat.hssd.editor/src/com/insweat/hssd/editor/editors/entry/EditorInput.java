package com.insweat.hssd.editor.editors.entry;

import java.lang.ref.WeakReference;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import com.insweat.hssd.editor.editors.hssd.HSSDEditor;
import com.insweat.hssd.lib.essence.EntryData;
import com.insweat.hssd.lib.tree.structured.TreeNode;

public class EditorInput implements IEditorInput {

    private int hashcode = 0;
    private final TreeNode treeNode;
    private final WeakReference<HSSDEditor> hssdEditor;

    public EditorInput(HSSDEditor hssdEditor, TreeNode treeNode) {
        this.hssdEditor = new WeakReference<>(hssdEditor);
        this.treeNode = treeNode;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(obj instanceof EditorInput) {
            final EditorInput other = (EditorInput)obj;
            return other.getHSSDEditor() == getHSSDEditor() &&
                    other.getEntryNode() == getEntryNode();
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        if(0 == hashcode) {
            hashcode = getHSSDEditor().hashCode() ^ getEntryNode().hashCode();
        }
        return hashcode;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object getAdapter(Class adapter) {
        return null;
    }

    @Override
    public boolean exists() {
        return treeNode != null;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return null;
    }

    @Override
    public String getName() {
        if(treeNode != null) {
            final EntryData treeData = EntryData.of(treeNode);
            return String.format("%s(%s)", treeNode.name(), treeData.entryID());
        }
        return "";
    }

    @Override
    public IPersistableElement getPersistable() {
        return null;
    }

    @Override
    public String getToolTipText() {
        if(treeNode != null) {
            return treeNode.path().toString();
        }
        return "";
    }

    public TreeNode getEntryNode() {
        return treeNode;
    }
    
    public HSSDEditor getHSSDEditor() {
        return hssdEditor.get();
    }
    
    @Override
    public String toString() {
        return String.format("%s(%s)", getClass().getSimpleName(), getName());
    }
}
