package com.millennialmedia.pyro.internal.parser.populator;

import com.millennialmedia.pyro.internal.parser.Row;
import com.millennialmedia.pyro.model.Line;
import com.millennialmedia.pyro.model.RobotModel;
import com.millennialmedia.pyro.model.Table;

/**
 * Base class for table populators. The specific logic for transforming a set of
 * raw cells in a row into specific model objects is inspired mainly by the
 * functionality of the Robot Framework's runtime parser.
 * 
 * @author spaxton
 */
public abstract class AbstractTablePopulator {
	private RobotModel model;
	private Table table;

	public void setModel(RobotModel model) {
		this.model = model;
	}

	protected RobotModel getModel() {
		return model;
	}

	public void setTable(Table table) {
		this.table = table;
	}

	protected Table getTable() {
		return table;
	}

	/**
	 * Given a raw row of text cells, generate a model line of the appropriate
	 * type.
	 */
	public abstract Line populate(Row row);

}
