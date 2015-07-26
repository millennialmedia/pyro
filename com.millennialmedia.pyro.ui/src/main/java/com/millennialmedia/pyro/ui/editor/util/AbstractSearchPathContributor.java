package com.millennialmedia.pyro.ui.editor.util;

import java.util.List;

import org.eclipse.core.resources.IResource;

import com.millennialmedia.pyro.ui.editor.AbstractEditorAwareContributor;

/**
 * Base class for contributions to the
 * {@code com.millennialmedia.pyro.ui.searchPathContributor} extension point.
 * 
 * @author spaxton
 */
public abstract class AbstractSearchPathContributor extends AbstractEditorAwareContributor {

	/**
	 * Returns a list of resources within the workspace representing folders to
	 * be searched for Robot assets. An example is any extra folders on the
	 * PYTHONPATH which are implicitly available to a running test suite during
	 * test execution.
	 */
	public abstract List<IResource> getSearchPaths();

}
