package com.millennialmedia.pyro.ui;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * UI plugin activator. Its main purpose is to expose a set of common icons for
 * reuse.
 * 
 * @author spaxton
 */
public class PyroUIPlugin extends AbstractUIPlugin {
	public static final String PLUGIN_ID = "com.millennialmedia.pyro.ui";

	public static final String IMAGE_TESTCASE = "image_testcase";
	public static final String IMAGE_KEYWORD = "image_keyword";
	public static final String IMAGE_VARIABLE = "image_variable";
	public static final String IMAGE_SETTING = "image_setting";
	public static final String IMAGE_ROBOT = "image_robot";
	public static final String IMAGE_AZSORT = "image_azsort";
	public static final String IMAGE_TABLE = "image_table";
	public static final String IMAGE_SHOW_AS_TREE = "image_show_as_tree";
	public static final String IMAGE_COLLAPSE_ALL = "image_collapse_all";
	public static final String IMAGE_EXPAND_ALL = "image_expand_all";

	private static PyroUIPlugin plugin;

	public PyroUIPlugin() {
		super();
		plugin = this;
	}

	public static PyroUIPlugin getDefault() {
		return plugin;
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		reg.put(IMAGE_TESTCASE, imageDescriptorFromPlugin(PLUGIN_ID, "icons/test.gif"));
		reg.put(IMAGE_KEYWORD, imageDescriptorFromPlugin(PLUGIN_ID, "icons/imp_obj.gif"));
		reg.put(IMAGE_VARIABLE, imageDescriptorFromPlugin(PLUGIN_ID, "icons/variable_view.gif"));
		reg.put(IMAGE_SETTING, imageDescriptorFromPlugin(PLUGIN_ID, "icons/thread_view.gif"));
		reg.put(IMAGE_ROBOT, imageDescriptorFromPlugin(PLUGIN_ID, "icons/robot.gif"));
		reg.put(IMAGE_AZSORT, imageDescriptorFromPlugin(PLUGIN_ID, "icons/alphab_sort_co.gif"));
		reg.put(IMAGE_TABLE, imageDescriptorFromPlugin(PLUGIN_ID, "icons/table.gif"));
		reg.put(IMAGE_SHOW_AS_TREE, imageDescriptorFromPlugin(PLUGIN_ID, "icons/problem_category.gif"));
		reg.put(IMAGE_COLLAPSE_ALL, imageDescriptorFromPlugin(PLUGIN_ID, "icons/collapseall.gif"));
		reg.put(IMAGE_EXPAND_ALL, imageDescriptorFromPlugin(PLUGIN_ID, "icons/expandall.gif"));
	}

}
