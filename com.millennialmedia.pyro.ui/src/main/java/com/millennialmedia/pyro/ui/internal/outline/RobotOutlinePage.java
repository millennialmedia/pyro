package com.millennialmedia.pyro.ui.internal.outline;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

import com.millennialmedia.pyro.model.Line;
import com.millennialmedia.pyro.model.RobotModel;
import com.millennialmedia.pyro.model.Step;
import com.millennialmedia.pyro.model.Step.StepType;
import com.millennialmedia.pyro.model.Table;
import com.millennialmedia.pyro.model.Table.TableType;
import com.millennialmedia.pyro.model.TableItemDefinition;
import com.millennialmedia.pyro.ui.PyroUIPlugin;
import com.millennialmedia.pyro.ui.editor.RobotFrameworkEditor;

/**
 * Outline view contributor for the Pyro editor.
 * 
 * @author spaxton
 */
public class RobotOutlinePage extends ContentOutlinePage {
	private RobotModel model;
	private RobotFrameworkEditor editor;
	private boolean triggeredSelectionChangeInProgress = false;
	private RobotContentProvider contentProvider;
	private static RobotLabelProvider labelProvider = new RobotLabelProvider();

	private LexicalSortAction lexicalSortAction;
	private TreeModeAction treeModeAction;
	private CollapseAllAction collapseAllAction;
	private ExpandAllAction expandAllAction;

	class RobotActionBase extends Action {
		private boolean canToggle;

		public RobotActionBase(String actionLabel, String imageName, boolean canToggle) {
			super();
			this.canToggle = canToggle;
			setText(actionLabel);
			setToolTipText(actionLabel);
			setDescription(actionLabel);
			setImageDescriptor(PyroUIPlugin.getDefault().getImageRegistry().getDescriptor(imageName));
			if (canToggle) {
				boolean checked = PyroUIPlugin.getDefault().getPreferenceStore()
						.getBoolean(this.getClass().getName() + ".isChecked");
				valueChanged(checked, false);
			}
		}

		@Override
		public void run() {
			valueChanged(isChecked(), true);
		}

		public void valueChanged(final boolean isOn, boolean store) {
			if (canToggle) {
				setChecked(isOn);
			}
			BusyIndicator.showWhile(getTreeViewer().getControl().getDisplay(), new Runnable() {
				public void run() {
					internalRun(isOn);
				}
			});

			if (canToggle && store) {
				PyroUIPlugin.getDefault().getPreferenceStore().setValue(this.getClass().getName() + ".isChecked", isOn);
			}
		}

		protected void internalRun(boolean isOn) {
		}
	}

	class LexicalSortAction extends RobotActionBase {
		private ViewerComparator alphaSortComparator = new ViewerComparator() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				return String.CASE_INSENSITIVE_ORDER.compare(labelProvider.getText(e1), labelProvider.getText(e2));
			};
		};

		public LexicalSortAction() {
			super("Sort", PyroUIPlugin.IMAGE_AZSORT, true);
		}

		@Override
		protected void internalRun(boolean isOn) {
			if (isOn) {
				getTreeViewer().setComparator(alphaSortComparator);
			} else {
				getTreeViewer().setComparator(null);
			}
			contentProvider.setCombinedRootTables(isOn);
			getTreeViewer().refresh();
		}
	}

	class ExpandAllAction extends RobotActionBase {
		public ExpandAllAction() {
			super("Expand All", PyroUIPlugin.IMAGE_EXPAND_ALL, false);
		}

		protected void internalRun(boolean isOn) {
			getTreeViewer().expandAll();
		}
	}

	class CollapseAllAction extends RobotActionBase {
		public CollapseAllAction() {
			super("Collapse All", PyroUIPlugin.IMAGE_COLLAPSE_ALL, false);
		}

		protected void internalRun(boolean isOn) {
			getTreeViewer().collapseAll();
		}
	}

	class TreeModeAction extends RobotActionBase {
		public TreeModeAction() {
			super("Show Tables", PyroUIPlugin.IMAGE_SHOW_AS_TREE, true);
		}

		@Override
		protected void internalRun(boolean isOn) {
			contentProvider.setFlattenedTreeMode(!isOn);

			expandAllAction.setEnabled(isOn);
			collapseAllAction.setEnabled(isOn);

			getTreeViewer().refresh();
		}
	}

	public RobotOutlinePage(final RobotModel model, final RobotFrameworkEditor editor) {
		this.model = model;
		this.editor = editor;
		this.contentProvider = new RobotContentProvider(model);

		// setup listener for position changes in the source editor
		editor.getViewer().getTextWidget().addCaretListener(new CaretListener() {
			@Override
			public void caretMoved(CaretEvent event) {
				try {
					if (triggeredSelectionChangeInProgress) {
						return;
					}

					int offset = event.caretOffset;

					// find the item in the outline view that corresponds to
					// this caret position and select it
					boolean underVariableTable = false;
					boolean underSettingTable = false;

					Line currentLine = model.getFirstLine();
					Line containingItem = currentLine;
					while (currentLine != null && currentLine.getLineOffset() <= offset) {
						if (currentLine instanceof TableItemDefinition
								|| (underSettingTable && currentLine instanceof Step && ((Step) currentLine).getStepType() == StepType.SETTING)
								|| (underVariableTable && currentLine instanceof Step)) {
							containingItem = currentLine;
						} else if (currentLine instanceof Table
								&& ((Table) currentLine).getTableType() == TableType.VARIABLE) {
							underVariableTable = true;
							underSettingTable = false;
							containingItem = currentLine;
						} else if (currentLine instanceof Table
								&& ((Table) currentLine).getTableType() == TableType.SETTING) {
							underSettingTable = true;
							underVariableTable = false;
							containingItem = currentLine;
						} else if (currentLine instanceof Table) {
							underVariableTable = false;
							underSettingTable = false;
							containingItem = currentLine;
						}
						currentLine = currentLine.getNextLine();
					}
					try {
						triggeredSelectionChangeInProgress = true;
						if (getTreeViewer() != null && getTreeViewer().getControl() != null && !getTreeViewer().getControl().isDisposed()) {
							getTreeViewer().setSelection(new StructuredSelection(containingItem));
							getTreeViewer().expandToLevel(containingItem, AbstractTreeViewer.ALL_LEVELS);
							getTreeViewer().reveal(containingItem);
						}
					} finally {
						triggeredSelectionChangeInProgress = false;
					}
				} catch (Exception e) {
					// do nothing, just prevent the listener from leaking an exception back to the text editor
				}
			}
		});
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		TreeViewer viewer = getTreeViewer();
		viewer.setContentProvider(contentProvider);
		viewer.setLabelProvider(labelProvider);
		viewer.addSelectionChangedListener(this);
		viewer.setInput(model);

		IActionBars actionBars = getSite().getActionBars();
		IToolBarManager toolbarManager = actionBars.getToolBarManager();

		lexicalSortAction = new LexicalSortAction();
		collapseAllAction = new CollapseAllAction();
		expandAllAction = new ExpandAllAction();
		treeModeAction = new TreeModeAction();

		toolbarManager.add(treeModeAction);
		toolbarManager.add(collapseAllAction);
		toolbarManager.add(expandAllAction);
		toolbarManager.add(lexicalSortAction);
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		if (triggeredSelectionChangeInProgress) {
			return;
		}

		StructuredSelection selection = (StructuredSelection) event.getSelection();
		Line line = (Line) selection.getFirstElement();
		if (line != null) {
			ISourceViewer viewer = editor.getViewer();
			try {
				triggeredSelectionChangeInProgress = true;
				int newSelectedLineOffset = line.getLineOffset();
				viewer.setSelectedRange(newSelectedLineOffset, 0);
				viewer.revealRange(newSelectedLineOffset, 0);
			} finally {
				triggeredSelectionChangeInProgress = false;
			}
		}
	}

	public void refresh() {
		if (getTreeViewer() != null && getTreeViewer().getControl() != null && !getTreeViewer().getControl().isDisposed()) {
			getTreeViewer().refresh(true);
		}
	}

}
