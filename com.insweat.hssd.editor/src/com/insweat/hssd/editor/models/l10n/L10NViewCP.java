package com.insweat.hssd.editor.models.l10n;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;

import scala.Option;

import com.insweat.hssd.lib.essence.EntryData;
import com.insweat.hssd.lib.essence.SchemaLike;
import com.insweat.hssd.lib.essence.Thype;
import com.insweat.hssd.lib.essence.ValueData;
import com.insweat.hssd.lib.essence.thypes.LStringThype;
import com.insweat.hssd.lib.interop.EssenceHelper;
import com.insweat.hssd.lib.tree.EntryTree;


final class Chunk {
    public final static int CHUNK_SIZE = 200;
    public final WeakReference<L10NViewCP> parent;
    public final int chunkIndex;
    public final long firstID;
    public final long lastID;
    public Row[] rows;
    
    public Chunk(L10NViewCP parent, int chunkIndex, long firstID, long lastID) {
    	this.parent = new WeakReference<>(parent);
        this.chunkIndex = chunkIndex;
        this.firstID = firstID;
        this.lastID = lastID;
    }

    public boolean filled() {
        return rows != null;
    }

    public void fill(LStringThype src, L10NViewCP.Column[] cols, long[] sIDs) {
        final int begin = chunkIndex * CHUNK_SIZE;
        final int end = Math.min(begin + CHUNK_SIZE, sIDs.length);
        final Row[] rows = new Row[end - begin];
        for(int i = 0; i < rows.length; ++i) {
            final Row row = new Row(this, cols.length);
            final long stringID = sIDs[begin + i];
            int n = 0;
            row.cols[n++] = stringID;
            row.cols[n++] = parent.get().getStringIDRefCount(stringID);
            for(int j = n; j < cols.length; ++j) {
                final String lang = cols[j].label;
                row.cols[j] = src.apply(stringID, lang);    
            }
            rows[i] = row;
        }
        this.rows = rows;
    }
}

public class L10NViewCP
        implements IStructuredContentProvider, ITreeContentProvider {

    public static class Column {
    	public final static int NUM_EXTRA_COLS = 2;
        public final static int ID_COL_WEIGHT = 2;
        public final static int COUNT_COL_WEIGHT = 1;
        public final static int LANG_COL_WEIGHT = 8;

        public final String label;
        public final int style;
        public final int weight;
        
        public Column(String label, int style, int weight) {
            this.label = label;
            this.style = style;
            this.weight = weight;
        }
    }

    private final Object[] NO_CHILDREN = new Object[0];

    private LStringThype strSrc;
    private Column[] columns;
    private long[] stringIDs;
    private Chunk[] chunks;
    private Map<Long, Integer> stringIDRefCount;


    @Override
    public void dispose() {
        chunks = null;
        columns = null;
        stringIDs = null;
        strSrc = null;
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        chunks = null;
        stringIDs = null;
        stringIDRefCount = null;

        if(!(newInput instanceof EntryTree)) {
        	return;
        }
        
        EntryTree tree = (EntryTree)newInput;
        strSrc = asLST(tree);
        if(strSrc == null) {
            return;
        }

        stringIDs = strSrc.allStringIDs();
        Arrays.sort(stringIDs);
        
        stringIDRefCount = new HashMap<>(stringIDs.length);
        EssenceHelper.foreach(tree, en -> {
        	EntryData ed = EntryData.of(en);
        	EssenceHelper.foreach(ed, vn -> {
        		ValueData vd = ValueData.of(vn);
        		if(!(vd.element().thype() instanceof LStringThype)) {
        			return;
        		}

        		if(vd.value().isError()) {
        			return;
        		}

        		LStringThype lst = (LStringThype)vd.element().thype();
        		Long sid = (Long)lst.fixed(vd.value().value());
        		if(sid == null) {
        			return;
        		}
        		
        		int count = stringIDRefCount.getOrDefault(sid, 0);
        		stringIDRefCount.put(sid, count + 1);
        	});
        });
        
        final float numChunks = stringIDs.length / (float)Chunk.CHUNK_SIZE;
        chunks = new Chunk[(int)Math.ceil(numChunks)];
        for(int i = 0; i < chunks.length; ++i) {
            final int firstIndex = i * Chunk.CHUNK_SIZE;
            final int lastIndex = Math.min(
                    firstIndex + Chunk.CHUNK_SIZE, stringIDs.length) - 1;
            final long firstID = stringIDs[firstIndex];
            final long lastID = stringIDs[lastIndex];
            chunks[i] = new Chunk(this, i, firstID, lastID);
        }
    }

    public Column[] createColumns(EntryTree tree) {
        final LStringThype src = asLST(tree);
        final String[] langs = src.allLangs(); 
        int n = 0;
        columns = new Column[langs.length + Column.NUM_EXTRA_COLS];
        columns[n++] = new Column("String ID", SWT.LEFT, Column.ID_COL_WEIGHT);
        columns[n++] = new Column(
        		"Ref Count", SWT.LEFT, Column.COUNT_COL_WEIGHT);
        final int langColWeight = Column.LANG_COL_WEIGHT;
        for(int i = 0; i < langs.length; ++i) {
            columns[i + n] = new Column(langs[i], SWT.LEFT, langColWeight);
        }
        return columns.clone();
    }

    private LStringThype asLST(EntryTree tree) {
        final SchemaLike sch = tree.schema();
        final Option<Thype> optLST = sch.get("LString");
        if(!optLST.isDefined()) {
            return null;
        }

        return (LStringThype)optLST.get();
    }
    
    @Override
    public Object[] getElements(Object inputElement) {
        if(chunks != null) {
            return chunks;
        }
        return NO_CHILDREN;
    }

    @Override
    public Object[] getChildren(Object parentElement) {
        if(parentElement instanceof Chunk) {
            final Chunk chunk = (Chunk)parentElement;
            if(!chunk.filled()) {
                chunk.fill(strSrc, columns, stringIDs);
            }
            return chunk.rows;
        }
        return NO_CHILDREN;
    }

    @Override
    public Object getParent(Object element) {
        if(element instanceof Row) {
            final Row row = (Row)element;
            return row.parent.get();
        }
        return null;
    }

    @Override
    public boolean hasChildren(Object element) {
        return element instanceof Chunk;
    }

    protected int getStringIDRefCount(long id) {
    	if(stringIDRefCount == null) {
    		return 0;
    	}
    	return stringIDRefCount.getOrDefault(id, 0);
    }
}
