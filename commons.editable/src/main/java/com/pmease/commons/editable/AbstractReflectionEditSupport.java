package com.pmease.commons.editable;

import java.io.Serializable;
import java.lang.reflect.Method;

import com.pmease.commons.editable.annotation.Editable;
import com.pmease.commons.util.BeanUtils;
import com.pmease.commons.util.ClassUtils;

public abstract class AbstractReflectionEditSupport implements EditSupport {
	
	@Override
	public BeanEditContext getBeanEditContext(Serializable bean) {
		if (bean.getClass().getAnnotation(Editable.class) != null && ClassUtils.isConcrete(bean.getClass()))
			return newReflectionBeanEditContext(bean);
		else
			return null;
	}

	@Override
	public PropertyEditContext getPropertyEditContext(Serializable bean, String propertyName) {
		Method propertyGetter = BeanUtils.getGetter(bean.getClass(), propertyName);
		Class<?> propertyClass = propertyGetter.getReturnType();
		if (propertyClass.getAnnotation(Editable.class) != null && ClassUtils.isConcrete(propertyClass)) {
			return newReflectionPropertyEditContext(bean, propertyName);
		} else {
			return null;
		}
	}

	protected abstract AbstractReflectionBeanEditContext newReflectionBeanEditContext(Serializable bean);
	
	protected abstract AbstractReflectionPropertyEditContext newReflectionPropertyEditContext(Serializable bean, String propertyName);

}
