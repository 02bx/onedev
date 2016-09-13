package com.pmease.gitplex.core.gatekeeper;

import org.eclipse.jgit.lib.ObjectId;
import org.hibernate.validator.constraints.NotEmpty;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.pmease.commons.wicket.editable.annotation.Editable;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.annotation.AccountChoice;
import com.pmease.gitplex.core.entity.Depot;
import com.pmease.gitplex.core.entity.PullRequest;
import com.pmease.gitplex.core.entity.PullRequestReview;
import com.pmease.gitplex.core.entity.Account;
import com.pmease.gitplex.core.gatekeeper.checkresult.GateCheckResult;
import com.pmease.gitplex.core.manager.AccountManager;

@Editable(order=200, icon="fa-user", category=GateKeeper.CATEGORY_USER, description=
		"This gatekeeper will be passed if the commit is approved by specified user")
public class ApprovedBySpecifiedUser extends AbstractGateKeeper {

	private static final long serialVersionUID = 1L;
	
    private String userName;

    @Editable(name="Select User")
    @AccountChoice
    @NotEmpty
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public GateCheckResult doCheckRequest(PullRequest request) {
        Account user = Preconditions.checkNotNull(GitPlex.getInstance(AccountManager.class).findByName(userName));

        PullRequestReview.Result result = user.checkReviewSince(request.getReferentialUpdate());
        if (result == null) {
            request.pickReviewers(Sets.newHashSet(user), 1);

            return pending(Lists.newArrayList("To be approved by " + user.getDisplayName()));
        } else if (result == PullRequestReview.Result.APPROVE) {
            return passed(Lists.newArrayList("Approved by " + user.getDisplayName()));
        } else {
            return failed(Lists.newArrayList("Disapproved by " + user.getDisplayName()));
        }
    }

	@Override
	protected GateCheckResult doCheckFile(Account user, Depot depot, String branch, String file) {
    	return pending(Lists.newArrayList("Need approval from " + user.getName())); 
	}

	@Override
	protected GateCheckResult doCheckPush(Account user, Depot depot, String refName, ObjectId oldCommit, ObjectId newCommit) {
    	return pending(Lists.newArrayList("Need approval from " + user.getName())); 
	}

	@Override
	public void onAccountRename(String oldName, String newName) {
		if (userName.equals(oldName))
			userName = newName;
	}

	@Override
	public boolean onAccountDelete(String accountName) {
		return userName.equals(accountName);
	}

}
