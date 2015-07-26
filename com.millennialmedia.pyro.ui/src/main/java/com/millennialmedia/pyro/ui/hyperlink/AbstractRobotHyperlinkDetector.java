package com.millennialmedia.pyro.ui.hyperlink;

import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;

import com.millennialmedia.pyro.ui.editor.AbstractEditorAwareContributor;

/**
 * Base class for hyperlink detectors contributed via the
 * {@code com.millennialmedia.pyro.ui.hyperlinkDetector} extension point.
 * 
 * @author spaxton
 */
public abstract class AbstractRobotHyperlinkDetector extends AbstractEditorAwareContributor implements
		IHyperlinkDetector {

}
