package com.pmease.commons.wicket.editable.enumeration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.ConversionException;

import com.pmease.commons.wicket.editable.ErrorContext;
import com.pmease.commons.wicket.editable.PathSegment;
import com.pmease.commons.wicket.editable.PropertyDescriptor;
import com.pmease.commons.wicket.editable.PropertyEditor;
import com.pmease.commons.wicket.editable.annotation.ExcludeValues;

@SuppressWarnings("serial")
public class EnumPropertyEditor extends PropertyEditor<Enum<?>> {

	private final EnumSet<?> enumSet;
	
	private DropDownChoice<String> input;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public EnumPropertyEditor(String id, PropertyDescriptor propertyDescriptor, IModel<Enum<?>> propertyModel) {
		super(id, propertyDescriptor, propertyModel);
		
        enumSet = EnumSet.allOf((Class<Enum>) propertyDescriptor.getPropertyClass());
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();

		Collection<String> excludeValues = new HashSet<>();
		ExcludeValues annotation = propertyDescriptor.getPropertyGetter().getAnnotation(ExcludeValues.class);
		if (annotation != null) {
			for (String excludeValue: annotation.value()) {
				excludeValues.add(excludeValue);
			}
		}
		List<String> choices = new ArrayList<>();
        for (Iterator<?> it = enumSet.iterator(); it.hasNext();) {
            Enum<?> value = (Enum<?>) it.next();
            if (!excludeValues.contains(value.name())) {
                choices.add(value.toString());
            }
        }

        String stringValue;
        if (getModelObject() != null)
        	stringValue = getModelObject().toString();
        else
        	stringValue = null;
        input = new DropDownChoice<String>("input", Model.of(stringValue), choices);

        input.setNullValid(!getPropertyDescriptor().isPropertyRequired());	
        add(input);
        
		add(new AttributeAppender("class", new LoadableDetachableModel<String>() {

			@Override
			protected String load() {
				if (hasErrors(true))
					return " has-error";
				else
					return "";
			}
			
		}));
        
    }

	@Override
	public ErrorContext getErrorContext(PathSegment pathSegment) {
		return null;
	}

	@Override
	protected Enum<?> convertInputToValue() throws ConversionException {
		String stringValue = input.getConvertedInput();
        for (Iterator<?> it = enumSet.iterator(); it.hasNext();) {
            Enum<?> value = (Enum<?>) it.next();
            if (value.toString().equals(stringValue)) 
            	return value;
        }
        return null;
	}

}
