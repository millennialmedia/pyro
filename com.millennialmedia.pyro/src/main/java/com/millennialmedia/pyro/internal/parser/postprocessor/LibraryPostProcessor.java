package com.millennialmedia.pyro.internal.parser.postprocessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import com.millennialmedia.pyro.model.util.IModelConstants;

/**
 * Post-processor to walk through the library references in any Settings tables
 * of the model and build an ordered list of library paths. In addition, a map
 * of paths to alias names is built (i.e. for "WITH NAME" library declarations).
 * Both the list and map are stored as custom properties on the model so they
 * don't need to be be recomputed.
 * 
 * @author spaxton
 */
public class LibraryPostProcessor extends AbstractModelPostProcessor {

	@Override
	public void postProcess(RobotModel model) {
		Map<String, String> libraryNameToPathMap = new HashMap<String, String>();
		List<String> orderedLibraryPaths = new ArrayList<String>();

		for (Table table : model.getTables()) {
			if (table.getTableType() == TableType.SETTING) {
				for (Line line : table.getTableLines()) {
					String name = null;
					String path = null;
					Step step = (Step) line;
					if (step.getStepType() == StepType.SETTING) {
						boolean foundLibrarySetting = false;
						for (StepSegment seg : step.getSegments()) {
							if (seg.getSegmentType() == SegmentType.SETTING_NAME
									&& "Library".equalsIgnoreCase(seg.getValue())) {
								foundLibrarySetting = true;
								continue;
							}

							if (foundLibrarySetting && seg.getValue() != null && !"".equals(seg.getValue())) {
								if ("WITH NAME".equalsIgnoreCase(seg.getValue())) {
									seg.setSegmentType(SegmentType.CONTROL_ARGUMENT);
									name = null;
								} else if (path == null) {
									path = seg.getValue();
									name = seg.getValue();
								} else if (name == null) {
									name = seg.getValue();
									break;
								}
							}
						}
						if (name != null && path != null) {
							libraryNameToPathMap.put(name, path);
							orderedLibraryPaths.add(path);
						}
					}
				}
			}
		}
		model.getCustomProperties().put(IModelConstants.PROPSKEY_LIBRARY_NAME_TO_PATH_MAP, libraryNameToPathMap);
		model.getCustomProperties().put(IModelConstants.PROPSKEY_ORDERED_LIBRARY_PATHS, orderedLibraryPaths);
	}

}
