package com.pmease.commons.wicket.behavior.modal;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.head.PriorityHeaderItem;

@SuppressWarnings("serial")
public class ModalBehavior extends AbstractDefaultAjaxBehavior {

	private final ModalPanel modalPanel;
	
	public ModalBehavior(ModalPanel modalPanel) {
		this.modalPanel = modalPanel;
		this.modalPanel.showImmediately = false;
	}
	
	@Override
	protected void respond(AjaxRequestTarget target) {
		modalPanel.load(target);
	}

	@Override
	public void renderHead(Component component, IHeaderResponse response) {
		super.renderHead(component, response);
		String script = String.format(
				"pmease.commons.modal.setupTrigger('%s', '%s', %s)", 
				getComponent().getMarkupId(), modalPanel.getMarkupId(), getCallbackFunction());
		response.render(new PriorityHeaderItem(OnDomReadyHeaderItem.forScript(script)));
	}
		
}
