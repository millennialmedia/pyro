package com.millennialmedia.pyro.ui.internal.registry;

import java.util.Collection;
import java.util.TreeMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

import com.millennialmedia.pyro.ui.contentassist.ContentAssistContributorBase;

/**
 * Registry reader singleton to load content assist processor instances
 * contributed to the {@link com.millennialmedia.pyro.ui.contentAssistProcessor}
 * extension point.
 * 
 * @author spaxton
 */
public class ContentAssistProcessorRegistryReader {
	private static final String PLUGIN_ID = "com.millennialmedia.pyro.ui";
	private static final String EXT_PT_ID = "contentAssistProcessor";
	private static final String ATTR_CLASS = "class";
	private static final String ATTR_PRIORITY = "priority";

	private static ContentAssistProcessorRegistryReader reader = new ContentAssistProcessorRegistryReader();
	private Collection<ContentAssistContributorBase> processors;

	private ContentAssistProcessorRegistryReader() {
		readRegistry();
	}

	public static ContentAssistProcessorRegistryReader getReader() {
		return reader;
	}

	public Collection<ContentAssistContributorBase> getProcessors() {
		return processors;
	}

	private void readRegistry() {
		TreeMap<Integer, ContentAssistContributorBase> priorityToProcessorMap = new TreeMap<Integer, ContentAssistContributorBase>();

		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(PLUGIN_ID, EXT_PT_ID);
		if (point != null) {
			for (IConfigurationElement element : point.getConfigurationElements()) {
				try {
					ContentAssistContributorBase processor = (ContentAssistContributorBase) element
							.createExecutableExtension(ATTR_CLASS);
					int priority = Integer.parseInt(element.getAttribute(ATTR_PRIORITY));
					priorityToProcessorMap.put(priority, processor);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}
		processors = priorityToProcessorMap.values();
	}

}
