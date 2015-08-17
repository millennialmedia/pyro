package com.millennialmedia.pyro.ui.contentassist;

import java.util.List;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;

import com.millennialmedia.pyro.model.Line;
import com.millennialmedia.pyro.model.RobotModel;
import com.millennialmedia.pyro.model.Step;
import com.millennialmedia.pyro.model.StepSegment;
import com.millennialmedia.pyro.model.StepSegment.SegmentType;
import com.millennialmedia.pyro.model.Table;
import com.millennialmedia.pyro.model.Table.TableType;
import com.millennialmedia.pyro.model.util.ModelUtil;
import com.millennialmedia.pyro.ui.editor.AbstractEditorAwareContributor;
import com.millennialmedia.pyro.ui.editor.util.PathUtil;

/**
 * Base class for all contributors that provide content assist proposals. The
 * general pattern is to create a separate contributor class for each
 * fairly-narrow scenario and aggregate the proposals by making multiple
 * contributions (with appropriate priority ordering) to the
 * {@code com.millennialmedia.pyro.ui.contentAssistProcessor} extension point.
 * 
 * @author spaxton
 */
public class ContentAssistContributorBase extends AbstractEditorAwareContributor {
	private Image proposalImage;

	/**
	 * Add any relevant completion proposals to the given list. In general,
	 * implementations should always call {@link #addCompletionProposal} when
	 * adding new contributions into the list.
	 */
	public void computeCompletionProposals(ITextViewer viewer, int offset, List<RobotCompletionProposal> proposals) {
	}

	protected void addCompletionProposal(List<RobotCompletionProposal> proposals, String replacementString,
			int replacementOffset, int replacementLength, int cursorPosition, String displayString,
			StyledString styledDisplayString) {
		for (RobotCompletionProposal proposal : proposals) {
			if (proposal.getReplacementString().equals(replacementString)) {
				// there's no scenario where we'd want to show the same proposal
				// from two different sources, so reject "duplicates".
				// an example scenario is multiple robot keywords of the same
				// name (ex. in different python libraries) - at runtime only
				// one keyword is available so we rely on the assist processors
				// being contributed in the correct order and simply return
				// the first found without adding a redundant (and
				// runtime-unreachable) proposal here.
				return;
			}
		}

		if (styledDisplayString == null) {
			styledDisplayString = new StyledString();
			styledDisplayString.append(displayString, RobotCompletionProposal.FOREGROUND_STYLER);

		}

		RobotCompletionProposal proposal = new RobotCompletionProposal(replacementString, replacementOffset,
				replacementLength, cursorPosition, proposalImage, displayString, new ContextInformation("", ""), "",
				styledDisplayString);
		proposals.add(proposal);
	}

	/**
	 * Sets the icon image to use for all subsequent calls to @
	 * #addCompletionProposal}
	 */
	protected void setProposalImage(Image image) {
		this.proposalImage = image;
	}

	/**
	 * If the given offset points to a location in a segment of the desired
	 * type, this method returns a pair of strings representing the portions
	 * before and after the caret location of that offset.
	 * 
	 * @param offset
	 *            integer offset location into document
	 * @return a 2-element string array with the string sections, or null if the
	 *         target offset is not of the given type
	 */
	protected String[] getStringFragments(int offset, SegmentType targetType) {
		RobotModel model = getEditor().getModel();
		Line line = model.getFirstLine();
		Line previousLine = line;
		// advance to a line that we know contains the caret location
		while (line != null && line.getLineOffset() <= offset) {
			previousLine = line;
			line = line.getNextLine();
		}

		// previousLine now points to our current line
		if (previousLine != null && previousLine instanceof Step) {
			Step step = (Step) previousLine;
			int lineOffset = previousLine.getLineOffset();
			
			// walk through the segment in this step to find the one containing the caret
			for (int i=0;i<step.getSegments().size(); i++) {
				StepSegment segment = step.getSegments().get(i);
				if (segment.getSegmentType() == targetType &&                 // is of the right type 
						lineOffset + segment.getOffsetInLine() <= offset &&   // beginning of cell is before the caret offset
						((lineOffset + 
							segment.getOffsetInLine() + 
							segment.getValue().length() >= offset) ||         // caret is before end of cell
						(i == step.getSegments().size()-1))                    // or if not, is just after the last cell (whitespace is trimmed)
						) {

					String value = segment.getValue();
					int offsetInSegment = offset - previousLine.getLineOffset() - segment.getOffsetInLine();
					if (value.length() < offsetInSegment ) {
						// if necessary, pad spaces to the right of the trimmed cell contents
						for (int paddingCount=0; paddingCount < offsetInSegment-value.length(); paddingCount++) {
							value = value + " ";
						}
					}

					return new String[] {
							value.substring(0,
									offset - previousLine.getLineOffset() - segment.getOffsetInLine()),
							value.substring(
									offset - previousLine.getLineOffset() - segment.getOffsetInLine()) };
				}
			}
		}

		return null;
	}

	protected Table.TableType getContainingTableType(int offset) {
		Table containingTable = ModelUtil.getContainingTable(getEditor().getModel(), offset);
		return containingTable == null ? null : containingTable.getTableType();
	}

	protected Line getRobotLineForOffset(int offset) {
		RobotModel model = getEditor().getModel();
		Line line = model.getFirstLine();
		while (line != null && line.getLineOffset() + line.getLineLength() <= offset) {
			line = line.getNextLine();
		}
		return line;
	}

	/**
	 * Checks whether or not the current offset is plausibly the beginning of a
	 * new step segment of the specified type.
	 */
	protected boolean isPotentialEmptyStepSegment(int offset, SegmentType segmentType) {
		TableType tableType = getContainingTableType(offset);
		if (tableType == TableType.TESTCASE || tableType == TableType.KEYWORD) {
			// we're inside the right kind of table
			Line line = getRobotLineForOffset(offset);
			if (offset < line.getLineOffset()) {
				// the actual line for this offset didn't parse into anything
				// useful in the robot model, probably because it's whitespace.
				// look for the possible conditions where we'd begin a new
				// segment
				int lineNum = getEditor().getViewer().getTextWidget().getLineAtOffset(offset);
				String lineContents = getEditor().getViewer().getTextWidget().getLine(lineNum);
				if (("tsv".equals(PathUtil.getEditorFile(getEditor()).getFileExtension()) && lineContents
						.contains("\t")) || // tsv case
						lineContents.length() > 1 && (lineContents.startsWith("  ") || lineContents.startsWith("| "))) { // txt,robot
																															// case
					return true;
				}
			} else {
				// we're on a valid parsed line - now look for an offset that's
				// beginning a new segment and validate it's allowable here
				if (line instanceof Step) {
					boolean seenKeyword = false;
					boolean hasNonEmptySegment = false;
					List<StepSegment> segments = ((Step) line).getSegments();
					for (StepSegment segment : segments) {
						if (!"".equals(segment.getValue().trim())) {
							hasNonEmptySegment = true;
							if (segment.getSegmentType() == SegmentType.KEYWORD_CALL) {
								seenKeyword = true;
							} else if (segment.getSegmentType() == SegmentType.LOOP_CONSTRUCT) {
								return false;
							} else if (segment.getSegmentType() == SegmentType.SETTING_NAME) {
								return false;
							}
						}

						if (line.getLineOffset() + segment.getOffsetInLine() <= offset
								&& line.getLineOffset() + segment.getOffsetInLine() + segment.getValue().length() >= offset) {
							if (isValidPosition(segment, segmentType, seenKeyword, hasNonEmptySegment)) {
								return true;
							}
						}
					}
					StepSegment lastSegment = segments.get(segments.size() - 1);
					if (line.getLineOffset() + lastSegment.getOffsetInLine() + lastSegment.getValue().length() < offset
							&& isValidPosition(null, segmentType, seenKeyword, hasNonEmptySegment)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean isValidPosition(StepSegment segment, SegmentType desiredSegmentType, boolean seenKeyword,
			boolean hasNonEmptySegment) {
		if (segment == null || segment.getValue().trim().equals("")) {
			if (desiredSegmentType == SegmentType.KEYWORD_CALL && !seenKeyword) {
				return true;
			} else if (desiredSegmentType == SegmentType.VARIABLE && !seenKeyword) {
				return true;
			} else if (desiredSegmentType == SegmentType.SETTING_NAME && !hasNonEmptySegment) {
				return true;
			} else if (desiredSegmentType == SegmentType.ARGUMENT && seenKeyword) {
				return true;
			}
		}
		return false;
	}

	protected boolean isEmptyLineInTable(int offset, TableType desiredTableType) {
		TableType tableType = getContainingTableType(offset);
		if (tableType != null && (desiredTableType == null || desiredTableType == tableType)) {
			// we're inside the right kind of table
			int lineNum = getEditor().getViewer().getTextWidget().getLineAtOffset(offset);
			String lineContents = getEditor().getViewer().getTextWidget().getLine(lineNum);
			if (lineContents.trim().equals("")) {
				return true;
			}
		}
		return false;
	}

}
