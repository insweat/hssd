package com.insweat.hssd.editor.editors.entry;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import scala.Option;
import scala.Tuple2;
import scala.util.Either;

import com.insweat.hssd.editor.editors.SelectCellEditor;
import com.insweat.hssd.editor.models.entry.ValueTreeLP;
import com.insweat.hssd.editor.services.IDService;
import com.insweat.hssd.editor.util.Helper;
import com.insweat.hssd.editor.util.LogSupport;
import com.insweat.hssd.lib.essence.EnumLike;
import com.insweat.hssd.lib.essence.Interpreted;
import com.insweat.hssd.lib.essence.SimpleThypeLike;
import com.insweat.hssd.lib.essence.Thype;
import com.insweat.hssd.lib.essence.ValExpr;
import com.insweat.hssd.lib.essence.ValueData;
import com.insweat.hssd.lib.essence.thypes.BoolThype;
import com.insweat.hssd.lib.essence.thypes.LStringThype;
import com.insweat.hssd.lib.essence.thypes.ReferenceThype;
import com.insweat.hssd.lib.interop.Interop;
import com.insweat.hssd.lib.tree.TreePath;
import com.insweat.hssd.lib.tree.flat.TreeNode;


public class EntryEditorEditingSupport extends EditingSupport {

    private final LogSupport log = new LogSupport("EntryEditor");
    private final WeakReference<EntryEditor> weakEditor;

    private final TextCellEditor textCellEditor;
    private final SelectCellEditor selectCellEditor;
    private final TextCellEditor textAreaCellEditor;

    public EntryEditorEditingSupport(EntryEditor editor) {
        super(editor.getEntryViewer());
        weakEditor = new WeakReference<EntryEditor>(editor);

        final Composite parent = editor.getEntryViewer().getTree();
        textCellEditor = new TextCellEditor(parent) {
        	@Override
        	public LayoutData getLayoutData() {
        		// NB the SDK would ignore minimumHeight in the returned
        		// LayoutData if it is SWT.DEFAULT. So in order to revert
        		// minimumHeight set by a previous textAreaCellEditor,
        		// we must force the minimumHeight to be zero.
        		LayoutData rv = super.getLayoutData();
        		rv.minimumHeight = 0;
        		return rv;
        	}
        };
        selectCellEditor = new SelectCellEditor(parent) {
        	@Override
        	public LayoutData getLayoutData() {
        		// See comments above.
        		LayoutData rv = super.getLayoutData();
        		rv.minimumHeight = 0;
        		return rv;
        	}
        };
        textAreaCellEditor = new TextCellEditor(
        		parent,
        		SWT.MULTI | SWT.V_SCROLL | SWT.BORDER | SWT.WRAP) {
        	@Override
        	public LayoutData getLayoutData() {
        		LayoutData rv = super.getLayoutData();
        		rv.minimumHeight = 100;
        		return rv;
        	}
        };
    }

    @Override
    protected CellEditor getCellEditor(Object element) {
        if(element instanceof TreeNode) {
            final TreeNode node = (TreeNode)element;
            final ValueData vd = ValueData.of(node);
            if(vd.element().thype() instanceof EnumLike) {
                return getEnumCellEditor(node);
            }
            else if(vd.element().thype() instanceof BoolThype) {
                return getBoolCellEditor(node);
            }
            else if(vd.element().thype() instanceof LStringThype) {
            	return getTextAreaCellEditor(node);
            }
        }
 
        return textCellEditor;
    }

    @Override
    protected boolean canEdit(Object element) {
        final ValueData vd = getVD(element);
        if(vd.element().thype() instanceof SimpleThypeLike) {
            SimpleThypeLike t = (SimpleThypeLike)vd.element().thype();
            return t.editable();
        }
        return false;
    }

    @Override
    protected Object getValue(Object element) {
        return readValue(element);
    }
    
    public static Object readValue(Object element) {
        final ValueData vd = getVD(element);
        final Thype thype = vd.element().thype();
        final Tuple2<Option<ValExpr>, Object> valex = vd.valex();

        if(!valex._1.isDefined() || (Boolean)valex._2) {
            return "";
        }

        ValExpr ve = valex._1.get();

        if(ve.isError()) {
            return "";
        }

        if(!(thype instanceof Interpreted)) {
            return ve.repr();
        }

        final Interpreted interp = (Interpreted)(thype);
        final Either<String, Object> e = interp.interpOut(element, ve.value());
        if(e.isRight()) {
            return ValExpr.fmt(ve.sym(), Interop.literal(e.right().get()));
        }
        else {
            return ValExpr.fmtErr(e.left().get());
        }
    }

    @Override
    protected void setValue(Object element, Object value) {
        writeValue(element, value, log);

        ValueData vd = ValueData.of((TreeNode)element);
        EntryEditor editor = weakEditor.get();
        if(vd.element().thype() instanceof ReferenceThype) {
        	editor.refresh(element, false);
        } else {
        	editor.update(element);
        	if(vd.path().equals(TreePath.fromStr("Root.Entry.caption"))) {
            	EditorInput input = (EditorInput)editor.getEditorInput();
            	input.getHSSDEditor().refresh(vd.entryNode(), false);
            }
        }

        editor.markDirty();
    }
    
    public static void writeValue(
            Object element, Object value, LogSupport log) {
        final String valueStr = Interop.literal(value);
        log.infof("Setting %s to '%s'", element, valueStr);

        final ValueData vd = getVD(element);
        if("".equals(valueStr)) {
        	if(vd.isOverridden()) {
                vd.valex_$eq(Interop.none());
        	}
        }
        else {
            String s = valueStr;
            final Thype thype = vd.element().thype();

            if(thype instanceof LStringThype) {
                // LString is copied on write
                if(!ValueTreeLP.isOverridden((TreeNode)element)) {
                    final IDService svc = Helper.getIDSvc();
                    final long sid = svc.acquire(Helper.getActiveProject(),
                            IDService.Namespace.STRING_ID);
                    final String sym = ValExpr.absSym();
                    final ValExpr ve = ValExpr.make(sym, thype, sid);
                    vd.valex_$eq(Option.apply(ve));
                }
            }

            final ValExpr ve;
            final Tuple2<String, String> sv = ValExpr.preparse(s);
            if(thype instanceof Interpreted) {
                final Interpreted interp = (Interpreted)thype;
                if(ValExpr.shpSym().equals(sv._1)) {
                    s = sv._2;
                    ve = ValExpr.parse(thype, ValExpr.absSym(), s);
                }
                else {
                    Either<String, Object> e = interp.interpIn(element, sv._2);
                    if(e.isRight()) {
                        s = Interop.literal(e.right().get());
                        ve = ValExpr.parse(thype, sv._1, s);    
                    }
                    else {
                        s = e.left().get();
                        ve = ValExpr.make(ValExpr.errSym(), thype, s);
                    }
                }
            }
            else {
                if(ValExpr.shpSym().equals(sv._1)) {
                    String msg = "%s regarded as %s on non-interpreted: %s";
                    log.warnf(msg, ValExpr.shpSym(), ValExpr.absSym(), element);
                    ve = ValExpr.parse(thype, ValExpr.absSym(), sv._2);
                }
                else {
                    ve = ValExpr.parse(thype, sv._1, sv._2);    
                }
            }

            vd.valex_$eq(Option.apply(ve));
        }
    }

    private static ValueData getVD(Object node) {
        return ValueData.of((TreeNode)node);
    }
    
    private CellEditor getTextAreaCellEditor(TreeNode node) {
    	return textAreaCellEditor;
    }
    
    private CellEditor getBoolCellEditor(TreeNode node) {
        return getSelectCellEditor(true, "true", "false");
    }
    
    private CellEditor getEnumCellEditor(TreeNode node) {
        final ValueData vd = getVD(node);
        final EnumLike enumLike = (EnumLike)vd.element().thype();
        final Object[] values = Interop.toArray(enumLike.values(node));

        for(int i = 0; i < values.length; ++i) {
            Either<String, Object> e = enumLike.interpOut(node, values[i]);
            if(e.isRight()) {
                values[i] = Interop.literal(e.right().get());    
            }
            else {
                values[i] = e.left().get();
            }
        }

        return getSelectCellEditor(true, values);
    }
    
    private CellEditor getSelectCellEditor(
            boolean nullable, Object ... values) {
        final ArrayList<String> options = 
                new ArrayList<String>(values.length + 1);
        if(nullable) {
            options.add("");
        }
        final String sym = ValExpr.absSym();
        for(int i = 0; i < values.length; ++i) {
            options.add(ValExpr.fmt(sym, Interop.literal(values[i])));
        }
        selectCellEditor.setItems(options.toArray(new String[options.size()]));
        return selectCellEditor;
    }
}
