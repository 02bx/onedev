package com.pmease.gitplex.core.manager;

import com.pmease.gitplex.core.model.Repository;

public interface IndexManager {
	IndexResult index(Repository repository, String revision);
}
