package com.insweat.hssd.editor.testers.server;

import com.insweat.hssd.editor.editors.hssd.HSSDEditor;
import com.insweat.hssd.editor.testers.AbstractPropertyTester;
import com.insweat.hssd.editor.util.Helper;

public class FileServerTester extends AbstractPropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		if("isRunning".equals(property)) {
			HSSDEditor editor = Helper.getLastHSSDEditor();
			if(editor == null) {
				return false;
			}
			Boolean actualValue = Boolean.valueOf(editor.isFileServerRunning());
			return actualValue.equals(expectedValue);
		}
		return false;
	}
	
}
