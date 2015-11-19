package com.insweat.hssd.editor.models;

import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.TextStyle;

public final class LPHelper {
    private LPHelper() {}

    private static abstract class BaseStyler extends Styler {

        protected Font getBoldFont() {
            final FontRegistry reg = JFaceResources.getFontRegistry();
            return reg.getBold(JFaceResources.DEFAULT_FONT);
        }

        protected Font getItalicFont() {
            final FontRegistry reg = JFaceResources.getFontRegistry();
            return reg.getItalic(JFaceResources.DEFAULT_FONT);
        }

        protected Color getColor(String name) {
            final ColorRegistry cr = JFaceResources.getColorRegistry();
            return cr.get(name);
        }
    }

    public static final Styler ITALIC_STYLER = new BaseStyler() {

        @Override
        public void applyStyles(TextStyle textStyle) {
            textStyle.font = getItalicFont();
            if (textStyle instanceof StyleRange) {
                ((StyleRange) textStyle).fontStyle = SWT.ITALIC;
            }
        }
    };

    public static final Styler BOLD_STYLER = new BaseStyler() {

        @Override
        public void applyStyles(TextStyle textStyle) {
            textStyle.font = getBoldFont();
            if (textStyle instanceof StyleRange) {
                ((StyleRange) textStyle).fontStyle = SWT.BOLD;
            }
        }
    };

    public static final Styler DIM_OUT_STYLER = new BaseStyler() {

        @Override
        public void applyStyles(TextStyle textStyle) {
            textStyle.font = getItalicFont();
            textStyle.foreground = getColor(JFacePreferences.DECORATIONS_COLOR);
            if (textStyle instanceof StyleRange) {
                ((StyleRange) textStyle).fontStyle = SWT.ITALIC;
            }
        }
    };

    public static final Styler ERROR_STYLER = 
            StyledString.createColorRegistryStyler(
                    JFacePreferences.ERROR_COLOR,
                    null);

    public static <T extends StyledCellLabelProvider>
            StyledCellLabelProvider wrap(T baseLP) {
        return new StyledCellLabelProvider() {
            
            @Override
            public void update(ViewerCell cell) {
                baseLP.update(cell);
            }
            
            @Override
            public String getToolTipText(Object element) {
                return baseLP.getToolTipText(element);
            }
        };
    }
}
