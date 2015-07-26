package com.millennialmedia.pyro.ui.hyperlink;

import java.util.Map;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.IHyperlink;

import com.millennialmedia.pyro.model.Line;
import com.millennialmedia.pyro.model.Step;
import com.millennialmedia.pyro.model.StepSegment;
import com.millennialmedia.pyro.model.StepSegment.SegmentType;
import com.millennialmedia.pyro.model.util.ModelUtil;
import com.millennialmedia.pyro.model.TableItemDefinition;

/**
 * Base class for hyperlink detection specifically for Robot keywords.
 * Subclasses must implement {@link #detectHyperlinks} and {@link #createLink}
 * 
 * @author spaxton
 */
public abstract class AbstractKeywordDefinitionHyperlinkDetector extends AbstractRobotHyperlinkDetector {
	public class KeywordDefinitionContext {
		private TableItemDefinition keywordDefinition;

		public TableItemDefinition getKeywordDefinition() {
			return keywordDefinition;
		}

		public void setKeywordDefinition(TableItemDefinition keywordDefinition) {
			this.keywordDefinition = keywordDefinition;
		}
	}

	public class KeywordCallContext {
		private String keywordName;
		private int keywordStartOffset;

		public String getKeywordName() {
			return keywordName;
		}

		public void setKeywordName(String keywordName) {
			this.keywordName = keywordName;
		}

		public int getKeywordStartOffset() {
			return keywordStartOffset;
		}

		public void setKeywordStartOffset(int keywordStartOffset) {
			this.keywordStartOffset = keywordStartOffset;
		}
	}

	public final KeywordCallContext getKeywordCallContext(IRegion region) {
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
			if (line instanceof Step) {
				int lineOffset = line.getLineOffset();
				for (StepSegment segment : ((Step) line).getSegments()) {
					if (segment.getSegmentType() == SegmentType.KEYWORD_CALL
							&& lineOffset + segment.getOffsetInLine() <= linkOffset
							&& lineOffset + segment.getOffsetInLine() + segment.getValue().length() >= linkOffset) {

						KeywordCallContext context = new KeywordCallContext();
						context.setKeywordName(segment.getValue());
						context.setKeywordStartOffset(lineOffset + segment.getOffsetInLine());
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
		KeywordCallContext keywordCallContext = getKeywordCallContext(region);

		if (keywordCallContext != null) {
			// get the candidate keywords that could be the target of this call
			Map<String, KeywordDefinitionContext> targetKeywordsMap = getTargetKeywords();

			// get the keyword call strings that need to be checked (handles BDD
			// prefixing)
			Map<String, Integer> candidateCallStrings = ModelUtil.getCandidateKeywordStrings(keywordCallContext
					.getKeywordName());

			for (String keyword : targetKeywordsMap.keySet()) {
				// for keyword definitions that embed variables, replace the
				// variables with a generic regex to match against
				String targetKeywordName = ModelUtil.normalizeKeywordName(keyword, isUserKeywordDetector());

				// check each candidate string for a matching keyword definition
				for (Map.Entry<String, Integer> candidate : candidateCallStrings.entrySet()) {
					// normalize the check to lower case because keywords are
					// case-insensitive
					if (ModelUtil.normalizeKeywordName(candidate.getKey(), false).matches(targetKeywordName)) {
						return new IHyperlink[] { createLink(
								keywordCallContext.getKeywordStartOffset() + candidate.getValue(), candidate.getKey()
										.length(), targetKeywordsMap.get(keyword)) };
					}
				}
			}
		}
		return null;
	}

	protected abstract boolean isUserKeywordDetector();

	protected Map<String, KeywordDefinitionContext> getTargetKeywords() {
		return null;
	};

	protected IHyperlink createLink(final int offset, final int length,
			final KeywordDefinitionContext keywordDefinitionContext) {
		return null;
	}

}
