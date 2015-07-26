package com.millennialmedia.pyro.ui.editor;

/**
 * Base class for any client contributions that will require access to the
 * current editor instance.
 * 
 * @author spaxton
 */
public class AbstractEditorAwareContributor implements Cloneable {
	private RobotFrameworkEditor editor;

	public final RobotFrameworkEditor getEditor() {
		return editor;
	}

	public final void setEditor(RobotFrameworkEditor editor) {
		this.editor = editor;
	};

	@Override
	public final AbstractEditorAwareContributor clone() {
		try {
			return (AbstractEditorAwareContributor) super.clone();
		} catch (CloneNotSupportedException e) {
		}
		return null;
	}

}
