package com.millennialmedia.pyro.ui.pydev.internal.hyperlink;

import com.google.common.collect.Multimap;
import com.millennialmedia.pyro.ui.pydev.internal.ModuleInfo;
import com.millennialmedia.pyro.ui.pydev.internal.PyDevUtil;

/**
 * Hyperlinking for built-in Robot keywords defined in the framework Python sources.
 * 
 * @author spaxton
 */
public class PythonStandardLibraryKeywordDefinitionHyperlinkDetector extends AbstractPythonLibraryKeywordDefinitionHyperlinkDetector {

	@Override
	protected Multimap<String, ModuleInfo> getLibraryModules() {
		return PyDevUtil.getBuiltInLibraryModules(getEditor());
	}

}
