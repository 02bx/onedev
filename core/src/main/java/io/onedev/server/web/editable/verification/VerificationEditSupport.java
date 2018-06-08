package io.onedev.server.web.editable.verification;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.eclipse.jgit.util.StringUtils;

import io.onedev.server.web.editable.EditSupport;
import io.onedev.server.web.editable.EditableUtils;
import io.onedev.server.web.editable.EmptyValueLabel;
import io.onedev.server.web.editable.PropertyContext;
import io.onedev.server.web.editable.PropertyDescriptor;
import io.onedev.server.web.editable.PropertyEditor;
import io.onedev.server.web.editable.PropertyViewer;
import io.onedev.server.web.editable.annotation.VerificationChoice;

@SuppressWarnings("serial")
public class VerificationEditSupport implements EditSupport {

	@Override
	public PropertyContext<?> getEditContext(PropertyDescriptor descriptor) {
        Method propertyGetter = descriptor.getPropertyGetter();
        VerificationChoice verificationChoice = propertyGetter.getAnnotation(VerificationChoice.class);
        if (verificationChoice != null) {
        	if (List.class.isAssignableFrom(propertyGetter.getReturnType()) 
        			&& EditableUtils.getElementClass(propertyGetter.getGenericReturnType()) == String.class) {
        		return new PropertyContext<List<String>>(descriptor) {

					@Override
					public PropertyViewer renderForView(String componentId, final IModel<List<String>> model) {
						return new PropertyViewer(componentId, descriptor) {

							@Override
							protected Component newContent(String id, PropertyDescriptor propertyDescriptor) {
						        List<String> verifications = model.getObject();
						        if (verifications != null && !verifications.isEmpty()) {
						            return new Label(id, StringUtils.join(verifications, ", " ));
						        } else {
									return new EmptyValueLabel(id, propertyDescriptor.getPropertyGetter());
						        }
							}
							
						};
					}

					@Override
					public PropertyEditor<List<String>> renderForEdit(String componentId, IModel<List<String>> model) {
						return new VerificationMultiChoiceEditor(componentId, descriptor, model);
					}
        			
        		};
        	} else {
        		throw new RuntimeException("Annotation 'VerificationChoice' should be applied to property with type "
        				+ "'List<String>'.");
        	}
        } else {
            return null;
        }
	}

}
