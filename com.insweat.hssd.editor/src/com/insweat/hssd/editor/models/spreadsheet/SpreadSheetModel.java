package com.insweat.hssd.editor.models.spreadsheet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.insweat.hssd.editor.util.S;
import com.insweat.hssd.lib.tree.TreePath;

import scala.Tuple2;

public class SpreadSheetModel {
	public static class RowKey implements Comparable<RowKey> {
		public final TreePath path;
		public final String caption;
	
		private RowKey(String path, String caption) {
			this.path = TreePath.fromStr(path);
			this.caption = caption;
		}

		@Override
		public boolean equals(Object obj) {
			if(obj == null) {
				return false;
			}
			if(obj.getClass() != getClass()) {
				return false;
			}
			return path.equals(((RowKey)obj).path);
		}

		@Override
		public int hashCode() {
			return path.hashCode();
		}

		@Override
		public int compareTo(RowKey o) {
            return path.compare(o.path);
		}
		
		@Override
		public String toString() {
			return S.fmt("%s(%s)", getClass().getSimpleName(), path);
		}
	}

	public static class Row {
		private final Map<String, String> cells = new HashMap<>();
		
		public Map<String, String> cells() {
			return Collections.unmodifiableMap(cells);
		}
	}

	private final Map<String, RowKey> keys = new HashMap<>();

	private final List<String> columns = new ArrayList<>();
	private final Map<RowKey, Row> rows = new HashMap<>();
	
	public RowKey key(String path, String caption) {
		RowKey rv = keys.get(path);
		if(rv == null) {
			rv = new RowKey(path, caption);
			keys.put(path, rv);
		}
		
		return rv;
	}
	
	public void addColumn(String name, List<Tuple2<RowKey, String>> content) {
		columns.add(name);
		for(Tuple2<RowKey, String> cell: content) {
			RowKey key = cell._1();
			String value = cell._2();

			Row r = rows.get(key);
			if(r == null) {
				r = new Row();
				rows.put(key, r);
			}
			r.cells.put(name, value);
		}
	}
	
	public List<String> cols() {
		return Collections.unmodifiableList(columns);
	}
	
	public Map<RowKey, Row> rows() {
		return Collections.unmodifiableMap(rows);
	}
}
