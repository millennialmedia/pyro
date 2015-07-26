package com.millennialmedia.pyro.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The first line of a new testcase or keyword within a Robot model.
 * 
 * @author spaxton
 */
public class TableItemDefinition extends Line {
	public static enum TableItemType {
		KEYWORD, TESTCASE
	};

	private TableItemType itemType;
	private String name;
	private int nameLength;   // because of leading whitespace, this may not always be name.length()
	private List<Step> steps = new ArrayList<Step>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Step> getSteps() {
		return steps;
	}

	public void setSteps(List<Step> steps) {
		this.steps = steps;
	}

	public TableItemType getItemType() {
		return itemType;
	}

	public void setItemType(TableItemType itemType) {
		this.itemType = itemType;
	}

	public int getNameLength() {
		return nameLength;
	}

	public void setNameLength(int nameLength) {
		this.nameLength = nameLength;
	}

}
