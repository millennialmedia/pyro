package com.millennialmedia.pyro.ui.editor.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;

import com.millennialmedia.pyro.ui.editor.RobotFrameworkEditor;
import com.millennialmedia.pyro.ui.internal.registry.SearchPathContributorRegistryReader;

/**
 * An assortment of path-related utilities that deal with paths and Eclipse file
 * resources.
 * 
 * @author spaxton
 */
public class PathUtil {
	private static Map<RobotFrameworkEditor, List<AbstractSearchPathContributor>> searchPathContributorMap = new WeakHashMap<RobotFrameworkEditor, List<AbstractSearchPathContributor>>();

	private PathUtil() {
	}

	public static String replaceBuiltInSlashVars(String originalPath) {
		String returnPath = originalPath.replace("${/}", "/");
		return returnPath;
	}

	public static IPath getRootPath(RobotFrameworkEditor editor) {
		IEditorInput input = editor.getEditorInput();
		IFile file = null;
		if (input instanceof IFileEditorInput) {
			file = ((IFileEditorInput) input).getFile();
			return file.getFullPath().removeLastSegments(1);
		}
		return null;
	}

	public static IFile getEditorFile(RobotFrameworkEditor editor) {
		IEditorInput input = editor.getEditorInput();
		if (input instanceof IFileEditorInput) {
			return ((IFileEditorInput) input).getFile();
		}
		return null;
	}

	public static IResource getResourceForPath(RobotFrameworkEditor editor, String path) {
		IResource target = null;
		String sanitizedPath = replaceBuiltInSlashVars(path);
		if (sanitizedPath.contains("${CURDIR}")) {
			int index = sanitizedPath.lastIndexOf("${CURDIR}");
			if (index < sanitizedPath.length() - 10) {
				target = getEditorFile(editor).getParent().findMember(sanitizedPath.substring(index + 10));
			}
		} else if (!sanitizedPath.startsWith("/")) {
			// relative path
			// first look relative to the current file's location
			target = getEditorFile(editor).getParent().findMember(sanitizedPath);
			if (target == null || !target.exists()) {
				target = null;
				// look for the resource relative to any search paths in the
				// project (i.e. the source folders in the PYTHONPATH)
				for (IResource resource : getSearchPaths(editor)) {
					if (resource instanceof IContainer) {
						target = ((IContainer) resource).findMember(sanitizedPath);
						if (target != null && target.exists()) {
							break;
						}
					}
				}
			}
		} else {
			// absolute path
			target = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(sanitizedPath));
		}

		if (target != null && target.exists()) {
			return target;
		}
		return null;
	}

	private static List<IResource> getSearchPaths(RobotFrameworkEditor editor) {
		List<AbstractSearchPathContributor> contributors = searchPathContributorMap.get(editor);
		if (contributors == null) {
			Collection<AbstractSearchPathContributor> registeredContributors = SearchPathContributorRegistryReader
					.getReader().getContributors();
			contributors = new ArrayList<AbstractSearchPathContributor>();
			for (AbstractSearchPathContributor contributor : registeredContributors) {
				AbstractSearchPathContributor newContributor = (AbstractSearchPathContributor) contributor.clone();
				newContributor.setEditor(editor);
				contributors.add(newContributor);
			}
			searchPathContributorMap.put(editor, contributors);
		}

		List<IResource> searchPaths = new ArrayList<IResource>();
		for (AbstractSearchPathContributor contributor : contributors) {
			List<IResource> resources = contributor.getSearchPaths();
			if (resources != null) {
				searchPaths.addAll(resources);
			}
		}
		return searchPaths;
	}

	/**
	 * Returns an ordered list of path segments for a particular reference.
	 * 
	 * @param pathString
	 *            path to a library or resource file
	 * @return list of segment strings split by allowable separators
	 */
	public static List<String> getPathSegments(String pathString) {
		if (pathString == null || "".equals(pathString)) {
			return new ArrayList<String>();
		} else if (pathString.contains("/")) {
			return new ArrayList<String>(Arrays.asList(pathString.split("/")));
		} else if (pathString.contains(".")) {
			return new ArrayList<String>(Arrays.asList(pathString.split("\\.")));
		} else {
			return new ArrayList<String>(Arrays.asList(new String[] { pathString }));
		}
	}

	/**
	 * Returns an ordered list of path segments for a particular reference, but
	 * with substitution variables and relative-path segments stripped off the
	 * front.
	 * 
	 * @param pathString
	 *            path to a library or resource file
	 * @return list of segment strings split by allowable separators
	 */
	public static List<String> getNormalizedPathSegments(String pathString) {
		List<String> pathSegments = PathUtil.getPathSegments(pathString);
		while (pathSegments.size() > 0 && (pathSegments.get(0).contains("${") || pathSegments.get(0).equals(".."))) {
			pathSegments.remove(0);
		}
		return pathSegments;
	}

}
