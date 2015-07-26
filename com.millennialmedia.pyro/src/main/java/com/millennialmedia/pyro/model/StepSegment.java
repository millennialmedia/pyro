package com.millennialmedia.pyro.model;

import com.millennialmedia.pyro.internal.parser.Cell;

/**
 * The model representation of a single cell in a Robot table.
 * 
 * @author spaxton
 */
public class StepSegment {
	public static enum SegmentType {
		ARGUMENT, COMMENT, KEYWORD_CALL, SETTING_NAME, SETTING_VALUE, VARIABLE, LOOP_CONSTRUCT, CONTROL_ARGUMENT, CONTINUATION, UNKNOWN
	};

	private Cell cell;
	private SegmentType segmentType;

	public StepSegment(Cell cell) {
		this.cell = cell;
	}

	public int getOffsetInLine() {
		return cell.getOffsetInRow();
	}

	public String getValue() {
		return cell.getParsedContents();
	}

	public Cell getCell() {
		return cell;
	}

	public SegmentType getSegmentType() {
		return segmentType;
	}

	public void setSegmentType(SegmentType segmentType) {
		this.segmentType = segmentType;
	}
}
