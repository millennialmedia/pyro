package com.millennialmedia.pyro.ui.pydev.internal.contentassist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import com.millennialmedia.pyro.ui.pydev.internal.LibraryInfo;
import com.millennialmedia.pyro.ui.pydev.internal.ModuleInfo;

/**
 * Base class for keyword content assist contributor for any Python-authored keywords,
 * both Robot Framework built-ins and 3rd-party provided libraries.
 * 
 * @author spaxton
 */
public abstract class AbstractPythonLibraryKeywordAssistContributor extends KeywordAssistContributorBase {

	protected abstract LibraryInfo getLibraryInfo();
	
	@Override
	public void computeCompletionProposals(ITextViewer viewer, int offset, List<RobotCompletionProposal> proposals) {
		String[] keywordFragments = getCurrentKeywordFragments(offset, viewer);
		if (keywordFragments == null) {
			return;
		}

		String keywordNamePrefix = keywordFragments[0];
		List<String> keywordLibSegments = PathUtil.getPathSegments(keywordNamePrefix);

		// get any Library imports
		LibraryInfo libraryInfo = getLibraryInfo();

		for (String libraryName : libraryInfo.getOrderedLibraries()) {
			Collection<ModuleInfo> moduleInfos = null;

			String referencedLibraryName = ModelUtil.getLibraryAlias(getEditor().getModel(), libraryName);

			List<String> librarySegments = PathUtil.getNormalizedPathSegments(referencedLibraryName);

			// if the keyword name starts with the library name, assume we're using a fully-qualified reference
			boolean prefixWithLibraryName = false;
			if (keywordNamePrefix.length() > 0
					&& keywordStartsWithLibraryName(keywordLibSegments, librarySegments)) {
				prefixWithLibraryName = true;
			}
			
			moduleInfos = libraryInfo.getModuleMap().get(libraryName);

			// reassign to use an alias name (if present)
			libraryName = referencedLibraryName;

			if (moduleInfos != null) {
				List<IToken> tokens = new ArrayList<IToken>();
				for (ModuleInfo moduleInfo : moduleInfos) {
					List<String> subclasses = PathUtil.getPathSegments(moduleInfo.getRemainingPath());
					tokens.addAll(Arrays.asList(moduleInfo.getModule().getGlobalTokens()));
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
								tokens.addAll(Arrays.asList(localScope.getAllLocalTokens()));
								break;
							}
						}
					}
					
					// also look for a class of the same name defined in the module
					// because the module name may include a package structure, match this via endsWith()
					for (IToken token : tokens) {
						if (moduleInfo.getModule().getName().endsWith(token.getRepresentation())) {
							int line = token.getLineDefinition();
							int col = token.getColDefinition();
							ILocalScope localScope = moduleInfo.getModule().getLocalScope(line, col);
							tokens.addAll(Arrays.asList(localScope.getAllLocalTokens()));
							break;
						}
					}
				}

				if (tokens.isEmpty()) {
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
