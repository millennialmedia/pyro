package com.millennialmedia.pyro.model.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * General Robot file constants.
 * 
 * @author spaxton
 */
public interface IModelConstants {
	String PROPSKEY_KEYWORD_NAME_TO_DEFINITION_MAP = "KEYWORDS_MAP";
	String PROPSKEY_RESOURCE_FILE_PATHS = "RESOURCE_FILE_PATHS";
	String PROPSKEY_VARIABLE_FILE_PATHS = "VARIABLE_FILE_PATHS";
	String PROPSKEY_LIBRARY_NAME_TO_PATH_MAP = "LIBRARY_NAME_TO_PATH_MAP";
	String PROPSKEY_ORDERED_LIBRARY_PATHS = "ORDERED_LIBRARY_PATHS";
	String PROPSKEY_VARIABLE_TABLE_VARS = "VARIABLE_TABLE_VARS";

	String GIVEN = "given";
	String WHEN = "when";
	String THEN = "then";
	String AND = "and";
	Set<String> BDD_PREFIXES = new HashSet<String>(Arrays.asList(new String[] { GIVEN, WHEN, THEN, AND }));

	String[] TESTCASE_SETTINGS = { "[Documentation]", "[Tags]", "[Setup]", "[Teardown]", "[Precondition]",
			"[Postcondition]", "[Timeout]", "[Template]" };

	String[] KEYWORD_SETTINGS = { "[Documentation]", "[Tags]", "[Arguments]", "[Return]", "[Teardown]", "[Timeout]" };

	String[] SETTINGS_TABLE_SETTINGS = { "Library", "Resource", "Variables", "Documentation", "Metadata",
			"Suite Setup", "Suite Teardown", "Suite Precondition", "Suite Postcondition", "Force Tags", "Default Tags",
			"Test Setup", "Test Teardown", "Test Precondition", "Test Postcondition", "Test Template", "Test Timeout" };

	String[] TABLE_NAMES = { "Test Case", "Test Cases", "Keyword", "Keywords", "User Keyword", "User Keywords",
			"Setting", "Settings", "Metadata", "Variable", "Variables" };

	Set<String> PREFERRED_TABLE_NAMES = new HashSet<String>(Arrays.asList(new String[] { "Test Cases", "Keywords",
			"Settings", "Variables" }));

	// from section 2.5.4 of Robot spec
	String[] BUILT_IN_VARIABLES = {
			"${True}",
			"${False}",
			"${None}",
			// "${Null}", // omitting because it's Java-only and Pyro only has Python support
			"${SPACE}", "${EMPTY}", "@{EMPTY}", "${/}", "${:}", "${\\n}", "${CURDIR}", "${TEMPDIR}", "${EXECDIR}",
			"${TEST NAME}", "@{TEST TAGS}", "${TEST DOCUMENTATION}", "${TEST STATUS}", "${TEST MESSAGE}",
			"${PREV TEST NAME}", "${PREV TEST STATUS}", "${PREV TEST MESSAGE}", "${SUITE NAME}", "${SUITE SOURCE}",
			"${SUITE DOCUMENTATION}", "${SUITE METADATA}", "${SUITE STATUS}", "${SUITE MESSAGE}", "${KEYWORD STATUS}",
			"${KEYWORD MESSAGE}", "${LOG LEVEL}", "${OUTPUT FILE}", "${LOG FILE}", "${REPORT FILE}", "${DEBUG FILE}",
			"${OUTPUT DIR}" };

}
