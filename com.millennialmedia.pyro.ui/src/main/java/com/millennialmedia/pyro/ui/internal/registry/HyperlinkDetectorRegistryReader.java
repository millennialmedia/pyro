package com.millennialmedia.pyro.ui.internal.registry;

import java.util.Collection;
import java.util.TreeMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

import com.millennialmedia.pyro.ui.hyperlink.AbstractRobotHyperlinkDetector;

/**
 * Registry reader singleton to load hyperlink detectors that are contributed to
 * the {@link com.millennialmedia.pyro.ui.hyperlinkDetector} extension point.
 * 
 * @author spaxton
 */
public class HyperlinkDetectorRegistryReader {
	private static final String PLUGIN_ID = "com.millennialmedia.pyro.ui";
	private static final String EXT_PT_ID = "hyperlinkDetector";
	private static final String ATTR_CLASS = "class";
	private static final String ATTR_PRIORITY = "priority";

	private static HyperlinkDetectorRegistryReader reader = new HyperlinkDetectorRegistryReader();
	private Collection<AbstractRobotHyperlinkDetector> detectors;

	private HyperlinkDetectorRegistryReader() {
		readRegistry();
	}

	public static HyperlinkDetectorRegistryReader getReader() {
		return reader;
	}

	public Collection<AbstractRobotHyperlinkDetector> getDetectors() {
		return detectors;
	}

	private void readRegistry() {
		TreeMap<Integer, AbstractRobotHyperlinkDetector> priorityToDetectorMap = new TreeMap<Integer, AbstractRobotHyperlinkDetector>();

		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(PLUGIN_ID, EXT_PT_ID);
		if (point != null) {
			for (IConfigurationElement element : point.getConfigurationElements()) {
				try {
					AbstractRobotHyperlinkDetector detector = (AbstractRobotHyperlinkDetector) element
							.createExecutableExtension(ATTR_CLASS);
					int priority = Integer.parseInt(element.getAttribute(ATTR_PRIORITY));
					priorityToDetectorMap.put(priority, detector);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}
		detectors = priorityToDetectorMap.values();
	}

}
