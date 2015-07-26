package com.millennialmedia.pyro.ui.internal.syntaxcoloring;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

/**
 * Color manager utility responsible for initializing and disposing of custom
 * syntax-coloring color values.
 * 
 * @author spaxton
 */
public class ColorManager {

	private Map<RGB, Color> colorTable = new HashMap<RGB, Color>();

	public void dispose() {
		for (Color color : colorTable.values()) {
			color.dispose();
		}
	}

	public Color getColor(RGB rgb) {
		Color color = (Color) colorTable.get(rgb);
		if (color == null) {
			color = new Color(Display.getCurrent(), rgb);
			colorTable.put(rgb, color);
		}
		return color;
	}
}
