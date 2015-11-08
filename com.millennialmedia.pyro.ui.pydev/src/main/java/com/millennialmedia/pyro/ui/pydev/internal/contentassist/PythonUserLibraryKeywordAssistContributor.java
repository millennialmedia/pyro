package com.millennialmedia.pyro.ui.pydev.internal.contentassist;

import com.google.common.collect.Multimap;
import com.millennialmedia.pyro.ui.pydev.internal.ModuleInfo;
import com.millennialmedia.pyro.ui.pydev.internal.PyDevUtil;
import com.millennialmedia.pyro.ui.pydev.internal.PyroPyDevPlugin;

/**
 * Keyword content assist contributor for user-defined Python-authored keywords.
 * 
 * @author spaxton
 */
public class PythonUserLibraryKeywordAssistContributor extends AbstractPythonLibraryKeywordAssistContributor {

	public PythonUserLibraryKeywordAssistContributor() {
		setProposalImage(PyroPyDevPlugin.getDefault().getImageRegistry().get(PyroPyDevPlugin.IMAGE_PYTHON));
	}

	@Override
	public Multimap<String, ModuleInfo> getLibraryModules() {
		return PyDevUtil.getNonBuiltInLibraryModules(getEditor());
	}

}
