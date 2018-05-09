package io.onedev.server.util.inputspec.userchoiceinput;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.validation.Validator;
import javax.validation.constraints.NotNull;

import io.onedev.server.OneDev;
import io.onedev.server.util.editable.annotation.Editable;
import io.onedev.server.util.editable.annotation.NameOfEmptyValue;
import io.onedev.server.util.facade.UserFacade;
import io.onedev.server.util.inputspec.InputSpec;
import io.onedev.server.util.inputspec.userchoiceinput.defaultvalueprovider.DefaultValueProvider;
import io.onedev.server.util.inputspec.userchoiceinput.defaultvalueprovider.SpecifiedDefaultValue;
import io.onedev.server.util.inputspec.userchoiceprovider.ChoiceProvider;
import io.onedev.server.util.inputspec.userchoiceprovider.GroupUsers;
import io.onedev.server.util.inputspec.userchoiceprovider.ProjectReaders;
import jersey.repackaged.com.google.common.collect.Lists;

@Editable(order=150, name=InputSpec.USER_CHOICE)
public class UserChoiceInput extends InputSpec {
	
	private static final long serialVersionUID = 1L;

	private ChoiceProvider choiceProvider = new ProjectReaders();

	private DefaultValueProvider defaultValueProvider;
	
	@Editable(order=1000, name="Available Choices")
	@NotNull(message="may not be empty")
	public ChoiceProvider getChoiceProvider() {
		return choiceProvider;
	}

	public void setChoiceProvider(ChoiceProvider choiceProvider) {
		this.choiceProvider = choiceProvider;
	}

	@Editable(order=1100, name="Default Value")
	@NameOfEmptyValue("No default value")
	public DefaultValueProvider getDefaultValueProvider() {
		return defaultValueProvider;
	}

	public void setDefaultValueProvider(DefaultValueProvider defaultValueProvider) {
		this.defaultValueProvider = defaultValueProvider;
	}

	@Override
	public List<String> getPossibleValues() {
		List<String> possibleValues = new ArrayList<>();
		if (OneDev.getInstance(Validator.class).validate(getChoiceProvider()).isEmpty()) {
			for (UserFacade user: getChoiceProvider().getChoices(true))
				possibleValues.add(user.getName());
		}
		return possibleValues;
	}

	@Override
	public String getPropertyDef(Map<String, Integer> indexes) {
		int index = indexes.get(getName());
		StringBuffer buffer = new StringBuffer();
		appendField(buffer, index, "String");
		appendCommonAnnotations(buffer, index);
		if (!isAllowEmpty())
			buffer.append("    @NotEmpty\n");
		appendChoiceProvider(buffer, index, "@UserChoice");
		if (defaultValueProvider != null)
			appendDefaultValueProvider(buffer, index);
		appendMethods(buffer, index, "String", choiceProvider, defaultValueProvider);
		
		return buffer.toString();
	}

	@Override
	public void onRenameUser(String oldName, String newName) {
		if (defaultValueProvider instanceof SpecifiedDefaultValue) {
			SpecifiedDefaultValue specifiedDefaultValue = (SpecifiedDefaultValue) defaultValueProvider;
			if (specifiedDefaultValue.getValue().equals(oldName))
				specifiedDefaultValue.setValue(newName);
		}
	}

	@Override
	public List<String> onDeleteUser(String userName) {
		List<String> usages = super.onDeleteUser(userName);
		if (defaultValueProvider instanceof SpecifiedDefaultValue) {
			SpecifiedDefaultValue specifiedDefaultValue = (SpecifiedDefaultValue) defaultValueProvider;
			if (specifiedDefaultValue.getValue().equals(userName))
				usages.add("Default Value");
		}
		return usages;
	}

	@Override
	public void onRenameGroup(String oldName, String newName) {
		if (choiceProvider instanceof GroupUsers) {
			GroupUsers groupUsers = (GroupUsers) choiceProvider;
			if (groupUsers.getGroupName().equals(oldName))
				groupUsers.setGroupName(newName);
		}
	}

	@Override
	public List<String> onDeleteGroup(String groupName) {
		List<String> usages = super.onDeleteGroup(groupName);
		if (choiceProvider instanceof GroupUsers) {
			GroupUsers groupUsers = (GroupUsers) choiceProvider;
			if (groupUsers.getGroupName().equals(groupName))
				usages.add("Available Choices");
		}
		return usages;
	}
	
	@Override
	public Object convertToObject(List<String> strings) {
		return strings.iterator().next();
	}

	@Override
	public List<String> convertToStrings(Object value) {
		return Lists.newArrayList((String) value);
	}

}
