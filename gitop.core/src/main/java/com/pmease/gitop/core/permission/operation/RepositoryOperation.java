package com.pmease.gitop.core.permission.operation;

public enum RepositoryOperation implements PrivilegedOperation {
	NO_ACCESS {

		@Override
		public boolean can(PrivilegedOperation operation) {
			return false;
		}
		
	},
	READ {

		@Override
		public boolean can(PrivilegedOperation operation) {
			return operation == READ;
		}
		
	},
	WRITE {

		@Override
		public boolean can(PrivilegedOperation operation) {
			return operation == WRITE || READ.can(operation);
		}
		
	},
	ADMINISTRATION {

		@Override
		public boolean can(PrivilegedOperation operation) {
			return true;
		}
		
	}
}
