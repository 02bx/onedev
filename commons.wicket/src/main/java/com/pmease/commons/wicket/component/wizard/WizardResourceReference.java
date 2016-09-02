package com.pmease.commons.wicket.component.wizard;

import org.apache.wicket.request.resource.CssResourceReference;

public class WizardResourceReference extends CssResourceReference {

	private static final long serialVersionUID = 1L;

	public WizardResourceReference() {
		super(WizardResourceReference.class, "wizard.css");
	}

}
