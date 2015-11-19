package com.insweat.hssd.editor.models;

import org.eclipse.jface.viewers.DecoratingStyledCellLabelProvider;
import org.eclipse.jface.viewers.IDecorationContext;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;

import com.insweat.hssd.editor.util.Helper;

public class DecoratingLabelProvider
        extends DecoratingStyledCellLabelProvider
        implements ILabelProvider {
    public DecoratingLabelProvider(
            IStyledLabelProvider labelProvider,
            ILabelDecorator decorator,
            IDecorationContext decorationContext) {
        super(labelProvider, decorator, decorationContext);
    }
    
    public DecoratingLabelProvider(
            IStyledLabelProvider labelProvider,
            ILabelDecorator decorator) {
        super(labelProvider, decorator, null);
    }

    public DecoratingLabelProvider(IStyledLabelProvider labelProvider) {
        super(
                labelProvider,
                Helper.getWB().getDecoratorManager().getLabelDecorator(),
                null);
    }

    @Override
    public String getText(Object element) {
        return getStyledText(element).getString();
    }
}
