package com.millennialmedia.pyro.ui.internal.contentassist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.ITextViewer;

import com.millennialmedia.pyro.model.util.ModelUtil;
import com.millennialmedia.pyro.ui.PyroUIPlugin;
import com.millennialmedia.pyro.ui.contentassist.KeywordAssistContributorBase;
import com.millennialmedia.pyro.ui.contentassist.RobotCompletionProposal;

/**
 * Contributor for local keywords defined in the same Robot source file.
 * 
 * @author spaxton
 */
public class LocalKeywordAssistContributor extends KeywordAssistContributorBase {

	public LocalKeywordAssistContributor() {
		setProposalImage(PyroUIPlugin.getDefault().getImageRegistry().get(PyroUIPlugin.IMAGE_KEYWORD));
	}

	@Override
	public void computeCompletionProposals(ITextViewer viewer, int offset, List<RobotCompletionProposal> proposals) {
		String[] keywordFragments = getCurrentKeywordFragments(offset, viewer);

		if (keywordFragments != null) {
			List<String> keywords = new ArrayList<String>(ModelUtil.getKeywords(getEditor().getModel()).keySet());
			Collections.sort(keywords);

			for (String keyword : keywords) {
				Map<String, Integer> candidateFragmentStrings = ModelUtil
						.getCandidateKeywordStrings(keywordFragments[0]);
				List<String> candidateFragments = new ArrayList<String>(candidateFragmentStrings.keySet());
				for (String fragment : candidateFragments) {
					if (ModelUtil.normalizeKeywordName(keyword, false).startsWith(
							ModelUtil.normalizeKeywordName(fragment, false))) {
						keyword = performSmartCapitalization(keyword, fragment);

						addCompletionProposal(proposals, keyword, offset - keywordFragments[0].length()
								+ candidateFragmentStrings.get(fragment), keywordFragments[0].length()
								- candidateFragmentStrings.get(fragment) + keywordFragments[1].length(),
								keyword.length(), keyword, null);
					}
				}
			}
		}
	}

}
