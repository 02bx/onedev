package com.pmease.gitop.web.editable.team;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;

import com.pmease.commons.util.BeanUtils;
import com.pmease.commons.util.JavassistUtils;
import com.pmease.commons.wicket.editable.BeanEditContext;
import com.pmease.commons.wicket.editable.EditSupport;
import com.pmease.commons.wicket.editable.PropertyEditContext;
import com.pmease.gitop.core.editable.TeamChoice;

public class TeamEditSupport implements EditSupport {

    @Override
    public BeanEditContext getBeanEditContext(Serializable bean) {
        return null;
    }

    @Override
    public PropertyEditContext getPropertyEditContext(Serializable bean, String propertyName) {
        Method propertyGetter = BeanUtils.getGetter(JavassistUtils.unproxy(bean.getClass()), propertyName);
        if (propertyGetter.getAnnotation(TeamChoice.class) != null) {
        	if (List.class.isAssignableFrom(propertyGetter.getReturnType()))
        		return new TeamMultiChoiceEditContext(bean, propertyName);
        	else
        		return new TeamSingleChoiceEditContext(bean, propertyName);
        } else {
            return null;
        }
    }

}
