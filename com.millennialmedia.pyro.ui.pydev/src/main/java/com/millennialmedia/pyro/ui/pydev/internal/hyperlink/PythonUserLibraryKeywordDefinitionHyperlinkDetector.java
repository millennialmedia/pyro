package com.millennialmedia.pyro.ui.pydev.internal.hyperlink;

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
 * Hyperlinking for built-in Robot keywords defined in the framework Python
 * sources.
 * 
 * @author spaxton
 */
public class PythonUserLibraryKeywordDefinitionHyperlinkDetector extends AbstractKeywordDefinitionHyperlinkDetector {

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
		List<String> libraries = ModelUtil.getLibraries(getEditor().getModel());
		Map<String, ModuleInfo> libraryModuleMap = PyDevUtil.findModules(libraries, getEditor());

		for (final String candidateKeywordName : candidateCallStrings.keySet()) {
			ModuleInfo moduleInfo = null;
			String keywordName = null;
			IToken matchingToken = null;

			for (String libraryName : libraries) {
				if (candidateKeywordName.contains(".")) {
					// break into segments and look up against the module map
					IToken[] tokens = null;
					String referencedLibraryName = ModelUtil.getLibraryAlias(getEditor().getModel(), libraryName);

					if (libraryNameMatches(referencedLibraryName,
							candidateKeywordName.substring(0, candidateKeywordName.lastIndexOf(".")))) {
						moduleInfo = libraryModuleMap.get(libraryName);

						String[] segments = candidateKeywordName.split("\\.");
						keywordName = segments[segments.length - 1];

						if (moduleInfo != null) {
							List<String> subclasses = PathUtil.getPathSegments(moduleInfo.getRemainingPath());
							tokens = moduleInfo.getModule().getGlobalTokens();
							while (!subclasses.isEmpty()) {
								String subclass = subclasses.remove(0);
								for (IToken token : tokens) {
									if (subclass.equalsIgnoreCase(token.getRepresentation())) {
										// found the token representing the next
										// portion of the library reference
										// inside this module
										int line = token.getLineDefinition();
										int col = token.getColDefinition();
										ILocalScope localScope = moduleInfo.getModule().getLocalScope(line, col);
										tokens = localScope.getAllLocalTokens();
										break;
									}
								}
							}
						}

						if (tokens == null) {
							continue;
						}
						for (IToken token : tokens) {
							if (ModelUtil.normalizeKeywordName(candidateKeywordName, false).contains(
									ModelUtil.normalizeKeywordName(token.getRepresentation(), false))) {
								matchingToken = token;
								break;
							}
						}
					}
				} else {
					// plain keyword without module qualifiers, search across
					// libraries in referenced order
					moduleInfo = libraryModuleMap.get(libraryName);
					keywordName = candidateKeywordName;
					IToken[] tokens = null;

					if (moduleInfo != null) {
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
					}

					if (tokens == null) {
						continue;
					}
					for (IToken token : tokens) {
						if (ModelUtil.normalizeKeywordName(candidateKeywordName, false).contains(
								ModelUtil.normalizeKeywordName(token.getRepresentation(), false))) {
							matchingToken = token;
							break;
						}
					}

				}

				if (moduleInfo != null && keywordName != null && matchingToken != null) {
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

	private boolean libraryNameMatches(String importedLibraryName, String referencedLibraryName) {
		// the raw library name used in the file may differ somewhat from the
		// form
		// in a keyword reference because of substitution variables. we'll strip
		// some segments
		// and then compare the lists
		List<String> importedSegments = PathUtil.getNormalizedPathSegments(importedLibraryName);

		List<String> referencedSegments = PathUtil.getPathSegments(referencedLibraryName);
		if (importedSegments.size() != referencedSegments.size()) {
			return false;
		}

		for (int i = 0; i < importedSegments.size(); i++) {
			if (!importedSegments.get(i).equalsIgnoreCase(referencedSegments.get(i))) {
				return false;
			}
		}
		return true;
	}

}
