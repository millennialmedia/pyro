package com.millennialmedia.pyro.ui.internal.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.millennialmedia.pyro.ui.PyroUIPlugin;
import com.millennialmedia.pyro.ui.internal.nls.Messages;

/**
 * Top-most Preference page in the tree.  Simply displays the Pyro version (of the .ui plugin) and
 * serves as the root for more-specific pages.
 * 
 * @author spaxton
 */
public class RootPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	@Override
	protected Control createContents(Composite parent) {
		Label label = new Label(parent, SWT.NORMAL);
		label.setText(Messages.RootPreferencePage_editor_version + getPyroVersion());
		return new Composite(parent, SWT.NONE);
	}

	@Override
	public void init(IWorkbench workbench) {
		noDefaultAndApplyButton();
	}
	
	private String getPyroVersion() {
		return PyroUIPlugin.getDefault().getBundle().getVersion().toString();
	}
	
}
