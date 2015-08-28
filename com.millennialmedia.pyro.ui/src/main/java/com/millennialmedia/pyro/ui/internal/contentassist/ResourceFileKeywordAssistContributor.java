package com.millennialmedia.pyro.ui.internal.contentassist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.viewers.StyledString;

import com.millennialmedia.pyro.model.ModelManager;
import com.millennialmedia.pyro.model.RobotModel;
import com.millennialmedia.pyro.model.TableItemDefinition;
import com.millennialmedia.pyro.model.util.ModelUtil;
import com.millennialmedia.pyro.ui.PyroUIPlugin;
import com.millennialmedia.pyro.ui.contentassist.KeywordAssistContributorBase;
import com.millennialmedia.pyro.ui.contentassist.RobotCompletionProposal;
import com.millennialmedia.pyro.ui.editor.util.PathUtil;

/**
 * Contributor for keywords defined in external Robot source files referenced by
 * the Resource setting.
 * 
 * @author spaxton
 */
public class ResourceFileKeywordAssistContributor extends KeywordAssistContributorBase {
	
	public ResourceFileKeywordAssistContributor() {
		setProposalImage(PyroUIPlugin.getDefault().getImageRegistry().get(PyroUIPlugin.IMAGE_KEYWORD));
	}

	@Override
	public void computeCompletionProposals(ITextViewer viewer, int offset, List<RobotCompletionProposal> proposals) {
		String[] keywordFragments = getCurrentKeywordFragments(offset, viewer);

		if (keywordFragments != null) {
			// collect all keyword names in any referenced Resource files (include indirect references)
			List<KeywordFilePair> targetKeywords = getTargetKeywords();
			Map<String, Integer> candidateFragmentStrings = 
					ModelUtil.getCandidateKeywordStrings(keywordFragments[0]);
			List<String> candidateFragments = 
					new ArrayList<String>(candidateFragmentStrings.keySet());
			for (KeywordFilePair pair : targetKeywords) {
				String keyword = pair.getKeywordName();
				for (String fragment : candidateFragments) {
					if (ModelUtil.normalizeKeywordName(keyword, false).startsWith(
							ModelUtil.normalizeKeywordName(fragment, false))) {
						keyword = performSmartCapitalization(keyword, fragment);

						StyledString styledDisplayString = new StyledString();
						styledDisplayString.append(keyword,
								RobotCompletionProposal.FOREGROUND_STYLER);
						styledDisplayString.append(" - " + pair.getFileName(),
								RobotCompletionProposal.QUALIFIER_STYLER);
						addCompletionProposal(
								proposals,
								keyword,
								offset - keywordFragments[0].length()
										+ candidateFragmentStrings.get(fragment),
								keywordFragments[0].length()
										- candidateFragmentStrings.get(fragment)
										+ keywordFragments[1].length(), keyword.length(), keyword
										+ " - " + pair.getFileName(), styledDisplayString);
					}
				}
			}
		}
	}

	protected List<KeywordFilePair> getTargetKeywords() {
		List<KeywordFilePair> targetKeywords = new ArrayList<KeywordFilePair>();
		
		// get any files referenced by a Resource setting
		List<IFile> files = PathUtil.collectReferencedResourceFiles(getEditor());
		for (IFile file : files) {
			RobotModel targetModel = ModelManager.getManager().getModel(file);
			// find any keyword definitions in this external file
			Map<String, TableItemDefinition> keywordsMap = ModelUtil.getKeywords(targetModel);

			List<KeywordFilePair> newKeywords = new ArrayList<KeywordFilePair>();
			
			for (String keywordName : keywordsMap.keySet()) {
				// check existing keywords first to short-circuit cycles of imports
				KeywordFilePair newPair = new KeywordFilePair();
				newPair.setKeywordName(keywordName);
				newPair.setFileName(file.getName());
				if (!targetKeywords.contains(newPair)) {
					newKeywords.add(newPair);
				}
			}
			
			Collections.sort(newKeywords);
			targetKeywords.addAll(newKeywords);
		}

		return targetKeywords;
	}
	
}
