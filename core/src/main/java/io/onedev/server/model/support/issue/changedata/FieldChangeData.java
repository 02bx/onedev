package io.onedev.server.model.support.issue.changedata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;

import io.onedev.server.OneDev;
import io.onedev.server.manager.GroupManager;
import io.onedev.server.manager.UserManager;
import io.onedev.server.model.Group;
import io.onedev.server.model.Issue;
import io.onedev.server.model.IssueChange;
import io.onedev.server.model.User;
import io.onedev.server.model.support.issue.PromptedField;
import io.onedev.server.util.diff.DiffUtils;
import io.onedev.server.util.inputspec.InputSpec;
import io.onedev.server.web.component.diff.plain.PlainDiffPanel;
import io.onedev.utils.HtmlUtils;
import io.onedev.utils.StringUtils;

public class FieldChangeData implements ChangeData {

	private static final long serialVersionUID = 1L;

	private final Map<String, PromptedField> oldFields;
	
	private final Map<String, PromptedField> newFields;
	
	public FieldChangeData(Map<String, PromptedField> oldFields, Map<String, PromptedField> newFields) {
		this.oldFields = copyFields(oldFields);
		this.newFields = copyFields(newFields);
		for (Iterator<Map.Entry<String, PromptedField>> it = this.oldFields.entrySet().iterator(); it.hasNext();) {
			Map.Entry<String, PromptedField> entry = it.next();
			if (entry.getValue().getValues().equals(this.newFields.get(entry.getKey()).getValues())) {
				this.newFields.remove(entry.getKey());
				it.remove();
			}
		}
	}

	public Map<String, PromptedField> getOldFields() {
		return oldFields;
	}

	public Map<String, PromptedField> getNewFields() {
		return newFields;
	}

	private Map<String, PromptedField> copyFields(Map<String, PromptedField> fields) {
		Map<String, PromptedField> copyOfFields = new LinkedHashMap<>();
		for (Map.Entry<String, PromptedField> entry: fields.entrySet()) {
			if (!entry.getValue().getValues().contains(null) && !entry.getValue().getValues().isEmpty())
				copyOfFields.put(entry.getKey(), entry.getValue());
		}
		return copyOfFields;
	}
	
	@Override
	public Component render(String componentId, IssueChange change) {
		return new PlainDiffPanel(componentId, getLines(oldFields), getLines(newFields));
	}

	@Override
	public String getTitle(IssueChange change, boolean external) {
		Issue issue = change.getIssue();
		if (external) 
			return String.format("[Custom Fields Changed] Issue #%d: %s", issue.getNumber(), issue.getTitle());  
		else 
			return "changed custom fields";
	}

	public List<String> getLines(Map<String, PromptedField> fields) {
		List<String> lines = new ArrayList<>();
		for (Map.Entry<String, PromptedField> entry: fields.entrySet()) {
			if (entry.getValue().getType().equals(InputSpec.ISSUE_CHOICE))
				lines.add(entry.getKey() + ": #" + entry.getValue().getValues().iterator().next());
			else
				lines.add(entry.getKey() + ": " + StringUtils.join(entry.getValue().getValues(), ", "));
		}
		return lines;
	}
	
	@Override
	public String describeAsHtml(IssueChange change) {
		String escaped = HtmlUtils.escapeHtml(change.getUser().getDisplayName());
		StringBuilder builder = new StringBuilder(String.format("<b>%s changed custom fields</b>", escaped));
		builder.append("<p style='margin: 16px 0;'>");
		builder.append(DiffUtils.diffAsHtml(getLines(oldFields), getLines(newFields)));
		return builder.toString();
	}

	@Override
	public String getComment() {
		return null;
	}

	@Override
	public Map<String, User> getNewUsers() {
		Map<String, User> newUsers = new HashMap<>();
		for (PromptedField field: newFields.values()) {
			if (field.getType().equals(InputSpec.USER_CHOICE)) {
				User user = OneDev.getInstance(UserManager.class).findByName(field.getValues().iterator().next());
				if (user != null)
					newUsers.put(field.getName(), user);
			}
		}
		return newUsers;
	}

	@Override
	public Map<String, Group> getNewGroups() {
		Map<String, Group> newGroups = new HashMap<>();
		for (PromptedField field: newFields.values()) {
			if (field.getType().equals(InputSpec.GROUP_CHOICE)) {
				Group group = OneDev.getInstance(GroupManager.class).find(field.getValues().iterator().next());
				if (group != null)
					newGroups.put(field.getName(), group);
			}
		}
		return newGroups;
	}

}
