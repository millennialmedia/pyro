package com.millennialmedia.pyro.internal.parser.populator;

import java.util.List;

import com.millennialmedia.pyro.internal.parser.Cell;
import com.millennialmedia.pyro.internal.parser.Parser;
import com.millennialmedia.pyro.internal.parser.Row;
import com.millennialmedia.pyro.model.Line;
import com.millennialmedia.pyro.model.Step;
import com.millennialmedia.pyro.model.Step.StepType;
import com.millennialmedia.pyro.model.StepSegment;
import com.millennialmedia.pyro.model.StepSegment.SegmentType;
import com.millennialmedia.pyro.model.Table;
import com.millennialmedia.pyro.model.Table.TableType;
import com.millennialmedia.pyro.model.TableItemDefinition;
import com.millennialmedia.pyro.model.TableItemDefinition.TableItemType;

/**
 * For keyword tables.
 * 
 * @author spaxton
 */
public class KeywordTablePopulator extends AbstractTablePopulator {

	private TableItemDefinition currentKeyword;
	private SegmentType lastCellSegmentType;

	@Override
	public Line populate(Row row) {
		if (row.getCells().isEmpty()) {
			return Line.EMPTY();
		}

		Cell firstCell = row.getCells().get(0);
		String firstCellStr = row.getCells().get(0).getParsedContents().trim();

		if (!firstCellStr.isEmpty() && !Parser.isCommentCell(firstCell)) {
			// there are real contents in the first cell
			// this means a new keyword is starting
			TableItemDefinition keywordLine = createKeyword(firstCellStr);
			keywordLine.setNameLength(firstCell.getOffsetInRow() + firstCell.getParsedContents().length());
			lastCellSegmentType = null;

			// if we have subsequent cells the first step-line is essentially
			// merged with the keyword declaration
			// we'll treat it as a separate line in the model but they'll have
			// the same line number
			if (row.getCells().size() > 1) {
				Line firstStepLine = addToKeyword(row.getCells());
				keywordLine.setNextLine(firstStepLine);
			}
			return keywordLine;

		}

		// additional lines that belong to the current keyword definition
		return addToKeyword(row.getCells());

	}

	private TableItemDefinition createKeyword(String keywordName) {
		currentKeyword = new TableItemDefinition();
		currentKeyword.setItemType(TableItemType.KEYWORD);
		currentKeyword.setName(keywordName);
		getTable().getTableLines().add(currentKeyword);
		return currentKeyword;
	}

	private Line addToKeyword(List<Cell> cells) {
		Step currentStep = new Step();
		currentStep.setStepType(StepType.STEP);
		boolean seenKeyword = false;
		boolean isSetting = false;
		boolean usingContinuation = false;

		for (int i = 0; i < cells.size(); i++) {
			Cell cell = cells.get(i);
			StepSegment newSegment = new StepSegment(cell);
			if (Parser.isCommentCell(cell)) {
				newSegment.setSegmentType(SegmentType.COMMENT);
				currentStep.getSegments().add(newSegment);
				// we don't care about the rest of the cells in this line since
				// they're all commented out
				break;
			}

			// always skip over the first cell (might be the initial keyword
			// name)
			if (i == 0) {
				continue;
			}

			// skip empty cells anywhere
			if (cell.getParsedContents().isEmpty() || "\\".equals(cell.getParsedContents())) {
				continue;
			}

			if (usingContinuation && lastCellSegmentType != null) {
				newSegment.setSegmentType(lastCellSegmentType);
			} else if (!seenKeyword) {
				String contents = cell.getParsedContents().trim();
				if (contents.startsWith("[")) {
					currentStep.setStepType(StepType.SETTING);
					newSegment.setSegmentType(SegmentType.SETTING_NAME);
					seenKeyword = true;
				} else if (contents.startsWith("$")
						|| contents.startsWith("@")
						|| contents.startsWith("%")) {
					newSegment.setSegmentType(SegmentType.VARIABLE);
				} else if ("...".equals(contents)) {
					newSegment.setSegmentType(SegmentType.CONTINUATION);
					usingContinuation = true;
				} else {
					seenKeyword = true;
					newSegment.setSegmentType(SegmentType.KEYWORD_CALL);
				}
			} else if (isSetting) {
				newSegment.setSegmentType(SegmentType.SETTING_VALUE);
			} else {
				newSegment.setSegmentType(SegmentType.ARGUMENT);
			}

			if (newSegment != null) {
				currentStep.getSegments().add(newSegment);
			}
		}
		
		if (currentKeyword != null) {
			currentKeyword.getSteps().add(currentStep);
		}

		StepSegment lastSegment = currentStep.getSegments().get(currentStep.getSegments().size() - 1);
		if (lastSegment != null && lastSegment.getSegmentType() != SegmentType.COMMENT) {
			lastCellSegmentType = lastSegment.getSegmentType();
		}

		return currentStep;
	}

	@Override
	public void setTable(Table table) {
		super.setTable(table);
		table.setTableType(TableType.KEYWORD);
	}

}
