package com.millennialmedia.pyro.internal.parser;

/**
 * Intermediate representation of a basic text cell within the Robot file's
 * tablular structure.
 * 
 * @author spaxton
 */
public class Cell {
	private int offsetInRow;
	private String parsedContents;

	public int getOffsetInRow() {
		return offsetInRow;
	}

	public void setOffsetInRow(int offsetInRow) {
		this.offsetInRow = offsetInRow;
	}

	public String getParsedContents() {
		return parsedContents;
	}

	public void setParsedContents(String parsedContents) {
		this.parsedContents = parsedContents;
	}

}
