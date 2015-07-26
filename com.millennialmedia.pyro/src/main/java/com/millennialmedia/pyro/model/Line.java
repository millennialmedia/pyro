package com.millennialmedia.pyro.model;

/**
 * Base class representing a single line in the source Robot file.
 * 
 * @author spaxton
 */
public class Line {
	public static final Line EMPTY() {
		return new Line();
	}

	public static final Line UNKNOWN() {
		return new Line();
	}

	private Line nextLine;
	private int lineOffset;
	private int lineLength;

	public Line getNextLine() {
		return nextLine;
	}

	public void setNextLine(Line nextLine) {
		this.nextLine = nextLine;
	}

	public int getLineOffset() {
		return lineOffset;
	}

	public void setLineOffset(int lineOffset) {
		this.lineOffset = lineOffset;
	}

	public int getLineLength() {
		return lineLength;
	}

	public void setLineLength(int lineLength) {
		this.lineLength = lineLength;
	}

}
