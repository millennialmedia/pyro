package com.millennialmedia.pyro.ui.internal.syntaxcoloring;

import org.eclipse.swt.graphics.RGB;

/**
 * RGB constants for Pyro syntax coloring.
 * 
 * @author spaxton
 */
public interface ColorDefaults {
	RGB COMMENT = new RGB(180, 180, 180);
	RGB TABLE = new RGB(140, 50, 50);
	RGB TESTCASE = new RGB(0, 175, 0);
	RGB KEYWORD_DEF = new RGB(50, 100, 250);
	RGB SETTING_NAME = new RGB(200, 20, 200);
	RGB SETTING_VALUE = new RGB(130, 0, 130);
	RGB KEYWORD_CALL = new RGB(0, 0, 180);
	RGB ARGUMENT = new RGB(200, 80, 20);
	RGB VARIABLE = new RGB(250, 150, 0);
	RGB CONTROL_ARGUMENT = new RGB(200, 100, 200);
	RGB LOOP_CONSTRUCT = new RGB(200, 100, 200);
	RGB CONTINUATION = new RGB(180, 80, 180);
	RGB UNKNOWN = new RGB(70, 70, 70);
}
