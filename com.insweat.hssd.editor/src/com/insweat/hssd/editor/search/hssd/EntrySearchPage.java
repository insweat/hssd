package com.insweat.hssd.editor.search.hssd;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.insweat.hssd.lib.tree.TreePath;

public class EntrySearchPage extends DialogPage implements ISearchPage {

    private final static int NUM_COLS = 12;
    private final static int NUM_GROUP_COLS = 3;

    private Text queryStr;
    private Button isCaseSensitive;
    private Button isRegex;

    private Group searchAs;
    private Button searchAsId;
    private Button searchAsName;
    private Button searchAsPath;
    private Button searchAsCaption;
    private Button searchAsValue;

    private Group limitedTo;
    private Button limitedToLeaf;
    private Button limitedToValuePath;
    private Text limitedToValuePathStr;

    @Override
    public void createControl(Composite parent) {
        parent = new Composite(parent, SWT.NONE);

        createLayout(parent);
        createRow0(parent);
        createRow1(parent);
        createRow2(parent);
        createRow3(parent);
        	
        setControl(parent);
    }

    private void createLayout(Composite parent) {
        final GridLayout layout = new GridLayout();
        layout.numColumns = NUM_COLS;
        layout.makeColumnsEqualWidth = true;
        parent.setLayout(layout);
    }

    private void createRow0(Composite parent) {
        GridData col = createCol(12);

        Label l = new Label(parent, SWT.NONE);
        l.setText("Search string (* = any string, ? = any character, \\ = escape for literals: * ? \\):");
        l.setLayoutData(col);
    }

    private void createRow1(Composite parent) {
        GridData colL = createCol(10);
        GridData colR = createCol(2);

        queryStr = new Text(parent, SWT.BORDER);
        queryStr.setLayoutData(colL);
        
        isCaseSensitive = new Button(parent, SWT.CHECK);
        isCaseSensitive.setText("Case sensitive");
        isCaseSensitive.setLayoutData(colR);
    }
    
    private void createRow2(Composite parent) {
    	GridData colL = createCol(10);
    	GridData colR = createCol(2);

    	Label l = new Label(parent, SWT.NONE);
    	l.setLayoutData(colL);

        isRegex = new Button(parent, SWT.CHECK);
        isRegex.setText("Regular expression");
        isRegex.setLayoutData(colR);
    }
    
    private void createRow3(Composite parent) {
        GridData colL = createCol(6);
        GridData colR = createCol(6);
        GridData col1 = createCol();
        GridData col2 = createCol(2);
        GridData col3 = createCol(3);

        searchAs = new Group(parent, SWT.BORDER);
        searchAs.setText("Search As");
        searchAs.setLayout(new GridLayout(NUM_GROUP_COLS, true));
        searchAs.setLayoutData(colL);
        searchAsId = new Button(searchAs, SWT.CHECK);
        searchAsId.setText("ID");
        searchAsId.setSelection(true);
        searchAsId.setLayoutData(col1);
        searchAsName = new Button(searchAs, SWT.CHECK);
        searchAsName.setText("Name");
        searchAsName.setSelection(true);
        searchAsName.setLayoutData(col1);
        searchAsPath = new Button(searchAs, SWT.CHECK);
        searchAsPath.setText("Path");
        searchAsPath.setSelection(true);
        searchAsPath.setLayoutData(col1);
        searchAsCaption = new Button(searchAs, SWT.CHECK);
        searchAsCaption.setText("Caption");
        searchAsCaption.setSelection(false);
        searchAsCaption.setLayoutData(col1);
        searchAsValue = new Button(searchAs, SWT.CHECK);
        searchAsValue.setText("Value");
        searchAsValue.setSelection(false);
        searchAsValue.setLayoutData(col1);
        searchAsValue.addSelectionListener(new SelectionListener(){

			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean b = searchAsValue.getSelection();
				limitedToValuePath.setEnabled(b);

				b &= limitedToValuePath.getSelection();
				limitedToValuePathStr.setEnabled(b);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
        	
        });
        
        limitedTo = new Group(parent, SWT.BORDER);
        limitedTo.setText("Limited To");
        limitedTo.setLayout(new GridLayout(NUM_GROUP_COLS, true));
        limitedTo.setLayoutData(colR);
        limitedToLeaf = new Button(limitedTo, SWT.CHECK);
        limitedToLeaf.setText("Leaf");
        limitedToLeaf.setSelection(true);
        limitedToLeaf.setLayoutData(col3);
        limitedToValuePath = new Button(limitedTo, SWT.CHECK);
        limitedToValuePath.setText("This value path:");
        limitedToValuePath.setEnabled(false);
        limitedToValuePath.setSelection(false);
        limitedToValuePath.setLayoutData(col1);
        limitedToValuePath.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean b = limitedToValuePath.getSelection();
				limitedToValuePathStr.setEnabled(b);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
        limitedToValuePathStr = new Text(limitedTo, SWT.BORDER);
        limitedToValuePathStr.setEnabled(false);
        limitedToValuePathStr.setLayoutData(col2);
    }

    protected GridData createCol() {
        return createCol(1);
    }
    
    protected GridData createCol(int span) {
        return createCol(span, GridData.FILL);
    }
    
    protected GridData createCol(int span, int align) {
        GridData rv = new GridData();
        rv.horizontalAlignment = align;
        rv.grabExcessHorizontalSpace = true;
        rv.horizontalSpan = span;
        return rv;
    }

    @Override
    public boolean performAction() {
    	EntrySearch.Pattern pat = new EntrySearch.Pattern(
    			queryStr.getText(),
    			isCaseSensitive.getSelection(),
    			isRegex.getSelection());
    	List<EntrySearch.Objective> objs = new ArrayList<>(10);
    	if(searchAsId.getSelection()) {
    		objs.add(EntrySearch.Objective.ID);
    	}
    	if(searchAsName.getSelection()) {
    		objs.add(EntrySearch.Objective.NAME);
    	}
    	if(searchAsPath.getSelection()) {
    		objs.add(EntrySearch.Objective.PATH);
    	}
    	if(searchAsCaption.getSelection()) {
    		objs.add(EntrySearch.Objective.CAPTION);
    	}
    	if(searchAsValue.getSelection()) {
    		objs.add(EntrySearch.Objective.VALUE);
    	}

    	String valuePath = null;
    	if(limitedToValuePath.isEnabled() && limitedToValuePath.getSelection()) {
    		valuePath = limitedToValuePathStr.getText();
    	}
    	EntrySearch.Constraint cons = new EntrySearch.Constraint(
    			limitedToLeaf.getSelection(),
    			valuePath != null ? TreePath.fromStr(valuePath) : null
    	);
        EntrySearchQuery query = new EntrySearchQuery(pat, objs, cons);

        try {
            if(query.canRunInBackground()) {
                NewSearchUI.runQueryInBackground(query);
            }
            else {
                NewSearchUI.runQueryInForeground(null, query);
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    @Override
    public void setContainer(ISearchPageContainer container) {
        // pass
    }

}
