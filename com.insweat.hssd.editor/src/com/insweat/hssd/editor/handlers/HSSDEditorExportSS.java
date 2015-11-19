package com.insweat.hssd.editor.handlers;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IFileEditorInput;

import com.insweat.hssd.editor.editors.hssd.HSSDEditor;
import com.insweat.hssd.editor.models.spreadsheet.SpreadSheetModel;
import com.insweat.hssd.editor.models.spreadsheet.SpreadSheetModel.RowKey;
import com.insweat.hssd.editor.util.S;
import com.insweat.hssd.lib.essence.CollectionThypeLike;
import com.insweat.hssd.lib.essence.EntryData;
import com.insweat.hssd.lib.essence.SimpleThypeLike;
import com.insweat.hssd.lib.essence.ValueData;
import com.insweat.hssd.lib.tree.structured.TreeNode;
import com.insweat.hssd.lib.interop.EssenceHelper;
import com.insweat.hssd.lib.interop.Interop;
import com.insweat.hssd.lib.tree.TreeNodeLike;
import com.insweat.hssd.lib.tree.TreePath;

import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;

import scala.Tuple2;

public class HSSDEditorExportSS extends AbstractCommandHandler {
	
	private static class WBContext {
		public final static String HLINK_STYLE = "HLINK_STYLE";
		public final static String TITLE_STYLE = "TITLE_STYLE";
		public final static String KEY_STYLE = "KEY_STYLE";

		public final Map<String, Object> props = new HashMap<>();
		public final Workbook wb;

		public WBContext(Workbook wb) {
			this.wb = wb;

			createHLinkStyle();
			createTitleStyle();
			createKeyStyle();
		}
		
		@SuppressWarnings("unchecked")
		public <T> T getProp(String key) {
			return (T)props.get(key);
		}
		
		public void close() throws Exception {
			props.clear();
			wb.close();
		}
		
		private void createHLinkStyle() {
			CellStyle s = wb.createCellStyle();
			props.put(HLINK_STYLE, s);
			
			Font font = wb.createFont();
			font.setUnderline(Font.U_SINGLE);
			font.setColor(IndexedColors.BLUE.getIndex());

			s.setFont(font);
		}
		
		private void createTitleStyle() {
			CellStyle s = wb.createCellStyle();
			props.put(TITLE_STYLE, s);
			
			s.setFillPattern(CellStyle.SOLID_FOREGROUND);
			s.setFillForegroundColor(IndexedColors.AQUA.getIndex());
		}

		private void createKeyStyle() {
			CellStyle s = wb.createCellStyle();
			props.put(KEY_STYLE, s);
			s.setFillPattern(CellStyle.SOLID_FOREGROUND);
			s.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
			s.setWrapText(true);
		}
	}
	
	private static class Coord {
		public final int indent;
		public final int column;
		public final TreeNode en;
		
		public Coord(int indent, int column, TreeNode en) {
			this.indent = indent;
			this.column = column;
			this.en = en;
		}
	}
	
	private static class HLink {
		public final String text;
		public final String addr;
		
		public HLink(EntryData ed) {
			this.text = SSHelper.mkColumnName(ed);
			this.addr = SSHelper.mkSheetLinkAddr(ed);
		}
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final HSSDEditor editor = getActiveHSSDEditor();
        if(editor == null) {
            return null;
        }

        EntryData ed = getSelectedEntry();
        IFileEditorInput fei = (IFileEditorInput)editor.getEditorInput(); 
        File folder = fei.getFile().getLocation().toFile().getParentFile();
        FileOutputStream fos = null;
        WBContext context = new WBContext(new XSSFWorkbook());
        try {
        	$$export(context, null, ed);
        	exportHierarchy(context, ed);

            String name = SSHelper.mkWorkbookName(ed, "xlsx");
            fos = new FileOutputStream(new File(folder, name));
            context.wb.write(fos);
        }
        catch(Exception e) {
        	log.errorf("Failed to export %s: %s", ed.entryID(), e);
        	throw new RuntimeException(e);
        }
        finally {
            if(fos != null) {
                try {
                    fos.close();
                }
                catch(Exception e) {
                    log.warnf("Failed to close file: %s", e);
                }
            }
        	try {
                context.close();
        	}
        	catch(Exception e) {
        		log.warnf("Failed to close workbook: %s", e);
        	}
        }
		return null;
	}

	private void exportHierarchy(WBContext context, EntryData ed) {
		String name = SSHelper.HIERARCHY;
		Sheet sheet = context.wb.createSheet(name);
		context.wb.setSheetOrder(name, 0);
		List<Coord> coords = new ArrayList<>();
		iterHierarchy(coords, 0, 0, ed.entryNode());
		
        Row titleRow = sheet.createRow(SSHelper.TITLE_ROW);
        {
            Cell cellName = titleRow.createCell(0);
            cellName.setCellValue(SSHelper.NAME);
            cellName.setCellStyle(context.getProp(WBContext.TITLE_STYLE));

            Cell cellId = titleRow.createCell(1);
            cellId.setCellValue(SSHelper.ID);
            cellId.setCellStyle(context.getProp(WBContext.TITLE_STYLE));

            Cell cellCap = titleRow.createCell(2);
            cellCap.setCellValue(SSHelper.CAPTION);
            cellCap.setCellStyle(context.getProp(WBContext.TITLE_STYLE));
        }

		for(int i = 0; i < coords.size(); ++i) {
            Coord coord = coords.get(i);
			char[] chars = new char[coord.indent];
			Arrays.fill(chars, ' ');
			Row row = sheet.createRow(SSHelper.DATA_ROW_START + i);

			EntryData $ed = EntryData.of(coord.en);
			row.createCell(0).setCellValue(S.fmt("%s%s", String.valueOf(chars),
					coord.en.name()));

			Cell cell = row.createCell(1);
			cell.setCellValue($ed.entryID());

			CreationHelper helper = context.wb.getCreationHelper();
			Hyperlink link = helper.createHyperlink(Hyperlink.LINK_DOCUMENT);
			if(coord.column != 0) {
				EntryData parentED = EntryData.of(coord.en.parent().get());
                link.setAddress(SSHelper.mkSheetLinkAddr(parentED, coord.column));
			}
			else {
                link.setAddress(SSHelper.mkSheetLinkAddr($ed));
			}
			cell.setHyperlink(link);
			cell.setCellStyle(context.getProp(WBContext.HLINK_STYLE));

			row.createCell(2).setCellValue($ed.caption());
		}

		sheet.autoSizeColumn(0);
		sheet.autoSizeColumn(1);
		sheet.autoSizeColumn(2);
	}
	
	private void iterHierarchy(
			List<Coord> coords, int indent, int col, TreeNode en) {
		coords.add(new Coord(indent, col, en));

		int column = 0;
		TreeNodeLike[] children = (TreeNodeLike[])Interop.toArray(
				en.children(), TreeNodeLike.class);
		Arrays.sort(children, new SSHelper.ENComparator());
		for(TreeNodeLike child: children) {
			if(child.isLeaf()) {
                ++column;
			}
			iterHierarchy(coords,
					indent + SSHelper.INDENTION,
					column,
					(TreeNode)child);
		};
	}
	
	private void $$export(WBContext context, HLink parent, EntryData ed) {
		SpreadSheetModel ssm = new SpreadSheetModel();
		addColumn(ssm, ed);
		
		TreeNodeLike[] children = (TreeNodeLike[])Interop.toArray(
				ed.entryNode().children(), TreeNodeLike.class);
		Arrays.sort(children, new SSHelper.ENComparator());
		for(TreeNodeLike en: children) {
            if(!en.isLeaf()) {
            	continue;
            }

            addColumn(ssm, EntryData.of((TreeNode)en));
        }

        String name = SSHelper.mkSheetName(ed);
        writeSheet(context, parent, name, ssm);

        HLink myLink = new HLink(ed);
        for(TreeNodeLike en: children) {
            if(en.isLeaf()) {
            	continue;
            }

            $$export(context, myLink, EntryData.of((TreeNode)en));
        }
	}
	
	private void addColumn(SpreadSheetModel ssm, EntryData ed) {
		final List<Tuple2<RowKey,String>> content = new ArrayList<>();
        EssenceHelper.foreach(ed, vn -> {
            ValueData vd = ValueData.of(vn);
            TreePath path = vd.path();
            if(path.length() > 1) {
            	String pathStr = path.toString();
            	String caption = vd.element().caption();
            	RowKey key = ssm.key(pathStr, caption);
                if(vd.element().thype() instanceof SimpleThypeLike) {
                	String value;
                	if(vd.isOverridden()) {
                		value = vd.valexText().value();
                	}
                	else {
                		// For the sake of clarity
                		// value = SSHelper.INHERITED + vd.valueText().value();
                		value = "";
                	}
                    content.add(Interop.tuple(key, value));
                }
                else if(vd.element().thype() instanceof CollectionThypeLike) {
                    String value = "";
                    if(!vd.valueTree().isOverridden(path)) {
                        value = SSHelper.INHERITED;
                    }
                    content.add(Interop.tuple(key, value));
                }
            }
        });

		String name = SSHelper.mkColumnName(ed);
		ssm.addColumn(name, content);
	}
	
	private void writeSheet(
			WBContext context,
			HLink parent,
			String name,
			SpreadSheetModel ssm) {
		Sheet sheet = context.wb.createSheet(name);

		Row headerRow = sheet.createRow(0);
        CreationHelper helper = context.wb.getCreationHelper();
        Hyperlink link = helper.createHyperlink(Hyperlink.LINK_DOCUMENT);
		if(parent != null) {
            headerRow.createCell(0).setCellValue(SSHelper.PARENT);
            Cell parentCell = headerRow.createCell(1);
            parentCell.setCellValue(parent.text);

			link.setAddress(parent.addr);
			parentCell.setHyperlink(link);
			parentCell.setCellStyle(context.getProp(WBContext.HLINK_STYLE));
		}

		headerRow = sheet.createRow(1);
        Cell hierarchyCell = headerRow.createCell(0);
        hierarchyCell.setCellValue(SSHelper.HIERARCHY);
        link.setAddress(S.fmt("'%s'!A1", SSHelper.HIERARCHY));
        hierarchyCell.setHyperlink(link);
        hierarchyCell.setCellStyle(context.getProp(WBContext.HLINK_STYLE));

		Row titleRow = sheet.createRow(SSHelper.TITLE_ROW);
		{
            Cell pathCell = titleRow.createCell(0);
            pathCell.setCellValue(SSHelper.PATH);
            pathCell.setCellStyle(context.getProp(WBContext.TITLE_STYLE));

            Cell capCell = titleRow.createCell(1);
            capCell.setCellValue(SSHelper.CAPTION);
            capCell.setCellStyle(context.getProp(WBContext.TITLE_STYLE));
		}

		for(int j = 0; j < ssm.cols().size(); ++j) {
			final int colIndex = SSHelper.DATA_COL_START + j;
			Cell cell = titleRow.createCell(colIndex);
			cell.setCellValue(ssm.cols().get(j));
			cell.setCellStyle(context.getProp(WBContext.TITLE_STYLE));
		}

		int rowIndex = SSHelper.DATA_ROW_START;
		List<RowKey> rowOrder = new ArrayList<>(ssm.rows().keySet());
		Collections.sort(rowOrder);
		for(RowKey key: rowOrder) {
			Row row = sheet.createRow(rowIndex);
			++rowIndex;
			
			SpreadSheetModel.Row r = ssm.rows().get(key);
			Cell pathCell = row.createCell(0);
			pathCell.setCellValue(key.path.toString());
			pathCell.setCellStyle(context.getProp(WBContext.KEY_STYLE));
			
			Cell capCell = row.createCell(1);
			capCell.setCellValue(key.caption);
			capCell.setCellStyle(context.getProp(WBContext.KEY_STYLE));

			for(int j = 0; j < ssm.cols().size(); ++j) {
                final int colIndex = SSHelper.DATA_COL_START + j;
                String col = ssm.cols().get(j);
                String value = r.cells().getOrDefault(col, SSHelper.EMPTY);
				row.createCell(colIndex).setCellValue(value);
			}
		}

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
		for(int j = 0; j < ssm.cols().size(); ++j) {
            final int colIndex = SSHelper.DATA_COL_START + j;
			sheet.autoSizeColumn(colIndex);
		}

		sheet.createFreezePane(
				SSHelper.DATA_COL_START,
				SSHelper.DATA_ROW_START,
				SSHelper.DATA_COL_START,
				SSHelper.DATA_ROW_START
		);
	}
}
