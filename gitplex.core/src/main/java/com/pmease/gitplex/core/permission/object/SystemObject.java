package com.pmease.gitplex.core.permission.object;

public class SystemObject implements ProtectedObject {

	@Override
	public boolean has(ProtectedObject object) {
		return true;
	}

}
