package com.pmease.gitplex.web.depotaccess;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;

import com.pmease.gitplex.core.security.privilege.DepotPrivilege;

public class IsPublicDepot implements PrivilegeSource {

	private static final long serialVersionUID = 1L;
	
	@Override
	public DepotPrivilege getPrivilege() {
		return DepotPrivilege.READ;
	}

	@Override
	public Component render(String componentId) {
		return new Label(componentId, "This repository is public");
	}

}
