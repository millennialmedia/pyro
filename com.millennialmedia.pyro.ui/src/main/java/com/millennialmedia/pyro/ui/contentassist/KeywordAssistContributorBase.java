package com.millennialmedia.pyro.ui.contentassist;

import org.eclipse.jface.preference.IPreferenceStore;

import com.millennialmedia.pyro.model.StepSegment.SegmentType;
import com.millennialmedia.pyro.ui.PyroUIPlugin;
import com.millennialmedia.pyro.ui.internal.preferences.IPreferenceConstants;
import com.millennialmedia.pyro.ui.internal.preferences.IPreferenceConstants.CapsMode;

/**
 * Contributor base class that adds extra functionality specifically for Robot
 * keyword name content assist proposals.
 * 
 * @author spaxton
 */
public class KeywordAssistContributorBase extends ContentAssistContributorBase {

	/**
	 * If the given offset points to a location in a keyword call, this method
	 * returns a pair of strings representing the portion before and after the
	 * caret location of that offset.
	 * 
	 * @param offset
	 *            integer offset location into document
	 * @return a 2-element string array with the keyword sections, or null if
	 *         the target offset is not a keyword
	 */
	protected String[] getCurrentKeywordFragments(int offset) {
		String[] fragments = getStringFragments(offset, SegmentType.KEYWORD_CALL);
		if (fragments == null) {
			// we're not in a segment that was parsed as a keyword, but see if
			// it's a suitable
			// position for a brand-new keyword:
			// -within a testcase or keyword definition
			// -2nd cell of table
			// -empty contents of that cell

			if (isPotentialEmptyStepSegment(offset, SegmentType.KEYWORD_CALL)) {
				fragments = new String[] { "", "" };
			}
		}
		return fragments;
	}

	protected boolean isApplicable(int offset) {
		return true;
	}

	protected String performSmartCapitalization(String keywordName, String partialKeywordPrefix) {
		boolean capitalizeFirstWord = false;
		boolean capitalizeAllWords = false;

		// capitalization is affected by a preference setting which stores the selected mode
		// if the "smart caps" mode is used, further logic is run to determine what case to use
		// for a keyword string
		IPreferenceStore store = PyroUIPlugin.getDefault().getPreferenceStore();
		IPreferenceConstants.CapsMode capsMode = CapsMode.valueOf(store.getString(IPreferenceConstants.CAPITALIZATION_KEYWORD_MODE));
		
		switch (capsMode) {
		case PRESERVE_CASE:
			// nothing to do
			return keywordName;
		case UPPERCASE:
			capitalizeAllWords = true;
			break;
		case SMART_CAPS:
			// first determine our capitalization scheme based on the current text
			// up to the caret location
			int lastDotIndex = partialKeywordPrefix.lastIndexOf(".") + 1;
			if (lastDotIndex < 0) {
				lastDotIndex = 0;
			}

			if (partialKeywordPrefix.length() > lastDotIndex) {
				String[] words = partialKeywordPrefix.substring(lastDotIndex).split(" ");
				if (words.length == 0) {
					capitalizeFirstWord = true;
				}
				if (words.length > 0 && (Character.isUpperCase(words[0].charAt(0)) || lastDotIndex > 0)) {
					capitalizeFirstWord = true;
				}
				if (words.length > 1 && Character.isUpperCase(words[1].charAt(0))) {
					capitalizeAllWords = true;
				}
			}
			break;
		}
		
		// now construct the newly-capitalized string using the full keyword name
		StringBuilder builder = new StringBuilder();
		int lastDotIndex = keywordName.lastIndexOf(".") + 1;
		if (lastDotIndex < 0) {
			lastDotIndex = 0;
		}
		String[] words = keywordName.substring(lastDotIndex).split(" ");
		builder.append(keywordName.substring(0, lastDotIndex));
		for (int i = 0; i < words.length; i++) {
			if (capitalizeAllWords || (i == 0 && capitalizeFirstWord)) {
				builder.append(Character.toUpperCase(words[i].charAt(0)));
			} else {
				builder.append(words[i].charAt(0));
			}
			builder.append(words[i].substring(1));
			builder.append(" ");
		}
		return builder.toString();
	}

}
