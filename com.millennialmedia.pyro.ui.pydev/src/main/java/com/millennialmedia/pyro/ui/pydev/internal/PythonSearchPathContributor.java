package com.millennialmedia.pyro.ui.pydev.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.plugin.nature.PythonNature;

import com.millennialmedia.pyro.ui.editor.util.AbstractSearchPathContributor;

/**
 * The contributed implementation of Pyro's search path contributor which passes
 * the PYTHONPATH folders back up to the main editor code.
 * 
 * @author spaxton
 */
public class PythonSearchPathContributor extends AbstractSearchPathContributor {

	@Override
	public List<IResource> getSearchPaths() {
		IProject project = getFile().getProject();
		PythonNature nature = PythonNature.getPythonNature(project);
		if (nature != null) {
			IPythonPathNature pathNature = nature.getPythonPathNature();
			if (pathNature != null) {
				try {
					return new ArrayList<IResource>(pathNature.getProjectSourcePathFolderSet());
				} catch (CoreException e) {
				}
			}
		}
		return null;
	}

}
