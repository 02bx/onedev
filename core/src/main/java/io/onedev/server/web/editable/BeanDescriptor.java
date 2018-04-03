package io.onedev.server.web.editable;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

import io.onedev.server.util.editable.EditableUtils;
import io.onedev.server.util.editable.annotation.Editable;
import io.onedev.utils.BeanUtils;

@SuppressWarnings("serial")
public class BeanDescriptor implements Serializable {

	private final Class<?> beanClass;
	
	protected final List<PropertyDescriptor> propertyDescriptors;
	
	public BeanDescriptor(Class<?> beanClass) {
		this(beanClass, Sets.newHashSet());
	}
	
	public BeanDescriptor(Class<?> beanClass, Set<String> excludedProperties) {
		this(beanClass, excludedProperties, Sets.newHashSet());
	}
	
	public BeanDescriptor(Class<?> beanClass, Set<String> excludedProperties, Set<String> optionalProperties) {
		this.beanClass = beanClass;
		
		propertyDescriptors = new ArrayList<>();

		List<Method> propertyGetters = BeanUtils.findGetters(getBeanClass());
		EditableUtils.sortAnnotatedElements(propertyGetters);
		
		for (Method propertyGetter: propertyGetters) {
			if (propertyGetter.getAnnotation(Editable.class) != null) {
				PropertyDescriptor propertyDescriptor = new PropertyDescriptor(propertyGetter); 
				propertyDescriptors.add(propertyDescriptor);
				String propertyName = BeanUtils.getPropertyName(propertyGetter);
				propertyDescriptor.setExcluded(BeanUtils.findSetter(propertyGetter) == null || excludedProperties.contains(propertyName));
				propertyDescriptor.setOptional(optionalProperties.contains(propertyName));
			}
		}
	}
	
	public BeanDescriptor(BeanDescriptor beanDescriptor) {
		this.beanClass = beanDescriptor.getBeanClass();
		this.propertyDescriptors = beanDescriptor.getPropertyDescriptors();
	}
	
	public Class<?> getBeanClass() {
		return beanClass;
	}

	public List<PropertyDescriptor> getPropertyDescriptors() {
		return propertyDescriptors;
	}

	public void copyProperties(Object from, Object to) {
		for (PropertyDescriptor propertyDescriptor: getPropertyDescriptors()) {
			if (!propertyDescriptor.isExcluded())
				propertyDescriptor.copyProperty(from, to);
		}
	}

	public Object newBeanInstance() {
		try {
			return getBeanClass().newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

}
