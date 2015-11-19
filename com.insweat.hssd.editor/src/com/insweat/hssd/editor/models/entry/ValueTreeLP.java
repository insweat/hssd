package com.insweat.hssd.editor.models.entry;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;

import scala.Option;
import scala.Tuple2;
import scala.util.Either;

import com.insweat.hssd.editor.Activator;
import com.insweat.hssd.editor.models.LPHelper;
import com.insweat.hssd.editor.util.Helper;
import com.insweat.hssd.lib.essence.CollectionThypeLike;
import com.insweat.hssd.lib.essence.Interpreted;
import com.insweat.hssd.lib.essence.SimpleThypeLike;
import com.insweat.hssd.lib.essence.Thype;
import com.insweat.hssd.lib.essence.TraitThypeLike;
import com.insweat.hssd.lib.essence.ValExpr;
import com.insweat.hssd.lib.essence.ValueData;
import com.insweat.hssd.lib.essence.ValueError;
import com.insweat.hssd.lib.essence.ValueText;
import com.insweat.hssd.lib.essence.thypes.LStringThype;
import com.insweat.hssd.lib.essence.thypes.ReferenceThype;
import com.insweat.hssd.lib.interop.Interop;
import com.insweat.hssd.lib.tree.flat.TreeNode;


public class ValueTreeLP
        extends StyledCellLabelProvider implements ILabelProvider {

    private final static String IMG_OBJ_TRAIT = 
            "/icons/full/obj16/trait_obj.gif";
    
    private final static String IMG_OBJ_COMPLEX = 
            "/icons/full/obj16/class_obj.gif";
    
    private final static String IMG_OBJ_LEAF = 
            "/icons/full/obj16/defpub_obj.gif";

    private final static String IMG_OBJ_REF = 
            "/icons/full/obj16/correction_rename.gif";
    
    private final static String IMG_OBJ_LSTR = 
            "/icons/full/obj16/sclassf_obj.gif";
    
    public static enum Column {
        CAPTION ("Caption", SWT.LEFT, 1),
        VALUE   ("Value", SWT.LEFT, 1),
        VALEX   ("Valex", SWT.LEFT, 1);

        public final String label;
        public final int style;
        public final int weight;
        
        Column(String label, int style, int weight) {
            this.label = label;
            this.style = style;
            this.weight = weight;
        }
    }


	@Override
    public void update(ViewerCell cell) {
		if(!(cell.getElement() instanceof TreeNode)) {
			final String text = String.format(
					"(%s, %s)", 
					cell.getElement(),
					cell.getColumnIndex()
			);
			cell.setText(text);
			super.update(cell);
			return;
		}

		final TreeNode node = (TreeNode)cell.getElement();
		final int columnIndex = cell.getColumnIndex();
	    final StyledString styledText = getStyledText(node, columnIndex);
	    
	    if(columnIndex == Column.CAPTION.ordinal()) {
            cell.setImage(getImage(node));
        }
	    
		cell.setText(styledText.toString());
		cell.setStyleRanges(styledText.getStyleRanges());
		super.update(cell);
	}


    /** This method, as well as ILabelProvider, is required by ViewerSorter.
     */
	@Override
	public String getText(Object element) {
		if(!(element instanceof TreeNode)) {
			return "";
		}
		final TreeNode vn = (TreeNode)element;
		final ValueData vd = ValueData.of(vn);
		return vd.element().caption();
	}

	@Override
	public String getToolTipText(Object element) {
	    if(!(element instanceof TreeNode)) {
	        return null;
	    }
	    final TreeNode vn = (TreeNode)element;
	    final ValueData vd = ValueData.of(vn);
	    final String desc = vd.element().description();
	    final String thype = vd.element().thype().name();
	    final String name = vd.element().name();
	    final String value = 
	            getStyledText(vn, Column.VALUE.ordinal()).getString();
	    if(!value.isEmpty()) {
	        return String.format("%s%n%s %s = %s", desc, thype, name, value);
	    }
	    else {
	        return String.format("%s%n%s %s", desc, thype, name);
	    }
	}

	@Override
	public Image getImage(Object element) {
		if(element instanceof TreeNode) {
			final TreeNode vn = (TreeNode)element;
			final ValueData vd = ValueData.of(vn);
			final Thype thype = vd.element().thype();
			if(thype instanceof TraitThypeLike) {
			    return Activator.getImage(IMG_OBJ_TRAIT);
			}
			else if(thype instanceof CollectionThypeLike) {
			    String key = ISharedImages.IMG_OBJ_ELEMENT;
			    return Helper.getSharedImage(key);
			}
			else if(thype instanceof ReferenceThype) {
			    return Activator.getImage(IMG_OBJ_REF);
			}
			else if(thype instanceof LStringThype) {
			    return Activator.getImage(IMG_OBJ_LSTR);
			}
			else if(vn.isLeaf()) {
			    return Activator.getImage(IMG_OBJ_LEAF);
			}
			else {
			    return Activator.getImage(IMG_OBJ_COMPLEX);
			}
		}
		return null;
	}
	
	private static String getCaption(TreeNode vn) {
	    ValueData vd = ValueData.of(vn);
	    return vd.element().caption();
	}
	
	private static Tuple2<String, Boolean> getValex(TreeNode vn) {
	    ValueData vd = ValueData.of(vn);
	    final Option<ValExpr> optVE = vd.valex()._1;
        final Thype thype = vd.element().thype();
        final boolean isSimple = thype instanceof SimpleThypeLike;

        if(isSimple && isOverridden(vn)) {
            String label;
            if(optVE.isDefined() && optVE.get().isError()){
                label = optVE.get().repr();
                return Interop.tuple(label, true);
            }
            else if(optVE.isDefined() && thype instanceof Interpreted) {
                label = optVE.get().repr();
                final Interpreted interp = (Interpreted)thype;
                Either<String, Object> e =
                        interp.interpOut(vn, optVE.get().value());
                if(e.isRight()) {
                    label = Interop.literal(e.right().get());
                    label = ValExpr.fmt(optVE.get().sym(), label);
                    return Interop.tuple(label, false);
                }
                else {
                    label = e.left().get();
                    return Interop.tuple(label, true);
                }
            }
            else {
                label = optVE.get().repr();
                return Interop.tuple(label, false);
            }
        }
        return Interop.tuple("", false);
	}

	public static StyledString getStyledText(TreeNode node, int columnIndex) {
	    boolean overridden = isOverridden(node);
        
	    if(columnIndex == Column.CAPTION.ordinal()) {
	        String text = getCaption(node);
	        return mkStyledText(text, false, overridden);
        }
        else if(columnIndex == Column.VALUE.ordinal()) {
        	ValueData vd = ValueData.of(node);
        	ValueText text = vd.valueText();
        	boolean isError = text instanceof ValueError;
            return mkStyledText(text.value(), isError, overridden);
        }
        else if(columnIndex == Column.VALEX.ordinal()) {
            Tuple2<String, Boolean> r = getValex(node);
            return mkStyledText(r._1(), r._2(), overridden);
        }
	    return mkStyledText("", false, false);
	}

	private static StyledString mkStyledText(
	        String text, boolean isError, boolean overridden) {
		
		if(isError) {
            return new StyledString(text, LPHelper.ERROR_STYLER);    
        }
		else if(overridden) {
			return new StyledString(text, LPHelper.BOLD_STYLER);
		}
		else {
			return new StyledString(text);
		}
	}
	
	public static boolean isOverridden(TreeNode node) {
	    final ValueData vd = ValueData.of(node);
	    final Tuple2<Option<ValExpr>, Object> valex = vd.valex();
	    return vd.valueTree().isOverridden(node.path()) 
	            && !(Boolean)valex._2 && valex._1.isDefined();
	}
}
