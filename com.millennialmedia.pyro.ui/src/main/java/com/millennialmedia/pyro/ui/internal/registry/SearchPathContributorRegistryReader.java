package com.millennialmedia.pyro.ui.internal.registry;

import java.util.Collection;
import java.util.TreeMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

import com.millennialmedia.pyro.ui.editor.util.AbstractSearchPathContributor;

/**
 * Registry reader singleton to load search path contributors via the
 * {@link com.millennialmedia.pyro.ui.searchPathContributor} extension point.
 * 
 * @author spaxton
 */
public class SearchPathContributorRegistryReader {
	private static final String PLUGIN_ID = "com.millennialmedia.pyro.ui";
	private static final String EXT_PT_ID = "searchPathContributor";
	private static final String ATTR_CLASS = "class";
	private static final String ATTR_PRIORITY = "priority";

	private static SearchPathContributorRegistryReader reader = new SearchPathContributorRegistryReader();
	private Collection<AbstractSearchPathContributor> contributors;

	/**
	 * Some files need to be searched for relative to a library path
	 * (pythonpath, in the case of pybot-launched tests) but that path
	 * configuration isn't necessarily available to this plugin. Read from an
	 * extension point to let the pydev integration plugin to pass this path
	 * back down to the generic editor features. This approach ensures that if
	 * pydev isn't installed the editor will still function, just without some
	 * file resolution capability.
	 */
	private SearchPathContributorRegistryReader() {
		readRegistry();
	}

	public static SearchPathContributorRegistryReader getReader() {
		return reader;
	}

	public Collection<AbstractSearchPathContributor> getContributors() {
		return contributors;
	}

	private void readRegistry() {
		TreeMap<Integer, AbstractSearchPathContributor> priorityToDetectorMap = new TreeMap<Integer, AbstractSearchPathContributor>();

		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(PLUGIN_ID, EXT_PT_ID);
		if (point != null) {
			for (IConfigurationElement element : point.getConfigurationElements()) {
				try {
					AbstractSearchPathContributor contributor = (AbstractSearchPathContributor) element
							.createExecutableExtension(ATTR_CLASS);
					int priority = Integer.parseInt(element.getAttribute(ATTR_PRIORITY));
					priorityToDetectorMap.put(priority, contributor);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}
		contributors = priorityToDetectorMap.values();
	}

}
