package com.millennialmedia.pyro.internal.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for readers which perform the tokenizing of a raw file into a
 * sequence of rows and cells.
 * 
 * @author spaxton
 */
public abstract class AbstractReader {
	private Map<Row, Integer> lineOffsetsMap = new HashMap<Row, Integer>();
	private Map<Row, Integer> lineLengthMap = new HashMap<Row, Integer>();

	public void reset() {
		lineOffsetsMap.clear();
		lineLengthMap.clear();
	}

	public List<Row> getRows(String buffer) {
		int rowOffset = 0;
		List<Row> rows = new ArrayList<Row>();
		String[] rawRows = splitIntoRows(buffer);
		for (int i=0;i<rawRows.length;i++) {
			String rawRow = rawRows[i];
			// line length in the model includes the terminating linefeed except for the last row (which may not have one)
			int lineLength = rawRow.length() + ((i == rawRows.length - 1) ? 0 : 1);
			String processedRow = processRow(rawRow);
			List<Cell> cells = splitRow(processedRow);
			Row newRow = new Row();
			newRow.getCells().addAll(cells);
			rows.add(newRow);
			lineOffsetsMap.put(newRow, rowOffset);
			lineLengthMap.put(newRow, lineLength);
			rowOffset += lineLength;
		}

		return rows;
	};

	public int getOffset(Row row) {
		return lineOffsetsMap.get(row);
	}

	public int getLength(Row row) {
		return lineLengthMap.get(row);
	}

	protected String[] splitIntoRows(String buffer) {
		return buffer.split("\n");
	};

	private String processRow(String row) {
		// replace any non-breaking spaces and strip trailing whitespace
		return rstrip(row.replace("\u00A0", " "));
	}

	private String rstrip(String src) {
		int i = src.length() - 1;
		while (i >= 0 && Character.isWhitespace(src.charAt(i))) {
			i--;
		}
		return src.substring(0, i + 1);
	}

	protected abstract List<Cell> splitRow(String row);

}
