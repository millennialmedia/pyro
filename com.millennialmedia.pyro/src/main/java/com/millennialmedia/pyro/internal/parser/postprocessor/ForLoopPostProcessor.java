package com.millennialmedia.pyro.internal.parser.postprocessor;

import java.util.List;

import com.millennialmedia.pyro.model.Line;
import com.millennialmedia.pyro.model.RobotModel;
import com.millennialmedia.pyro.model.Step;
import com.millennialmedia.pyro.model.StepSegment;
import com.millennialmedia.pyro.model.StepSegment.SegmentType;
import com.millennialmedia.pyro.model.util.AbstractModelPostProcessor;

/**
 * Post-processor to mark looping constructs in the model
 * 
 * @author spaxton
 */
public class ForLoopPostProcessor extends AbstractModelPostProcessor {

	@Override
	public void postProcess(RobotModel model) {
		Line line = model.getFirstLine();
		while (line != null) {
			if (line instanceof Step) {
				// loop through all the segments in this line
				List<StepSegment> segments = ((Step) line).getSegments();
				for (int i = 0; i < segments.size(); i++) {
					if (":FOR".equalsIgnoreCase(segments.get(i).getValue())) {
						segments.get(i).setSegmentType(SegmentType.LOOP_CONSTRUCT);
						if (segments.size() > i) {
							segments.get(i + 1).setSegmentType(SegmentType.VARIABLE);
						}
						for (int index = i + 1; index < segments.size(); index++) {
							if ("IN".equalsIgnoreCase(segments.get(index).getValue())
									|| "IN RANGE".equalsIgnoreCase(segments.get(index).getValue())) {
								segments.get(index).setSegmentType(SegmentType.LOOP_CONSTRUCT);
							}
						}
					}
				}
			}
			line = line.getNextLine();
		}
	}

}
