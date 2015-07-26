package com.millennialmedia.pyro.internal.parser.postprocessor;

import com.millennialmedia.pyro.model.Line;
import com.millennialmedia.pyro.model.RobotModel;
import com.millennialmedia.pyro.model.Step;
import com.millennialmedia.pyro.model.Step.StepType;
import com.millennialmedia.pyro.model.StepSegment;
import com.millennialmedia.pyro.model.StepSegment.SegmentType;
import com.millennialmedia.pyro.model.Table;
import com.millennialmedia.pyro.model.Table.TableType;
import com.millennialmedia.pyro.model.util.AbstractModelPostProcessor;
import com.millennialmedia.pyro.model.TableItemDefinition;

/**
 * Post processor to handle testcase templates.
 * 
 * @author spaxton
 */
public class TestCaseTemplatePostProcessor extends AbstractModelPostProcessor {
	private static final String TEMPLATE = "[Template]";

	@Override
	public void postProcess(RobotModel model) {
		// handle testcases that are based on templates
		for (Table table : model.getTables()) {
			if (table.getTableType() == TableType.TESTCASE) {
				for (Line testcaseLine : table.getTableLines()) {
					TableItemDefinition testcase = (TableItemDefinition) testcaseLine;
					if (testcaseHasTemplate(testcase)) {
						// this testcase is based on a template, so the member
						// steps need to be adjusted to be purely arguments
						// (i.e. not starting with a keyword call)

						for (Step step : testcase.getSteps()) {
							if (step.getStepType() == StepType.SETTING) {
								continue;
							} else {
								for (int i = 0; i < step.getSegments().size(); i++) {
									StepSegment seg = step.getSegments().get(i);
									if (seg.getSegmentType() == SegmentType.KEYWORD_CALL) {
										StepSegment newSeg = new StepSegment(seg.getCell());
										newSeg.setSegmentType(SegmentType.ARGUMENT);
										step.getSegments().set(i, newSeg);
										break;
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private boolean testcaseHasTemplate(TableItemDefinition testcase) {
		if (testcase != null) {
			for (Step step : testcase.getSteps()) {
				if (step.getStepType() == StepType.SETTING) {
					for (StepSegment segment : step.getSegments()) {
						if (segment.getSegmentType() == SegmentType.SETTING_NAME && TEMPLATE.equals(segment.getValue())) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

}
