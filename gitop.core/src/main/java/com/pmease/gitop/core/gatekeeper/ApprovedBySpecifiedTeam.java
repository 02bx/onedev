package com.pmease.gitop.core.gatekeeper;

import java.util.Collection;
import java.util.HashSet;

import javax.validation.constraints.Min;

import com.pmease.gitop.core.model.MergeRequest;
import com.pmease.gitop.core.model.TeamMembership;
import com.pmease.gitop.core.model.User;
import com.pmease.gitop.core.model.Vote;

@SuppressWarnings("serial")
public class ApprovedBySpecifiedTeam extends TeamAwareGateKeeper {

	private int leastApprovals = 1;
	
	@Min(1)
	public int getLeastApprovals() {
		return leastApprovals;
	}

	public void setLeastApprovals(int leastApprovals) {
		this.leastApprovals = leastApprovals;
	}

	@Override
	public CheckResult check(MergeRequest request) {
		Collection<User> members = new HashSet<User>();
		for (TeamMembership membership: getTeam().getMemberships())
			members.add(membership.getUser());
		
		int approvals = 0;
		int pendings = 0;
		for (User member: members) {
			Vote.Result result = member.checkVoteSince(request.getBaseUpdate());
			if (result == null) {
				pendings++;
			} else if (result.isAccept()) {
				approvals++;
			}
		}
		
		if (approvals >= getLeastApprovals()) {
			return accept("Get at least " + getLeastApprovals() + " approvals from team '" + getTeam().getName() + "'.");
		} else if (getLeastApprovals() - approvals > pendings) {
			return reject("Can not get at least " + getLeastApprovals() + " approvals from team '" + getTeam().getName() + "'.");
		} else {
			int lackApprovals = getLeastApprovals() - approvals;

			request.inviteToVote(members, lackApprovals);
			
			return pending("To be approved by " + lackApprovals + " user(s) from team '" + getTeam().getName() + "'.");
		}
	}

}
