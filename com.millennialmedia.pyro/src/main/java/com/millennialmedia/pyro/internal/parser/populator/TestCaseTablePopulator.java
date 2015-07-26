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
 * For testcases tables.
 * 
 * @author spaxton
 */
public class TestCaseTablePopulator extends AbstractTablePopulator {
	private static final String TEMPLATE = "[Template]";

	private TableItemDefinition currentTestCase;
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
			// this means a new testcase is starting
			TableItemDefinition testcaseLine = createTestCase(firstCellStr);
			testcaseLine.setNameLength(firstCell.getOffsetInRow() + firstCell.getParsedContents().length());
			lastCellSegmentType = null;
			
			// if we have subsequent cells the first step-line is essentially
			// merged with the testcase declaration
			// we'll treat it as a separate line in the model but they'll have
			// the same line number in the document
			if (row.getCells().size() > 1) {
				Line firstStepLine = addToTestCase(row.getCells());
				testcaseLine.setNextLine(firstStepLine);
			}
			return testcaseLine;
		}

		// additional lines that belong to the current testcase
		return addToTestCase(row.getCells());

	}

	private TableItemDefinition createTestCase(String testCaseName) {
		currentTestCase = new TableItemDefinition();
		currentTestCase.setItemType(TableItemType.TESTCASE);
		currentTestCase.setName(testCaseName);
		getTable().getTableLines().add(currentTestCase);
		return currentTestCase;
	}

	private Line addToTestCase(List<Cell> cells) {
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

			// always skip over the first cell (might be the initial testcase name)
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
					seenKeyword = true;
					isSetting = true;
					newSegment.setSegmentType(SegmentType.SETTING_NAME);
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
			} else if (stepDefinesTemplate(currentStep)) {
				newSegment.setSegmentType(SegmentType.KEYWORD_CALL);
			} else if (isSetting) {
				newSegment.setSegmentType(SegmentType.SETTING_VALUE);
			} else {
				newSegment.setSegmentType(SegmentType.ARGUMENT);
			}

			if (newSegment != null) {
				currentStep.getSegments().add(newSegment);
			}
			
		}

		if (currentTestCase != null) {
			currentTestCase.getSteps().add(currentStep);
		}

		StepSegment lastSegment = currentStep.getSegments().get(currentStep.getSegments().size() - 1);
		if (lastSegment != null && lastSegment.getSegmentType() != SegmentType.COMMENT) {
			lastCellSegmentType = lastSegment.getSegmentType();
		}
		
		return currentStep;
	}

	private boolean stepDefinesTemplate(Step step) {
		if (step.getStepType() == StepType.SETTING) {
			for (StepSegment segment : step.getSegments()) {
				if (segment.getSegmentType() == SegmentType.SETTING_NAME && TEMPLATE.equals(segment.getValue())) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void setTable(Table table) {
		super.setTable(table);
		table.setTableType(TableType.TESTCASE);
	}

}
