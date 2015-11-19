package com.insweat.hssd.editor.editors.hssd;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.services.IServiceLocator;

import com.insweat.hssd.editor.util.Helper;
import com.insweat.hssd.lib.essence.EntryData;
import com.insweat.hssd.lib.tree.structured.TreeNode;

public class DropEntryHandler extends ViewerDropAdapter {
    private final static String CMD_ID_MOVE_ENTRY =
            "com.insweat.hssd.editor.command.hssd.MoveEntry"; 
    public DropEntryHandler(TreeViewer viewer) {
        super(viewer);
    }

    @Override
    public boolean performDrop(Object data) {
        IServiceLocator locator = Helper.getWB();
        ICommandService svc = (ICommandService)locator.getService(
                ICommandService.class);
        Command cmd = svc.getCommand(CMD_ID_MOVE_ENTRY);

        Map<String, String> params = new HashMap<>();

        params.put("source", data.toString());

        TreeNode en = (TreeNode)getCurrentTarget();
        EntryData ed = EntryData.of(en);
        params.put("target", String.valueOf(ed.entryID()));

        try {
            cmd.executeWithChecks(
            		new ExecutionEvent(cmd, params, getCurrentEvent(), null));
        } catch (ExecutionException | NotDefinedException | NotEnabledException
                | NotHandledException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    @Override
    public boolean validateDrop(Object target, int operation,
            TransferData transferType) {
        if(getCurrentLocation() == LOCATION_ON && target instanceof TreeNode) {
            TreeNode en = (TreeNode)target;
            if(en.isLeaf()) {
                return false;
            }

            Transfer transfer = TextTransfer.getInstance();
            return transfer.isSupportedType(transferType);
        }
        return false;
    }

}
