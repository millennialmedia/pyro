package com.millennialmedia.pyro.ui.internal.syntaxcoloring;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.swt.graphics.RGB;

import com.millennialmedia.pyro.model.Line;
import com.millennialmedia.pyro.model.RobotModel;
import com.millennialmedia.pyro.model.Step;
import com.millennialmedia.pyro.model.StepSegment;
import com.millennialmedia.pyro.model.StepSegment.SegmentType;
import com.millennialmedia.pyro.model.Table;
import com.millennialmedia.pyro.model.TableItemDefinition;
import com.millennialmedia.pyro.model.TableItemDefinition.TableItemType;
import com.millennialmedia.pyro.ui.editor.RobotFrameworkEditor;

/**
 * The scanner responsible for syntax-coloring of Robot source files.
 * 
 * @author spaxton
 */
public class Scanner implements ITokenScanner {
	class TokenWrapper {
		IToken token;
		int offset;
		int length;
	}

	private RobotFrameworkEditor editor;
	private ColorManager colorManager;
	private IDocument document;
	private Line modelLine;
	private List<TokenWrapper> pendingTokens = new LinkedList<TokenWrapper>();
	private TokenWrapper currentTokenWrapper;
	private TokenWrapper lastParsedTokenWrapper;

	public Scanner(RobotFrameworkEditor editor, ColorManager manager) {
		this.editor = editor;
		this.colorManager = manager;
	}

	@Override
	public void setRange(IDocument document, int offset, int length) {
		RobotModel model = editor.getModel();
		if (model != null) {
			this.document = document;
			modelLine = model.getFirstLine();
		}
	}

	@Override
	public IToken nextToken() {
		while (pendingTokens.isEmpty()) {
			fillPendingTokens();
		}
		currentTokenWrapper = pendingTokens.remove(0);
		return currentTokenWrapper.token;
	}

	@Override
	public int getTokenOffset() {
		return currentTokenWrapper.offset;
	}

	@Override
	public int getTokenLength() {
		return currentTokenWrapper.length;
	}

	private void fillPendingTokens() {
		if (modelLine == null) {
			// EOF
			TokenWrapper wrapper = new TokenWrapper();
			wrapper.token = Token.EOF;
			wrapper.offset = document.getLength() - 1;
			wrapper.length = 0;
			pendingTokens.add(wrapper);
			return;
		}

		// emit undefined tokens between actual contents we're looking for
		int lineStartOffset = modelLine.getLineOffset();
		addFillerTokenIfNecessary(lineStartOffset);

		// now emit specific tokens
		if (modelLine instanceof Table) {
			addToken(new Token(new TextAttribute(colorManager.getColor(ColorDefaults.TABLE))), lineStartOffset,
					((Table) modelLine).getLineLength());
		} else if (modelLine instanceof TableItemDefinition) {
			TableItemDefinition definition = (TableItemDefinition) modelLine;
			if (definition.getItemType() == TableItemType.TESTCASE) {
				addToken(new Token(new TextAttribute(colorManager.getColor(ColorDefaults.TESTCASE))), lineStartOffset,
						definition.getNameLength());
			} else if (definition.getItemType() == TableItemType.KEYWORD) {
				addToken(new Token(new TextAttribute(colorManager.getColor(ColorDefaults.KEYWORD_DEF))),
						lineStartOffset, definition.getNameLength());
			}
		} else if (modelLine instanceof Step) {
			List<StepSegment> segments = ((Step) modelLine).getSegments();
			for (StepSegment seg : segments) {
				int segmentStartOffset = lineStartOffset + seg.getOffsetInLine();
				addFillerTokenIfNecessary(segmentStartOffset);

				if (seg.getSegmentType() == SegmentType.COMMENT) {
					// comments take up the rest of the row
					addToken(new Token(new TextAttribute(colorManager.getColor(ColorDefaults.COMMENT))),
							segmentStartOffset, modelLine.getLineLength() - seg.getOffsetInLine());
				} else {
					RGB rgbColor = null;
					switch (seg.getSegmentType()) {
					case KEYWORD_CALL:
						rgbColor = ColorDefaults.KEYWORD_CALL;
						break;
					case VARIABLE:
						rgbColor = ColorDefaults.VARIABLE;
						break;
					case ARGUMENT:
						rgbColor = ColorDefaults.ARGUMENT;
						break;
					case SETTING_NAME:
						rgbColor = ColorDefaults.SETTING_NAME;
						break;
					case SETTING_VALUE:
						rgbColor = ColorDefaults.SETTING_VALUE;
						break;
					case CONTROL_ARGUMENT:
						rgbColor = ColorDefaults.CONTROL_ARGUMENT;
						break;
					case LOOP_CONSTRUCT:
						rgbColor = ColorDefaults.LOOP_CONSTRUCT;
						break;
					case CONTINUATION:
						rgbColor = ColorDefaults.CONTINUATION;
						break;
					case UNKNOWN:
						rgbColor = ColorDefaults.UNKNOWN;
						break;
					default:
						rgbColor = ColorDefaults.UNKNOWN;
						break;
					}

					addToken(new Token(new TextAttribute(colorManager.getColor(rgbColor))), segmentStartOffset, seg
							.getValue().length());
				}
			}
		}

		modelLine = modelLine.getNextLine();
	}

	private void addFillerTokenIfNecessary(int newStartOffset) {
		int previousEndOffset = 0;
		if (lastParsedTokenWrapper != null) {
			previousEndOffset = lastParsedTokenWrapper.offset + lastParsedTokenWrapper.length;
		}
		if (previousEndOffset < newStartOffset) {
			addToken(Token.UNDEFINED, previousEndOffset, newStartOffset - previousEndOffset);
		}
	}

	private void addToken(IToken token, int offset, int length) {
		TokenWrapper wrapper = new TokenWrapper();
		wrapper.token = token;
		wrapper.length = length;
		wrapper.offset = offset;

		pendingTokens.add(wrapper);
		lastParsedTokenWrapper = wrapper;
	}

}
