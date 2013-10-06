package com.pmease.gitop.web.common.form.flatcheckbox;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;

import com.pmease.gitop.web.common.form.checkbox.CheckBoxElement;

@SuppressWarnings("serial")
public class FlatCheckBoxElement extends CheckBoxElement {

	public FlatCheckBoxElement(String id, IModel<Boolean> inputModel, IModel<String> description) {
		this(id, "", inputModel, description);
	}

	public FlatCheckBoxElement(String id, String label, IModel<Boolean> inputModel, IModel<String> description) {
		super(id, label, inputModel, description);
	}
	
	@Override
	protected Component createInputComponent(String id) {
		FlatCheckBoxElementPanel panel = new FlatCheckBoxElementPanel(id);
	    panel.setRenderBodyOnly(true);

	    checkbox = new CheckBox("check", model);
	    panel.add(checkbox);
	    Label label = (Label) createDescriptionLabel("description");

	    panel.add(label);
	    panel.add(new FlatCheckBoxBehavior());
	    return panel;
	}
}
