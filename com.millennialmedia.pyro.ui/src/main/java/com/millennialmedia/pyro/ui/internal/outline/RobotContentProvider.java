package com.millennialmedia.pyro.ui.internal.outline;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.millennialmedia.pyro.model.Line;
import com.millennialmedia.pyro.model.RobotModel;
import com.millennialmedia.pyro.model.Step;
import com.millennialmedia.pyro.model.StepSegment;
import com.millennialmedia.pyro.model.Step.StepType;
import com.millennialmedia.pyro.model.StepSegment.SegmentType;
import com.millennialmedia.pyro.model.Table;
import com.millennialmedia.pyro.model.Table.TableType;
import com.millennialmedia.pyro.model.TableItemDefinition;
import com.millennialmedia.pyro.model.TableItemDefinition.TableItemType;
import com.millennialmedia.pyro.model.util.ModelUtil;

/**
 * Content provider for Pyro's Outline view.
 * 
 * @author spaxton
 */
public class RobotContentProvider implements ITreeContentProvider {
	private RobotModel model;
	private boolean useFlattenedTreeMode = false;
	private boolean useCombinedRootTables = false;
	private Table internalTestcaseTable;
	private Table internalKeywordTable;
	private Table internalSettingTable;
	private Table internalVariableTable;

	public RobotContentProvider(RobotModel model) {
		this.model = model;
	}

	public void setFlattenedTreeMode(boolean flattened) {
		useFlattenedTreeMode = flattened;
	}

	public void setCombinedRootTables(boolean combined) {
		useCombinedRootTables = combined;
	}

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		List<Object> elements = new ArrayList<Object>();
		if (parentElement instanceof RobotModel) {
			internalTestcaseTable = new Table();
			internalTestcaseTable.setTableType(TableType.TESTCASE);
			internalKeywordTable = new Table();
			internalKeywordTable.setTableType(TableType.KEYWORD);
			internalSettingTable = new Table();
			internalSettingTable.setTableType(TableType.SETTING);
			internalVariableTable = new Table();
			internalVariableTable.setTableType(TableType.VARIABLE);

			for (Table table : ((RobotModel) parentElement).getTables()) {
				if (useFlattenedTreeMode) {
					elements.addAll(getChildrenFromTable(table));
				} else {
					if (!useCombinedRootTables) {
						elements.add(table);
					} else {
						switch (table.getTableType()) {
						case TESTCASE:
							internalTestcaseTable.getTableLines().addAll(table.getTableLines());
							break;
						case KEYWORD:
							internalKeywordTable.getTableLines().addAll(table.getTableLines());
							break;
						case SETTING:
							internalSettingTable.getTableLines().addAll(table.getTableLines());
							break;
						case VARIABLE:
							internalVariableTable.getTableLines().addAll(table.getTableLines());
							break;
						}
						;
					}
				}
			}

			if (useCombinedRootTables && !useFlattenedTreeMode) {
				elements.add(internalTestcaseTable);
				elements.add(internalKeywordTable);
				elements.add(internalSettingTable);
				elements.add(internalVariableTable);
			}
		} else if (parentElement instanceof Table) {
			elements.addAll(getChildrenFromTable(parentElement));
		}

		return elements.toArray(new Object[0]);
	}

	private List<Object> getChildrenFromTable(Object parentElement) {
		List<Object> elements = new ArrayList<Object>();
		if (parentElement instanceof Table && ((Table) parentElement).getTableType() == TableType.TESTCASE) {
			for (Line line : ((Table) parentElement).getTableLines()) {
				if (line instanceof TableItemDefinition) {
					TableItemDefinition item = (TableItemDefinition) line;
					if (getTestcaseName(item) != null) {
						elements.add(item);
					}
				}
			}
		} else if (parentElement instanceof Table && ((Table) parentElement).getTableType() == TableType.KEYWORD) {
			for (Line line : ((Table) parentElement).getTableLines()) {
				if (line instanceof TableItemDefinition) {
					TableItemDefinition item = (TableItemDefinition) line;
					if (getKeywordName(item) != null) {
						elements.add(item);
					}
				}
			}
		} else if (parentElement instanceof Table && ((Table) parentElement).getTableType() == TableType.SETTING) {
			for (Line line : ((Table) parentElement).getTableLines()) {
				Step step = (Step) line;
				if (getSettingName(step) != null) {
					elements.add(step);
				}
			}
		} else if (parentElement instanceof Table && ((Table) parentElement).getTableType() == TableType.VARIABLE) {
			for (Line line : ((Table) parentElement).getTableLines()) {
				Step step = (Step) line;
				if (getVariableName(step) != null) {
					elements.add(step);
				}
			}
		}
		return elements;
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof Line && !useFlattenedTreeMode) {
			if (useCombinedRootTables) {
				if (element instanceof TableItemDefinition) {
					if (((TableItemDefinition) element).getItemType() == TableItemType.TESTCASE) {
						return internalTestcaseTable;
					} else {
						return internalKeywordTable;
					}
				} else if (element instanceof Step) {
					Step step = (Step) element;
					if (step.getStepType() == StepType.SETTING) {
						return internalSettingTable;
					} else {
						return internalVariableTable;
					}
				}
			} else {
				return ModelUtil.getContainingTable(model, ((Line) element).getLineOffset());
			}
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return (element instanceof Table);
	}

	static String getSettingName(Step step) {
		for (StepSegment segment : step.getSegments()) {
			if (segment.getSegmentType() == SegmentType.SETTING_NAME) {
				return segment.getValue();
			}
		}
		return null;
	}

	static String getVariableName(Step step) {
		for (StepSegment segment : step.getSegments()) {
			if (segment.getSegmentType() == SegmentType.VARIABLE) {
				return segment.getValue();
			}
		}
		return null;
	}

	static String getTestcaseName(TableItemDefinition testcase) {
		return testcase.getName();
	}

	static String getKeywordName(TableItemDefinition keyword) {
		return keyword.getName();
	}

}
