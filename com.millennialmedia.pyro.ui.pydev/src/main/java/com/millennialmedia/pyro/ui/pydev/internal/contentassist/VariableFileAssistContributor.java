package com.millennialmedia.pyro.ui.pydev.internal.contentassist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.viewers.StyledString;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IToken;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.visitors.scope.EasyASTIteratorVisitor;

import com.millennialmedia.pyro.model.ModelManager;
import com.millennialmedia.pyro.model.RobotModel;
import com.millennialmedia.pyro.model.util.ModelUtil;
import com.millennialmedia.pyro.ui.PyroUIPlugin;
import com.millennialmedia.pyro.ui.contentassist.RobotCompletionProposal;
import com.millennialmedia.pyro.ui.contentassist.VariableNameAssistContributorBase;
import com.millennialmedia.pyro.ui.editor.RobotFrameworkEditor;
import com.millennialmedia.pyro.ui.editor.util.PathUtil;
import com.millennialmedia.pyro.ui.pydev.internal.LibraryInfo;
import com.millennialmedia.pyro.ui.pydev.internal.ModuleInfo;
import com.millennialmedia.pyro.ui.pydev.internal.PyDevUtil;

/**
 * Content assist contributor for python variable files imported via the Settings table.
 * 
 * @author spaxton
 */
public class VariableFileAssistContributor extends VariableNameAssistContributorBase {

	public VariableFileAssistContributor() {
		setProposalImage(PyroUIPlugin.getDefault().getImageRegistry().get(PyroUIPlugin.IMAGE_VARIABLE));
	}

	@Override
	public void computeCompletionProposals(ITextViewer viewer, int offset, List<RobotCompletionProposal> proposals) {
		String variableBeginning = extractVariableBeginning(offset, viewer);
		if (variableBeginning == null) {
			return;
		}

		// find variable files
		List<String> variableFilePaths = collectReferencedVariableFilePaths(getEditor());
		LibraryInfo libraryInfo = PyDevUtil.findModules(variableFilePaths, PathUtil.getEditorFile(getEditor()));
		
		for (String path : variableFilePaths) {
			Collection<ModuleInfo> infos = libraryInfo.getModuleMap().get(path);
			if (infos == null || infos.isEmpty()) {
				continue;
			}
			
			ModuleInfo info  = infos.iterator().next();
			
			final List<String> varNames = new ArrayList<String>();
			
			IToken[] tokens = info.getModule().getGlobalTokens();
			for (final IToken token : tokens) {
				if (token.getType() == IToken.TYPE_ATTR && token instanceof SourceToken) {
					if ("__all__".equals(token.getRepresentation())) {
						// just take the given var names from the array assignment and ignore everything else in the file
						varNames.clear();
						
						IModule module = info.getModule();
						if (module instanceof SourceModule) {
							SimpleNode node = ((SourceModule) module).getAst();
							try {
								// go traverse the whole AST - we're looking to match the token we just found
								node.traverse(new EasyASTIteratorVisitor() {
									@Override
									public Object visitAssign(Assign node) throws Exception {
										for (exprType expr : node.targets) {
											if (expr == ((SourceToken) token).getAst()) {
												// we've found the __all__ assignment, now find the value
												exprType valueExpr = node.value;
												if (valueExpr instanceof org.python.pydev.parser.jython.ast.List) {
													org.python.pydev.parser.jython.ast.List list = (org.python.pydev.parser.jython.ast.List) valueExpr;
													for (exprType listMember: list.elts) {
														// add each variable
														if (listMember instanceof Str) {
															varNames.add("${" + ((Str) listMember).s + "}");
														}
													}
												}
											}
										}
										return super.visitAssign(node);
									}
								});
							} catch (Exception e) {
							}
						}
						
						break;
					} else if (token.getRepresentation().startsWith("list__")) {
						varNames.add("@{" + token.getRepresentation() + "}");
					} else {
						varNames.add("${" + token.getRepresentation() + "}");
					}
				}
			}
		
			Collections.sort(varNames);
			
			createProposals(varNames, offset, variableBeginning, proposals, info.getModule().getFile().getName());
		}
	}

	protected void createProposals(List<String> varNames, 
			int offset, 
			String variableBeginning,
			List<RobotCompletionProposal> proposals,
			String variableFileName) {
		for (String varName : varNames) {
			if (varName.startsWith(variableBeginning)) {
				StyledString styledDisplayString = new StyledString();
				styledDisplayString.append(varName,
						RobotCompletionProposal.FOREGROUND_STYLER);
				styledDisplayString.append(" - " + variableFileName,
						RobotCompletionProposal.QUALIFIER_STYLER);

				addCompletionProposal(proposals, varName, offset - variableBeginning.length(),
						variableBeginning.length(), varName.length(), varName, styledDisplayString);
			}
		}
	}

	
	public static List<String> collectReferencedVariableFilePaths(RobotFrameworkEditor editor) {
		List<String> filePaths = new ArrayList<String>();

		IFile file = PathUtil.getEditorFile(editor);
		if (file == null) {
			return filePaths;
		}

		// add any variable files referenced from this local file
		List<String> variableFilePaths = ModelUtil.getVariableFilePaths(editor.getModel());
		for (String varPath : variableFilePaths) {
			if (!filePaths.contains(varPath)) {
				filePaths.add(varPath);
			}
		}
		
		List<String> resourceFilePaths = ModelUtil.getResourceFilePaths(editor.getModel());
		if (!resourceFilePaths.isEmpty()) {
			IPath rootPath = PathUtil.getRootPath(editor);
			if (rootPath != null) {
				// for each resource file in the Settings table(s)
				for (String resourceFilePath : resourceFilePaths) {
					addIndirectlyReferencedFiles(filePaths, file, resourceFilePath);
				}
			}
		}
		return filePaths;
	}
	
	public static void addIndirectlyReferencedFiles(List<String> filePaths, IFile localFile, String resourceFilePath) {
		// add any variable files referenced from this local file
		List<String> variableFilePaths = ModelUtil.getVariableFilePaths(ModelManager.getManager().getModel(localFile));
		for (String varPath : variableFilePaths) {
			if (!filePaths.contains(varPath)) {
				filePaths.add(varPath);
			}
		}
		
		// now find any resource files and recursively check against those too
		IResource resource = PathUtil.getResourceForPath(localFile, resourceFilePath);
		if (resource != null && resource instanceof IFile) {
			IFile targetFile = (IFile) resource;
			RobotModel targetModel = ModelManager.getManager().getModel(targetFile);

			// repeat for any files contained within this model (transitive resource file imports)
			List<String> resourceFilePaths = ModelUtil.getResourceFilePaths(targetModel);
			if (!resourceFilePaths.isEmpty()) {
				for (String transitiveResourceFilePath : resourceFilePaths) {
					addIndirectlyReferencedFiles(filePaths, targetFile, transitiveResourceFilePath);
				}
			}
		}
	}
	
}
