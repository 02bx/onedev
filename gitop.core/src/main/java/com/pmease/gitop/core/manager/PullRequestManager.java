package com.pmease.gitop.core.manager;

import javax.annotation.Nullable;

import com.google.inject.ImplementedBy;
import com.pmease.commons.hibernate.dao.GenericDao;
import com.pmease.gitop.core.manager.impl.DefaultPullRequestManager;
import com.pmease.gitop.model.Branch;
import com.pmease.gitop.model.PullRequest;
import com.pmease.gitop.model.User;

@ImplementedBy(DefaultPullRequestManager.class)
public interface PullRequestManager extends GenericDao<PullRequest> {
    
    @Nullable PullRequest findOpen(Branch target, @Nullable Branch source, User user);
    
    void refresh(PullRequest request);
    
    boolean merge(PullRequest request);
    
    void decline(PullRequest request);
}
