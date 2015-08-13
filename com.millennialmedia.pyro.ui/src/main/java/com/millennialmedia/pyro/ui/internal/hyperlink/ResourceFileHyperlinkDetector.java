package com.millennialmedia.pyro.ui.internal.hyperlink;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

import com.millennialmedia.pyro.model.Line;
import com.millennialmedia.pyro.model.Step;
import com.millennialmedia.pyro.model.Step.StepType;
import com.millennialmedia.pyro.model.StepSegment;
import com.millennialmedia.pyro.model.StepSegment.SegmentType;
import com.millennialmedia.pyro.ui.editor.RobotFrameworkEditor;
import com.millennialmedia.pyro.ui.editor.util.PathUtil;
import com.millennialmedia.pyro.ui.hyperlink.AbstractRobotHyperlinkDetector;

/**
 * Link detector for opening external source files referenced by a Resource
 * setting.
 * 
 * @author spaxton
 */
public class ResourceFileHyperlinkDetector extends AbstractRobotHyperlinkDetector {
	public class ResourceFileReferenceContext {
		private String resourceName;
		private int resourceStartOffset;

		public String getResourceName() {
			return resourceName;
		}

		public void setResourceName(String resourceName) {
			this.resourceName = resourceName;
		}

		public int getResourceStartOffset() {
			return resourceStartOffset;
		}

		public void setResourceStartOffset(int resourceStartOffset) {
			this.resourceStartOffset = resourceStartOffset;
		}
	}

	@Override
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
		ResourceFileReferenceContext resourceRef = getTargetResourceReference(region);
		if (resourceRef != null) {
			IResource resource = PathUtil.getResourceForPath(PathUtil.getEditorFile(getEditor()), resourceRef.getResourceName());
			if (resource != null && resource.exists()) {
				return new IHyperlink[] { createLink(resourceRef.getResourceStartOffset(), resourceRef
						.getResourceName().length(), resource) };
			}
		}
		return null;
	}

	private ResourceFileReferenceContext getTargetResourceReference(IRegion region) {
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
					if (segment.getSegmentType() == SegmentType.SETTING_NAME
							&& !segment.getValue().equalsIgnoreCase("Resource")) {
						return null;
					}
					if (segment.getSegmentType() == SegmentType.SETTING_VALUE
							&& lineOffset + segment.getOffsetInLine() <= linkOffset
							&& lineOffset + segment.getOffsetInLine() + segment.getValue().length() >= linkOffset) {

						ResourceFileReferenceContext reference = new ResourceFileReferenceContext();
						reference.setResourceName(segment.getValue());
						reference.setResourceStartOffset(lineOffset + segment.getOffsetInLine());
						return reference;
					}
				}
			}
			line = line.getNextLine();
		}
		return null;
	}

	private IHyperlink createLink(final int startOffset, final int length, final IResource resource) {
		return new IHyperlink() {

			@Override
			public void open() {
				try {
					IDE.openEditor(getEditor().getSite().getPage(), (IFile) resource, RobotFrameworkEditor.EDITOR_ID);
				} catch (PartInitException e) {
				}
			}

			@Override
			public String getTypeLabel() {
				return null;
			}

			@Override
			public String getHyperlinkText() {
				return resource.getName();
			}

			@Override
			public IRegion getHyperlinkRegion() {
				return new Region(startOffset, length);
			}
		};

	}

}
