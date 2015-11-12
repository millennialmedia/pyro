package com.millennialmedia.pyro.ui.internal.contentassist;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.text.ITextViewer;

import com.millennialmedia.pyro.model.util.IModelConstants;
import com.millennialmedia.pyro.ui.PyroUIPlugin;
import com.millennialmedia.pyro.ui.contentassist.RobotCompletionProposal;
import com.millennialmedia.pyro.ui.contentassist.VariableNameAssistContributorBase;

/**
 * A  contributor to handle assist for built-in Robot variables inserted into argument
 * cells or added as the left-hand side of a variable assignment.
 * 
 * @author spaxton
 */
public class BuiltInVariableNameAssistContributor extends VariableNameAssistContributorBase {

	public BuiltInVariableNameAssistContributor() {
		setProposalImage(PyroUIPlugin.getDefault().getImageRegistry().get(PyroUIPlugin.IMAGE_ROBOT));
	}
	
	@Override
	public void computeCompletionProposals(ITextViewer viewer, int offset, List<RobotCompletionProposal> proposals) {
		String variableBeginning = extractVariableBeginning(offset, viewer);
		if (variableBeginning == null) {
			return;
		}

		// Robot built-in variables
		handleBuiltInVariables(offset, variableBeginning, proposals);
	}

	private void handleBuiltInVariables(int offset, String variableBeginning, List<RobotCompletionProposal> proposals) {
		List<String> varNames = Arrays.asList(IModelConstants.BUILT_IN_VARIABLES);
		// do not sort, they've already been ordered in related groupings
		createProposals(varNames, offset, variableBeginning, proposals);
	}

}
