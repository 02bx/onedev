package io.onedev.server.util.inputspec;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.lang.SerializationUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unbescape.java.JavaEscape;

import com.google.common.collect.Lists;

import io.onedev.server.util.GroovyUtils;
import io.onedev.server.util.OneContext;
import io.onedev.server.util.inputspec.showcondition.ShowCondition;
import io.onedev.server.util.validation.annotation.InputName;
import io.onedev.server.web.editable.EditableUtils;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.Multiline;
import io.onedev.server.web.editable.annotation.NameOfEmptyValue;
import io.onedev.server.web.page.project.setting.issueworkflow.fields.ShowConditionListWrapper;
import io.onedev.server.web.page.project.setting.issueworkflow.fields.ShowConditionOuterWrapper;

@Editable
public abstract class InputSpec implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private static final Logger logger = LoggerFactory.getLogger(InputSpec.class);
	
	public static final String BOOLEAN = "Checkbox";

	public static final String TEXT = "Text";
	
	public static final String DATE = "Date";
	
	public static final String PASSWORD = "Password";
	
	public static final String NUMBER = "Number";
	
	public static final String COMMIT = "Commit";
	
	public static final String ENUMERATION = "Enumeration";
	
	public static final String USER = "User";
	
	public static final String GROUP = "Group";
	
	public static final String ISSUE = "Issue";
	
	public static final String BUILD = "Build";

	public static final String PULLREQUEST = "Pull request";
	
	private String name;

	private String description;

	private boolean allowMultiple;
	
	private boolean allowEmpty;
	
	private String nameOfEmptyValue;
	
	private List<ShowCondition> showConditions;
	
	// Added to edit/view show conditions in a user-friendly way
	private transient ShowConditionListWrapper showConditionListWrapper;
	
	@Editable(order=10)
	@InputName
	@NotEmpty
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Editable(order=30, description="resource.input.description")
	@NameOfEmptyValue("No description")
	@Multiline
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Editable(order=35, description="resource.input.allowMultiple")
	public boolean isAllowMultiple() {
		return allowMultiple;
	}

	public void setAllowMultiple(boolean allowMultiple) {
		this.allowMultiple = allowMultiple;
	}

	public List<ShowCondition> getShowConditions() {
		return showConditions;
	}

	public void setShowConditions(List<ShowCondition> showConditions) {
		this.showConditions = showConditions;
	}

	@Editable(order=40, name="Show Conditionally", description="resource.input.showCondition")
	@NameOfEmptyValue("Always")
	public ShowConditionListWrapper getShowConditionListWrapper() {
		return showConditionListWrapper;
	}

	public void setShowConditionListWrapper(ShowConditionListWrapper showConditionListWrapper) {
		this.showConditionListWrapper = showConditionListWrapper;
	}
	
	@Editable(order=50, name="Allow Empty Value", description="resource.input.allowEmpty")
	public boolean isAllowEmpty() {
		return allowEmpty;
	}

	public void setAllowEmpty(boolean allowEmpty) {
		this.allowEmpty = allowEmpty;
	}
	
	@Editable(order=60)
	@io.onedev.server.web.editable.annotation.ShowCondition("isNameOfEmptyValueVisible")
	@NotEmpty
	public String getNameOfEmptyValue() {
		return nameOfEmptyValue;
	}

	public void setNameOfEmptyValue(String nameOfEmptyValue) {
		this.nameOfEmptyValue = nameOfEmptyValue;
	}
	
	@SuppressWarnings("unused")
	private static boolean isNameOfEmptyValueVisible() {
		return (boolean) OneContext.get().getEditContext().getInputValue("allowEmpty");
	}
	
	public List<String> getPossibleValues() {
		return Lists.newArrayList();
	}

	protected String escape(String string) {
		String escaped = JavaEscape.escapeJava(string);
		// escape $ character since it has special meaning in groovy string
		escaped = escaped.replace("$", "\\$");

		return escaped;
	}
	
	public abstract String getPropertyDef(Map<String, Integer> indexes);
	
	protected String getLiteral(byte[] bytes) {
		StringBuffer buffer = new StringBuffer("[");
		for (byte eachByte: bytes) {
			buffer.append(String.format("%d", eachByte)).append(",");
		}
		buffer.append("] as byte[]");
		return buffer.toString();
	}

	protected void appendField(StringBuffer buffer, int index, String type) {
		buffer.append("    private Optional<" + type + "> input" + index + ";\n");
		buffer.append("\n");
	}
	
	protected void appendChoiceProvider(StringBuffer buffer, int index, String annotation) {
		buffer.append("    " + annotation + "(\"getInput" + index + "Choices\")\n");		
	}
	
	protected void appendCommonAnnotations(StringBuffer buffer, int index) {
		if (description != null) {
			buffer.append("    @Editable(name=\"" + escape(name) + "\", description=\"" + 
					escape(description) + "\", order=" + index + ")\n");
		} else {
			buffer.append("    @Editable(name=\"" + escape(name) + 
					"\", order=" + index + ")\n");
		}
		if (showConditions != null) 
			buffer.append("    @ShowCondition(\"isInput" + index + "Visible\")\n");
		if (getNameOfEmptyValue() != null)
			buffer.append("    @NameOfEmptyValue(\"" + escape(getNameOfEmptyValue()) + "\")");
	}
	
	protected void appendMethods(StringBuffer buffer, int index, String type, 
			@Nullable Serializable choiceProvider, @Nullable Serializable defaultValueProvider) {
		String literalBytes = getLiteral(SerializationUtils.serialize(defaultValueProvider));
		buffer.append("    public " + type + " getInput" + index + "() {\n");
		buffer.append("        if (input" + index + "!=null) {\n");
		buffer.append("            return input" + index + ".orNull();\n");
		buffer.append("        } else {\n");
		if (defaultValueProvider != null)
			buffer.append("            return SerializationUtils.deserialize(" + literalBytes + ").getDefaultValue();\n");
		else
			buffer.append("        return null;\n");
		buffer.append("        }\n");
		buffer.append("    }\n");
		buffer.append("\n");
		
		buffer.append("    public void setInput" + index + "(" + type + " value) {\n");
		buffer.append("        this.input" + index + "=Optional.fromNullable(value);\n");
		buffer.append("    }\n");
		buffer.append("\n");
		
		if (showConditions != null) {
			buffer.append("    private static boolean isInput" + index + "Visible() {\n");
			literalBytes = getLiteral(SerializationUtils.serialize((Serializable) showConditions));
			buffer.append("        for (condition in SerializationUtils.deserialize(" + literalBytes + ")) {\n");
			buffer.append("            if (!condition.isVisible())\n");
			buffer.append("                return false;\n");
			buffer.append("        }\n");
			buffer.append("        return true;\n");
			buffer.append("    }\n");
			buffer.append("\n");
		}

		if (choiceProvider != null) {
			buffer.append("    private static List getInput" + index + "Choices() {\n");
			literalBytes = getLiteral(SerializationUtils.serialize(choiceProvider));
			if (choiceProvider instanceof io.onedev.server.util.inputspec.choiceinput.choiceprovider.ChoiceProvider) {
				buffer.append("        return new ArrayList(SerializationUtils.deserialize(" + literalBytes + ").getChoices(false).keySet());\n");
			} else {
				buffer.append("        return SerializationUtils.deserialize(" + literalBytes + ").getChoices(false);\n");
			}
			buffer.append("    }\n");
			buffer.append("\n");
		}
	}

	public void setupShowConditionsForDisplay() {
		if (showConditions != null) {
			showConditionListWrapper = new ShowConditionListWrapper();
			for (ShowCondition condition: showConditions) {
				ShowConditionOuterWrapper outerWrapper = new ShowConditionOuterWrapper();
				outerWrapper.getInnerWrapper().setCondition(condition);
				showConditionListWrapper.getOuterWrappers().add(outerWrapper);
			}
		} else {
			showConditionListWrapper = null;
		}
	}
	
	public void setupShowConditionsForStorage() {
		if (showConditionListWrapper != null) {
			showConditions = new ArrayList<>();
			for (ShowConditionOuterWrapper outerWrapper: showConditionListWrapper.getOuterWrappers()) 
				showConditions.add(outerWrapper.getInnerWrapper().getCondition());
		} else {
			showConditions = null;
		}
	}
	
	public void onRenameInput(String oldName, String newName) {
		if (showConditions != null) {
			for (ShowCondition condition: showConditions) {
				if (oldName.equals(condition.getInputName()))
					condition.setInputName(newName);
			}
		}
	}
	
	public boolean onDeleteInput(String inputName) {
		if (showConditions != null) {
			for (Iterator<ShowCondition> it = showConditions.iterator(); it.hasNext();) {
				if (inputName.equals(it.next().getInputName()))
					it.remove();
			}
			if (showConditions.isEmpty())
				showConditions = null;
		} 
		return false;
	}
	
	public static Class<?> defineClass(String className, List<InputSpec> inputs) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("import org.apache.commons.lang3.SerializationUtils;\n");
		buffer.append("import com.google.common.base.Optional;\n");
		buffer.append("import io.onedev.server.web.editable.annotation.*;\n");
		buffer.append("import io.onedev.util.*;\n");
		buffer.append("import io.onedev.server.util.*;\n");
		buffer.append("import io.onedev.server.util.facade.*;\n");
		buffer.append("import java.util.*;\n");
		buffer.append("import javax.validation.constraints.*;\n");
		buffer.append("import org.hibernate.validator.constraints.*;\n");
		buffer.append("\n");
		buffer.append("@Editable\n");
		buffer.append("class " + className + " implements java.io.Serializable {\n");
		buffer.append("\n");
		buffer.append("    private static final long serialVersionUID = 1L;\n");
		buffer.append("\n");
		Map<String, Integer> indexes = new HashMap<>();
		int index = 1;
		for (InputSpec input: inputs)
			indexes.put(input.getName(), index++);
		for (InputSpec input: inputs)
			buffer.append(input.getPropertyDef(indexes));

		buffer.append("}\n");
		buffer.append("return " + className + ";\n");
		
		logger.trace("Class definition script:\n" + buffer.toString());
		
		return (Class<?>) GroovyUtils.evalScript(buffer.toString(), new HashMap<>());
	}

	public void onRenameUser(String oldName, String newName) {
		
	}
	
	public void onRenameGroup(String oldName, String newName) {
		
	}

	public boolean onDeleteUser(String userName) {
		return false;
	}
	
	public boolean onDeleteGroup(String groupName) {
		return false;
	}
	
	public abstract List<String> convertToStrings(Object object);

	/**
	 * Convert list of strings to object
	 * 
	 * @param strings
	 * 			list of strings, will not be empty
	 * @return
	 * 			converted object
	 */
	public abstract Object convertToObject(List<String> strings);
	
	public long getOrdinal(Object fieldValue) {
		return -1;
	}
	
	public String getType() {
		return EditableUtils.getDisplayName(getClass());		
	}
	
}
