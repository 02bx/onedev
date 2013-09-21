package com.pmease.gitop.core.permission.operation;

public enum UserOperation implements PrivilegedOperation {
	READ {

		@Override
		public boolean can(PrivilegedOperation operation) {
			return operation == READ || RepositoryOperation.READ.can(operation);
		}
		
	},
	WRITE {

		@Override
		public boolean can(PrivilegedOperation operation) {
			return operation == WRITE || READ.can(operation) || RepositoryOperation.WRITE.can(operation);
		}
		
	},
	ADMINISTRATION {

		@Override
		public boolean can(PrivilegedOperation operation) {
			return true;
		}
		
	}
}
