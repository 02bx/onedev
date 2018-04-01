package io.onedev.server.util.input.passwordinput.defaultvalueprovider;

import java.io.Serializable;

import io.onedev.server.util.editable.annotation.Editable;

@Editable
public interface DefaultValueProvider extends Serializable {
	
	String getDefaultValue();
	
}
