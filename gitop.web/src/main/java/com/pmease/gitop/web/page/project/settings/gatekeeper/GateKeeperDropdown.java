package com.pmease.gitop.web.page.project.settings.gatekeeper;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;

import com.pmease.commons.wicket.behavior.dropdown.DropdownPanel;
import com.pmease.gitop.model.gatekeeper.GateKeeper;

@SuppressWarnings("serial")
public abstract class GateKeeperDropdown extends DropdownPanel {

	public GateKeeperDropdown(String id) {
		super(id);
	}

	@Override
	protected Component newContent(String id) {
		return new GateKeeperSelector(id) {
			
			@Override
			protected void onSelect(AjaxRequestTarget target, Class<? extends GateKeeper> gateKeeperClass) {
				close(target);
				GateKeeperDropdown.this.onSelect(gateKeeperClass);
			}
		};
	}

	protected abstract void onSelect(Class<? extends GateKeeper> gateKeeperClass);
}
