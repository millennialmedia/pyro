package com.millennialmedia.pyro.ui.editor;

import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import com.millennialmedia.pyro.model.ModelManager;
import com.millennialmedia.pyro.model.RobotModel;
import com.millennialmedia.pyro.ui.internal.editor.RobotSourceViewerConfiguration;
import com.millennialmedia.pyro.ui.internal.outline.RobotOutlinePage;
import com.millennialmedia.pyro.ui.internal.syntaxcoloring.ColorManager;

/**
 * Entry point for the editor UI.
 * 
 * @author spaxton
 */
public class RobotFrameworkEditor extends TextEditor {
	class RobotDocumentProvider extends FileDocumentProvider {
		private RobotFrameworkEditor editor;

		RobotDocumentProvider(RobotFrameworkEditor editor) {
			this.editor = editor;
		}

		protected IDocument createDocument(Object element) throws CoreException {
			IDocument document = super.createDocument(element);

			document.addDocumentListener(new IDocumentListener() {
				@Override
				public void documentChanged(DocumentEvent event) {
					editor.documentChanged();
				}

				@Override
				public void documentAboutToBeChanged(DocumentEvent event) {
				}
			});
			return document;
		}

	}

	public static final String EDITOR_ID = "com.millennialmedia.pyro.ui.editor.RobotFrameworkEditor";

	private ColorManager colorManager;
	private RobotOutlinePage outlinePage;
	private RobotModel model;
	private static Map<IDocument, RobotModel> docToModelMap = new WeakHashMap<IDocument, RobotModel>();

	public RobotFrameworkEditor() {
		super();
		colorManager = new ColorManager();
		setSourceViewerConfiguration(new RobotSourceViewerConfiguration(this, colorManager));
		setDocumentProvider(new RobotDocumentProvider(this));
	}

	public void dispose() {
		colorManager.dispose();
		super.dispose();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Class adapter) {
		if (IContentOutlinePage.class.equals(adapter)) {
			return getOutlinePage();
		}

		return super.getAdapter(adapter);
	}

	private synchronized RobotOutlinePage getOutlinePage() {
		if (outlinePage == null) {
			outlinePage = new RobotOutlinePage(getModel(), this);
		}
		return outlinePage;
	}

	public RobotModel getModel() {
		if (model == null) {
			IEditorInput input = getEditorInput();
			IDocument document = getDocumentProvider().getDocument(input);
			if (input instanceof IFileEditorInput) {
				IFile file = ((IFileEditorInput) input).getFile();
				model = ModelManager.getManager().getModel(file, document.get());
				docToModelMap.put(document, model);
			}
		}
		return model;
	}

	public ISourceViewer getViewer() {
		return getSourceViewer();
	}

	void documentChanged() {
		ModelManager.getManager().reparse(getModel(), getViewer().getDocument().get());
		getOutlinePage().refresh();
	}

	public static RobotModel getModelFor(IDocument document) {
		return docToModelMap.get(document);
	}

}
