package com.millennialmedia.pyro.test;


import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

import org.eclipse.core.resources.IFile;
import org.junit.Assert;
import org.junit.Test;

import com.millennialmedia.pyro.internal.parser.Parser;
import com.millennialmedia.pyro.model.Line;
import com.millennialmedia.pyro.model.RobotModel;
import com.millennialmedia.pyro.model.Step;
import com.millennialmedia.pyro.model.StepSegment.SegmentType;
import com.millennialmedia.pyro.model.Table;
import com.millennialmedia.pyro.model.Table.TableType;
import com.millennialmedia.pyro.model.TableItemDefinition;
import com.millennialmedia.pyro.test.mock.MockIFile;

/**
 * A set of basic parser tests to ensure accurate construction of RobotModel objects.  These tests exercise the main parser class,
 * the table populators, and the core set of model post-processors included in the pyro plugin.
 * 
 * @author spaxton
 */
public class ParserTest {
	private IFile mockTxtFile = new MockIFile("txt");
	private IFile mockTsvFile = new MockIFile("tsv");
	
	@Test
	public void testBasicParseOfTabSeparatedFile() throws Exception {
		String buffer = loadFile("samplefiles/parserTest.tsv");
		Parser parser = new Parser(mockTsvFile);
		RobotModel model = parser.parse(buffer);

		Assert.assertNotNull("Parser failure - model is null", model);
		
		Assert.assertEquals("There should be exactly 4 tables (bogus table name shouldn't get a node in the model)", 4, model.getTables().size());

		// we start with several comment lines before the first table
		Line line = model.getFirstLine();
		Assert.assertEquals("Expected comment line", SegmentType.COMMENT, ((Step) line).getSegments().get(0).getSegmentType());
		line = line.getNextLine();
		Assert.assertEquals("Expected comment line", SegmentType.COMMENT, ((Step) line).getSegments().get(0).getSegmentType());
		line = line.getNextLine();
		Assert.assertEquals("Expected comment line", SegmentType.COMMENT, ((Step) line).getSegments().get(0).getSegmentType());
		
		//  SETTING table
		Table settingsTable = model.getTables().get(0);
		Assert.assertEquals("Not the settings table!", settingsTable.getTableType(), TableType.SETTING);

		List<Line> tableLines = settingsTable.getTableLines();
		Step step = (Step) tableLines.get(0);
		// tests SetupTeardownPostProcessor
		Assert.assertEquals("Expected test setup", "Test Setup", step.getSegments().get(0).getValue());
		Assert.assertEquals("Expected setting name type", SegmentType.SETTING_NAME, step.getSegments().get(0).getSegmentType());
		Assert.assertEquals("Expected keyword invocation as target of test setup setting", SegmentType.KEYWORD_CALL, step.getSegments().get(1).getSegmentType());

		step = (Step) tableLines.get(1);
		Assert.assertEquals("Expected documentation", "Documentation", step.getSegments().get(0).getValue());
		Assert.assertEquals("Expected documentation setting value", SegmentType.SETTING_VALUE, step.getSegments().get(1).getSegmentType());

		// continuation of documentation onto second line
		step = (Step) tableLines.get(2);
		Assert.assertEquals("Expected continuation cell", SegmentType.CONTINUATION, step.getSegments().get(0).getSegmentType());
		Assert.assertEquals("Expected continued documentation setting value", SegmentType.SETTING_VALUE, step.getSegments().get(1).getSegmentType());

		// tests LibraryPostProcessor
		step = (Step) tableLines.get(4);
		Assert.assertEquals("Expected library", "Library", step.getSegments().get(0).getValue());
		Assert.assertEquals("Expected setting value for library name", SegmentType.SETTING_VALUE, step.getSegments().get(1).getSegmentType());
		Assert.assertEquals("Expected WITH NAME as control argument", SegmentType.CONTROL_ARGUMENT, step.getSegments().get(2).getSegmentType());
		Assert.assertEquals("Expected alias to be setting value", SegmentType.SETTING_VALUE, step.getSegments().get(3).getSegmentType());
		
		// check bad setting name
		step = (Step) tableLines.get(6);
		Assert.assertEquals("Expected bad setting name", "Bad setting name", step.getSegments().get(0).getValue());
		Assert.assertEquals("Expected unknown segment", SegmentType.UNKNOWN, step.getSegments().get(0).getSegmentType());
		Assert.assertEquals("Expected unknown segment", SegmentType.UNKNOWN, step.getSegments().get(1).getSegmentType());
		
		// multi-cell comment
		step = (Step) tableLines.get(7);
		Assert.assertEquals("Expected single step segment", 1, step.getSegments().size());
		Assert.assertEquals("Expected comment", SegmentType.COMMENT, step.getSegments().get(0).getSegmentType());
		
		// VARIABLE table
		Table variableTable = model.getTables().get(1);
		tableLines = variableTable.getTableLines();
		step = (Step) tableLines.get(0);
		
		Assert.assertEquals("Expected variable type", SegmentType.VARIABLE, step.getSegments().get(0).getSegmentType());
		Assert.assertEquals("Expected argument", SegmentType.ARGUMENT, step.getSegments().get(1).getSegmentType());
		
		step = (Step) tableLines.get(1);
		Assert.assertEquals("Expected 4 segments", 4, step.getSegments().size());
		
		step = (Step) tableLines.get(2);
		Assert.assertEquals("Expected continuation cell", SegmentType.CONTINUATION, step.getSegments().get(0).getSegmentType());
		Assert.assertEquals("Expected argument", SegmentType.ARGUMENT, step.getSegments().get(3).getSegmentType());
		
		// TESTCASE table
		Table testcaseTable = model.getTables().get(2);
		TableItemDefinition testcase = (TableItemDefinition) testcaseTable.getTableLines().get(0);
		
		// verify the multi-line documentation is correct
		step = testcase.getSteps().get(0);
		Assert.assertEquals("Expected documentation setting", SegmentType.SETTING_NAME, step.getSegments().get(0).getSegmentType());
		step = testcase.getSteps().get(1);
		Assert.assertEquals("Expected comment", SegmentType.COMMENT, step.getSegments().get(0).getSegmentType());
		step = testcase.getSteps().get(2);
		Assert.assertEquals("Expected continuation", SegmentType.CONTINUATION, step.getSegments().get(0).getSegmentType());
		Assert.assertEquals("Expected setting value after continuation", SegmentType.SETTING_VALUE, step.getSegments().get(1).getSegmentType());
		
		// verify the Run Keyword If 2nd arg is a keyword invocation
		// tests BuiltInKeywordArgumentsPostProcessor
		step = testcase.getSteps().get(3);
		Assert.assertEquals("Expected keyword", SegmentType.KEYWORD_CALL, step.getSegments().get(0).getSegmentType());
		Assert.assertEquals("Expected keyword call as argument", SegmentType.KEYWORD_CALL, step.getSegments().get(2).getSegmentType());
		
		testcase = (TableItemDefinition) testcaseTable.getTableLines().get(1);
		step = testcase.getSteps().get(0);
		// even though the template setting is on the same line, it parses as a logically-next line as the first step
		Assert.assertEquals("Expected testcase template setting on declaration line", SegmentType.SETTING_NAME, step.getSegments().get(0).getSegmentType());
		Assert.assertEquals("Expected testcase template cell", "[Template]", step.getSegments().get(0).getValue());
		// tests TestCaseTemplatePostProcessor
		Assert.assertEquals("Expected keyword as target of testcase template", SegmentType.KEYWORD_CALL, step.getSegments().get(1).getSegmentType());
		
		// KEYWORD table
		Table keywordTable = model.getTables().get(3);
		TableItemDefinition keyword = (TableItemDefinition) keywordTable.getTableLines().get(0);
		
		step = keyword.getSteps().get(0);
		Assert.assertEquals("Expected variable", SegmentType.VARIABLE, step.getSegments().get(0).getSegmentType());
		step = keyword.getSteps().get(1);
		// tests ForLoopPostProcessor
		Assert.assertEquals("Expected for loop", SegmentType.LOOP_CONSTRUCT, step.getSegments().get(0).getSegmentType());
		Assert.assertEquals("Expected variable", SegmentType.VARIABLE, step.getSegments().get(1).getSegmentType());
		Assert.assertEquals("Expected loop construct", SegmentType.LOOP_CONSTRUCT, step.getSegments().get(2).getSegmentType());
		Assert.assertEquals("Expected argument", SegmentType.ARGUMENT, step.getSegments().get(3).getSegmentType());
		
		keyword = (TableItemDefinition) (TableItemDefinition) keywordTable.getTableLines().get(1);
		Assert.assertEquals("Expected 2nd keyword body", "Pass execution", keyword.getSteps().get(0).getSegments().get(0).getValue());
		
		// check last line is valid comment - has no terminating linefeed so parser must stop early
		step = keyword.getSteps().get(keyword.getSteps().size()-1);
		Assert.assertEquals("Expected comment as last line", SegmentType.COMMENT, step.getSegments().get(0).getSegmentType());
	}
	
	@Test
	public void testBasicParseOfSpaceSeparatedFile() throws Exception {
		String buffer = loadFile("samplefiles/parserTest.robot");
		Parser parser = new Parser(mockTxtFile);
		RobotModel model = parser.parse(buffer);

		Assert.assertNotNull("Parser failure - model is null", model);
		
		Assert.assertEquals("There should be exactly 4 tables (bogus table name shouldn't get a node in the model)", 4, model.getTables().size());

		// we start with several comment lines before the first table
		Line line = model.getFirstLine();
		Assert.assertEquals("Expected comment line", SegmentType.COMMENT, ((Step) line).getSegments().get(0).getSegmentType());
		line = line.getNextLine();
		Assert.assertEquals("Expected comment line", SegmentType.COMMENT, ((Step) line).getSegments().get(0).getSegmentType());
		line = line.getNextLine();
		Assert.assertEquals("Expected comment line", SegmentType.COMMENT, ((Step) line).getSegments().get(0).getSegmentType());
		
		//  SETTING table
		Table settingsTable = model.getTables().get(0);
		Assert.assertEquals("Not the settings table!", settingsTable.getTableType(), TableType.SETTING);

		List<Line> tableLines = settingsTable.getTableLines();
		Step step = (Step) tableLines.get(0);
		// tests SetupTeardownPostProcessor
		Assert.assertEquals("Expected test setup", "Test Setup", step.getSegments().get(0).getValue());
		Assert.assertEquals("Expected setting name type", SegmentType.SETTING_NAME, step.getSegments().get(0).getSegmentType());
		Assert.assertEquals("Expected keyword invocation as target of test setup setting", SegmentType.KEYWORD_CALL, step.getSegments().get(1).getSegmentType());

		step = (Step) tableLines.get(1);
		Assert.assertEquals("Expected documentation", "Documentation", step.getSegments().get(0).getValue());
		Assert.assertEquals("Expected documentation setting value", SegmentType.SETTING_VALUE, step.getSegments().get(1).getSegmentType());

		// continuation of documentation onto second line
		step = (Step) tableLines.get(2);
		Assert.assertEquals("Expected continuation cell", SegmentType.CONTINUATION, step.getSegments().get(0).getSegmentType());
		Assert.assertEquals("Expected continued documentation setting value", SegmentType.SETTING_VALUE, step.getSegments().get(1).getSegmentType());

		// tests LibraryPostProcessor
		step = (Step) tableLines.get(4);
		Assert.assertEquals("Expected library", "Library", step.getSegments().get(0).getValue());
		Assert.assertEquals("Expected setting value for library name", SegmentType.SETTING_VALUE, step.getSegments().get(1).getSegmentType());
		Assert.assertEquals("Expected WITH NAME as control argument", SegmentType.CONTROL_ARGUMENT, step.getSegments().get(2).getSegmentType());
		Assert.assertEquals("Expected alias to be setting value", SegmentType.SETTING_VALUE, step.getSegments().get(3).getSegmentType());
		
		// check bad setting name
		step = (Step) tableLines.get(6);
		Assert.assertEquals("Expected bad setting name", "Bad setting name", step.getSegments().get(0).getValue());
		Assert.assertEquals("Expected unknown segment", SegmentType.UNKNOWN, step.getSegments().get(0).getSegmentType());
		Assert.assertEquals("Expected unknown segment", SegmentType.UNKNOWN, step.getSegments().get(1).getSegmentType());
		
		// multi-cell comment
		step = (Step) tableLines.get(7);
		Assert.assertEquals("Expected single step segment", 1, step.getSegments().size());
		Assert.assertEquals("Expected comment", SegmentType.COMMENT, step.getSegments().get(0).getSegmentType());
		
		// VARIABLE table
		Table variableTable = model.getTables().get(1);
		tableLines = variableTable.getTableLines();
		step = (Step) tableLines.get(0);
		
		Assert.assertEquals("Expected variable type", SegmentType.VARIABLE, step.getSegments().get(0).getSegmentType());
		Assert.assertEquals("Expected argument", SegmentType.ARGUMENT, step.getSegments().get(1).getSegmentType());
		
		step = (Step) tableLines.get(1);
		Assert.assertEquals("Expected 4 segments", 4, step.getSegments().size());
		
		step = (Step) tableLines.get(2);
		Assert.assertEquals("Expected continuation cell", SegmentType.CONTINUATION, step.getSegments().get(0).getSegmentType());
		Assert.assertEquals("Expected argument", SegmentType.ARGUMENT, step.getSegments().get(3).getSegmentType());
		
		// TESTCASE table
		Table testcaseTable = model.getTables().get(2);
		TableItemDefinition testcase = (TableItemDefinition) testcaseTable.getTableLines().get(0);
		
		// verify the multi-line documentation is correct
		step = testcase.getSteps().get(0);
		Assert.assertEquals("Expected documentation setting", SegmentType.SETTING_NAME, step.getSegments().get(0).getSegmentType());
		step = testcase.getSteps().get(1);
		Assert.assertEquals("Expected comment", SegmentType.COMMENT, step.getSegments().get(0).getSegmentType());
		step = testcase.getSteps().get(2);
		Assert.assertEquals("Expected continuation", SegmentType.CONTINUATION, step.getSegments().get(0).getSegmentType());
		Assert.assertEquals("Expected setting value after continuation", SegmentType.SETTING_VALUE, step.getSegments().get(1).getSegmentType());
		
		// verify the Run Keyword If 2nd arg is a keyword invocation
		// tests BuiltInKeywordArgumentsPostProcessor
		step = testcase.getSteps().get(3);
		Assert.assertEquals("Expected keyword", SegmentType.KEYWORD_CALL, step.getSegments().get(0).getSegmentType());
		Assert.assertEquals("Expected keyword call as argument", SegmentType.KEYWORD_CALL, step.getSegments().get(2).getSegmentType());
		
		testcase = (TableItemDefinition) testcaseTable.getTableLines().get(1);
		step = testcase.getSteps().get(0);
		// even though the template setting is on the same line, it parses as a logically-next line as the first step
		Assert.assertEquals("Expected testcase template setting on declaration line", SegmentType.SETTING_NAME, step.getSegments().get(0).getSegmentType());
		Assert.assertEquals("Expected testcase template cell", "[Template]", step.getSegments().get(0).getValue());
		// tests TestCaseTemplatePostProcessor
		Assert.assertEquals("Expected keyword as target of testcase template", SegmentType.KEYWORD_CALL, step.getSegments().get(1).getSegmentType());
		
		// KEYWORD table
		Table keywordTable = model.getTables().get(3);
		TableItemDefinition keyword = (TableItemDefinition) keywordTable.getTableLines().get(0);
		
		step = keyword.getSteps().get(0);
		Assert.assertEquals("Expected variable", SegmentType.VARIABLE, step.getSegments().get(0).getSegmentType());
		step = keyword.getSteps().get(1);
		// tests ForLoopPostProcessor
		Assert.assertEquals("Expected for loop", SegmentType.LOOP_CONSTRUCT, step.getSegments().get(0).getSegmentType());
		Assert.assertEquals("Expected variable", SegmentType.VARIABLE, step.getSegments().get(1).getSegmentType());
		Assert.assertEquals("Expected loop construct", SegmentType.LOOP_CONSTRUCT, step.getSegments().get(2).getSegmentType());
		Assert.assertEquals("Expected argument", SegmentType.ARGUMENT, step.getSegments().get(3).getSegmentType());
		
		keyword = (TableItemDefinition) (TableItemDefinition) keywordTable.getTableLines().get(1);
		Assert.assertEquals("Expected 2nd keyword body", "Pass execution", keyword.getSteps().get(0).getSegments().get(0).getValue());
		
		// check last line is valid comment - has no terminating linefeed so parser must stop early
		step = keyword.getSteps().get(keyword.getSteps().size()-1);
		Assert.assertEquals("Expected comment as last line", SegmentType.COMMENT, step.getSegments().get(0).getSegmentType());
	}
	
	
	
	private String loadFile(String testFileName) throws IOException {
		InputStream stream = null;
		try {
			stream = this.getClass().getResourceAsStream(testFileName);
			return new Scanner(stream).useDelimiter("\\A").next();
		} finally {
			if (stream != null) {
				stream.close();
			}
		}
		
	}
	
}