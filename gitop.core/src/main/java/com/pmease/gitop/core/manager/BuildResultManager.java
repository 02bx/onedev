package com.pmease.gitop.core.manager;

import java.util.Collection;

import com.google.inject.ImplementedBy;
import com.pmease.gitop.core.manager.impl.DefaultBuildResultManager;
import com.pmease.gitop.model.BuildResult;

@ImplementedBy(DefaultBuildResultManager.class)
public interface BuildResultManager {
	
	void save(BuildResult buildResult);
	
	void delete(BuildResult buildResult);
	
	Collection<BuildResult> findBy(String commit);
	
	BuildResult findBy(String commit, String configuration);
}
