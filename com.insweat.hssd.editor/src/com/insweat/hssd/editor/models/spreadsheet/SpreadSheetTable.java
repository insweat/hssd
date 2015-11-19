package com.insweat.hssd.editor.models.spreadsheet;

import java.util.ArrayList;
import java.util.List;


public class SpreadSheetTable {
    private final static String SPLIT_EOL = "\\r|\\n|\\r\\n";
    private final static String TAB = "\t";
    private final static String EPT = "";

    private StringBuilder sb = new StringBuilder();
    private List<String[]> table = new ArrayList<>();
    private int numCols;

    public SpreadSheetTable() {
    }

    public SpreadSheetTable(String content) {
        String[] rows = content.split(SPLIT_EOL);

        String[][] cells = new String[rows.length][];
        for(int i = 0; i < rows.length; ++i) {
            cells[i] = rows[i].split(TAB);
        }

        for(int i = 0; i < rows.length; ++i) {
            String[] row = cells[i];
            addRowInternal(row);
        }
    }

    public int numRows() {
        return table.size();
    }

    public int numCols() {
        return numCols;
    }

    public void addRow(String[] row) {
        addRowInternal(row.clone());
    }
    
    public String get(int i, int j) {
        String[] row = table.get(i);
        return get(row, j);
    }
    
    private void addRowInternal(String[] row) {
        numCols = Math.max(numCols, row.length);
        table.add(row);
        sb.setLength(0);
    }
    
    private String get(String[] row, int j) {
        if(j < row.length) {
            return row[j];
        }
        else {
            return EPT;
        }
    }

    @Override
    public String toString() {
        if(sb.length() == 0 && !table.isEmpty()) {
            for(int i = 0; i < table.size(); ++i) {
                String[] row = table.get(i);
                for(int j = 0; j < numCols; ++j) {
                    if(j > 0) {
                        sb.append(TAB);
                    }
                    sb.append(get(row, j));
                }
                sb.append(String.format("%n"));
            }
        }
        return sb.toString();
    }
}
