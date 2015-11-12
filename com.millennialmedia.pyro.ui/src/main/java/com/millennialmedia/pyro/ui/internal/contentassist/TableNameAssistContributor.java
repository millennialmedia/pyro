package com.millennialmedia.pyro.ui.internal.contentassist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.text.ITextViewer;

import com.millennialmedia.pyro.model.util.IModelConstants;
import com.millennialmedia.pyro.ui.PyroUIPlugin;
import com.millennialmedia.pyro.ui.contentassist.ContentAssistContributorBase;
import com.millennialmedia.pyro.ui.contentassist.RobotCompletionProposal;

/**
 * Contributor to Robot table names (signaled by a leading "*" character)
 * 
 * @author spaxton
 */
public class TableNameAssistContributor extends ContentAssistContributorBase {

	public TableNameAssistContributor() {
		setProposalImage(PyroUIPlugin.getDefault().getImageRegistry().get(PyroUIPlugin.IMAGE_TABLE));
	}

	@Override
	public void computeCompletionProposals(ITextViewer viewer, int offset, List<RobotCompletionProposal> proposals) {
		int lineNum = viewer.getTextWidget().getLineAtOffset(offset);
		int offsetAtLine = viewer.getTextWidget().getOffsetAtLine(lineNum);
		String lineContents = viewer.getTextWidget().getLine(lineNum);
		String textBeforeCaret = lineContents.substring(0, offset - offsetAtLine);

		if (textBeforeCaret != null &&
				(textBeforeCaret.startsWith("*") || 
				("".equals(textBeforeCaret.trim()) && 
				(!textBeforeCaret.contains("\t") && !textBeforeCaret.contains("  "))))) {
			String tableNameBeginning = lineContents.substring(0,
					offset - viewer.getTextWidget().getOffsetAtLine(lineNum));
			tableNameBeginning = tableNameBeginning.replace("*", "").trim();

			List<String> proposedNames = new ArrayList<String>();
			// try to match one of our preferred names first
			for (String candidateTableName : IModelConstants.PREFERRED_TABLE_NAMES) {
				if (candidateTableName.startsWith(tableNameBeginning)) {
					proposedNames.add("*** " + candidateTableName + " ***");
				}
			}

			// only if nothing matched will we use the expanded set of
			// table-name aliases
			if (proposedNames.isEmpty()) {
				for (String candidateTableName : IModelConstants.TABLE_NAMES) {
					if (candidateTableName.startsWith(tableNameBeginning)) {
						proposedNames.add("*** " + candidateTableName + " ***");
					}
				}
			}

			Collections.sort(proposedNames);

			for (String proposedName : proposedNames) {
				addCompletionProposal(proposals, proposedName, viewer.getTextWidget().getOffsetAtLine(lineNum),
						lineContents.length(), proposedName.length(), proposedName, null);
			}
		}
	}

}
