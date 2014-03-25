package com.pmease.gitop.core.manager;

import com.google.inject.ImplementedBy;
import com.pmease.commons.hibernate.dao.GenericDao;
import com.pmease.gitop.core.manager.impl.DefaultVoteInvitationManager;
import com.pmease.gitop.model.PullRequest;
import com.pmease.gitop.model.User;
import com.pmease.gitop.model.VoteInvitation;

@ImplementedBy(DefaultVoteInvitationManager.class)
public interface VoteInvitationManager extends GenericDao<VoteInvitation> {
	
	VoteInvitation find(User reviewer, PullRequest request);
	
}
