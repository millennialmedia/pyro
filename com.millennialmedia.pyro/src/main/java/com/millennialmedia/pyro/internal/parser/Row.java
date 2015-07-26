package com.millennialmedia.pyro.internal.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Intermediate representation of a row (source line) in a Robot file.
 * 
 * @author spaxton
 */
public class Row {
	private List<Cell> cells = new ArrayList<Cell>();

	public List<Cell> getCells() {
		return cells;
	}

	public void setCells(List<Cell> cells) {
		this.cells = cells;
	}

}
