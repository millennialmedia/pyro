package com.millennialmedia.pyro.ui.internal.contentassist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
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
	class KeywordFilePair implements Comparable<KeywordFilePair> {
		String keywordName;
		IFile file;
		
		@Override
		public int compareTo(KeywordFilePair o) {
			return keywordName.compareTo(o.keywordName);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((file == null) ? 0 : file.hashCode());
			result = prime * result + ((keywordName == null) ? 0 : keywordName.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			KeywordFilePair other = (KeywordFilePair) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (file == null) {
				if (other.file != null)
					return false;
			} else if (!file.getLocation().equals(other.file.getLocation()))
				return false;
			if (keywordName == null) {
				if (other.keywordName != null)
					return false;
			} else if (!keywordName.equals(other.keywordName))
				return false;
			return true;
		}

		private ResourceFileKeywordAssistContributor getOuterType() {
			return ResourceFileKeywordAssistContributor.this;
		}
		
		
	}
	
	public ResourceFileKeywordAssistContributor() {
		setProposalImage(PyroUIPlugin.getDefault().getImageRegistry().get(PyroUIPlugin.IMAGE_KEYWORD));
	}

	@Override
	public void computeCompletionProposals(ITextViewer viewer, int offset, List<RobotCompletionProposal> proposals) {
		String[] keywordFragments = getCurrentKeywordFragments(offset);

		if (keywordFragments != null) {
			// collect all keyword names in any referenced Resource files (include indirect references)
			List<KeywordFilePair> targetKeywords = getTargetKeywords();
			for (KeywordFilePair pair : targetKeywords) {
				String keyword = pair.keywordName;
				Map<String, Integer> candidateFragmentStrings = ModelUtil
						.getCandidateKeywordStrings(keywordFragments[0]);
				List<String> candidateFragments = new ArrayList<String>(
						candidateFragmentStrings.keySet());
				for (String fragment : candidateFragments) {
					if (ModelUtil.normalizeKeywordName(keyword, false).startsWith(
							ModelUtil.normalizeKeywordName(fragment, false))) {
						keyword = performSmartCapitalization(keyword, fragment);

						StyledString styledDisplayString = new StyledString();
						styledDisplayString.append(keyword,
								RobotCompletionProposal.FOREGROUND_STYLER);
						styledDisplayString.append(" - " + pair.file.getName(),
								RobotCompletionProposal.QUALIFIER_STYLER);
						addCompletionProposal(
								proposals,
								keyword,
								offset - keywordFragments[0].length()
										+ candidateFragmentStrings.get(fragment),
								keywordFragments[0].length()
										- candidateFragmentStrings.get(fragment)
										+ keywordFragments[1].length(), keyword.length(), keyword
										+ " - " + pair.file.getName(), styledDisplayString);
					}
				}
			}
		}
	}

	protected List<KeywordFilePair> getTargetKeywords() {
		List<KeywordFilePair> targetKeywords = new ArrayList<KeywordFilePair>();
		
		// get any files referenced by a Resource setting
		List<String> resourceFilePaths = ModelUtil.getResourceFilePaths(getEditor().getModel());

		if (!resourceFilePaths.isEmpty()) {
			IFile file = PathUtil.getEditorFile(getEditor());
			if (file == null) {
				return targetKeywords;
			}

			IPath rootPath = PathUtil.getRootPath(getEditor());

			if (rootPath != null) {
				// for each resource file in the Settings table(s)
				for (String resourceFilePath : resourceFilePaths) {
					addTargetAndTransitiveLinks(targetKeywords, file, resourceFilePath);
				}
			}
		}

		return targetKeywords;
	}
	
	private void addTargetAndTransitiveLinks(List<KeywordFilePair> targetKeywords, IFile localFile, String resourceFilePath) {
		IResource resource = PathUtil.getResourceForPath(localFile, resourceFilePath);

		if (resource != null && resource instanceof IFile) {
			IFile targetFile = (IFile) resource;
			RobotModel targetModel = ModelManager.getManager().getModel(targetFile);
			// find any keyword definitions in this external file
			Map<String, TableItemDefinition> keywordsMap = ModelUtil.getKeywords(targetModel);

			List<KeywordFilePair> newKeywords = new ArrayList<KeywordFilePair>();
			
			for (String keywordName : keywordsMap.keySet()) {
				// check existing keywords first to short-circuit cycles of imports
				KeywordFilePair newPair = new KeywordFilePair();
				newPair.keywordName = keywordName;
				newPair.file = targetFile;
				if(!targetKeywords.contains(newPair)) {
					newKeywords.add(newPair);
				}
			}
			
			Collections.sort(newKeywords);
			targetKeywords.addAll(newKeywords);

			// repeat for any resource files contained within this model (transitive resource file imports)
			List<String> resourceFilePaths = ModelUtil.getResourceFilePaths(targetModel);
			if (!resourceFilePaths.isEmpty()) {
				for (String transitiveResourceFilePath : resourceFilePaths) {
					addTargetAndTransitiveLinks(targetKeywords, targetFile, transitiveResourceFilePath);
				}
			}
		}
	}	
}
