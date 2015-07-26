package com.millennialmedia.pyro.internal.parser.populator;

import java.util.List;

import com.millennialmedia.pyro.internal.parser.Cell;
import com.millennialmedia.pyro.internal.parser.Parser;
import com.millennialmedia.pyro.internal.parser.Row;
import com.millennialmedia.pyro.model.Line;
import com.millennialmedia.pyro.model.Step;
import com.millennialmedia.pyro.model.StepSegment;
import com.millennialmedia.pyro.model.Table;
import com.millennialmedia.pyro.model.StepSegment.SegmentType;
import com.millennialmedia.pyro.model.Table.TableType;

/**
 * For variables tables.
 * 
 * @author spaxton
 */
public class VariableTablePopulator extends AbstractTablePopulator {

	@Override
	public Line populate(Row row) {
		Step newVariableLine = new Step();
		List<Cell> cells = row.getCells();
		boolean seenVariableName = false;

		for (int i = 0; i < cells.size(); i++) {
			Cell cell = cells.get(i);
			StepSegment newSegment = new StepSegment(cell);
			if (Parser.isCommentCell(cell)) {
				newSegment.setSegmentType(SegmentType.COMMENT);
				newVariableLine.getSegments().add(newSegment);
				// we don't care about the rest of the cells in this line since
				// they're all commented out
				getTable().getTableLines().add(newVariableLine);
				break;
			}

			// skip empty cells anywhere
			if (cell.getParsedContents().isEmpty() || "\\".equals(cell.getParsedContents())) {
				continue;
			}

			if (seenVariableName) {
				newSegment.setSegmentType(SegmentType.ARGUMENT);
			} else {
				String contents = cell.getParsedContents().trim();
				if (contents.startsWith("$") || contents.startsWith("@")) {
					newSegment.setSegmentType(SegmentType.VARIABLE);
				} else if ("...".equals(contents)) {
					newSegment.setSegmentType(SegmentType.CONTINUATION);
				} else {
					newSegment.setSegmentType(SegmentType.UNKNOWN);
				}
				seenVariableName = true;
			}

			if (newSegment != null) {
				newVariableLine.getSegments().add(newSegment);
			}

		}
		if (seenVariableName) {
			getTable().getTableLines().add(newVariableLine);
		}
		return newVariableLine;
	}

	@Override
	public void setTable(Table table) {
		super.setTable(table);
		table.setTableType(TableType.VARIABLE);
	}

}
