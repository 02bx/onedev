package io.onedev.server.model.support.issue.changedata;

import java.util.Map;

import org.apache.wicket.Component;

import io.onedev.server.model.Group;
import io.onedev.server.model.IssueChange;
import io.onedev.server.model.Project;
import io.onedev.server.model.User;
import io.onedev.server.util.CommentSupport;
import io.onedev.server.web.component.diff.plain.PlainDiffPanel;
import jersey.repackaged.com.google.common.collect.Lists;

public class IssueTitleChangeData implements IssueChangeData {

	private static final long serialVersionUID = 1L;

	private final String oldTitle;
	
	private final String newTitle;
	
	public IssueTitleChangeData(String oldTitle, String newTitle) {
		this.oldTitle = oldTitle;
		this.newTitle = newTitle;
	}
	
	@Override
	public Component render(String componentId, IssueChange change) {
		return new PlainDiffPanel(componentId, Lists.newArrayList(oldTitle), "a.txt", Lists.newArrayList(newTitle), "b.txt", true);
	}
	
	@Override
	public String getDescription() {
		return "changed title";
	}

	@Override
	public CommentSupport getCommentSupport() {
		return null;
	}
	
	@Override
	public Map<String, User> getNewUsers(Project project) {
		return null;
	}

	@Override
	public Map<String, Group> getNewGroups(Project project) {
		return null;
	}
	
}
