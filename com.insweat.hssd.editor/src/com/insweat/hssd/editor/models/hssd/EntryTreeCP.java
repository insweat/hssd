package com.insweat.hssd.editor.models.hssd;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;

import scala.Option;

import com.insweat.hssd.editor.util.LogSupport;
import com.insweat.hssd.lib.essence.Database;
import com.insweat.hssd.lib.essence.SchemaLike;
import com.insweat.hssd.lib.interop.Interop;
import com.insweat.hssd.lib.persistence.xml.XMLDatabaseLoader;
import com.insweat.hssd.lib.persistence.xml.XMLDatabaseSaver;
import com.insweat.hssd.lib.tree.EntryTree;
import com.insweat.hssd.lib.tree.structured.TreeNode;
import com.insweat.hssd.lib.util.logging.Logger;

public class EntryTreeCP
        implements IStructuredContentProvider, ITreeContentProvider {

    private final LogSupport log = new LogSupport("EntryTree");
    
    private final static Object[] NO_CHILDREN = new Object[0];

    private Database db = null;

    public void dispose() {
    }

    
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    	log.noticef("inputChanged: %s, %s, %s", viewer, oldInput, newInput);
    	if(newInput != null && newInput instanceof FileEditorInput) {
    		final FileEditorInput input = (FileEditorInput) newInput;
    		db = loadDB(input.getFile(), log.getLogger());
    	}
    	else {
    		db = null;
    	}
    }
 
    
    public Object[] getChildren(Object parentElement) {
    	if(parentElement instanceof FileEditorInput) {
    		return getRootChildren();
    	}
    	else if (parentElement instanceof TreeNode) {
    		final TreeNode node = (TreeNode)parentElement;
    		return Interop.toArray(node.children());
    	}
    	return NO_CHILDREN;
    } 
    
    public Object getParent(Object element) {
    	if(element instanceof TreeNode) {
    		final TreeNode node = (TreeNode)element;
    		if(node.hasParent()) {
    			return node.parent().get();
    		}
    	}
    	return null;
    }
 
    public boolean hasChildren(Object element) {
    	if(element instanceof FileEditorInput) {
    		return db != null;
    	}
    	else if(element instanceof TreeNode) {
    		final TreeNode node = (TreeNode)element;
    		return node.childCount() > 0;
    	}
    	return false;
    }

    public Object[] getElements(Object inputElement) {
    	return getChildren(inputElement);
    }

    
    @Override
    public String toString() {
        return "EntryTreeContentProvider";
    }
    
    
    public boolean isDBChanged() {
        return db != null && db.changed();
    }

    
    public void markDBChanged() {
        if(db != null) {
            db.changed_$eq(true);
        }
    }

    
    public void save(IEditorInput input) {
        final IFile desc = ((FileEditorInput)input).getFile();
        final File file = desc.getLocation().toFile();
        final XMLDatabaseSaver saver = new XMLDatabaseSaver(file);
        saver.save(Interop.opt(log.getLogger()), db);
        db.changed_$eq(false);
    }

    public Database getDB() {
        return db;
    }

    public SchemaLike getActiveSchema() {
        if(db == null) {
            return null;
        }
        return db.activeSchema();
    }

    public EntryTree getEntryTree() {
        if(db != null) {
            return db.entries();
        }
        return null;
    }
    
    public String getSetting(String name) {
    	if(db != null) {
    		return Interop.or(db.settings().get(name));
    	}
    	return null;
    }

    public static Database loadDB(IFile desc, Logger log) {
        final File file = desc.getLocation().toFile();
        final XMLDatabaseLoader loader = new XMLDatabaseLoader(file);
        final Option<Database> rv = loader.load(Interop.opt(log));
        return Interop.or(rv);
    }

    private Object[] getRootChildren() {
        if(db != null && db.entries().hasRoot()) {
            final TreeNode root = db.entries().root().get();
            return getChildren(root);
        }
        else {
            return NO_CHILDREN;
        }
    }
}

