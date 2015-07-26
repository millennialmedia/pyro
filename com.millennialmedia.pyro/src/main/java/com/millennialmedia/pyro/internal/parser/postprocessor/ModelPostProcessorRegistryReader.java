package com.millennialmedia.pyro.internal.parser.postprocessor;

import java.util.Collection;
import java.util.TreeMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

import com.millennialmedia.pyro.model.util.AbstractModelPostProcessor;

/**
 * Registry reader singleton to load model post-processors that are contributed
 * via the {@code com.millennialmedia.pyro.modelPostProcessor} extension point.
 * 
 * @author spaxton
 */
public class ModelPostProcessorRegistryReader {
	private static final String PLUGIN_ID = "com.millennialmedia.pyro";
	private static final String EXT_PT_ID = "modelPostProcessor";
	private static final String ATTR_CLASS = "class";
	private static final String ATTR_PRIORITY = "priority";

	private static ModelPostProcessorRegistryReader reader = new ModelPostProcessorRegistryReader();
	private Collection<AbstractModelPostProcessor> postProcessors;

	private ModelPostProcessorRegistryReader() {
		readRegistry();
	}

	public static ModelPostProcessorRegistryReader getReader() {
		return reader;
	}

	public Collection<AbstractModelPostProcessor> getPostProcessors() {
		return postProcessors;
	}

	private void readRegistry() {
		// use treemap to sort by priority
		TreeMap<Integer, AbstractModelPostProcessor> priorityToProcessorMap = new TreeMap<Integer, AbstractModelPostProcessor>();

		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(PLUGIN_ID, EXT_PT_ID);
		if (point != null) {
			for (IConfigurationElement element : point.getConfigurationElements()) {
				try {
					AbstractModelPostProcessor detector = (AbstractModelPostProcessor) element
							.createExecutableExtension(ATTR_CLASS);
					int priority = Integer.parseInt(element.getAttribute(ATTR_PRIORITY));
					priorityToProcessorMap.put(priority, detector);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}
		postProcessors = priorityToProcessorMap.values();
	}

}
