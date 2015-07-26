package com.millennialmedia.pyro.internal.parser.postprocessor;

import java.util.List;

import com.millennialmedia.pyro.model.Line;
import com.millennialmedia.pyro.model.RobotModel;
import com.millennialmedia.pyro.model.Step;
import com.millennialmedia.pyro.model.StepSegment;
import com.millennialmedia.pyro.model.StepSegment.SegmentType;
import com.millennialmedia.pyro.model.util.AbstractModelPostProcessor;
import com.millennialmedia.pyro.model.util.ModelUtil;

/**
 * Post-processor to identify keywords that take other keyword names as
 * arguments and later invoke them. These keyword references need to be properly
 * identified in the model so hyperlinking will work properly.
 * 
 * @author spaxton
 */
public class BuiltInKeywordArgumentsPostProcessor extends AbstractModelPostProcessor {

	@Override
	public void postProcess(RobotModel model) {
		// walk the model looking for keyword calls
		Line line = model.getFirstLine();
		while (line != null) {
			if (line instanceof Step) {
				// loop through all the segments in this line running the logic
				// to alter arguments into keyword calls.
				// some keyword-invoking keywords may be nested so we need to
				// continue through all the segments
				List<StepSegment> segments = ModelUtil.collectStepSegments((Step) line);
				for (int i = 0; i < segments.size(); i++) {
					if (segments.get(i).getSegmentType() == SegmentType.KEYWORD_CALL) {
						String lcStrippedKeywordName = ModelUtil.stripBDDPrefixes(segments.get(i).getValue())
								.toLowerCase();
						// find built-in keywords that take a keyword name as an
						// argument

						// in the 1st cell after the current keyword
						if ("keyword should exist".equals(lcStrippedKeywordName)
								|| "run keyword".equals(lcStrippedKeywordName)
								|| "run keyword and continue on failure".equals(lcStrippedKeywordName)
								|| "run keyword and ignore error".equals(lcStrippedKeywordName)
								|| "run keyword and return".equals(lcStrippedKeywordName)
								|| "run keyword and return status".equals(lcStrippedKeywordName)
								|| "run keyword if all critical tests passed".equals(lcStrippedKeywordName)
								|| "run keyword if all tests passed".equals(lcStrippedKeywordName)
								|| "run keyword if any critical tests failed".equals(lcStrippedKeywordName)
								|| "run keyword if any tests failed".equals(lcStrippedKeywordName)
								|| "run keyword if test failed".equals(lcStrippedKeywordName)
								|| "run keyword if test passed".equals(lcStrippedKeywordName)
								|| "run keyword if timeout occurred".equals(lcStrippedKeywordName)) {
							if (segments.size() > i + 1) {
								segments.get(i + 1).setSegmentType(SegmentType.KEYWORD_CALL);
							}
							// in the 2nd cell after the current keyword
						} else if ("repeat keyword".equals(lcStrippedKeywordName)
								|| "run keyword and expect error".equals(lcStrippedKeywordName)
								|| "run keyword and return if".equals(lcStrippedKeywordName)
								|| "run keyword if".equals(lcStrippedKeywordName)
								|| "run keyword unless".equals(lcStrippedKeywordName)) {
							if (segments.size() > i + 2) {
								segments.get(i + 2).setSegmentType(SegmentType.KEYWORD_CALL);
							}
							// in the 3rd cell after the current keyword
						} else if ("wait until keyword succeeds".equals(lcStrippedKeywordName)) {
							if (segments.size() > i + 3) {
								segments.get(i + 3).setSegmentType(SegmentType.KEYWORD_CALL);
							}
							// the Run Keywords builtin function is a special
							// case
							// it can invoke a variable number of keywords, some
							// of which may take arguments
						} else if ("run keywords".equals(lcStrippedKeywordName)) {
							// first look for any "AND" control argument in the
							// subsequent steps
							boolean hasAnd = false;
							if (i < segments.size()) {
								for (int index = i + 1; index < segments.size(); index++) {
									if ("AND".equalsIgnoreCase(segments.get(index).getValue())) {
										hasAnd = true;
										break;
									}
								}
							}

							if (hasAnd) {
								// if AND is present we use it to break keywords
								boolean nextSegmentIsKeyword = true;
								for (int index = i + 1; index < segments.size(); index++) {
									if (segments.get(index).getSegmentType() == SegmentType.COMMENT) {
										break;
									} else if (nextSegmentIsKeyword) {
										segments.get(index).setSegmentType(SegmentType.KEYWORD_CALL);
										nextSegmentIsKeyword = false;
									} else if ("AND".equalsIgnoreCase(segments.get(index).getValue())) {
										segments.get(index).setSegmentType(SegmentType.CONTROL_ARGUMENT);
										nextSegmentIsKeyword = true;
									} else {
										segments.get(index).setSegmentType(SegmentType.ARGUMENT);
									}
								}
							} else {
								// otherwise every argument after this segment
								// is to be treated as a keyword
								for (int index = i + 1; index < segments.size(); index++) {
									if (segments.get(index).getSegmentType() == SegmentType.COMMENT) {
										break;
									}
									segments.get(index).setSegmentType(SegmentType.KEYWORD_CALL);
								}
							}
						}
					}
				}
			}
			line = line.getNextLine();
		}
	}

}
