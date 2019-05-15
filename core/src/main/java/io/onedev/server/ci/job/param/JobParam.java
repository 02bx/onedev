package io.onedev.server.ci.job.param;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.validation.ValidationException;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import io.onedev.server.util.inputspec.InputSpec;

public class JobParam implements Serializable {

	private static final long serialVersionUID = 1L;
	
	public static final String STRING_SEPARATOR = "@@";
	
	private String name;
	
	private ValuesProvider valuesProvider = new SpecifiedValues();

	@NotEmpty
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@NotNull
	public ValuesProvider getValuesProvider() {
		return valuesProvider;
	}

	public void setValuesProvider(ValuesProvider valuesProvider) {
		this.valuesProvider = valuesProvider;
	}

	@Nullable
	public static String toValue(List<String> strings) {
		if (!strings.isEmpty())
			return Joiner.on(STRING_SEPARATOR).join(strings);
		else
			return null;
	}
	
	public static List<String> fromValue(@Nullable String value) {
		if (value != null) 
			return Splitter.on(STRING_SEPARATOR).splitToList(value);
		else 
			return new ArrayList<>();
	}
	
	public static void validateValues(List<String> values) {
		if (values.isEmpty())
			throw new ValidationException("At least one value needs to be specified");
		Set<String> valueSet = new HashSet<>();
		for (String value: values) {
			if (valueSet.contains(value)) {
				if (value != null)
					throw new ValidationException("Duplicate values are not allowed: " + value);
				else
					throw new ValidationException("Empty value added multiple times");
			} else {
				valueSet.add(value);
			}
		}
	}
	
	public static void validateParams(List<InputSpec> paramSpecs, Map<String, List<String>> params) {
		Map<String, InputSpec> paramSpecMap = new HashMap<>();
		for (InputSpec paramSpec: paramSpecs) {
			paramSpecMap.put(paramSpec.getName(), paramSpec);
			if (!params.containsKey(paramSpec.getName()))
				throw new ValidationException("Missing job parameter: " + paramSpec.getName());
		}
		
		for (Map.Entry<String, List<String>> entry: params.entrySet()) {
			InputSpec paramSpec = paramSpecMap.get(entry.getKey());
			if (paramSpec == null)
				throw new ValidationException("Unknown job parameter: " + entry.getKey());
			
			if (entry.getValue() != null) {
				try {
					validateValues(entry.getValue());
				} catch (ValidationException e) {
					throw new ValidationException("Error validating values of parameter '" 
							+ entry.getKey() + "': " + e.getMessage());
				}
				
				for (String value: entry.getValue()) {
					List<String> strings = fromValue(value);
					try {
						paramSpec.convertToObject(strings);
					} catch (Exception e) {
						throw new ValidationException("Error validating value '" + value + "' of parameter '" 
								+ entry.getKey() + "': " + e.getMessage());
					}
				}
			}
		}
	}
	
	public static void validateParams(List<InputSpec> paramSpecs, List<JobParam> params) {
		Map<String, List<String>> paramMap = new HashMap<>();
		for (JobParam param: params) {
			List<String> values;
			if (param.getValuesProvider() instanceof SpecifiedValues)
				values = param.getValuesProvider().getValues();
			else
				values = null;
			if (paramMap.put(param.getName(), values) != null)
				throw new ValidationException("Duplicate param: " + param.getName());
		}
		validateParams(paramSpecs, paramMap);
	}
	
	public static Map<String, List<String>> getParamMatrix(Map<String, String> paramMap) {
		Map<String, List<String>> paramMatrix = new HashMap<>();
		for (Map.Entry<String, String> entry: paramMap.entrySet()) 
			paramMatrix.put(entry.getKey(), Lists.newArrayList(entry.getValue()));
		return paramMatrix;
	}
	
}
