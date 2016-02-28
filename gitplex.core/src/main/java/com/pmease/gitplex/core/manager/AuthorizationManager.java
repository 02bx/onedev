package com.pmease.gitplex.core.manager;

import java.util.Collection;

import com.pmease.commons.hibernate.dao.EntityDao;
import com.pmease.gitplex.core.entity.Account;
import com.pmease.gitplex.core.entity.Authorization;

public interface AuthorizationManager extends EntityDao<Authorization> {
	
	Collection<Authorization> findAuthorizations(Account organization);
	
}
