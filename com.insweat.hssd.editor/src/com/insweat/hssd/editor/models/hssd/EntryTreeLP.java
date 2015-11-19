package com.insweat.hssd.editor.models.hssd;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.part.FileEditorInput;

import com.insweat.hssd.editor.util.Helper;
import com.insweat.hssd.editor.util.S;
import com.insweat.hssd.lib.essence.EntryData;
import com.insweat.hssd.lib.tree.structured.TreeNode;

public class EntryTreeLP extends LabelProvider implements IStyledLabelProvider {

	@Override
	public StyledString getStyledText(Object element) {
	    if(element instanceof FileEditorInput) {
	        final FileEditorInput input = (FileEditorInput)element;
	        final StyledString rv = new StyledString();
	        rv.append(input.getFile().getFullPath().toString());
	        return rv;
	    }
	    else if(element instanceof TreeNode) {
			final TreeNode node = (TreeNode)element;
			final StyledString rv = new StyledString(node.name());
			final EntryData ed = EntryData.of(node);
			
			if(node.isLeaf()) {
				String caption;
				try {
					caption = ed.caption();
				}
				catch(Exception e) {
					String exClass = e.getClass().getSimpleName();
					String exMessage = e.getMessage();
					caption = S.fmt("%s(%s)", exClass, exMessage);
				}
				if(!caption.isEmpty()) {
					caption = S.fmt(" %s", caption);
					rv.append(caption, StyledString.DECORATIONS_STYLER); 
				}
			}
			return rv;
		}
		return null;
	}

	@Override
	public String getText(Object element) {
	    if(element instanceof FileEditorInput) {
	        final FileEditorInput input = (FileEditorInput)element;
	        return input.getFile().getFullPath().toString();
	    }
	    else if(element instanceof TreeNode) {
			final TreeNode node = (TreeNode)element;
			return node.name();
		}
		return null;
	}

	@Override
	public Image getImage(Object element) {
	    if(element instanceof IEditorInput) {
	        final String imageKey = ISharedImages.IMG_DEF_VIEW;
	        return Helper.getSharedImage(imageKey);
	    }
	    else if(element instanceof TreeNode) {
			final String imageKey;
			final TreeNode node = (TreeNode)element;
			if(node.isLeaf()) {
				imageKey = ISharedImages.IMG_OBJ_FILE;
			}
			else {
				imageKey = ISharedImages.IMG_OBJ_FOLDER;
			}
			return Helper.getSharedImage(imageKey);
		}
		return null;
	}
}
