package com.pmease.gitop.core.manager;

import com.google.inject.ImplementedBy;
import com.pmease.commons.hibernate.dao.GenericDao;
import com.pmease.gitop.core.manager.impl.DefaultPullRequestUpdateManager;
import com.pmease.gitop.core.model.PullRequestUpdate;

@ImplementedBy(DefaultPullRequestUpdateManager.class)
public interface PullRequestUpdateManager extends GenericDao<PullRequestUpdate> {
}
