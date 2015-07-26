package com.millennialmedia.pyro.model;

import java.util.ArrayList;
import java.util.List;

import com.millennialmedia.pyro.internal.parser.Cell;
import com.millennialmedia.pyro.model.StepSegment.SegmentType;

/**
 * A line in a Robot file representing either a loose setting declaration or a
 * single step in a testcase or keyword definition. Steps will contain a list of
 * {@link StepSegment}.
 * 
 * @author spaxton
 */
public class Step extends Line {
	public static enum StepType {
		STEP, SETTING
	};

	// factory method for a full-line comment - it only adds a comment segment
	// as the first cell but everything that
	// consumes the model knows to ignore the rest of the line
	public static final Step COMMENT() {
		Step step = new Step();
		StepSegment comment = new StepSegment(new Cell());
		comment.setSegmentType(SegmentType.COMMENT);
		step.getSegments().add(comment);
		return step;
	}

	private List<StepSegment> segments = new ArrayList<StepSegment>();
	private StepType stepType;

	public List<StepSegment> getSegments() {
		return segments;
	}

	public StepType getStepType() {
		return stepType;
	}

	public void setStepType(StepType stepType) {
		this.stepType = stepType;
	}

}
