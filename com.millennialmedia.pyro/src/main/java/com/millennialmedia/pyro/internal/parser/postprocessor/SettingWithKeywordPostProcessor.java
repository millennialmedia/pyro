package com.millennialmedia.pyro.internal.parser.postprocessor;

import com.millennialmedia.pyro.model.Line;
import com.millennialmedia.pyro.model.RobotModel;
import com.millennialmedia.pyro.model.Step;
import com.millennialmedia.pyro.model.Step.StepType;
import com.millennialmedia.pyro.model.StepSegment;
import com.millennialmedia.pyro.model.StepSegment.SegmentType;
import com.millennialmedia.pyro.model.Table;
import com.millennialmedia.pyro.model.TableItemDefinition;
import com.millennialmedia.pyro.model.util.AbstractModelPostProcessor;

/**
 * Post processor to mark several values in the settings table (setup, teardown, test templates)
 * as keyword invocations.  Also handles setting lines within testcase and keyword tables.
 * 
 * @author spaxton
 */
public class SettingWithKeywordPostProcessor extends AbstractModelPostProcessor {

	@Override
	public void postProcess(RobotModel model) {
		for (Table table : model.getTables()) {
			switch (table.getTableType()) {
			case SETTING:
				for (Line line : table.getTableLines()) {
					Step step = (Step) line;
					if (step.getStepType() == StepType.SETTING) {
						boolean foundSettingWithKeywordArg = false;
						for (StepSegment seg : step.getSegments()) {
							if (seg.getSegmentType() == SegmentType.SETTING_NAME
									&& ("Suite Setup".equalsIgnoreCase(seg.getValue()) ||
										"Suite Teardown".equalsIgnoreCase(seg.getValue()) ||
										"Test Setup".equalsIgnoreCase(seg.getValue()) ||
										"Test Teardown".equalsIgnoreCase(seg.getValue()) ||
										"Test Template".equalsIgnoreCase(seg.getValue()))) {
								foundSettingWithKeywordArg = true;
								continue;
							}

							if (foundSettingWithKeywordArg && seg.getValue() != null && !"".equals(seg.getValue())) {
								seg.setSegmentType(SegmentType.KEYWORD_CALL);
							}
						}
					}
				}
				break;

			case TESTCASE:
			case KEYWORD:
				for (Line line : table.getTableLines()) {
					if (line instanceof TableItemDefinition) {
						for (Step step : ((TableItemDefinition) line).getSteps()) {
							if (step.getStepType() == StepType.SETTING) {
								boolean foundSettingWithKeywordArg = false;
								for (StepSegment seg : step.getSegments()) {
									if (seg.getSegmentType() == SegmentType.SETTING_NAME
											&& ("[Setup]".equalsIgnoreCase(seg.getValue()) ||
												"[Teardown]".equalsIgnoreCase(seg.getValue()) ||
												"[Precondition]".equalsIgnoreCase(seg.getValue()) ||
												"[Postcondition]".equalsIgnoreCase(seg.getValue()))) {
										foundSettingWithKeywordArg = true;
										continue;
									}

									if (foundSettingWithKeywordArg && seg.getValue() != null && !"".equals(seg.getValue())) {
										seg.setSegmentType(SegmentType.KEYWORD_CALL);
									}
								}
							}
						}
					}
				}
				break;

			default: 
				break;
			}
		}
	}

}
