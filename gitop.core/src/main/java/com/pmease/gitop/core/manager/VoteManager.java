package com.pmease.gitop.core.manager;

import com.google.inject.ImplementedBy;
import com.pmease.commons.hibernate.dao.GenericDao;
import com.pmease.gitop.core.manager.impl.DefaultVoteManager;
import com.pmease.gitop.model.PullRequestUpdate;
import com.pmease.gitop.model.User;
import com.pmease.gitop.model.Vote;

@ImplementedBy(DefaultVoteManager.class)
public interface VoteManager extends GenericDao<Vote> {

	Vote find(User reviewer, PullRequestUpdate update);

}
