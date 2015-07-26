package com.millennialmedia.pyro.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The container model for a Robot source file.
 * 
 * @author spaxton
 */
public class RobotModel {
	private List<Table> tables = new ArrayList<Table>();
	private Line firstLine;
	private Map<String, Object> customProperties = new HashMap<String, Object>();

	public List<Table> getTables() {
		return tables;
	}

	void setTables(List<Table> tables) {
		this.tables = tables;
	}

	public Line getFirstLine() {
		return firstLine;
	}

	public void setFirstLine(Line firstLine) {
		this.firstLine = firstLine;
	}

	public Map<String, Object> getCustomProperties() {
		return customProperties;
	}

}
