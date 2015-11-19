package com.insweat.hssd.editor.handlers;

import java.util.Comparator;

import com.insweat.hssd.editor.models.IntAwareCmp;
import com.insweat.hssd.editor.util.S;
import com.insweat.hssd.lib.essence.EntryData;
import com.insweat.hssd.lib.tree.TreeNodeLike;

public final class SSHelper {
	private SSHelper() {
	}
	
	public final static String INHERITED = "<INHERITED>";
	public final static String EMPTY = "<EMPTY>";

	public final static String PARENT = "Parent";
	public final static String PATH = "Path";
	public final static String CAPTION = "Caption";
	public final static String NAME = "Name";
	public final static String ID = "Id";

	public final static String HIERARCHY = "Hierarchy";
	
	public final static int TITLE_ROW = 2;
	public final static int DATA_ROW_START = TITLE_ROW + 1;
	public final static int DATA_COL_START = 2;
	
	public final static int INDENTION = 4;

	public static class ENComparator implements Comparator<TreeNodeLike> {
		@Override
		public int compare(TreeNodeLike a, TreeNodeLike b) {
			return IntAwareCmp.instance.compare(a.name(), b.name());
		}
	}
	
	public static String mkWorkbookName(EntryData ed, String ext) {
		return S.fmt("%s(%s).%s", ed.entryNode().name(), ed.entryID(), ext);
	}
	
	public static String mkSheetName(EntryData ed) {
		return mkColumnName(ed);
	}

	public static String mkColumnName(EntryData ed) {
		return S.fmt("%s(%s)", ed.entryNode().name(), ed.entryID());
	}
	
	public static String mkSheetLinkAddr(EntryData ed) {
		return mkSheetLinkAddr(ed, 0);
	}

	public static String mkSheetLinkAddr(EntryData ed, int column) {
		char c = (char)((int)'C' + column);
		return S.fmt("'%s'!%s%d", mkSheetName(ed), c, DATA_ROW_START);
	}
}
