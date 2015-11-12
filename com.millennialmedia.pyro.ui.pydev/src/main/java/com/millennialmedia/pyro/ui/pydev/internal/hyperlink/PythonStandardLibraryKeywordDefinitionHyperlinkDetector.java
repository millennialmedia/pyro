package com.millennialmedia.pyro.ui.pydev.internal.hyperlink;

import com.millennialmedia.pyro.ui.pydev.internal.LibraryInfo;
import com.millennialmedia.pyro.ui.pydev.internal.PyDevUtil;

/**
 * Hyperlinking for built-in Robot keywords defined in the framework Python sources.
 * 
 * @author spaxton
 */
public class PythonStandardLibraryKeywordDefinitionHyperlinkDetector extends AbstractPythonLibraryKeywordDefinitionHyperlinkDetector {

	@Override
	protected LibraryInfo getLibraryInfo() {
		return PyDevUtil.getBuiltInLibraryModules(getEditor());
	}

}
