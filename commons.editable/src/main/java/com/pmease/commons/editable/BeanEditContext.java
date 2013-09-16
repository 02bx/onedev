package com.pmease.commons.editable;

import java.io.Serializable;

@SuppressWarnings("serial")
public abstract class BeanEditContext extends AbstractEditContext {

	public BeanEditContext(Serializable bean) {
		super(bean);
	}

}
