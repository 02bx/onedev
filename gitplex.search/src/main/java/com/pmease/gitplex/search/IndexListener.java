package com.pmease.gitplex.search;

import com.pmease.gitplex.core.model.Repository;

public interface IndexListener {
	
	void commitIndexed(Repository repository, String revision);
	
	void indexRemoving(Repository repository);
	
}
