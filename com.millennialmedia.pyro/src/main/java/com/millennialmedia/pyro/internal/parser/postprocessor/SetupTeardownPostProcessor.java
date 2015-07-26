package com.millennialmedia.pyro.internal.parser.postprocessor;

import java.util.HashMap;
import java.util.Map;

import com.millennialmedia.pyro.model.Line;
import com.millennialmedia.pyro.model.RobotModel;
import com.millennialmedia.pyro.model.Step;
import com.millennialmedia.pyro.model.Step.StepType;
import com.millennialmedia.pyro.model.StepSegment;
import com.millennialmedia.pyro.model.StepSegment.SegmentType;
import com.millennialmedia.pyro.model.Table;
import com.millennialmedia.pyro.model.Table.TableType;
import com.millennialmedia.pyro.model.util.AbstractModelPostProcessor;

/**
 * Post processor to mark the target of setup and teardown settings as keyword
 * invocations.
 * 
 * @author spaxton
 */
public class SetupTeardownPostProcessor extends AbstractModelPostProcessor {

	@Override
	public void postProcess(RobotModel model) {
		Map<String, String> libraryNameToPathMap = new HashMap<String, String>();
		for (Table table : model.getTables()) {
			if (table.getTableType() == TableType.SETTING) {
				for (Line line : table.getTableLines()) {
					String name = null;
					String path = null;
					Step step = (Step) line;
					if (step.getStepType() == StepType.SETTING) {
						boolean foundSetupTeardownSetting = false;
						for (StepSegment seg : step.getSegments()) {
							if (seg.getSegmentType() == SegmentType.SETTING_NAME
									&& ("Suite Setup".equalsIgnoreCase(seg.getValue())
											|| "Suite Teardown".equalsIgnoreCase(seg.getValue())
											|| "Test Setup".equalsIgnoreCase(seg.getValue()) || "Test Teardown"
												.equalsIgnoreCase(seg.getValue()))) {
								foundSetupTeardownSetting = true;
								continue;
							}

							if (foundSetupTeardownSetting && seg.getValue() != null && !"".equals(seg.getValue())) {
								seg.setSegmentType(SegmentType.KEYWORD_CALL);
							}
						}
						libraryNameToPathMap.put(name, path);
					}
				}
			}
		}
	}

}
