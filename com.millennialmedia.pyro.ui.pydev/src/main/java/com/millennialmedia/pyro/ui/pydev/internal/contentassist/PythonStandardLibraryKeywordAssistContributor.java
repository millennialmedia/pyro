package com.millennialmedia.pyro.ui.pydev.internal.contentassist;

import java.util.ArrayList;
import java.util.Arrays;
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
import com.millennialmedia.pyro.ui.PyroUIPlugin;
import com.millennialmedia.pyro.ui.contentassist.KeywordAssistContributorBase;
import com.millennialmedia.pyro.ui.contentassist.RobotCompletionProposal;
import com.millennialmedia.pyro.ui.editor.util.PathUtil;
import com.millennialmedia.pyro.ui.pydev.internal.ModuleInfo;
import com.millennialmedia.pyro.ui.pydev.internal.PyDevUtil;

/**
 * Keyword content assist contributor for built-in Robot keywords.
 * 
 * @author spaxton
 */
public class PythonStandardLibraryKeywordAssistContributor extends KeywordAssistContributorBase {

	public PythonStandardLibraryKeywordAssistContributor() {
		setProposalImage(PyroUIPlugin.getDefault().getImageRegistry().get(PyroUIPlugin.IMAGE_ROBOT));
	}

	@Override
	public void computeCompletionProposals(ITextViewer viewer, int offset, List<RobotCompletionProposal> proposals) {
		String[] keywordFragments = getCurrentKeywordFragments(offset);
		if (keywordFragments == null) {
			return;
		}

		Map<String, List<String>> standardLibsMap = PyDevUtil.getStandardLibsMap();
		List<String> referencedLibraries = PyDevUtil.getReferencedLibraries(PathUtil.getEditorFile(getEditor()));
		referencedLibraries.add("BuiltIn");

		for (String libraryName : referencedLibraries) {
			if (!standardLibsMap.containsKey(libraryName)) {
				continue;
			}

			ModuleInfo moduleInfo = PyDevUtil.findStandardLibModule(libraryName, PathUtil.getEditorFile(getEditor()));
			if (moduleInfo == null) {
				return;
			}

			String referencedLibraryName = ModelUtil.getLibraryAlias(getEditor().getModel(), libraryName);

			boolean prefixWithLibraryName = (referencedLibraryName.toLowerCase().startsWith(
					keywordFragments[0].toLowerCase()) && keywordFragments[0].length() > 0);

			List<String> classNames = new ArrayList<String>(standardLibsMap.get(libraryName));

			List<IToken> tokensList = new ArrayList<IToken>();

			IToken[] tokens = moduleInfo.getModule().getGlobalTokens();
			while (!classNames.isEmpty()) {
				String subclass = classNames.remove(0);
				for (IToken token : tokens) {
					if (subclass.equalsIgnoreCase(token.getRepresentation())) {
						int line = token.getLineDefinition();
						int col = token.getColDefinition();
						ILocalScope localScope = moduleInfo.getModule().getLocalScope(line, col);
						tokensList.addAll(Arrays.asList(localScope.getAllLocalTokens()));
						break;
					}
				}
			}

			List<String> keywords = new ArrayList<String>();
			for (IToken token : tokensList) {
				String tokenName = token.getRepresentation();
				// filter out internal methods
				if (tokenName.startsWith("_")) {
					continue;
				}

				// also filter out non-functions (ex. constants, including some
				// expected robot constants like ROBOT_LIBRARY_* (scope,
				// version, doc format))
				if (token instanceof SourceToken) {
					SourceToken sourceToken = (SourceToken) token;
					if (!(sourceToken.getAst() instanceof FunctionDef)) {
						continue;
					}
				}

				String targetKeyword = tokenName.replace("_", " ");
				if (prefixWithLibraryName) {
					keywords.add(referencedLibraryName + "." + targetKeyword);
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
						styledDisplayString.append(" - " + libraryName, RobotCompletionProposal.QUALIFIER_STYLER);
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
