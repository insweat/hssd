package com.insweat.hssd.editor.models.l10n;

import java.lang.ref.WeakReference;

public final class Row {
	public final WeakReference<Chunk> parent;
	public final Object[] cols;
    
    Row(Chunk parent, int numCols) {
        this.parent = new WeakReference<>(parent);
        this.cols = new Object[numCols];
    }
}