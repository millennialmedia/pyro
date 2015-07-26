package com.millennialmedia.pyro.internal.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Reader class for tab-separated file format.
 * 
 * @author spaxton
 */
public class TsvReader extends AbstractReader {
	private static final String TAB = "\t";

	@Override
	protected List<Cell> splitRow(String row) {
		if (row == null) {
			return Collections.emptyList();
		}
		List<Cell> cells = new ArrayList<Cell>();
		int offset = 0;

		StringTokenizer tokenizer = new StringTokenizer(row, TAB, true);
		boolean firstToken = true;
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (!TAB.equals(token)) {
				cells.add(createCell(token.trim(), offset + token.indexOf(token.trim())));
			} else {
				if (firstToken) {
					cells.add(createCell("", offset));
				}
			}
			firstToken = false;
			offset += token.length();
		}

		return cells;
	}

	private Cell createCell(String token, int offset) {
		Cell newCell = new Cell();
		newCell.setOffsetInRow(offset);
		newCell.setParsedContents(token);
		return newCell;
	}

}
