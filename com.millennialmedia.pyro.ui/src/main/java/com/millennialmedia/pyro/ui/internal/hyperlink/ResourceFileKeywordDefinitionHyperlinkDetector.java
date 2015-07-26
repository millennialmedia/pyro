package com.millennialmedia.pyro.ui.internal.hyperlink;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

import com.millennialmedia.pyro.model.ModelManager;
import com.millennialmedia.pyro.model.RobotModel;
import com.millennialmedia.pyro.model.TableItemDefinition;
import com.millennialmedia.pyro.model.util.ModelUtil;
import com.millennialmedia.pyro.ui.editor.RobotFrameworkEditor;
import com.millennialmedia.pyro.ui.editor.util.PathUtil;
import com.millennialmedia.pyro.ui.hyperlink.AbstractKeywordDefinitionHyperlinkDetector;

/**
 * Link detector for keywords defined in external Robot source files referenced
 * by a Resource setting.
 * 
 * @author spaxton
 */
public class ResourceFileKeywordDefinitionHyperlinkDetector extends AbstractKeywordDefinitionHyperlinkDetector {
	public class ResourceFileKeywordDefinitionContext extends KeywordDefinitionContext {
		private IFile file;

		public IFile getFile() {
			return file;
		}

		public void setFile(IFile file) {
			this.file = file;
		}
	}

	@Override
	protected boolean isUserKeywordDetector() {
		return true;
	}

	@Override
	protected Map<String, KeywordDefinitionContext> getTargetKeywords() {
		Map<String, KeywordDefinitionContext> targetKeywords = new HashMap<String, KeywordDefinitionContext>();

		// get any files referenced by a Resource setting
		List<String> resourceFilePaths = ModelUtil.getResourceFilePaths(getEditor().getModel());

		if (!resourceFilePaths.isEmpty()) {
			IFile file = PathUtil.getEditorFile(getEditor());
			if (file == null) {
				return targetKeywords;
			}

			IPath rootPath = PathUtil.getRootPath(getEditor());

			if (rootPath != null) {
				// for each resource file in the Settings table(s)
				for (String resourceFilePath : resourceFilePaths) {
					IResource resource = PathUtil.getResourceForPath(getEditor(), resourceFilePath);

					if (resource != null && resource instanceof IFile) {
						IFile targetFile = (IFile) resource;
						RobotModel targetModel = ModelManager.getManager().getModel(targetFile);
						// find any keyword definitions in this external file
						Map<String, TableItemDefinition> keywordsMap = ModelUtil.getKeywords(targetModel);

						// and copy them into our specific context subclass in
						// order to remember the containing file
						for (String keywordName : keywordsMap.keySet()) {
							TableItemDefinition definition = keywordsMap.get(keywordName);
							ResourceFileKeywordDefinitionContext context = new ResourceFileKeywordDefinitionContext();
							context.setKeywordDefinition(definition);
							context.setFile(targetFile);
							targetKeywords.put(keywordName, context);
						}
					}
				}
			}
		}

		return targetKeywords;
	}

	@Override
	protected IHyperlink createLink(final int offset, final int length,
			final KeywordDefinitionContext keywordDefinitionContext) {
		return new IHyperlink() {

			@Override
			public void open() {
				ResourceFileKeywordDefinitionContext context = (ResourceFileKeywordDefinitionContext) keywordDefinitionContext;
				try {
					IEditorPart editor = IDE.openEditor(getEditor().getSite().getPage(), context.getFile(),
							RobotFrameworkEditor.EDITOR_ID);
					((RobotFrameworkEditor) editor).selectAndReveal(context.getKeywordDefinition().getLineOffset(), 0);
				} catch (PartInitException e) {
				}
			}

			@Override
			public String getTypeLabel() {
				return null;
			}

			@Override
			public String getHyperlinkText() {
				return keywordDefinitionContext.getKeywordDefinition().getName();
			}

			@Override
			public IRegion getHyperlinkRegion() {
				return new IRegion() {

					@Override
					public int getOffset() {
						return offset;
					}

					@Override
					public int getLength() {
						return length;
					}
				};
			}
		};

	}

}
