package com.millennialmedia.pyro.ui.internal.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

import com.millennialmedia.pyro.ui.PyroUIPlugin;
import com.millennialmedia.pyro.ui.editor.RobotFrameworkEditor;
import com.millennialmedia.pyro.ui.hyperlink.AbstractRobotHyperlinkDetector;
import com.millennialmedia.pyro.ui.internal.contentassist.ContentAssistProcessor;
import com.millennialmedia.pyro.ui.internal.registry.HyperlinkDetectorRegistryReader;
import com.millennialmedia.pyro.ui.internal.syntaxcoloring.ColorManager;
import com.millennialmedia.pyro.ui.internal.syntaxcoloring.Scanner;

/**
 * Internal configuration for the Pyro editor's syntax-coloring, hyperlinking,
 * and content-assist contributions.
 * 
 * @author spaxton
 */
public class RobotSourceViewerConfiguration extends SourceViewerConfiguration {

	private ColorManager colorManager;
	private RobotFrameworkEditor editor;
	private IContentAssistant contentAssistant;

	public RobotSourceViewerConfiguration(RobotFrameworkEditor editor, ColorManager colorManager) {
		this.editor = editor;
		this.colorManager = colorManager;
		this.contentAssistant = createContentAssistant();
	}

	@Override
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();

		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(new Scanner(editor, colorManager));
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		return reconciler;
	}

	@Override
	public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
		List<IHyperlinkDetector> detectorsList = new ArrayList<IHyperlinkDetector>();
		detectorsList.addAll(Arrays.asList(super.getHyperlinkDetectors(sourceViewer)));

		for (AbstractRobotHyperlinkDetector detector : HyperlinkDetectorRegistryReader.getReader().getDetectors()) {
			AbstractRobotHyperlinkDetector editorSpecificDetector = (AbstractRobotHyperlinkDetector) detector.clone();
			editorSpecificDetector.setEditor(editor);
			detectorsList.add(editorSpecificDetector);
		}

		return detectorsList.toArray(new IHyperlinkDetector[0]);
	}

	private ContentAssistant createContentAssistant() {
		ContentAssistant assistant = new ContentAssistant();
		assistant.enableAutoActivation(true);
		assistant.enableAutoInsert(false);
		assistant.enableColoredLabels(true);

		assistant.setAutoActivationDelay(500);
		assistant.setProposalPopupOrientation(IContentAssistant.PROPOSAL_OVERLAY);
		assistant.setRestoreCompletionProposalSize(PyroUIPlugin.getDefault().getDialogSettings());

		assistant.setContentAssistProcessor(new ContentAssistProcessor(editor), IDocument.DEFAULT_CONTENT_TYPE);
		return assistant;
	}

	@Override
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		return contentAssistant;
	}
}