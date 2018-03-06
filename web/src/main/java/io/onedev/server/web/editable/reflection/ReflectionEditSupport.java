package io.onedev.server.web.editable.reflection;

import java.io.Serializable;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import io.onedev.server.util.editable.annotation.Editable;
import io.onedev.server.web.editable.BeanContext;
import io.onedev.server.web.editable.BeanDescriptor;
import io.onedev.server.web.editable.BeanEditor;
import io.onedev.server.web.editable.BeanViewer;
import io.onedev.server.web.editable.EditSupport;
import io.onedev.server.web.editable.NotDefinedLabel;
import io.onedev.server.web.editable.PropertyContext;
import io.onedev.server.web.editable.PropertyDescriptor;
import io.onedev.server.web.editable.PropertyEditor;
import io.onedev.server.web.editable.PropertyViewer;
import io.onedev.utils.ClassUtils;

@SuppressWarnings("serial")
public class ReflectionEditSupport implements EditSupport {

	@Override
	public BeanContext<?> getBeanEditContext(Class<?> beanClass, Set<String> excludeProperties) {
		if (beanClass.getAnnotation(Editable.class) != null && ClassUtils.isConcrete(beanClass)) {
			return new BeanContext<Serializable>(beanClass, excludeProperties) {

				@Override
				public BeanViewer renderForView(String componentId, final IModel<Serializable> model) {
					return new BeanViewer(componentId, this) {

						@Override
						protected Component newContent(String id, BeanDescriptor beanDescriptor) {
							return new ReflectionBeanViewer(id, beanDescriptor, model);
						}
						
					};
				}

				@Override
				public BeanEditor<Serializable> renderForEdit(String componentId, IModel<Serializable> model) {
					return new ReflectionBeanEditor(componentId, this, model);
				}
				
			};
		} else {
			return null;
		}
	}

	@Override
	public PropertyContext<?> getPropertyEditContext(Class<?> beanClass, String propertyName) {
		PropertyDescriptor propertyDescriptor = new PropertyDescriptor(beanClass, propertyName);
		Class<?> propertyClass = propertyDescriptor.getPropertyClass();
		if (propertyClass.getAnnotation(Editable.class) != null && ClassUtils.isConcrete(propertyClass)) {
			return new PropertyContext<Serializable>(propertyDescriptor) {

				@Override
				public PropertyViewer renderForView(String componentId, final IModel<Serializable> model) {
					return new PropertyViewer(componentId, this) {

						@Override
						protected Component newContent(String id, PropertyDescriptor propertyDescriptor) {
							if (model.getObject() != null) {
								return BeanContext.viewBean(id, model.getObject());
							} else {
								return new NotDefinedLabel(id);
							}
						}
						
					};
				}

				@Override
				public PropertyEditor<Serializable> renderForEdit(String componentId, IModel<Serializable> model) {
					return new ReflectionPropertyEditor(componentId, this, model);
				}
				
			};
		} else {
			return null;
		}
	}

}
