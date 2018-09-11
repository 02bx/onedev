package io.onedev.server.manager;

import java.util.List;

import javax.annotation.Nullable;

import io.onedev.server.model.Build;
import io.onedev.server.model.Configuration;
import io.onedev.server.model.Project;
import io.onedev.server.persistence.dao.EntityManager;

public interface BuildManager extends EntityManager<Build> {
	
	List<Build> findAll(Project project, String commit);
	
	@Nullable
	Build find(Configuration configuration, String commit);
	
	@Nullable
	Build find(Project project, long number);
	
	List<Build> query(Project project, String term, int count);
	
	@Nullable
	Build findPrevious(Build build);

	@Nullable
	Build find(String uuid);
	
	List<Build> findAllAfter(Project project, @Nullable String commentUUID, int count);
	
}
