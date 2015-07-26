package com.millennialmedia.pyro.ui.pydev.internal;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * Activator for the PyDev-specific ui contributions. Exposes an additional icon
 * image.
 * 
 * @author spaxton
 */
public class PyroPyDevPlugin extends AbstractUIPlugin {
	public static final String PLUGIN_ID = "com.millennialmedia.pyro.ui.pydev";

	public static final String IMAGE_PYTHON = "image_python";

	private static PyroPyDevPlugin plugin;

	public PyroPyDevPlugin() {
		super();
		plugin = this;
	}

	public static PyroPyDevPlugin getDefault() {
		return plugin;
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		reg.put(IMAGE_PYTHON, imageDescriptorFromPlugin(PLUGIN_ID, "icons/python.gif"));
	}

}
