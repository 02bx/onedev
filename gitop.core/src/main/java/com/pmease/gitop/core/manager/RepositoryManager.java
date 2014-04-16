package com.pmease.gitop.core.manager;

import javax.annotation.Nullable;

import com.google.inject.ImplementedBy;
import com.pmease.commons.hibernate.dao.GenericDao;
import com.pmease.gitop.core.manager.impl.DefaultRepositoryManager;
import com.pmease.gitop.model.Repository;
import com.pmease.gitop.model.User;

@ImplementedBy(DefaultRepositoryManager.class)
public interface RepositoryManager extends GenericDao<Repository> {
	
	@Nullable Repository findBy(String ownerName, String repositoryName);
	
	@Nullable Repository findBy(User owner, String repositoryName);

	/**
	 * Fork specified repository as specified user.
	 * 
	 * @param repository
	 * 			repository to be forked
	 * @param user
	 * 			user forking the repository
	 * @return
	 * 			newly forked repository. If the repository has already been forked, return the 
	 * 			repository forked previously
	 */
	Repository fork(Repository repository, User user);
	
	void checkSanity();
	
	void checkSanity(Repository repository);
}
