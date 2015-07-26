package com.millennialmedia.pyro.ui.internal.contentassist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

import com.millennialmedia.pyro.ui.contentassist.ContentAssistContributorBase;
import com.millennialmedia.pyro.ui.contentassist.RobotCompletionProposal;
import com.millennialmedia.pyro.ui.editor.RobotFrameworkEditor;
import com.millennialmedia.pyro.ui.internal.registry.ContentAssistProcessorRegistryReader;

/**
 * The internal content-assist processor for the Robot editor. It purely
 * delegates to client contributors.
 * 
 * @author spaxton
 */
public final class ContentAssistProcessor implements IContentAssistProcessor {
	private Collection<ContentAssistContributorBase> processorDelegates;

	public ContentAssistProcessor(RobotFrameworkEditor editor) {
		processorDelegates = new ArrayList<ContentAssistContributorBase>();
		for (ContentAssistContributorBase processorDelegate : ContentAssistProcessorRegistryReader.getReader()
				.getProcessors()) {
			ContentAssistContributorBase editorSpecificDelegate = (ContentAssistContributorBase) processorDelegate
					.clone();
			editorSpecificDelegate.setEditor(editor);
			processorDelegates.add(editorSpecificDelegate);
		}
		;
	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		List<RobotCompletionProposal> proposalsList = new ArrayList<RobotCompletionProposal>();
		for (ContentAssistContributorBase processor : processorDelegates) {
			try {
				processor.computeCompletionProposals(viewer, offset, proposalsList);
			} catch (Exception e) {
				// e.printStackTrace();
			}
		}

		return proposalsList.toArray(new ICompletionProposal[0]);
	}

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		return null;
	}

	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		return null;
	}

	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	@Override
	public String getErrorMessage() {
		return null;
	}

	@Override
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

}
