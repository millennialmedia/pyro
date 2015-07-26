package com.millennialmedia.pyro.internal.parser.populator;

import com.millennialmedia.pyro.internal.parser.Parser;
import com.millennialmedia.pyro.internal.parser.Row;
import com.millennialmedia.pyro.model.Line;
import com.millennialmedia.pyro.model.Step;

/**
 * Placeholder populator just so parsing functions correctly across the entire
 * file. This handles edge cases like comments or whitespace before the first
 * Robot table as well as other pathological cases for a source file with
 * in-progress modifications.
 * 
 * @author spaxton
 */
public class UnknownTablePopulator extends AbstractTablePopulator {

	@Override
	public Line populate(Row row) {
		Line line;
		if (Parser.isCommentRow(row)) {
			line = Step.COMMENT();
		} else {
			line = Line.UNKNOWN();
		}

		return line;
	}

}
