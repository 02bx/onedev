package com.gitplex.server.security.privilege;

public class SystemAdmin implements Privilege {

	private static final long serialVersionUID = 1L;

	@Override
	public boolean can(Privilege privilege) {
		return true;
	}

}
