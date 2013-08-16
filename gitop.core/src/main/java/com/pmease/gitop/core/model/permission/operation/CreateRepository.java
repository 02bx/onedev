package com.pmease.gitop.core.model.permission.operation;

public class CreateRepository implements PrivilegedOperation {

	@Override
	public boolean can(PrivilegedOperation operation) {
		return operation instanceof CreateRepository;
	}

}
