package com.millennialmedia.pyro.ui.pydev.internal.contentassist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.viewers.StyledString;
import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IToken;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.parser.jython.ast.FunctionDef;

import com.millennialmedia.pyro.model.util.ModelUtil;
import com.millennialmedia.pyro.ui.contentassist.KeywordAssistContributorBase;
import com.millennialmedia.pyro.ui.contentassist.RobotCompletionProposal;
import com.millennialmedia.pyro.ui.editor.util.PathUtil;
import com.millennialmedia.pyro.ui.pydev.internal.ModuleInfo;
import com.millennialmedia.pyro.ui.pydev.internal.PyDevUtil;
import com.millennialmedia.pyro.ui.pydev.internal.PyroPyDevPlugin;

/**
 * Keyword content assist contributor for user-defined Python-authored keywords.
 * 
 * @author spaxton
 */
public class PythonUserLibraryKeywordAssistContributor extends KeywordAssistContributorBase {

	public PythonUserLibraryKeywordAssistContributor() {
		setProposalImage(PyroPyDevPlugin.getDefault().getImageRegistry().get(PyroPyDevPlugin.IMAGE_PYTHON));
	}

	@Override
	public void computeCompletionProposals(ITextViewer viewer, int offset, List<RobotCompletionProposal> proposals) {
		String[] keywordFragments = getCurrentKeywordFragments(offset);
		if (keywordFragments == null) {
			return;
		}

		String keywordNamePrefix = keywordFragments[0];

		if (keywordFragments != null) {
			// get any Library imports
			Map<String, ModuleInfo> libraryModuleMap = PyDevUtil.findAllReferencedLibraryModules(PathUtil.getEditorFile(getEditor()));

			if (!libraryModuleMap.isEmpty()) {
				for (String libraryName : libraryModuleMap.keySet()) {
					ModuleInfo moduleInfo = null;

					String referencedLibraryName = ModelUtil.getLibraryAlias(getEditor().getModel(), libraryName);

					List<String> keywordLibSegments = PathUtil.getPathSegments(keywordNamePrefix);
					List<String> librarySegments = PathUtil.getNormalizedPathSegments(referencedLibraryName);

					// see if we can find a match for a fully-qualified
					// reference to this library, otherwise just use look up the
					// module directly
					boolean prefixWithLibraryName = false;
					if (keywordNamePrefix.length() > 0
							&& keywordStartsWithLibraryName(keywordLibSegments, librarySegments)) {
						prefixWithLibraryName = true;
						moduleInfo = libraryModuleMap.get(libraryName);
					} else if (!prefixWithLibraryName) {
						moduleInfo = libraryModuleMap.get(libraryName);
					}

					libraryName = referencedLibraryName;

					if (moduleInfo != null) {
						IToken[] tokens = null;
						List<String> subclasses = PathUtil.getPathSegments(moduleInfo.getRemainingPath());
						tokens = moduleInfo.getModule().getGlobalTokens();
						while (!subclasses.isEmpty()) {
							String subclass = subclasses.remove(0);
							for (IToken token : tokens) {
								if (subclass.equalsIgnoreCase(token.getRepresentation())) {
									// found the token representing the next
									// portion of the library reference inside
									// this module
									int line = token.getLineDefinition();
									int col = token.getColDefinition();
									ILocalScope localScope = moduleInfo.getModule().getLocalScope(line, col);
									tokens = localScope.getAllLocalTokens();
									break;
								}
							}
						}

						if (tokens == null) {
							continue;
						}

						List<String> keywords = new ArrayList<String>();
						for (IToken token : tokens) {
							String tokenName = token.getRepresentation();
							// filter out internal methods
							if (tokenName.startsWith("_")) {
								continue;
							}

							// also filter out non-functions (ex. constants,
							// including some expected robot constants like
							// ROBOT_LIBRARY_* (scope, version, doc format))
							if (token instanceof SourceToken) {
								SourceToken sourceToken = (SourceToken) token;
								if (!(sourceToken.getAst() instanceof FunctionDef)) {
									continue;
								}
							}

							String targetKeyword = tokenName.replace("_", " ");
							if (prefixWithLibraryName) {
								keywords.add(libraryName + "." + targetKeyword);
							} else {
								keywords.add(targetKeyword);
							}
						}

						Collections.sort(keywords);
						for (String keyword : keywords) {
							Map<String, Integer> candidateFragmentStrings = ModelUtil
									.getCandidateKeywordStrings(keywordFragments[0]);
							List<String> candidateFragments = new ArrayList<String>(candidateFragmentStrings.keySet());
							for (String fragment : candidateFragments) {
								if (ModelUtil.normalizeKeywordName(keyword, false).startsWith(
										ModelUtil.normalizeKeywordName(fragment, false))) {
									keyword = performSmartCapitalization(keyword, fragment);

									StyledString styledDisplayString = new StyledString();
									styledDisplayString.append(keyword, RobotCompletionProposal.FOREGROUND_STYLER);
									
									String libraryPath = PathUtil.joinPathSegments(PathUtil.getNormalizedPathSegments(libraryName));
									styledDisplayString.append(" - " + libraryPath,
											RobotCompletionProposal.QUALIFIER_STYLER);
									addCompletionProposal(proposals, keyword, offset - keywordFragments[0].length()
											+ candidateFragmentStrings.get(fragment), keywordFragments[0].length()
											- candidateFragmentStrings.get(fragment) + keywordFragments[1].length(),
											keyword.length(), keyword + " - " + libraryName, styledDisplayString);
								}
							}
						}
					}
				}
			}
		}
	}

	private boolean keywordStartsWithLibraryName(List<String> keywordLibSegments, List<String> librarySegments) {
		int libLength = librarySegments.size();
		for (int i = 0; i < keywordLibSegments.size(); i++) {
			if (i == libLength) {
				return true;
			} else if (i == libLength - 1
					&& !librarySegments.get(i).toLowerCase().startsWith(keywordLibSegments.get(i).toLowerCase())) {
				return false;
			} else if (!librarySegments.get(i).toLowerCase().startsWith(keywordLibSegments.get(i).toLowerCase())) {
				return false;
			}
		}
		return true;
	}

}
