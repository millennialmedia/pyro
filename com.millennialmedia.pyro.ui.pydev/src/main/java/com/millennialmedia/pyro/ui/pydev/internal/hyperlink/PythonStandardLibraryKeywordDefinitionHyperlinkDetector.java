package com.millennialmedia.pyro.ui.pydev.internal.hyperlink;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IToken;
import org.python.pydev.editor.actions.PyOpenAction;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.shared_core.structure.Location;

import com.millennialmedia.pyro.model.util.ModelUtil;
import com.millennialmedia.pyro.ui.editor.util.PathUtil;
import com.millennialmedia.pyro.ui.hyperlink.AbstractKeywordDefinitionHyperlinkDetector;
import com.millennialmedia.pyro.ui.pydev.internal.ModuleInfo;
import com.millennialmedia.pyro.ui.pydev.internal.PyDevUtil;

/**
 * Hyperlinking for user-defined keywords implemented in Python libraries.
 * 
 * @author spaxton
 */
public class PythonStandardLibraryKeywordDefinitionHyperlinkDetector extends AbstractKeywordDefinitionHyperlinkDetector {

	@Override
	protected boolean isUserKeywordDetector() {
		return false;
	}

	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
		final KeywordCallContext keywordCallContext = getKeywordCallContext(region);
		if (keywordCallContext == null) {
			return null;
		}

		final Map<String, Integer> candidateCallStrings = ModelUtil.getCandidateKeywordStrings(keywordCallContext
				.getKeywordName());
		Map<String, List<String>> standardLibsMap = PyDevUtil.getStandardLibsMap();
		List<String> referencedLibraries = PyDevUtil.getReferencedLibraries(PathUtil.getEditorFile(getEditor()));
		referencedLibraries.add("BuiltIn");

		for (String libraryName : referencedLibraries) {
			if (!standardLibsMap.containsKey(libraryName)) {
				continue;
			}

			ModuleInfo moduleInfo = PyDevUtil.findStandardLibModule(libraryName, PathUtil.getEditorFile(getEditor()));
			if (moduleInfo == null) {
				return null;
			}

			String referencedLibraryName = ModelUtil.getLibraryAlias(getEditor().getModel(), libraryName);

			List<String> classNames = standardLibsMap.get(libraryName);

			List<IToken> tokensList = new ArrayList<IToken>();

			for (final String candidateKeywordName : candidateCallStrings.keySet()) {
				String keywordMethodName = candidateKeywordName;
				if (candidateKeywordName.contains(".") && referencedLibraryName != null
						&& candidateKeywordName.startsWith(referencedLibraryName)) {
					// we already know we're looking at the right library
					String[] segments = candidateKeywordName.split("\\.");
					keywordMethodName = segments[segments.length - 1];
				}

				IToken matchingToken = null;

				if (moduleInfo != null) {
					IToken[] globalTokens = moduleInfo.getModule().getGlobalTokens();

					for (IToken classToken : globalTokens) {
						if (classNames.contains(classToken.getRepresentation())) {
							int line = classToken.getLineDefinition();
							int col = classToken.getColDefinition();
							ILocalScope localScope = moduleInfo.getModule().getLocalScope(line, col);
							tokensList.addAll(Arrays.asList(localScope.getAllLocalTokens()));
						}
					}
				}

				for (IToken token : tokensList) {
					if (ModelUtil.normalizeKeywordName(keywordMethodName, false).equals(
							ModelUtil.normalizeKeywordName(token.getRepresentation(), false))) {
						matchingToken = token;
						break;
					}
				}

				if (matchingToken != null) {
					final ItemPointer pointer = new ItemPointer(moduleInfo.getModule().getFile(), new Location(
							matchingToken.getLineDefinition() - 1, matchingToken.getColDefinition() - 1), new Location(
							matchingToken.getLineDefinition() - 1, matchingToken.getColDefinition() - 1));
					final IProject project = PathUtil.getEditorFile(getEditor()).getProject();
					IHyperlink link = new IHyperlink() {
						@Override
						public void open() {
							new PyOpenAction().run(pointer, project, null);
						}

						@Override
						public String getTypeLabel() {
							return "";
						}

						@Override
						public String getHyperlinkText() {
							return "";
						}

						@Override
						public IRegion getHyperlinkRegion() {
							return new Region(keywordCallContext.getKeywordStartOffset()
									+ candidateCallStrings.get(candidateKeywordName), candidateKeywordName.length());
						}
					};
					return new IHyperlink[] { link };
				}
			}

		}

		return null;
	}
	
}
