package com.millennialmedia.pyro.ui.pydev.internal.hyperlink;

import java.util.Collection;
import java.util.List;

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

import com.millennialmedia.pyro.model.Line;
import com.millennialmedia.pyro.model.Step;
import com.millennialmedia.pyro.model.Step.StepType;
import com.millennialmedia.pyro.model.StepSegment;
import com.millennialmedia.pyro.model.StepSegment.SegmentType;
import com.millennialmedia.pyro.ui.editor.util.PathUtil;
import com.millennialmedia.pyro.ui.hyperlink.AbstractRobotHyperlinkDetector;
import com.millennialmedia.pyro.ui.pydev.internal.LibraryInfo;
import com.millennialmedia.pyro.ui.pydev.internal.ModuleInfo;
import com.millennialmedia.pyro.ui.pydev.internal.PyDevUtil;
import com.millennialmedia.pyro.ui.pydev.internal.contentassist.VariableFileAssistContributor;

/**
 * Hyperlink detector for external Python libraries referenced by the Library
 * setting or Variables setting in Robot source files.
 * 
 * @author spaxton
 */
public class PythonLibraryHyperlinkDetector extends AbstractRobotHyperlinkDetector {
	public class LibraryReferenceContext {
		private String libraryName;
		private int libraryStartOffset;

		public String getLibraryName() {
			return libraryName;
		}

		public void setLibraryName(String libraryName) {
			this.libraryName = libraryName;
		}

		public int getLibraryStartOffset() {
			return libraryStartOffset;
		}

		public void setLibraryStartOffset(int libraryStartOffset) {
			this.libraryStartOffset = libraryStartOffset;
		}
	}

	public final LibraryReferenceContext getLibraryReferenceContext(IRegion region) {
		int linkOffset = region.getOffset();

		// advance to the line containing the possible link
		Line line = getEditor().getModel().getFirstLine();
		while (line.getLineOffset() + line.getLineLength() < linkOffset) {
			line = line.getNextLine();
		}

		// now walk through each step in the line to see if a keyword is the
		// link origin
		// since some parser scenarios can have two line objects sharing a
		// single document line, keep searching unless we've run past the target
		// offset
		while (line != null && line.getLineOffset() <= linkOffset) {
			if (line instanceof Step && ((Step) line).getStepType() == StepType.SETTING) {
				int lineOffset = line.getLineOffset();
				for (StepSegment segment : ((Step) line).getSegments()) {
					if (segment.getSegmentType() == SegmentType.SETTING_NAME && 
							!(segment.getValue().equalsIgnoreCase("Library") ||
							segment.getValue().equalsIgnoreCase("Variables"))) {
						return null;
					}
					if (segment.getSegmentType() == SegmentType.SETTING_VALUE
							&& lineOffset + segment.getOffsetInLine() <= linkOffset
							&& lineOffset + segment.getOffsetInLine() + segment.getValue().length() >= linkOffset) {

						LibraryReferenceContext context = new LibraryReferenceContext();
						context.setLibraryName(segment.getValue());
						context.setLibraryStartOffset(lineOffset + segment.getOffsetInLine());
						return context;
					}
				}
			}
			line = line.getNextLine();
		}
		return null;
	}

	@Override
	public IHyperlink[] detectHyperlinks(final ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
		LibraryReferenceContext libraryReferenceContext = getLibraryReferenceContext(region);
		if (libraryReferenceContext == null) {
			return null;
		}

		ModuleInfo moduleInfo = getModuleInfo(libraryReferenceContext.getLibraryName());
		if (moduleInfo == null) {
			return null;
		}

		List<String> subclasses = PathUtil.getPathSegments(moduleInfo.getRemainingPath());
		IToken[] tokens = moduleInfo.getModule().getGlobalTokens();
		IToken targetToken = null;
		while (!subclasses.isEmpty()) {
			String subclass = subclasses.remove(0);
			for (IToken token : tokens) {
				if (subclass.equalsIgnoreCase(token.getRepresentation())) {
					// found the token representing the next portion of the
					// library reference inside this module
					int line = token.getLineDefinition();
					int col = token.getColDefinition();
					ILocalScope localScope = moduleInfo.getModule().getLocalScope(line, col);
					targetToken = token;
					tokens = localScope.getAllLocalTokens();
					break;
				}
			}
		}

		if (targetToken != null) {
			return new IHyperlink[] { createLink(libraryReferenceContext.libraryStartOffset,
					libraryReferenceContext.libraryName.length(), moduleInfo, targetToken) };
		}

		// otherwise we're pointing at a whole module file to just link to the
		// beginning of the file
		return new IHyperlink[] { createLink(libraryReferenceContext.libraryStartOffset,
				libraryReferenceContext.libraryName.length(), moduleInfo, 0, 0) };
	}

	protected ModuleInfo getModuleInfo(String libraryName) {
		LibraryInfo libraryInfo = PyDevUtil.getNonBuiltInLibraryModules(getEditor());
		
		LibraryInfo builtInLibraryInfo = PyDevUtil.getBuiltInLibraryModules(getEditor());
		libraryInfo.getOrderedLibraries().addAll(builtInLibraryInfo.getOrderedLibraries());
		libraryInfo.getModuleMap().putAll(builtInLibraryInfo.getModuleMap());
		
		List<String> variableFilePaths = VariableFileAssistContributor.collectReferencedVariableFilePaths(getEditor());
		LibraryInfo variableFilesLibraryInfo = PyDevUtil.findModules(variableFilePaths, PathUtil.getEditorFile(getEditor()));
		libraryInfo.getOrderedLibraries().addAll(variableFilesLibraryInfo.getOrderedLibraries());
		libraryInfo.getModuleMap().putAll(variableFilesLibraryInfo.getModuleMap());
		
		Collection<ModuleInfo> moduleInfos = libraryInfo.getModuleMap().get(libraryName);
		if (moduleInfos != null && !moduleInfos.isEmpty()) {
			return moduleInfos.iterator().next();
		}
		return null;
	}

	protected IHyperlink createLink(final int offset, final int length, final ModuleInfo moduleInfo,
			final IToken targetToken) {
		return createLink(offset, length, moduleInfo, targetToken.getLineDefinition() - 1,
				targetToken.getColDefinition() - 1);
	}

	protected IHyperlink createLink(final int offset, final int length, final ModuleInfo moduleInfo,
			final int targetLine, final int targetCol) {
		final ItemPointer pointer = new ItemPointer(moduleInfo.getModule().getFile(), new Location(targetLine,
				targetCol), new Location(targetLine, targetCol));
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
				return new Region(offset, length);
			}
		};
		return link;
	}

}
