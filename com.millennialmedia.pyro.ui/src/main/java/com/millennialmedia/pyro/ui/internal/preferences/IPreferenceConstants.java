package com.millennialmedia.pyro.ui.internal.preferences;

/**
 * Constants for the preference store key names and values for editor behaviors.
 * 
 * @author spaxton
 */
public interface IPreferenceConstants {

	/*
	 * The capitalization mode used for keyword-name content assist proposals.  Users may use a 
	 * standard capitalization style that differs from the source of some Robot keyword libraries.
	 * This preference controls the editor behavior in conforming to one of those styles.
	 */
	String CAPITALIZATION_KEYWORD_MODE = "caps.keyword.mode";
	
	/*
	 * Enum of modes for capitalization for keyword-name content assist.
	 */
	enum CapsMode {
		SMART_CAPS, UPPERCASE, PRESERVE_CASE 
	};
	
	
}
