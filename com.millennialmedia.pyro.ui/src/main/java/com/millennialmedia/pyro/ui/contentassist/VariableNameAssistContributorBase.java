package com.millennialmedia.pyro.ui.contentassist;

import java.util.List;

import org.eclipse.jface.text.ITextViewer;

import com.millennialmedia.pyro.model.StepSegment.SegmentType;

public class VariableNameAssistContributorBase extends ContentAssistContributorBase {

	protected void createProposals(List<String> varNames, int offset, String variableBeginning,
			List<RobotCompletionProposal> proposals) {
		for (String varName : varNames) {
			if (varName.startsWith(variableBeginning)) {
				addCompletionProposal(proposals, varName, offset - variableBeginning.length(),
						variableBeginning.length(), varName.length(), varName, null);
			}
		}
	}

	protected String extractVariableBeginning(int offset, ITextViewer viewer) {
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
