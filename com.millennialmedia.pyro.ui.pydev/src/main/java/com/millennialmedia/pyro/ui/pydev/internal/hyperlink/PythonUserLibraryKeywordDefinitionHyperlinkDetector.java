package com.millennialmedia.pyro.ui.pydev.internal.hyperlink;

import com.millennialmedia.pyro.ui.pydev.internal.LibraryInfo;
import com.millennialmedia.pyro.ui.pydev.internal.PyDevUtil;

/**
 * Hyperlinking for user-defined keywords implemented in Python libraries.
 * 
 * @author spaxton
 */
public class PythonUserLibraryKeywordDefinitionHyperlinkDetector extends AbstractPythonLibraryKeywordDefinitionHyperlinkDetector {

	@Override
	protected LibraryInfo getLibraryInfo() {
		return PyDevUtil.getNonBuiltInLibraryModules(getEditor());
	}

}
