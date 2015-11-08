package com.millennialmedia.pyro.ui.pydev.internal.hyperlink;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

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

import com.google.common.collect.Multimap;
import com.millennialmedia.pyro.model.util.ModelUtil;
import com.millennialmedia.pyro.ui.editor.util.PathUtil;
import com.millennialmedia.pyro.ui.hyperlink.AbstractKeywordDefinitionHyperlinkDetector;
import com.millennialmedia.pyro.ui.pydev.internal.ModuleInfo;

/**
 * Base class for hyperlink detection of keywords implemented in Python libraries.
 * 
 * @author spaxton
 */
public abstract class AbstractPythonLibraryKeywordDefinitionHyperlinkDetector extends AbstractKeywordDefinitionHyperlinkDetector {

	@Override
	protected boolean isUserKeywordDetector() {
		return false;
	}

	protected abstract Multimap<String, ModuleInfo> getLibraryModules();

	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
		final KeywordCallContext keywordCallContext = getKeywordCallContext(region);
		if (keywordCallContext == null) {
			return null;
		}

		final Map<String, Integer> candidateCallStrings = 
				ModelUtil.getCandidateKeywordStrings(keywordCallContext.getKeywordName());

		Multimap<String, ModuleInfo> libraryModuleMap = getLibraryModules();

		for (final String candidateKeywordName : candidateCallStrings.keySet()) {
			Collection<ModuleInfo> moduleInfos = null;
			String keywordName = null;
			IToken matchingToken = null;
			Map<IToken, ModuleInfo> tokenToModuleInfoMap = new HashMap<IToken, ModuleInfo>();

			for (String libraryName : libraryModuleMap.keySet()) {
				if (candidateKeywordName.contains(".")) {
					// break into segments and look up against the module map
					List<IToken> tokens = new CopyOnWriteArrayList<IToken>();
					String referencedLibraryName = ModelUtil.getLibraryAlias(getEditor().getModel(), libraryName);

					if (libraryNameMatches(referencedLibraryName,
							candidateKeywordName.substring(0, candidateKeywordName.lastIndexOf(".")))) {
						moduleInfos = libraryModuleMap.get(libraryName);

						String[] segments = candidateKeywordName.split("\\.");
						keywordName = segments[segments.length - 1];

						if (moduleInfos != null) {
							for (ModuleInfo moduleInfo : moduleInfos) {
								List<String> subclasses = PathUtil.getPathSegments(moduleInfo.getRemainingPath());
								List<IToken> tokenList = Arrays.asList(moduleInfo.getModule().getGlobalTokens());
								tokens.addAll(tokenList);
								for (IToken copyToken : tokenList) {
									tokenToModuleInfoMap.put(copyToken, moduleInfo);
								}
								
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
											tokenList = Arrays.asList(localScope.getAllLocalTokens());
											tokens.addAll(tokenList);
											for (IToken copyToken : tokenList) {
												tokenToModuleInfoMap.put(copyToken, moduleInfo);
											}
											break;
										}
									}
								}
							}
						}

						if (tokens.isEmpty()) {
							continue;
						}
						
						for (IToken token : tokens) {
							if (!token.getRepresentation().startsWith("_") &&
									ModelUtil.normalizeKeywordName(candidateKeywordName, false)
									.endsWith(ModelUtil.normalizeKeywordName(token.getRepresentation(), false))
								) {
								matchingToken = token;
								break;
							}
						}
						
						if (matchingToken == null) {
							// haven't found anything yet - check for a Class of the same name defined inside this module and repeat 
							for (IToken token : tokens) {
								for (ModuleInfo moduleInfo : moduleInfos) {
									if (moduleInfo.getModule().getName().endsWith(token.getRepresentation())) {
										int line = token.getLineDefinition();
										int col = token.getColDefinition();
										ILocalScope localScope = moduleInfo.getModule().getLocalScope(line, col);

										List<IToken> tokenList = Arrays.asList(localScope.getAllLocalTokens());
										tokens.addAll(tokenList);
										for (IToken copyToken : tokenList) {
											tokenToModuleInfoMap.put(copyToken, moduleInfo);
										}
										break;
									}
								}
							}

							if (tokens.isEmpty()) {
								continue;
							}
							
							for (IToken token : tokens) {
								if (!token.getRepresentation().startsWith("_") &&
										ModelUtil.normalizeKeywordName(candidateKeywordName, false)
										.endsWith(ModelUtil.normalizeKeywordName(token.getRepresentation(), false))
									) {
									matchingToken = token;
									break;
								}
							}
						}
					}
				} else {
					// plain keyword without module qualifiers, search across
					// libraries in referenced order
					moduleInfos = libraryModuleMap.get(libraryName);
					keywordName = candidateKeywordName;
					List<IToken> tokens = new CopyOnWriteArrayList<IToken>();

					if (moduleInfos != null) {
						for (ModuleInfo moduleInfo : moduleInfos) {
							List<String> subclasses = PathUtil.getPathSegments(moduleInfo.getRemainingPath());
							List<IToken> tokenList = Arrays.asList(moduleInfo.getModule().getGlobalTokens());
							tokens.addAll(tokenList);
							for (IToken copyToken : tokenList) {
								tokenToModuleInfoMap.put(copyToken, moduleInfo);
							}
							
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
										tokenList = Arrays.asList(localScope.getAllLocalTokens());
										tokens.addAll(tokenList);
										for (IToken copyToken : tokenList) {
											tokenToModuleInfoMap.put(copyToken, moduleInfo);
										}
										break;
									}
								}
							}
						}
					}

					if (tokens.isEmpty()) {
						continue;
					}
					
					for (IToken token : tokens) {
						if (!token.getRepresentation().startsWith("_") &&
								ModelUtil.normalizeKeywordName(candidateKeywordName, false)
								.endsWith(ModelUtil.normalizeKeywordName(token.getRepresentation(), false))
							) {
							matchingToken = token;
							break;
						}
					}

					if (matchingToken == null) {
						// haven't found anything yet - check for a Class of the same name defined inside this module and repeat 
						for (IToken token : tokens) {
							for (ModuleInfo moduleInfo : moduleInfos) {
								if (moduleInfo.getModule().getName().endsWith(token.getRepresentation())) {
									int line = token.getLineDefinition();
									int col = token.getColDefinition();
									ILocalScope localScope = moduleInfo.getModule().getLocalScope(line, col);

									List<IToken> tokenList = Arrays.asList(localScope.getAllLocalTokens());
									tokens.addAll(tokenList);
									for (IToken copyToken : tokenList) {
										tokenToModuleInfoMap.put(copyToken, moduleInfo);
									}
									break;
								}
							}
						}

						if (tokens.isEmpty()) {
							continue;
						}
						
						for (IToken token : tokens) {
							if (!token.getRepresentation().startsWith("_") &&
									ModelUtil.normalizeKeywordName(candidateKeywordName, false)
									.endsWith(ModelUtil.normalizeKeywordName(token.getRepresentation(), false))
								) {
								matchingToken = token;
								break;
							}
						}
					}
				}

				if (keywordName != null && matchingToken != null) {
					ModuleInfo matchingModuleInfo = tokenToModuleInfoMap.get(matchingToken);
					if (matchingModuleInfo != null) {
						final ItemPointer pointer = new ItemPointer(matchingModuleInfo.getModule().getFile(), new Location(
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
