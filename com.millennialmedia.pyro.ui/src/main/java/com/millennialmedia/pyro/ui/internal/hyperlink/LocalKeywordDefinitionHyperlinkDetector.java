package com.millennialmedia.pyro.ui.internal.hyperlink;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;

import com.millennialmedia.pyro.model.TableItemDefinition;
import com.millennialmedia.pyro.model.util.ModelUtil;
import com.millennialmedia.pyro.ui.hyperlink.AbstractKeywordDefinitionHyperlinkDetector;

/**
 * Link detector for keywords within the same Robot source file.
 * 
 * @author spaxton
 */
public class LocalKeywordDefinitionHyperlinkDetector extends AbstractKeywordDefinitionHyperlinkDetector {

	@Override
	protected boolean isUserKeywordDetector() {
		return true;
	}

	@Override
	protected Map<String, KeywordDefinitionContext> getTargetKeywords() {
		Map<String, KeywordDefinitionContext> returnMap = new HashMap<String, KeywordDefinitionContext>();

		// get all the keywords defined in the local file
		Map<String, TableItemDefinition> keywordsMap = ModelUtil.getKeywords(getEditor().getModel());

		// copy them into the more generic context wrapper
		for (String keywordName : keywordsMap.keySet()) {
			TableItemDefinition definition = keywordsMap.get(keywordName);
			KeywordDefinitionContext context = new KeywordDefinitionContext();
			context.setKeywordDefinition(definition);
			returnMap.put(keywordName, context);
		}
		return returnMap;
	}

	@Override
	protected IHyperlink createLink(final int offset, final int length,
			final KeywordDefinitionContext keywordDefinitionContext) {
		return new IHyperlink() {

			@Override
			public void open() {
				// this detector only handles linking within the same editor, so
				// just reveal the target location to jump to that line
				int newSelectedLineOffset = keywordDefinitionContext.getKeywordDefinition().getLineOffset();
				getEditor().selectAndReveal(newSelectedLineOffset, 0);
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
