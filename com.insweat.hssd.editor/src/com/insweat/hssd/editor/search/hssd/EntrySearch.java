package com.insweat.hssd.editor.search.hssd;

import org.eclipse.search.core.text.TextSearchEngine;

import com.insweat.hssd.lib.tree.TreePath;

public class EntrySearch {
	
	public static class Pattern {
		public final String queryStr;
		public final java.util.regex.Pattern pattern;
		
		public Pattern(String qs, boolean caseAware, boolean regex) {
			this.queryStr = qs;
			this.pattern = TextSearchEngine.createPattern(qs, caseAware, regex);
		}
	}

	public static enum Objective {
		ID, NAME, PATH, CAPTION, VALUE, FWD_REF, BWD_REF, LSTR_REF
	}

	public static class Constraint {
		public final boolean isLeaf;
		public final TreePath valuePath;
		
		public Constraint(boolean isLeaf, TreePath valuePath) {
			this.isLeaf = isLeaf;
			this.valuePath = valuePath;
		}
	}
}
