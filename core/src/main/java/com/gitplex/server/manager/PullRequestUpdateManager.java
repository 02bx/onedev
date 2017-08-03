package com.gitplex.server.manager;

import java.util.List;

import javax.annotation.Nullable;

import com.gitplex.server.model.Project;
import com.gitplex.server.model.PullRequestUpdate;
import com.gitplex.server.persistence.dao.EntityManager;

public interface PullRequestUpdateManager extends EntityManager<PullRequestUpdate> {
	
	/**
	 * @param update
	 * 			update to be saved
	 * @param independent
	 * 			whether or not this update is an independent update. An independent update is 
	 * 			not created as result of other actions such as open and merge
	 */
	void save(PullRequestUpdate update, boolean independent);
	
	@Nullable
	PullRequestUpdate find(String uuid);
	
	List<PullRequestUpdate> findAllAfter(Project project, @Nullable String updateUUID, int count);
}
