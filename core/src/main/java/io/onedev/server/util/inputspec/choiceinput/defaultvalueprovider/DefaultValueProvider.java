package io.onedev.server.util.inputspec.choiceinput.defaultvalueprovider;

import java.io.Serializable;

import io.onedev.server.util.editable.annotation.Editable;

@Editable
public interface DefaultValueProvider extends Serializable {
	
	String getDefaultValue();
	
}
