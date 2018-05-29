package io.onedev.server.web.editable.string;

import java.lang.reflect.Method;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.ConversionException;

import io.onedev.server.util.editable.EditableUtils;
import io.onedev.server.util.editable.annotation.Multiline;
import io.onedev.server.util.editable.annotation.OmitName;
import io.onedev.server.web.behavior.OnTypingDoneBehavior;
import io.onedev.server.web.editable.ErrorContext;
import io.onedev.server.web.editable.PathSegment;
import io.onedev.server.web.editable.PropertyDescriptor;
import io.onedev.server.web.editable.PropertyEditor;

@SuppressWarnings("serial")
public class StringPropertyEditor extends PropertyEditor<String> {

	private FormComponent<String> input;
	
	public StringPropertyEditor(String id, PropertyDescriptor propertyDescriptor, IModel<String> propertyModel) {
		super(id, propertyDescriptor, propertyModel);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		Method getter = getPropertyDescriptor().getPropertyGetter();
		if (getter.getAnnotation(Multiline.class) != null) {
			Fragment fragment = new Fragment("content", "multiLineFrag", this);
			fragment.add(input = new TextArea<String>("input", Model.of(getModelObject())));
			input.setType(getPropertyDescriptor().getPropertyClass());
			add(fragment);
		} else {
			Fragment fragment = new Fragment("content", "singleLineFrag", this);
			fragment.add(input = new TextField<String>("input", Model.of(getModelObject())));
			input.setType(getPropertyDescriptor().getPropertyClass());
			add(fragment);
		}
		input.setLabel(Model.of(getPropertyDescriptor().getDisplayName(this)));		
		
		input.add(new OnTypingDoneBehavior() {

			@Override
			protected void onTypingDone(AjaxRequestTarget target) {
				onPropertyUpdating(target);
			}
			
		});
		
		if (getter.getAnnotation(OmitName.class) != null)
			input.add(AttributeModifier.replace("placeholder", EditableUtils.getDisplayName(getter)));
	}

	@Override
	public ErrorContext getErrorContext(PathSegment pathSegment) {
		return null;
	}

	@Override
	protected String convertInputToValue() throws ConversionException {
		return input.getConvertedInput();
	}

}
