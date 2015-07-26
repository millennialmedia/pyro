package com.millennialmedia.pyro.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The line containing the start of a new Robot table.
 * 
 * @author spaxton
 */
public class Table extends Line {
	public static enum TableType {
		TESTCASE, KEYWORD, VARIABLE, SETTING
	};

	private String tableName;
	private TableType tableType;
	private List<Line> tableLines = new ArrayList<Line>();

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public TableType getTableType() {
		return tableType;
	}

	public void setTableType(TableType tableType) {
		this.tableType = tableType;
	}

	public List<Line> getTableLines() {
		return tableLines;
	}

}
