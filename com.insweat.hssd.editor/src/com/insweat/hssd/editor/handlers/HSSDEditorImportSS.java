package com.insweat.hssd.editor.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.navigator.CommonNavigator;

import com.insweat.hssd.editor.editors.entry.EntryEditorEditingSupport;
import com.insweat.hssd.editor.editors.hssd.HSSDEditor;
import com.insweat.hssd.editor.models.spreadsheet.SpreadSheetModel;
import com.insweat.hssd.editor.models.spreadsheet.SpreadSheetModel.RowKey;
import com.insweat.hssd.editor.models.spreadsheet.SpreadSheetTable;
import com.insweat.hssd.editor.util.Helper;
import com.insweat.hssd.editor.util.S;
import com.insweat.hssd.lib.essence.CollectionThypeLike;
import com.insweat.hssd.lib.essence.EntryData;
import com.insweat.hssd.lib.essence.Thype;
import com.insweat.hssd.lib.essence.ValueData;
import com.insweat.hssd.lib.essence.thypes.ArrayThype;
import com.insweat.hssd.lib.essence.thypes.MapThype;
import com.insweat.hssd.lib.interop.Interop;
import com.insweat.hssd.lib.tree.EntryTree;
import com.insweat.hssd.lib.tree.TreePath;
import com.insweat.hssd.lib.tree.ValueTree;
import com.insweat.hssd.lib.tree.flat.TreeNode;

import org.apache.poi.ss.usermodel.*;

import scala.Option;
import scala.Tuple2;

public class HSSDEditorImportSS extends AbstractCommandHandler {
	
	private final Pattern COL_NAME_PAT = Pattern.compile("[^\\(]+\\((\\d+)\\)");

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final HSSDEditor editor = Helper.getLastHSSDEditor();
        if(editor == null) {
            return null;
        }

        EntryTree et = editor.getMasterCP().getEntryTree();
        
        CommonNavigator nc = Helper.findCommonNavigator("org.eclipse.ui.navigator.ProjectExplorer");

        IStructuredSelection sel = (IStructuredSelection)nc.getSite().getSelectionProvider().getSelection();
        IFile file = (IFile)sel.getFirstElement();
        Workbook wb = null;
        try {
        	wb = WorkbookFactory.create(file.getLocation().toFile());
        	$$import(wb, et);
        }
        catch(Exception e) {
        	log.errorf("Failed to import %s: %s", file, e);
        	throw new RuntimeException(e);
        }
        finally {
        	try {
                wb.close();
        	}
        	catch(Exception e) {
        		log.warnf("Failed to close workbook: %s", e);
        	}
        }

        editor.refresh(null, false);
        editor.markDirty();
		return null;
	}
	
	private void $$import(Workbook wb, EntryTree et) {
        FormulaEvaluator fe = wb.getCreationHelper().createFormulaEvaluator();

		int n = wb.getNumberOfSheets();
		for(int i = 0; i < n; ++i) {
            Sheet sheet = wb.getSheetAt(i);
            if(SSHelper.HIERARCHY.equals(sheet.getSheetName())) {
            	continue;
            }

			SpreadSheetModel ssm = readSheet(sheet, fe);
			for(String col: ssm.cols()) {
				importColumn(ssm, col, et);
			}
		}
	}
	
	private SpreadSheetModel readSheet(Sheet sheet, FormulaEvaluator fe) {
		Row titleRow = sheet.getRow(SSHelper.TITLE_ROW);

		final int lastRow = sheet.getLastRowNum();
		final int lastCol = titleRow.getLastCellNum();

		SpreadSheetModel ssm = new SpreadSheetModel();
        for(int j = SSHelper.DATA_COL_START; j <= lastCol; ++j) {
        	List<Tuple2<RowKey, String>> content = new ArrayList<>(lastRow + 1);
        	Cell cell = titleRow.getCell(j);
        	if(cell == null) {
        		// lastCol seems to be null
        		continue;
        	}

        	String name = cell.getStringCellValue();
            for(int i = SSHelper.DATA_ROW_START; i <= lastRow; ++ i) {
            	Row row = sheet.getRow(i);
            	if(row == null) {
                    // lastRow seems to be valid, but just in case.
            		continue;
            	}

            	String path = readCell(row, 0, fe);
            	String caption = readCell(row, 1, fe);
            	RowKey key = ssm.key(path, caption);
            	String value = readCell(row, j, fe);
            	content.add(Interop.tuple(key, value));
            }
            ssm.addColumn(name, content);
        }
		return ssm;
	}
	
	private String readCell(Row row, int cellIndex, FormulaEvaluator fe) {
		Cell cell = row.getCell(cellIndex);
		CellValue cellValue = fe.evaluate(cell);
		if(cellValue == null) {
			return "";
		}

		switch (cellValue.getCellType()) {
	    case Cell.CELL_TYPE_BOOLEAN:
	    	return String.valueOf(cellValue.getBooleanValue());
	    case Cell.CELL_TYPE_NUMERIC:
	    	return String.valueOf(cellValue.getNumberValue());
	    case Cell.CELL_TYPE_STRING:
	    	return String.valueOf(cellValue.getStringValue());
	    case Cell.CELL_TYPE_BLANK:
	    	return "";
	    case Cell.CELL_TYPE_ERROR:
	    	return cellValue.formatAsString();
	    default:
            return S.fmt("Invalid cellValue (type=%d): %s",
            		cellValue.getCellType(),
            		cellValue.formatAsString());
		}
	}
	
	private void importColumn(SpreadSheetModel ssm, String name, EntryTree et) {
		Matcher m = COL_NAME_PAT.matcher(name);
		if(!m.matches()) {
			log.warnf("Invalid column: %s", name);
			return;
		}

		String idStr = m.group(1);
		int id = Integer.valueOf(idStr);
		Option<com.insweat.hssd.lib.tree.structured.TreeNode> en =
				et.nodesByID().get(id);
		if(!en.isDefined()) {
			log.warnf("No such entry: %s", name);
			return;
		}

		EntryData ed = EntryData.of(en.get());
        ValueTree vt = ed.valueTree();
        Map<TreeNode, List<Integer>> colls = new HashMap<>();
        SpreadSheetTable table = new SpreadSheetTable();
        for(Map.Entry<RowKey, SpreadSheetModel.Row> e: ssm.rows().entrySet()) {
        	RowKey key = e.getKey();
        	SpreadSheetModel.Row r = e.getValue();
            String value = r.cells().get(name);
            if(value.startsWith(SSHelper.EMPTY)) {
            	continue;
            }

            TreePath path = key.path;
            Option<TreeNode> optVN = vt.search(path);
            if(!optVN.isDefined() ||
                    optVN.get().path().length() + 1 < path.length()) {
                log.errorf("Invalid path: %s", path);
                return;
            }

            TreeNode vn = optVN.get();
            ValueData vd = ValueData.of(vn);
            Thype thype = vd.element().thype();
            boolean writeValue = false;
            if(thype instanceof CollectionThypeLike) {
            	if(vn.path().length() == path.length()) {
                    updateInherited(colls, vn, value);
            	}
            	else {
                    updateInherited(colls, vn, "");
                    List<Integer> rows = colls.get(vn);
                    rows.add(table.numRows());
            	}
            }
            else {
                TreeNode parentVN = vn.parent().get();
                ValueData parentVD = ValueData.of(parentVN);
                if(parentVD.element().thype() instanceof CollectionThypeLike) {
                    updateInherited(colls, parentVN, "");
                    List<Integer> rows = colls.get(parentVN);
                    rows.add(table.numRows());
                }
                else {
                	writeValue = true;
                }
            }
            if(value != null && value.startsWith(SSHelper.INHERITED)) {
            	value = "";
            }
            if(writeValue) {
                EntryEditorEditingSupport.writeValue(vn, value, log);
            }

            table.addRow(new String[]{key.path.toString(), value});
        } 
        
        for(Map.Entry<TreeNode, List<Integer>> e: colls.entrySet()) {
            TreeNode parent = e.getKey();
            List<Integer> rows = e.getValue();
            if(rows != null) {
                ValueData parentVD = ValueData.of(parent);
                Thype parentThype = parentVD.element().thype();
                if(parentThype instanceof ArrayThype) {
                    EntryEditorPasteContent.addArrayElements(
                            e.getKey(), table, rows, log);
                }
                else if(parentThype instanceof MapThype) {
                    EntryEditorPasteContent.addMapElements(
                            e.getKey(), table, rows, log);
                }
                else {
                    ElementHelper.unsupportedThype(parentThype);
                }
            }
            else {
                ElementHelper.removeChildren(parent, true);
            }
        }
        ed.markDirty();
	}

	private static void updateInherited(
			Map<TreeNode, List<Integer>> colls,
			TreeNode vn,
			String value) {
        if(value == null || !value.startsWith(SSHelper.INHERITED)) {
            List<Integer> rows = colls.get(vn);
        	if(rows == null) {
        		rows = new ArrayList<>();
        		colls.put(vn, rows);
        	}
        }
        else if(!colls.containsKey(vn)) {
            colls.put(vn, null);
        }
	}
}
