package com.millennialmedia.pyro.ui.pydev.internal.hyperlink;

import com.google.common.collect.Multimap;
import com.millennialmedia.pyro.ui.pydev.internal.ModuleInfo;
import com.millennialmedia.pyro.ui.pydev.internal.PyDevUtil;

/**
 * Hyperlinking for user-defined keywords implemented in Python libraries.
 * 
 * @author spaxton
 */
public class PythonUserLibraryKeywordDefinitionHyperlinkDetector extends AbstractPythonLibraryKeywordDefinitionHyperlinkDetector {

	@Override
	protected Multimap<String, ModuleInfo> getLibraryModules() {
		return PyDevUtil.getNonBuiltInLibraryModules(getEditor());
	}

}
