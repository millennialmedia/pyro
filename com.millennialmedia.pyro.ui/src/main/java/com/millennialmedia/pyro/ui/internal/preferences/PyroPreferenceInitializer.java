package com.millennialmedia.pyro.ui.internal.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.millennialmedia.pyro.ui.PyroUIPlugin;

/**
 * Initializer for Pyro Preference default values.
 * 
 * @author spaxton
 */
public class PyroPreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = PyroUIPlugin.getDefault().getPreferenceStore();

		// call each page to initialize their default preference set
		CapitalizationPreferencePage.initializeDefaults(store);
	}

}
