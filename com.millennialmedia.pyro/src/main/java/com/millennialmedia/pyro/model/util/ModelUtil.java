package com.millennialmedia.pyro.model.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.millennialmedia.pyro.model.Line;
import com.millennialmedia.pyro.model.RobotModel;
import com.millennialmedia.pyro.model.Step;
import com.millennialmedia.pyro.model.Step.StepType;
import com.millennialmedia.pyro.model.StepSegment;
import com.millennialmedia.pyro.model.StepSegment.SegmentType;
import com.millennialmedia.pyro.model.Table;
import com.millennialmedia.pyro.model.Table.TableType;
import com.millennialmedia.pyro.model.TableItemDefinition;
import com.millennialmedia.pyro.model.TableItemDefinition.TableItemType;

/**
 * Common utility methods for working with Robot models/values.
 * 
 * @author spaxton
 */
public class ModelUtil {

	@SuppressWarnings("unchecked")
	public static Map<String, TableItemDefinition> getKeywords(RobotModel model) {
		Map<String, TableItemDefinition> keywordsMap = new HashMap<String, TableItemDefinition>();
		if (model != null) {
			if (model.getCustomProperties().containsKey(IModelConstants.PROPSKEY_KEYWORD_NAME_TO_DEFINITION_MAP)) {
				return (Map<String, TableItemDefinition>) model.getCustomProperties().get(
						IModelConstants.PROPSKEY_KEYWORD_NAME_TO_DEFINITION_MAP);
			}

			for (Table table : model.getTables()) {
				if (table.getTableType() == TableType.KEYWORD) {
					for (Line keywordLine : table.getTableLines()) {
						TableItemDefinition keyword = (TableItemDefinition) keywordLine;
						keywordsMap.put(keyword.getName(), keyword);
					}
				}
			}
			model.getCustomProperties().put(IModelConstants.PROPSKEY_KEYWORD_NAME_TO_DEFINITION_MAP, keywordsMap);
		}
		return keywordsMap;
	}

	public static List<String> getResourceFilePaths(RobotModel model) {
		return getSettingFilePaths(model, "Resource", IModelConstants.PROPSKEY_RESOURCE_FILE_PATHS);
	}

	public static List<String> getVariableFilePaths(RobotModel model) {
		return getSettingFilePaths(model, "Variables", IModelConstants.PROPSKEY_VARIABLE_FILE_PATHS);
	}
	
	@SuppressWarnings("unchecked")
	private static List<String> getSettingFilePaths(RobotModel model, String settingName, String cachePropertyName) {
		if (model.getCustomProperties().containsKey(cachePropertyName)) {
			return (List<String>) model.getCustomProperties().get(cachePropertyName);
		}

		List<String> paths = new ArrayList<String>();
		for (Table table : model.getTables()) {
			if (table.getTableType() == TableType.SETTING) {
				for (Line line : table.getTableLines()) {
					Step step = (Step) line;
					if (step.getStepType() == StepType.SETTING) {
						boolean foundDesiredSetting = false;
						for (StepSegment seg : step.getSegments()) {
							if (seg.getSegmentType() == SegmentType.SETTING_NAME && settingName.equals(seg.getValue())) {
								foundDesiredSetting = true;
								continue;
							}

							if (foundDesiredSetting && seg.getValue() != null && !"".equals(seg.getValue())) {
								paths.add(seg.getValue());
								break;
							}
						}
					}
				}
			}
		}
		model.getCustomProperties().put(cachePropertyName, paths);
		return paths;
	}

	@SuppressWarnings("unchecked")
	public static List<String> getLibraries(RobotModel model) {
		// TODO handle Import Library keyword - see robot user guide section 2.4
		// TODO handle Set Library Search Order - robot user guide section 2.8.1
		List<String> paths = new ArrayList<String>();
		paths.addAll((List<String>) model.getCustomProperties().get(IModelConstants.PROPSKEY_ORDERED_LIBRARY_PATHS));
		return paths;
	}

	@SuppressWarnings("unchecked")
	public static String getLibraryAlias(RobotModel model, String libraryPath) {
		Map<String, String> libraryNameToPathMap = ((Map<String, String>) model.getCustomProperties().get(
				IModelConstants.PROPSKEY_LIBRARY_NAME_TO_PATH_MAP));
		for (Entry<String, String> entry : libraryNameToPathMap.entrySet()) {
			if (entry.getValue().equals(libraryPath)) {
				return entry.getKey();
			}
		}
		return libraryPath;
	}

	/**
	 * Creates a map of possible keyword name strings and relative offsets into
	 * the given original keyword name. This logic handles cases where BDD-style
	 * prefixes are at the beginning of a line and the keyword search algorithm
	 * needs to consider matching both with and without these prefixes. The
	 * relative offsets can be added to the document offset to determine the
	 * link location for purposes of underlining.
	 */
	public static Map<String, Integer> getCandidateKeywordStrings(String keywordName) {
		Map<String, Integer> candidates = new HashMap<String, Integer>();
		candidates.put(keywordName, 0);
		String targetName = keywordName;

		String[] words = targetName.split(" ");
		while (words.length > 0 && IModelConstants.BDD_PREFIXES.contains(words[0].toLowerCase())) {
			targetName = targetName.substring(words[0].length()).trim();
			candidates.put(targetName, keywordName.indexOf(targetName));
			words = targetName.split(" ");
		}
		return candidates;
	}

	public static String stripBDDPrefixes(String keywordName) {
		String[] words = keywordName.split(" ");
		while (words.length > 0 && IModelConstants.BDD_PREFIXES.contains(words[0].toLowerCase())) {
			keywordName = keywordName.substring(words[0].length()).trim();
			words = keywordName.split(" ");
		}
		return keywordName;
	}

	public static String[] getKeywordSegments(String keywordName) {
		return keywordName.split(".");
	}

	/**
	 * collect all the segments that are logically part of the current line.
	 * this handles the continuation ("...") string that splits parameter lists
	 * across physical lines of source
	 */
	public static List<StepSegment> collectStepSegments(Step step) {
		List<StepSegment> segments = new ArrayList<StepSegment>(step.getSegments());
		Line nextLine = step.getNextLine();
		boolean quit = false;
		while (!quit && nextLine != null && nextLine instanceof Step) {
			for (StepSegment segment : ((Step) nextLine).getSegments()) {
				if (segment.getValue() == null || segment.getValue().isEmpty()) {
					continue;
				} else if (segment.getSegmentType() == SegmentType.CONTINUATION) {
					for (StepSegment segmentToCopy : ((Step) nextLine).getSegments()) {
						if (segmentToCopy.getSegmentType() != SegmentType.CONTINUATION
								&& segmentToCopy.getSegmentType() != SegmentType.COMMENT) {
							segments.add(segmentToCopy);
						}
						if (segmentToCopy.getSegmentType() == SegmentType.COMMENT) {
							// skip the rest of the line
							break;
						}
					}
					break;
				} else {
					quit = true;
				}
			}
			nextLine = nextLine.getNextLine();
		}
		return segments;
	}

	public static Table getContainingTable(RobotModel model, int offset) {
		Line line = model.getFirstLine();
		Table lastTable = null;
		while (line != null && line.getLineOffset() <= offset) {
			if (line instanceof Table) {
				lastTable = (Table) line;
			}
			line = line.getNextLine();
		}
		return lastTable;
	}

	public static TableItemDefinition getContainingTableItem(RobotModel model, int offset,
			TableItemDefinition.TableItemType tableItemType) {
		Line line = model.getFirstLine();
		Table lastTable = null;
		TableItemDefinition lastTableItem = null;
		while (line != null && line.getLineOffset() <= offset) {
			if (line instanceof Table) {
				lastTable = (Table) line;
			} else if (line instanceof TableItemDefinition) {
				if ((TableItemType.KEYWORD == tableItemType && lastTable.getTableType() == TableType.KEYWORD)
						|| (TableItemType.TESTCASE == tableItemType && lastTable.getTableType() == TableType.TESTCASE)) {
					lastTableItem = (TableItemDefinition) line;
				} else {
					lastTableItem = null;
				}
			}
			line = line.getNextLine();
		}
		return lastTableItem;
	}

	/**
	 * Computes a map of testcases and keywords that call the given keyword name
	 * in the given model, with the map's values capturing all steps that makes
	 * the keyword call. This information can be used to understand the context
	 * in which a keyword call is made (ex. defined test variables, etc.)
	 */
	public static Map<TableItemDefinition, List<Step>> getLocalKeywordCallersMap(RobotModel model, String keywordName) {
		String targetKeywordPattern = normalizeKeywordName(keywordName, true);
		Map<TableItemDefinition, List<Step>> callersMap = new HashMap<TableItemDefinition, List<Step>>();
		for (Table table : model.getTables()) {
			if (table.getTableType() == TableType.TESTCASE || table.getTableType() == TableType.KEYWORD) {
				for (Line line : table.getTableLines()) {
					if (line instanceof TableItemDefinition) {
						TableItemDefinition tableItem = (TableItemDefinition) line;
						for (Step step : tableItem.getSteps()) {
							for (StepSegment segment : step.getSegments()) {
								if (segment.getSegmentType() == SegmentType.KEYWORD_CALL) {
									Set<String> possibleKeywordNames = getCandidateKeywordStrings(segment.getValue())
											.keySet();
									for (String possibleKeywordName : possibleKeywordNames)
										if (normalizeKeywordName(possibleKeywordName, false).matches(
												targetKeywordPattern)) {
											if (callersMap.containsKey(tableItem)) {
												callersMap.get(tableItem).add(step);
											} else {
												List<Step> steps = new ArrayList<Step>();
												steps.add(step);
												callersMap.put(tableItem, steps);
											}
										}
								}
							}
						}
					}
				}
			}
		}
		return callersMap;
	}

	/**
	 * Per the robot framework user guide, keyword name matching is
	 * case-insensitive with spaces and underscores ignored. All comparisons
	 * should pass through this utility for consistency. The boolean parameter
	 * is used to replace a target keyword's embedded variables (inline ${foo}
	 * patterns) with a regex string to allow .matches() to work.
	 */
	public static String normalizeKeywordName(String keywordName, boolean replaceVarsWithRegex) {
		String name = keywordName.toLowerCase().replace(" ", "").replace("_", "");
		return replaceVarsWithRegex ? name.replaceAll("\\$\\{[^\\{]*\\}", "\\.*?") : name;
	}
}
