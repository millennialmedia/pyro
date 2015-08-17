package com.millennialmedia.pyro.ui.internal.preferences;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.millennialmedia.pyro.ui.PyroUIPlugin;
import com.millennialmedia.pyro.ui.internal.nls.Messages;
import com.millennialmedia.pyro.ui.internal.preferences.IPreferenceConstants.CapsMode;

/**
 * Preference page for Pyro's capitalization mode.
 * 
 * @author spaxton
 */
public class CapitalizationPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	private Map<IPreferenceConstants.CapsMode, Button> buttonModeMap = new HashMap<IPreferenceConstants.CapsMode, Button>();
	
	@Override
	protected Control createContents(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.CapitalizationPreferencePage_select_capitalization);
		for (CapsMode mode : IPreferenceConstants.CapsMode.values()) {
			Button button = new Button(parent, SWT.RADIO);
			button.setText(getUserStringForMode(mode));
			buttonModeMap.put(mode, button);
		}
		
		setButtonState();
		return new Composite(parent, SWT.NONE);
	}
	
	private String getUserStringForMode(CapsMode mode) {
		switch (mode) {
		case SMART_CAPS:
			return Messages.CapitalizationPreferencePage_smart_caps;
		case UPPERCASE:
			return Messages.CapitalizationPreferencePage_uppercase;
		case PRESERVE_CASE:
			return Messages.CapitalizationPreferencePage_preserve_case;
		}
		return ""; //$NON-NLS-1$
	}

	private void setButtonState() {
		CapsMode selectedMode = CapsMode.valueOf(getPreferenceStore().getString(IPreferenceConstants.CAPITALIZATION_KEYWORD_MODE));
		for (CapsMode mode : IPreferenceConstants.CapsMode.values()) {
			buttonModeMap.get(mode).setSelection(mode == selectedMode);
		}
	}
	
	@Override
	public boolean performOk() {
		for (CapsMode mode : IPreferenceConstants.CapsMode.values()) {
			if (buttonModeMap.get(mode).getSelection()) {
				getPreferenceStore().setValue(IPreferenceConstants.CAPITALIZATION_KEYWORD_MODE, mode.name());
			}
		}
		return true;
	}
	
	@Override
	protected void performDefaults() {
		super.performDefaults();
		getPreferenceStore().setToDefault(IPreferenceConstants.CAPITALIZATION_KEYWORD_MODE);
		setButtonState();
	}

	static void initializeDefaults(IPreferenceStore store) {
		store.setDefault(IPreferenceConstants.CAPITALIZATION_KEYWORD_MODE, IPreferenceConstants.CapsMode.SMART_CAPS.name());
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return PyroUIPlugin.getDefault().getPreferenceStore();
	}
	
}
