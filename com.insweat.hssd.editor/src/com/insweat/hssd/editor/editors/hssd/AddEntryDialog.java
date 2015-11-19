package com.insweat.hssd.editor.editors.hssd;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.insweat.hssd.editor.editors.hssd.ui.TraitsDialog;
import com.insweat.hssd.editor.editors.hssd.ui.UIHelper;
import com.insweat.hssd.editor.util.S;
import com.insweat.hssd.editor.util.TraitsHelper;
import com.insweat.hssd.lib.essence.EntryData;
import com.insweat.hssd.lib.essence.TraitThypeLike;
import com.insweat.hssd.lib.interop.Interop;
import com.insweat.hssd.lib.tree.structured.TreeNode;

public class AddEntryDialog extends MessageDialog {

    private final static String MESSAGE = "Adding new entry under %s";
    
    private final HSSDEditor editor;
    private final TreeNode parentEN;

    private Label validatorFeedback;
    
    private Text entryName;
    private Text entryTraits;

    private Button createCategory;
    private Button createLeaf;
    
    private TreeNode parentENValue;
    private String entryNameValue;
    private boolean isLeafValue;

    private final List<TraitThypeLike> traits = new ArrayList<>();
    
    public AddEntryDialog(Shell parent, HSSDEditor editor, TreeNode parentEN) {
        super(parent, "Add Entry", null,
                S.fmt(MESSAGE, parentEN.name()),
                NONE,
                new String[]{
                    IDialogConstants.OK_LABEL,
                    IDialogConstants.CANCEL_LABEL,
                }, 1);
        this.editor = editor;
        this.parentEN = parentEN;
    }

    @Override
    protected Control createCustomArea(Composite parent) {
        final GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        parent.setLayout(layout);
        
        // Row 0
        validatorFeedback = new Label(parent, SWT.FILL);
        final GridData validatorFBLD = new GridData(GridData.FILL_HORIZONTAL);
        validatorFBLD.horizontalSpan = 2;
        validatorFeedback.setLayoutData(validatorFBLD);

        // Row 1
        createCategory = new Button(parent, SWT.CHECK);
        createCategory.setText("Create a new category?");
        createCategory.addSelectionListener(new SelectionChangeListener(){
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                if(createCategory.getSelection()) {
                    createLeaf.setSelection(false);
                }
                setMessage(S.fmt(MESSAGE, internalGetParent().name()));
                createLeaf.setEnabled(!createCategory.getSelection());
            }
        });
        new Label(parent, SWT.FILL);

        // Row 2
        new Label(parent, SWT.FILL).setText("Entry Name:");
        entryName = new Text(parent, SWT.BORDER);
        final GridData entryNameLD = new GridData(GridData.FILL_HORIZONTAL);
        entryName.setLayoutData(entryNameLD);
        entryName.addModifyListener(new ModifyListener() {
            
            @Override
            public void modifyText(ModifyEvent e) {
                validateInput();
            }
        });

        // Row 3
        createLeaf = new Button(parent, SWT.CHECK);
        createLeaf.setText("Create a leaf entry?");
        createLeaf.addSelectionListener(new SelectionChangeListener());
        new Label(parent, SWT.FILL);

        // Row 4
        new Label(parent, SWT.NONE).setText("Traits:");
        final Button button = new Button(parent, SWT.PUSH);
        button.setText("Edit Traits");
        button.addSelectionListener(new SelectionListener(){

            @Override
            public void widgetSelected(SelectionEvent e) {
                final TraitsDialog dlg = new TraitsDialog(getShell(), editor);
                dlg.setSelection(traits);
                dlg.setInheritedTraits(getInheritedTraits());
                if(0 != dlg.open()) {
                    return;
                }

                traits.clear();
                traits.addAll(dlg.getSelection());

                final TraitThypeLike[] sa = TraitsHelper.sortedArray(traits);
                final String traitNames = S.join(
                        ", ", (tr) -> tr.caption(), sa);
                entryTraits.setText(String.join(", ", traitNames));
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        // Row 5:
        entryTraits = new Text(parent,
                SWT.READ_ONLY | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        final GridData entryTraitsLD = new GridData(GridData.FILL_HORIZONTAL);
        entryTraitsLD.horizontalSpan = 2;
        entryTraitsLD.heightHint = 100;
        entryTraits.setLayoutData(entryTraitsLD);

        if(parentEN == parentEN.owner().root().get()) {
            createCategory.setSelection(true);
            createCategory.setEnabled(false);
            createLeaf.setSelection(false);
            createLeaf.setEnabled(false);
        }

        return parent;
    }
    
    @Override
    protected Button createButton(Composite parent, int id, String label,
            boolean defaultButton) {
        final Button rv = super.createButton(parent, id, label, defaultButton);
        if(id == IDialogConstants.OK_ID) {
            rv.setEnabled(false);
        }
        return rv;
    }
    
    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.OK_ID) {
            parentENValue = internalGetParent();
            entryNameValue = entryName.getText();
            isLeafValue = createLeaf.getSelection();
        }
        else {
            parentENValue = null;
            entryNameValue = "";
            isLeafValue = false;
        }
        super.buttonPressed(buttonId);
    }    

    private TreeNode internalGetParent() {
        if(createCategory == null) {
            return null;
        }
        if(createCategory.getSelection()) {
            return parentEN.owner().root().get();
        }
        return parentEN;
    }
    
    public TreeNode getParent() {
        return parentENValue;
    }

    public String getName() {
        return entryNameValue;
    }
    
    public boolean isLeaf() {
        return isLeafValue;
    }

    public List<TraitThypeLike> getTraits() {
        if(traits == null) {
            return new ArrayList<>();
        }
        return traits;
    }
    
    private void setMessage(String msg) {
        this.message = msg;
        this.messageLabel.setText(msg);
    }

    private void validateInput() {
        final IInputValidator validator = new UIHelper.EntryNameValidator(
                parentEN, createLeaf.getSelection());

        String error = validator.isValid(entryName.getText());
        getButton(IDialogConstants.OK_ID).setEnabled(error == null);

        if(error == null) {
            error = "";
        }
        validatorFeedback.setText(error);
    }
    
    private List<TraitThypeLike> getInheritedTraits() {
        final List<TraitThypeLike> rv = new ArrayList<>();
        final EntryData parentED = EntryData.of(parentEN);
        Interop.foreach(parentED.traits(), (TraitThypeLike tr) -> {
            rv.add(tr);
        });
        return rv;
    }
    
    class SelectionChangeListener implements SelectionListener {

        @Override
        public void widgetSelected(SelectionEvent e) {
            validateInput();
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent e) {
        }
    }
}
