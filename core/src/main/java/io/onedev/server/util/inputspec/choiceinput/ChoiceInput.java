package io.onedev.server.util.inputspec.choiceinput;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.validation.Validator;
import javax.validation.constraints.NotNull;

import io.onedev.server.OneDev;
import io.onedev.server.util.editable.annotation.Editable;
import io.onedev.server.util.editable.annotation.NameOfEmptyValue;
import io.onedev.server.util.inputspec.InputSpec;
import io.onedev.server.util.inputspec.choiceinput.defaultvalueprovider.DefaultValueProvider;
import io.onedev.server.util.inputspec.choiceprovider.ChoiceProvider;
import io.onedev.server.util.inputspec.choiceprovider.SpecifiedChoices;

@Editable(order=145, name=InputSpec.CHOICE)
public class ChoiceInput extends InputSpec {
	
	private static final long serialVersionUID = 1L;

	private ChoiceProvider choiceProvider = new SpecifiedChoices();

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
		if (OneDev.getInstance(Validator.class).validate(getChoiceProvider()).isEmpty())
			return getChoiceProvider().getChoices(true);
		else
			return new ArrayList<>();
	}

	@Override
	public String getPropertyDef(Map<String, Integer> indexes) {
		int index = indexes.get(getName());
		StringBuffer buffer = new StringBuffer();
		appendField(buffer, index, "String");
		appendAnnotations(buffer, index, "NotEmpty", "ChoiceProvider", defaultValueProvider!=null);
		appendMethods(buffer, index, "String", choiceProvider, defaultValueProvider);
		
		return buffer.toString();
	}

	@Override
	public Object toObject(String string) {
		return string;
	}

	@Override
	public String toString(Object value) {
		return (String) value;
	}

}
