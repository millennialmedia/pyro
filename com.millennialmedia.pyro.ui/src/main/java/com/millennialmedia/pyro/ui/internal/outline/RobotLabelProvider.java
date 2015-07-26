package com.millennialmedia.pyro.ui.internal.outline;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.millennialmedia.pyro.model.Step;
import com.millennialmedia.pyro.model.Step.StepType;
import com.millennialmedia.pyro.model.Table;
import com.millennialmedia.pyro.model.Table.TableType;
import com.millennialmedia.pyro.model.TableItemDefinition;
import com.millennialmedia.pyro.model.TableItemDefinition.TableItemType;
import com.millennialmedia.pyro.ui.PyroUIPlugin;

/**
 * Label provider for Pyro's Outline view.
 * 
 * @author spaxton
 */
public class RobotLabelProvider extends LabelProvider {

	@Override
	public String getText(Object element) {
		if (element instanceof TableItemDefinition) {
			return ((TableItemDefinition) element).getName();
		} else if (element instanceof Table && ((Table) element).getTableType() == TableType.TESTCASE) {
			return "Test Cases";
		} else if (element instanceof Table && ((Table) element).getTableType() == TableType.KEYWORD) {
			return "Keywords";
		} else if (element instanceof Table && ((Table) element).getTableType() == TableType.SETTING) {
			return "Settings";
		} else if (element instanceof Table && ((Table) element).getTableType() == TableType.VARIABLE) {
			return "Variables";
		} else if (element instanceof Step) {
			Step step = (Step) element;
			if (step.getStepType() == StepType.SETTING) {
				String settingName = RobotContentProvider.getSettingName(step);
				String suffix = "";
				if (step.getSegments().size() > 1) {
					suffix = " [" + step.getSegments().get(1).getValue().trim() + "]";
				}
				return (settingName + suffix);
			} else {
				return RobotContentProvider.getVariableName((Step) element);
			}
		}

		return super.getText(element);
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof TableItemDefinition
				&& ((TableItemDefinition) element).getItemType() == TableItemType.TESTCASE) {
			return PyroUIPlugin.getDefault().getImageRegistry().get(PyroUIPlugin.IMAGE_TESTCASE);
		} else if (element instanceof TableItemDefinition
				&& ((TableItemDefinition) element).getItemType() == TableItemType.KEYWORD) {
			return PyroUIPlugin.getDefault().getImageRegistry().get(PyroUIPlugin.IMAGE_KEYWORD);
		} else if (element instanceof Step) {
			if (((Step) element).getStepType() == StepType.SETTING) {
				return PyroUIPlugin.getDefault().getImageRegistry().get(PyroUIPlugin.IMAGE_SETTING);
			} else {
				return PyroUIPlugin.getDefault().getImageRegistry().get(PyroUIPlugin.IMAGE_VARIABLE);
			}
		} else if (element instanceof Table) {
			return PyroUIPlugin.getDefault().getImageRegistry().get(PyroUIPlugin.IMAGE_TABLE);
		}

		return super.getImage(element);
	}

}
