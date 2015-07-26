package com.millennialmedia.pyro.ui.contentassist;

import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension6;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

/**
 * Pyro's specific implementation of {@link ICompletionProposal} for all
 * content-assist usages.
 * 
 * @author spaxton
 */
public class RobotCompletionProposal implements ICompletionProposal, ICompletionProposalExtension6 {
	private String replacementString;
	private int replacementOffset;
	private int replacementLength;
	private int cursorPosition;
	private Image image;
	private String displayString;
	private IContextInformation contextInformation;
	private String additionalProposalInfo;
	private StyledString styledDisplayString;

	public static final Styler FOREGROUND_STYLER = StyledString.createColorRegistryStyler(
			JFacePreferences.CONTENT_ASSIST_FOREGROUND_COLOR, null);
	public static final Styler QUALIFIER_STYLER = StyledString.createColorRegistryStyler(
			JFacePreferences.QUALIFIER_COLOR, null);

	public RobotCompletionProposal(String replacementString, int replacementOffset, int replacementLength,
			int cursorPosition, Image image, String displayString, IContextInformation contextInformation,
			String additionalProposalInfo, StyledString styledDisplayString) {
		this.replacementString = replacementString;
		this.replacementOffset = replacementOffset;
		this.replacementLength = replacementLength;
		this.cursorPosition = cursorPosition;
		this.image = image;
		this.displayString = displayString;
		this.contextInformation = contextInformation;
		this.additionalProposalInfo = additionalProposalInfo;
		this.styledDisplayString = styledDisplayString;
	}

	@Override
	public void apply(IDocument document) {
		if (document.get().substring(replacementOffset, replacementOffset + replacementLength)
				.equalsIgnoreCase(replacementString)) {
			// if the new text matches the existing text (case-insensitive),
			// stop without modifying the document
			return;
		}

		try {
			document.replace(replacementOffset, replacementLength, replacementString);
		} catch (BadLocationException x) {
		}
	}

	@Override
	public Point getSelection(IDocument document) {
		return new Point(replacementOffset + cursorPosition, 0);
	}

	@Override
	public String getAdditionalProposalInfo() {
		return additionalProposalInfo;
	}

	@Override
	public String getDisplayString() {
		return displayString;
	}

	@Override
	public Image getImage() {
		return image;
	}

	@Override
	public IContextInformation getContextInformation() {
		return contextInformation;
	}

	@Override
	public StyledString getStyledDisplayString() {
		return styledDisplayString;
	}

	public String getReplacementString() {
		return replacementString;
	}

}
