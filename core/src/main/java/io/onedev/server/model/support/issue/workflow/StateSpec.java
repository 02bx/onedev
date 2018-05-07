package io.onedev.server.model.support.issue.workflow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.validator.constraints.NotEmpty;

import io.onedev.server.util.editable.annotation.ChoiceProvider;
import io.onedev.server.util.editable.annotation.Color;
import io.onedev.server.util.editable.annotation.Editable;
import io.onedev.server.util.editable.annotation.NameOfEmptyValue;
import io.onedev.server.util.inputspec.InputSpec;
import io.onedev.server.web.page.project.setting.issueworkflow.states.IssueStatesPage;
import io.onedev.server.web.util.WicketUtils;

@Editable
public class StateSpec implements Serializable {

	private static final long serialVersionUID = 1L;

	private String name;
	
	private String description;
	
	private String color = "#777777";
	
	private List<String> fields = new ArrayList<>();
	
	@Editable(order=100)
	@NotEmpty
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Editable(order=200)
	@NameOfEmptyValue("No description")
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Editable(order=400, description="Specify color of the state for displaying purpose")
	@Color
	@NotEmpty(message="choose a color for this state")
	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	@Editable(order=500, name="Required Fields", description="Select issue fields required by this state. "
			+ "Required fields will be prompted for user input when issue transits to this state")
	@ChoiceProvider("getFieldChoices")
	@NameOfEmptyValue("No required fields")
	public List<String> getFields() {
		return fields;
	}

	public void setFields(List<String> fields) {
		this.fields = fields;
	}
	
	@SuppressWarnings("unused")
	private static List<String> getFieldChoices() {
		List<String> fields = new ArrayList<>();
		IssueStatesPage page = (IssueStatesPage) WicketUtils.getPage();
		for (InputSpec field: page.getWorkflow().getFields())
			fields.add(field.getName());
		return fields;
	}

	public void onFieldRename(String oldName, String newName) {
		for (int i=0; i<getFields().size(); i++) {
			if (getFields().get(i).equals(oldName))
				getFields().set(i, newName);
		}
	}
	
	public List<String> onFieldDelete(String fieldName) {
		for (Iterator<String> it = getFields().iterator(); it.hasNext();) {
			if (it.next().equals(fieldName))
				it.remove();
		}
		return new ArrayList<>();
	}
	
}
