package com.millennialmedia.pyro.ui.internal.nls;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.millennialmedia.pyro.ui.internal.nls.messages"; //$NON-NLS-1$
	public static String CapitalizationPreferencePage_preserve_case;
	public static String CapitalizationPreferencePage_select_capitalization;
	public static String CapitalizationPreferencePage_smart_caps;
	public static String CapitalizationPreferencePage_uppercase;
	public static String RootPreferencePage_editor_version;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
