package com.gitplex.core.gatekeeper;

import java.util.Collection;

import org.eclipse.jgit.lib.ObjectId;

import com.gitplex.core.entity.Account;
import com.gitplex.core.entity.Depot;
import com.gitplex.core.entity.PullRequest;
import com.gitplex.core.gatekeeper.checkresult.GateCheckResult;
import com.google.common.collect.Lists;
import com.gitplex.commons.wicket.editable.annotation.Editable;

@Editable(order=500, icon="fa-group", category=GateKeeper.CATEGORY_USER, description=
		"This gatekeeper will be passed if the commit is submitted by a member of specified team")
public class SubmittedBySpecifiedTeam extends TeamAwareGateKeeper {

	private static final long serialVersionUID = 1L;
	
	@Override
    public GateCheckResult doCheckRequest(PullRequest request) {
    	return checkSubmitter(request.getSubmitter(), request.getTargetDepot().getAccount());
    }

	private GateCheckResult checkSubmitter(Account user, Account organization) {
    	Collection<Account> members = getTeamMembers(organization);
		if (members.contains(user)) {
			return passed(Lists.newArrayList("Submitted by a member of team " + getTeamName()));
		}
		return failed(Lists.newArrayList("Not submitted by a member of team " + getTeamName()));
	}

	@Override
	protected GateCheckResult doCheckPush(Account user, Depot depot, String refName, 
			ObjectId oldObjectId, ObjectId newObjectId) {
		return checkSubmitter(user, depot.getAccount());
	}

	@Override
	protected GateCheckResult doCheckFile(Account user, Depot depot, String branch, String file) {
		return checkSubmitter(user, depot.getAccount());
	}

}
