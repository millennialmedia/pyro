package com.millennialmedia.pyro.ui.internal.contentassist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.ITextViewer;

import com.millennialmedia.pyro.model.Line;
import com.millennialmedia.pyro.model.ModelManager;
import com.millennialmedia.pyro.model.RobotModel;
import com.millennialmedia.pyro.model.Step;
import com.millennialmedia.pyro.model.Step.StepType;
import com.millennialmedia.pyro.model.StepSegment;
import com.millennialmedia.pyro.model.StepSegment.SegmentType;
import com.millennialmedia.pyro.model.Table;
import com.millennialmedia.pyro.model.Table.TableType;
import com.millennialmedia.pyro.model.TableItemDefinition;
import com.millennialmedia.pyro.model.TableItemDefinition.TableItemType;
import com.millennialmedia.pyro.model.util.IModelConstants;
import com.millennialmedia.pyro.model.util.ModelUtil;
import com.millennialmedia.pyro.ui.PyroUIPlugin;
import com.millennialmedia.pyro.ui.contentassist.ContentAssistContributorBase;
import com.millennialmedia.pyro.ui.contentassist.RobotCompletionProposal;
import com.millennialmedia.pyro.ui.editor.util.PathUtil;

/**
 * A single contributor to handle assist for variables inserted into argument
 * cells or added as the left-hand side of a variable assignment. Note that 
 * unlike other contributions, variable assist is implemented in a single larger 
 * class because all of the sources for candidate variables are found within the 
 * same local file.
 * 
 * If Pyro was ever to support external variable files, that might mean
 * splitting this class up to do local variables first, then another contributor
 * of lower priority to handle the variable file(s), and then a third to propose
 * the built-in Robot vars (thus removing them from the end of this class's
 * proposals).
 * 
 * @author spaxton
 */
public class VariableNameAssistContributor extends ContentAssistContributorBase {
	private Pattern SCALAR_PATTERN = Pattern.compile("\\$\\{[^\\{]*\\}");
	private Pattern LIST_PATTERN = Pattern.compile("\\@\\{[^\\{]*\\}");
	private Pattern ENV_PATTERN = Pattern.compile("\\%\\{[^\\{]*\\}");

	@Override
	public void computeCompletionProposals(ITextViewer viewer, int offset, List<RobotCompletionProposal> proposals) {
		String variableBeginning = extractVariableBeginning(offset, viewer);
		if (variableBeginning == null) {
			return;
		}

		setProposalImage(PyroUIPlugin.getDefault().getImageRegistry().get(PyroUIPlugin.IMAGE_VARIABLE));

		// add assist proposals in sequence according to the following
		// scenarios:

		// arguments within local keyword (arguments setting or embedded in
		// keyword name)
		handleKeywordArguments(offset, variableBeginning, proposals);

		// locally-assigned variables in prior steps within the same testcase or
		// keyword
		handleLocallyAssignedVariables(offset, variableBeginning, proposals);

		// variables defined by Set Test/Suite/Global Variable calls in
		// potential callers
		handleTestVariablesFromPotentialCallers(offset, variableBeginning, proposals);

		// variables defined by Set Test/Suite/Global Variable calls in test or
		// suite setup keywords
		handleTestVariablesFromSetupKeywords(offset, variableBeginning, proposals);

		// global test variables from variables table
		handleVariableTableVars(offset, variableBeginning, proposals);

		// global test variables from external resource files
		handleVariableTableVarsFromResourceFiles(offset, variableBeginning, proposals);

		// Robot built-in variables
		setProposalImage(PyroUIPlugin.getDefault().getImageRegistry().get(PyroUIPlugin.IMAGE_ROBOT));
		handleBuiltInVariables(offset, variableBeginning, proposals);
	}

	private void handleKeywordArguments(int offset, String variableBeginning, List<RobotCompletionProposal> proposals) {
		List<String> varNames = new ArrayList<String>();
		TableItemDefinition containingKeyword = ModelUtil.getContainingTableItem(getEditor().getModel(), offset,
				TableItemType.KEYWORD);
		if (containingKeyword != null) {
			// variables defined inline in keyword name
			String keywordName = containingKeyword.getName();
			Matcher matcher = SCALAR_PATTERN.matcher(keywordName);
			while (matcher.find()) {
				varNames.add(matcher.group());
			}

			// variables defined in Arguments setting
			for (Step step : containingKeyword.getSteps()) {
				List<StepSegment> segments = ModelUtil.collectStepSegments(step);
				boolean isArgsSetting = false;
				for (StepSegment seg : segments) {
					if (seg.getSegmentType() == SegmentType.SETTING_NAME
							&& "[Arguments]".equalsIgnoreCase(seg.getValue())) {
						isArgsSetting = true;
					} else if (isArgsSetting && seg.getSegmentType() == SegmentType.ARGUMENT) {
						addVariableIfFound(seg.getValue(), varNames);
					}
				}
			}

			Collections.sort(varNames);
			createProposals(varNames, offset, variableBeginning, proposals);
		}
	}

	private void handleLocallyAssignedVariables(int offset, String variableBeginning,
			List<RobotCompletionProposal> proposals) {
		Line currentLine = getRobotLineForOffset(offset);
		List<String> varNames = new ArrayList<String>();
		TableItemDefinition containingTableItem = ModelUtil.getContainingTableItem(getEditor().getModel(), offset,
				TableItemType.KEYWORD);
		if (containingTableItem == null) {
			containingTableItem = ModelUtil.getContainingTableItem(getEditor().getModel(), offset,
					TableItemType.TESTCASE);
		}

		if (containingTableItem == null) {
			return;
		}

		for (Step step : containingTableItem.getSteps()) {
			if (step == currentLine) {
				break;
			}
			for (StepSegment seg : step.getSegments()) {
				if (seg.getSegmentType() == SegmentType.VARIABLE) {
					// use the regex matchers to isolate only the variable and
					// ignore trailing = signs
					addVariableIfFound(seg.getValue(), varNames);
				}
			}
		}

		Collections.sort(varNames);
		createProposals(varNames, offset, variableBeginning, proposals);
	}

	private void handleTestVariablesFromPotentialCallers(int offset, String variableBeginning,
			List<RobotCompletionProposal> proposals) {
		TableItemDefinition targetKeyword = ModelUtil.getContainingTableItem(getEditor().getModel(), offset,
				TableItemType.KEYWORD);
		if (targetKeyword == null) {
			return;
		}

		List<String> varNames = new ArrayList<String>();
		Map<TableItemDefinition, List<Step>> callersMap = ModelUtil.getLocalKeywordCallersMap(getEditor().getModel(),
				targetKeyword.getName());
		for (TableItemDefinition item : callersMap.keySet()) {
			collectTestVariables(getEditor().getModel(), item, callersMap.get(item), varNames);
		}

		Collections.sort(varNames);
		createProposals(varNames, offset, variableBeginning, proposals);
	}

	private void handleTestVariablesFromSetupKeywords(int offset, String variableBeginning,
			List<RobotCompletionProposal> proposals) {
		List<String> varNames = new ArrayList<String>();
		for (Table table : getEditor().getModel().getTables()) {
			if (table.getTableType() == TableType.SETTING) {
				for (Line line : table.getTableLines()) {
					Step step = (Step) line;
					if (step.getStepType() == StepType.SETTING) {
						boolean foundSetupSetting = false;
						for (StepSegment seg : step.getSegments()) {
							if (foundSetupSetting) {
								Set<String> possibleKeywordNames = ModelUtil.getCandidateKeywordStrings(seg.getValue())
										.keySet();
								for (String possibleKeywordName : possibleKeywordNames) {
									String normalizedKeyword = ModelUtil.normalizeKeywordName(possibleKeywordName,
											false);
									Map<String, TableItemDefinition> keywordMap = ModelUtil.getKeywords(getEditor()
											.getModel());
									for (Map.Entry<String, TableItemDefinition> entry : keywordMap.entrySet()) {
										String targetKeyword = ModelUtil.normalizeKeywordName(entry.getKey(), true);
										if (normalizedKeyword.matches(targetKeyword)) {
											collectTestVariables(getEditor().getModel(), entry.getValue(), null,
													varNames);
										}
									}
								}
							} else if (seg.getSegmentType() == SegmentType.SETTING_NAME
									&& ("Test Setup".equals(seg.getValue()) || "Suite Setup".equals(seg.getValue()))) {
								foundSetupSetting = true;
							}
						}
					}

				}
			}
		}

		Collections.sort(varNames);
		createProposals(varNames, offset, variableBeginning, proposals);
	}

	private void collectTestVariables(RobotModel model, TableItemDefinition tableItem, List<Step> callingSteps,
			List<String> variableNames) {
		List<Step> stepsToFind = new ArrayList<Step>();
		if (callingSteps != null) {
			stepsToFind.addAll(callingSteps);
		}

		for (Step step : tableItem.getSteps()) {
			// keep going until we've seen all the steps with keyword calls that
			// we're looking for. if no specific
			// steps have been specified, this logic will never trigger so we'll
			// search every line
			if (stepsToFind.contains(step)) {
				stepsToFind.remove(step);
				if (stepsToFind.isEmpty()) {
					break;
				}
			}

			boolean captureVariableName = false;
			for (StepSegment segment : step.getSegments()) {
				if (captureVariableName) {
					addVariableIfFound(segment.getValue(), variableNames);
					captureVariableName = false;
				} else if (segment.getSegmentType() == SegmentType.KEYWORD_CALL) {
					Set<String> possibleKeywordNames = ModelUtil.getCandidateKeywordStrings(segment.getValue())
							.keySet();
					for (String possibleKeywordName : possibleKeywordNames) {
						String normalizedKeyword = ModelUtil.normalizeKeywordName(possibleKeywordName, false);
						if (normalizedKeyword.contains("settestvariable")
								|| normalizedKeyword.contains("setsuitevariable")
								|| normalizedKeyword.contains("setglobalvariable")) {
							captureVariableName = true;
						} else {
							Map<String, TableItemDefinition> keywordMap = ModelUtil.getKeywords(model);
							for (Map.Entry<String, TableItemDefinition> entry : keywordMap.entrySet()) {
								String targetKeyword = ModelUtil.normalizeKeywordName(entry.getKey(), true);
								if (normalizedKeyword.matches(targetKeyword)) {
									collectTestVariables(model, entry.getValue(), null, variableNames);
								}
							}
						}
					}
				}
			}
		}
	}

	private void handleVariableTableVars(int offset, String variableBeginning, List<RobotCompletionProposal> proposals) {
		List<String> varNames = collectVarsFromVariableTables(getEditor().getModel());
		Collections.sort(varNames);
		createProposals(varNames, offset, variableBeginning, proposals);
	}

	private void handleVariableTableVarsFromResourceFiles(int offset, String variableBeginning, List<RobotCompletionProposal> proposals) {
		// recursively walk through resource file imports to collect variables
		List<IFile> resourceFiles = PathUtil.collectReferencedResourceFiles(getEditor());
		
		for (IFile file : resourceFiles) {
			RobotModel model = ModelManager.getManager().getModel(file);
			if (model != null) {
				List<String> varNames = collectVarsFromVariableTables(model);
				Collections.sort(varNames);
				createProposals(varNames, offset, variableBeginning, proposals);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private List<String> collectVarsFromVariableTables(RobotModel model) {
		if (model.getCustomProperties().containsKey(IModelConstants.PROPSKEY_VARIABLE_TABLE_VARS)) {
			return (List<String>) model.getCustomProperties().get(IModelConstants.PROPSKEY_VARIABLE_TABLE_VARS);
		} else {
			List<String> variables = new ArrayList<String>();
			for (Table table : model.getTables()) {
				if (table.getTableType() == TableType.VARIABLE) {
					for (Line line : table.getTableLines()) {
						if (line instanceof Step) {
							for (StepSegment segment : ((Step) line).getSegments()) {
								if (segment.getSegmentType() == SegmentType.VARIABLE) {
									// use the regex matchers to isolate only
									// the variable and ignore trailing = signs
									addVariableIfFound(segment.getValue(), variables);
									break;
								}
							}
						}
					}
				}
			}
			model.getCustomProperties().put(IModelConstants.PROPSKEY_VARIABLE_TABLE_VARS, variables);
			return variables;
		}
	}

	private void addVariableIfFound(String sourceString, List<String> variables) {
		Matcher matcher = SCALAR_PATTERN.matcher(sourceString);
		if (matcher.find()) {
			variables.add(matcher.group());
		} else {
			matcher = LIST_PATTERN.matcher(sourceString);
			if (matcher.find()) {
				variables.add(matcher.group());
			} else {
				matcher = ENV_PATTERN.matcher(sourceString);
				if (matcher.find()) {
					variables.add(matcher.group());
				}
			}
		}
	}

	private void handleBuiltInVariables(int offset, String variableBeginning, List<RobotCompletionProposal> proposals) {
		List<String> varNames = Arrays.asList(IModelConstants.BUILT_IN_VARIABLES);
		// do not sort, they've already been ordered in related groupings
		createProposals(varNames, offset, variableBeginning, proposals);
	}

	private void createProposals(List<String> varNames, int offset, String variableBeginning,
			List<RobotCompletionProposal> proposals) {
		for (String varName : varNames) {
			if (varName.startsWith(variableBeginning)) {
				addCompletionProposal(proposals, varName, offset - variableBeginning.length(),
						variableBeginning.length(), varName.length(), varName, null);
			}
		}
	}

	private String extractVariableBeginning(int offset, ITextViewer viewer) {
		// first see if we're in an argument cell
		String[] variableFragments = getStringFragments(offset, SegmentType.ARGUMENT, viewer);
		if (variableFragments == null && isPotentialEmptyStepSegment(offset, SegmentType.ARGUMENT)) {
			variableFragments = new String[] { "", "" };
		}
		
		// now check if this may be a variable assignment instead
		if (variableFragments == null) {
			variableFragments = getStringFragments(offset, SegmentType.VARIABLE, viewer);
			if (variableFragments == null && isPotentialEmptyStepSegment(offset, SegmentType.VARIABLE)) {
				variableFragments = new String[] { "", "" };
			}
		}

		if (variableFragments != null) {
			String extractedVariableBeginning = variableFragments[0];
			int lastScalar = extractedVariableBeginning.lastIndexOf("$");
			int lastList = extractedVariableBeginning.lastIndexOf("@");
			int startIndex = Math.max(lastScalar, lastList);
			if (startIndex > -1) {
				extractedVariableBeginning = extractedVariableBeginning.substring(startIndex);
			} else {
				extractedVariableBeginning = "";
			}
			return extractedVariableBeginning;
		}
		return null;
	}

}
