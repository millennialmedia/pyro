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
		String[] keywordFragments = getCurrentKeywordFragments(offset);

		if (keywordFragments != null) {
			// get any files referenced by a Resource setting
			List<String> resourceFilePaths = ModelUtil.getResourceFilePaths(getEditor().getModel());
			Collections.sort(resourceFilePaths);

			if (!resourceFilePaths.isEmpty()) {
				IFile file = PathUtil.getEditorFile(getEditor());
				if (file != null) {
					IPath rootPath = PathUtil.getRootPath(getEditor());

					if (rootPath != null) {
						// for each resource file in the Settings table(s)
						for (String resourceFilePath : resourceFilePaths) {
							IResource resource = PathUtil.getResourceForPath(getEditor(), resourceFilePath);

							if (resource != null && resource instanceof IFile) {
								IFile targetFile = (IFile) resource;
								RobotModel targetModel = ModelManager.getManager().getModel(targetFile);
								// find any keyword definitions in this external
								// file
								List<String> keywords = new ArrayList<String>(ModelUtil.getKeywords(targetModel)
										.keySet());
								Collections.sort(keywords);

								for (String keyword : keywords) {
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
											styledDisplayString.append(" - " + resource.getName(),
													RobotCompletionProposal.QUALIFIER_STYLER);
											addCompletionProposal(
													proposals,
													keyword,
													offset - keywordFragments[0].length()
															+ candidateFragmentStrings.get(fragment),
													keywordFragments[0].length()
															- candidateFragmentStrings.get(fragment)
															+ keywordFragments[1].length(), keyword.length(), keyword
															+ " - " + resource.getName(), styledDisplayString);
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

}
