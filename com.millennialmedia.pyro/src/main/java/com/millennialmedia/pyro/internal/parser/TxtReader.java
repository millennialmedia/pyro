package com.millennialmedia.pyro.internal.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Reader class for space-separated file format.
 * 
 * @author spaxton
 */
public class TxtReader extends AbstractReader {
	private static final String TAB = "\t";
	private static final Pattern SPACE_PATTERN = Pattern.compile(" {2,}");
	private static final Pattern PIPE_PATTERN = Pattern.compile(" \\|(?= )");

	@Override
	protected List<Cell> splitRow(String row) {
		if (row == null) {
			return Collections.emptyList();
		}

		String processedRow = row.replace(TAB, "  ");

		String[] rawCells = null;
		if (row.startsWith("| ")) {
			if (processedRow.endsWith(" |")) {
				processedRow = processedRow.substring(1, processedRow.length() - 2);
			} else {
				processedRow = processedRow.substring(1);
			}
			rawCells = PIPE_PATTERN.split(processedRow);
		} else {
			rawCells = SPACE_PATTERN.split(processedRow);
		}

		List<Cell> cells = new ArrayList<Cell>();
		int offset = 0;
		processedRow = row;
		for (int i = 0; i < rawCells.length; i++) {
			String rawCell = rawCells[i];
			offset = offset + processedRow.indexOf(rawCell);
			cells.add(createCell(rawCell, offset));
			offset = offset + rawCell.length();
			if (i < rawCells.length - 1) {
				processedRow = row.substring(offset);
			}
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
