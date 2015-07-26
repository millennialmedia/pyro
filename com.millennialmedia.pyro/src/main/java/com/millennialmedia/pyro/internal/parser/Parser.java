package com.millennialmedia.pyro.internal.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import com.millennialmedia.pyro.internal.parser.populator.KeywordTablePopulator;
import com.millennialmedia.pyro.internal.parser.populator.SettingTablePopulator;
import com.millennialmedia.pyro.internal.parser.populator.AbstractTablePopulator;
import com.millennialmedia.pyro.internal.parser.populator.TestCaseTablePopulator;
import com.millennialmedia.pyro.internal.parser.populator.UnknownTablePopulator;
import com.millennialmedia.pyro.internal.parser.populator.VariableTablePopulator;
import com.millennialmedia.pyro.internal.parser.postprocessor.ModelPostProcessorRegistryReader;
import com.millennialmedia.pyro.model.Line;
import com.millennialmedia.pyro.model.RobotModel;
import com.millennialmedia.pyro.model.Table;
import com.millennialmedia.pyro.model.util.AbstractModelPostProcessor;

/**
 * The parser turns Robot source files into models. The design is based loosely
 * on the Python implementation of the pybot runtime parser for test execution.
 * First the file is tokenized by the appropriate reader implementation and then
 * a set of table populators turns the raw text cells into Robot-specific model
 * objects.
 * 
 * This class is internal and not exposed beyond the core plugin. Client access
 * to the parsing functionality is entirely through the ModelManager.
 * 
 * @author spaxton
 */
public final class Parser {
	private enum TableTypeEnum {
		UNKNOWN(new UnknownTablePopulator()), 
		TESTCASE(new TestCaseTablePopulator()), 
		KEYWORD(new KeywordTablePopulator()), 
		SETTING(new SettingTablePopulator()), 
		VARIABLE(new VariableTablePopulator());

		private AbstractTablePopulator populator;

		TableTypeEnum(AbstractTablePopulator populator) {
			this.populator = populator;
		}

		public AbstractTablePopulator getPopulator() {
			return populator;
		}
	}

	// set up the map of all allowable table names to the associated table
	// populators
	private static Map<String, TableTypeEnum> tableMappings = new HashMap<String, TableTypeEnum>();
	static {
		tableMappings.put("Test Case", TableTypeEnum.TESTCASE);
		tableMappings.put("Test Cases", TableTypeEnum.TESTCASE);

		tableMappings.put("Keyword", TableTypeEnum.KEYWORD);
		tableMappings.put("Keywords", TableTypeEnum.KEYWORD);
		tableMappings.put("User Keyword", TableTypeEnum.KEYWORD);
		tableMappings.put("User Keywords", TableTypeEnum.KEYWORD);

		tableMappings.put("Setting", TableTypeEnum.SETTING);
		tableMappings.put("Settings", TableTypeEnum.SETTING);
		tableMappings.put("Metadata", TableTypeEnum.SETTING);

		tableMappings.put("Variable", TableTypeEnum.VARIABLE);
		tableMappings.put("Variables", TableTypeEnum.VARIABLE);
	}

	private IFile file;
	private AbstractReader reader;

	public Parser(IFile file) {
		this.file = file;
	}

	public RobotModel parse() {
		return parse(getContents(file));
	}

	public RobotModel parse(String buffer) {
		RobotModel model = new RobotModel();
		try {
			getReader().reset();
			List<Row> rows = getReader().getRows(buffer);
			populateTables(model, rows);
			postProcess(model);
		} catch (Exception e) {
		}
		return model;
	}

	private void populateTables(RobotModel model, List<Row> rows) {
		Line firstLineHolder = Line.UNKNOWN();
		Line previousLine = firstLineHolder;
		AbstractTablePopulator populator = new UnknownTablePopulator();
		for (Row row : rows) {
			if (isEmpty(row)) {
				continue;
			} else if (row.getCells().get(0).getParsedContents().startsWith("*")) {
				// start of new table
				TableTypeEnum tableType = determineTableType(row);
				if(tableType == TableTypeEnum.UNKNOWN) {
					// we found a leading * but it's not a valid table name - this is ignored
					continue;
				}
				populator = tableType.getPopulator();
				populator.setModel(model);
				Table table = createTable(populator);
				table.setTableName(row.getCells().get(0).getParsedContents());
				table.setLineOffset(getReader().getOffset(row));
				table.setLineLength(getReader().getLength(row));
				model.getTables().add(table);
				previousLine.setNextLine(table);
				previousLine = table;
			} else {
				Line newLine = populator.populate(row);
				newLine.setLineOffset(getReader().getOffset(row));
				newLine.setLineLength(getReader().getLength(row));
				previousLine.setNextLine(newLine);

				// we may be returned a chain of lines - always advance to the
				// last one
				while (previousLine.getNextLine() != null) {
					previousLine = previousLine.getNextLine();
					previousLine.setLineOffset(getReader().getOffset(row));
					previousLine.setLineLength(getReader().getLength(row));
				}
			}
		}
		model.setFirstLine(firstLineHolder.getNextLine());
	}

	private Table createTable(AbstractTablePopulator populator) {
		Table table = new Table();
		populator.setTable(table);
		return table;
	}

	private AbstractReader getReader() {
		if (reader == null) {
			String extension = file.getFileExtension();
			if (extension != null) {
				if ("tsv".equalsIgnoreCase(extension)) {
					// tab-separated format
					reader = new TsvReader();
				} else if ("txt".equalsIgnoreCase(extension) || "robot".equalsIgnoreCase(extension)) {
					// plain text format
					reader = new TxtReader();
				} else {
					// TODO the markup-based file formats would be read here, if
					// we ever support them
					// safety fallback, a no-op reader that returns a truly
					// empty model
					reader = new AbstractReader() {
						public List<Row> getRows(String buffer) {
							return Collections.emptyList();
						};

						@Override
						protected List<Cell> splitRow(String row) {
							return null;
						}
					};
				}
			}
		}
		return reader;
	}

	private TableTypeEnum determineTableType(Row row) {
		StringBuilder header = new StringBuilder();
		for (Cell cell : row.getCells()) {
			header.append(cell.getParsedContents().replace("*", ""));
		}

		String strippedHeader = header.toString().trim();

		for (String tableHeader : tableMappings.keySet()) {
			if (strippedHeader.startsWith(tableHeader) && tableMappings.containsKey(tableHeader)) {
				return tableMappings.get(tableHeader);
			}
		}
		return TableTypeEnum.UNKNOWN;
	}

	private boolean isEmpty(Row row) {
		// even if there ARE cells, if they all trim() to empty strings that's
		// also considered a blank line
		boolean containsNonEmptyCell = false;
		for (Cell cell : row.getCells()) {
			if (!cell.getParsedContents().isEmpty()) {
				containsNonEmptyCell = true;
				break;
			}
		}
		return row.getCells().isEmpty() || !containsNonEmptyCell;
	}

	public static boolean isCommentRow(Row row) {
		for (Cell cell : row.getCells()) {
			if (isCommentCell(cell)) {
				return true;
			} else if (!cell.getParsedContents().trim().isEmpty()) {
				return false;
			}
		}
		return false;
	}

	public static boolean isCommentCell(Cell cell) {
		if (cell.getParsedContents().trim().startsWith("#")) {
			return true;
		}
		return false;
	}

	private String getContents(IFile file) {
		try {
			InputStream input = file.getContents();
			String contents = getContents(input);
			input.close();
			return contents;
		} catch (CoreException e) {
		} catch (IOException e) {
		}
		return null;
	}

	private String getContents(InputStream in) throws IOException {
		StringBuffer buffer = new StringBuffer();
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String line;
		while ((line = reader.readLine()) != null) {
			buffer.append(line);
			buffer.append('\n');
		}
		return buffer.toString();
	}

	private void postProcess(RobotModel model) {
		// now that the complete model is available to analyze, adjust the model
		// to correct some semantic issues.
		// the work is delegated to post-processors contributed via extension
		// point
		for (AbstractModelPostProcessor processor : ModelPostProcessorRegistryReader.getReader().getPostProcessors()) {
			try {
				processor.postProcess(model);
			} catch (Exception e) {
				// ignore it and continue, but don't let an exception blow up
				// the rest of the model construction
			}
		}
	}

}
