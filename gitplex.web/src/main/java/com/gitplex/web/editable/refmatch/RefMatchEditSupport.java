package com.gitplex.web.editable.refmatch;

import java.lang.reflect.Method;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.gitplex.core.annotation.RefMatch;
import com.gitplex.commons.wicket.editable.BeanContext;
import com.gitplex.commons.wicket.editable.PropertyDescriptor;
import com.gitplex.commons.wicket.editable.EditSupport;
import com.gitplex.commons.wicket.editable.NotDefinedLabel;
import com.gitplex.commons.wicket.editable.PropertyContext;
import com.gitplex.commons.wicket.editable.PropertyEditor;
import com.gitplex.commons.wicket.editable.PropertyViewer;

@SuppressWarnings("serial")
public class RefMatchEditSupport implements EditSupport {

	@Override
	public BeanContext<?> getBeanEditContext(Class<?> beanClass, Set<String> excludeProperties) {
		return null;
	}

	@Override
	public PropertyContext<?> getPropertyEditContext(Class<?> beanClass, String propertyName) {
		PropertyDescriptor propertyDescriptor = new PropertyDescriptor(beanClass, propertyName);
		Method propertyGetter = propertyDescriptor.getPropertyGetter();
        if (propertyGetter.getAnnotation(RefMatch.class) != null) {
        	if (propertyGetter.getReturnType() != String.class) {
	    		throw new RuntimeException("Annotation 'RefMatch' should be applied to property "
	    				+ "with type 'String'.");
        	}
    		return new PropertyContext<String>(propertyDescriptor) {

				@Override
				public PropertyViewer renderForView(String componentId, final IModel<String> model) {
					return new PropertyViewer(componentId, this) {

						@Override
						protected Component newContent(String id, PropertyDescriptor propertyDescriptor) {
					        String refMatch = model.getObject();
					        if (refMatch != null) {
					        	return new Label(id, refMatch);
					        } else {
								return new NotDefinedLabel(id);
					        }
						}
						
					};
				}

				@Override
				public PropertyEditor<String> renderForEdit(String componentId, IModel<String> model) {
		        	return new RefMatchEditor(componentId, this, model);
				}
    			
    		};
        } else {
            return null;
        }
	}

}
