package com.millennialmedia.pyro.ui.pydev.internal.contentassist;

import com.google.common.collect.Multimap;
import com.millennialmedia.pyro.ui.PyroUIPlugin;
import com.millennialmedia.pyro.ui.pydev.internal.ModuleInfo;
import com.millennialmedia.pyro.ui.pydev.internal.PyDevUtil;

/**
 * Keyword content assist contributor for built-in Robot keywords.
 * 
 * @author spaxton
 */
public class PythonStandardLibraryKeywordAssistContributor extends AbstractPythonLibraryKeywordAssistContributor {

	public PythonStandardLibraryKeywordAssistContributor() {
		setProposalImage(PyroUIPlugin.getDefault().getImageRegistry().get(PyroUIPlugin.IMAGE_ROBOT));
	}

	@Override
	public Multimap<String, ModuleInfo> getLibraryModules() {
		return PyDevUtil.getBuiltInLibraryModules(getEditor());
	}

}
