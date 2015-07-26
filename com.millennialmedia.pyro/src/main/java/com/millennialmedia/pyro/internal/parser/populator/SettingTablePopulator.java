package com.millennialmedia.pyro.internal.parser.populator;

import java.util.Arrays;
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
import com.millennialmedia.pyro.model.util.IModelConstants;

/**
 * For settings tables.
 * 
 * @author spaxton
 */
public class SettingTablePopulator extends AbstractTablePopulator {
	private static List<String> SETTINGS = Arrays.asList(IModelConstants.SETTINGS_TABLE_SETTINGS);

	@Override
	public Line populate(Row row) {
		Step newSetting = new Step();
		newSetting.setStepType(StepType.SETTING);

		List<Cell> cells = row.getCells();
		boolean seenSettingName = false;
		boolean unknownSetting = false;

		for (int i = 0; i < cells.size(); i++) {
			Cell cell = cells.get(i);
			StepSegment newSegment = new StepSegment(cell);
			if (Parser.isCommentCell(cell)) {
				newSegment.setSegmentType(SegmentType.COMMENT);
				newSetting.getSegments().add(newSegment);
				// we don't care about the rest of the cells in this line since
				// they're all commented out
				getTable().getTableLines().add(newSetting);
				break;
			}

			// skip empty cells anywhere
			if (cell.getParsedContents().isEmpty() || "\\".equals(cell.getParsedContents())) {
				continue;
			}

			if (seenSettingName && !unknownSetting) {
				newSegment.setSegmentType(SegmentType.SETTING_VALUE);
			} else {
				String settingString = cell.getParsedContents().trim();
				// the setting name can also end in a colon for readability
				if (settingString.lastIndexOf(":") == settingString.length() - 1) {
					settingString = settingString.substring(0, settingString.length() - 1);
				}

				if (SETTINGS.contains(settingString)) {
					newSegment.setSegmentType(SegmentType.SETTING_NAME);
				} else if ("...".equals(settingString)) {
					newSegment.setSegmentType(SegmentType.CONTINUATION);
				} else {
					newSegment.setSegmentType(SegmentType.UNKNOWN);
					unknownSetting = true;
				}
				seenSettingName = true;
			}
			newSetting.getSegments().add(newSegment);
		}

		if (seenSettingName) {
			getTable().getTableLines().add(newSetting);
		}
		return newSetting;
	}

	@Override
	public void setTable(Table table) {
		super.setTable(table);
		table.setTableType(TableType.SETTING);
	}

}
