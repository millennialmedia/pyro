package com.millennialmedia.pyro.ui.internal.contentassist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.text.ITextViewer;

import com.millennialmedia.pyro.model.StepSegment.SegmentType;
import com.millennialmedia.pyro.model.Table.TableType;
import com.millennialmedia.pyro.model.util.IModelConstants;
import com.millennialmedia.pyro.ui.PyroUIPlugin;
import com.millennialmedia.pyro.ui.contentassist.ContentAssistContributorBase;
import com.millennialmedia.pyro.ui.contentassist.RobotCompletionProposal;

/**
 * Contributor for setting names within Setting, Testcase, or Keyword tables.
 * 
 * @author spaxton
 */
public class SettingsAssistContributor extends ContentAssistContributorBase {

	public SettingsAssistContributor() {
		setProposalImage(PyroUIPlugin.getDefault().getImageRegistry().get(PyroUIPlugin.IMAGE_SETTING));
	}

	@Override
	public void computeCompletionProposals(ITextViewer viewer, int offset, List<RobotCompletionProposal> proposals) {
		TableType tableType = getContainingTableType(offset);
		String[] settingFragments = getStringFragments(offset, SegmentType.SETTING_NAME);

		// for in-progress words under a setting table, they will parse as unknown 
		// but may be the beginning of a valid setting name
		if(settingFragments == null && tableType == TableType.SETTING) {
			settingFragments = getStringFragments(offset, SegmentType.UNKNOWN);
		}
		
		// if we still don't have a partial string, see if we're starting a line or a valid cell 
		if (settingFragments == null
				&& (isPotentialEmptyStepSegment(offset, SegmentType.SETTING_NAME) 
					|| isEmptyLineInTable(offset, TableType.SETTING))) {
			settingFragments = new String[] { "", "" };
		}

		if (settingFragments != null) {
			String[] availableSettings = null;
			switch (tableType) {
			case TESTCASE:
				availableSettings = IModelConstants.TESTCASE_SETTINGS;
				break;
			case KEYWORD:
				availableSettings = IModelConstants.KEYWORD_SETTINGS;
				break;
			case SETTING:
				availableSettings = IModelConstants.SETTINGS_TABLE_SETTINGS;
			default:
				break;
			}

			if (availableSettings != null) {
				List<String> possibleSettings = getCandidateSettings(settingFragments[0], availableSettings);

				for (String setting : possibleSettings) {
					addCompletionProposal(proposals, setting, offset - settingFragments[0].length(),
							settingFragments[0].length() + settingFragments[1].length(), setting.length(), setting,
							null);
				}
			}
		}
	}

	private List<String> getCandidateSettings(String fragment, String[] availableSettings) {
		String fragmentLc = fragment.toLowerCase();
		List<String> candidates = new ArrayList<String>();
		for (String availableSetting : availableSettings) {
			if (availableSetting.toLowerCase().startsWith(fragmentLc)) {
				candidates.add(availableSetting);
			}
		}
		Collections.sort(candidates);
		return candidates;
	}

}
