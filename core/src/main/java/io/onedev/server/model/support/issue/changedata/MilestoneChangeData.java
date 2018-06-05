package io.onedev.server.model.support.issue.changedata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.wicket.Component;

import io.onedev.server.model.Group;
import io.onedev.server.model.Issue;
import io.onedev.server.model.IssueChange;
import io.onedev.server.model.User;
import io.onedev.server.util.diff.DiffUtils;
import io.onedev.server.web.component.diff.plain.PlainDiffPanel;
import io.onedev.utils.HtmlUtils;

public class MilestoneChangeData implements ChangeData {

	private static final long serialVersionUID = 1L;

	private final String oldMilestone;
	
	private final String newMilestone;
	
	public MilestoneChangeData(@Nullable String oldMilestone, @Nullable String newMilestone) {
		this.oldMilestone = oldMilestone;
		this.newMilestone = newMilestone;
	}
	
	@Override
	public Component render(String componentId, IssueChange change) {
		return new PlainDiffPanel(componentId, getOldLines(), getNewLines());
	}
	
	private List<String> getOldLines() {
		List<String> oldLines = new ArrayList<>();
		if (oldMilestone != null)
			oldLines.add(oldMilestone);
		return oldLines;
	}

	private List<String> getNewLines() {
		List<String> newLines = new ArrayList<>();
		if (newMilestone != null)
			newLines.add(newMilestone);
		return newLines;
	}
	
	public String getOldMilestone() {
		return oldMilestone;
	}

	public String getNewMilestone() {
		return newMilestone;
	}

	@Override
	public String getTitle(IssueChange change, boolean external) {
		Issue issue = change.getIssue();
		if (external) 
			return String.format("[Milestone Changed] Issue #%d: %s", issue.getNumber(), issue.getTitle());  
		else 
			return "changed milestone";
	}

	@Override
	public String describeAsHtml(IssueChange change) {
		String escaped = HtmlUtils.escapeHtml(change.getUser().getDisplayName());
		StringBuilder builder = new StringBuilder(String.format("<b>%s changed milestone</b>", escaped));
		builder.append("<p style='margin: 16px 0;'>");
		builder.append(DiffUtils.diffAsHtml(getOldLines(), getNewLines()));
		return builder.toString();
	}

	@Override
	public String getComment() {
		return null;
	}

	@Override
	public Map<String, User> getNewUsers() {
		return new HashMap<>();
	}

	@Override
	public Map<String, Group> getNewGroups() {
		return new HashMap<>();
	}
	
}
