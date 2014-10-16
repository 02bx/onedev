package com.pmease.gitplex.core.gatekeeper;

import java.util.Collection;
import java.util.HashSet;

import javax.validation.constraints.Min;

import com.pmease.commons.editable.annotation.Editable;
import com.pmease.gitplex.core.gatekeeper.checkresult.CheckResult;
import com.pmease.gitplex.core.gatekeeper.voteeligibility.CanVoteBySpecifiedTeam;
import com.pmease.gitplex.core.model.Branch;
import com.pmease.gitplex.core.model.Membership;
import com.pmease.gitplex.core.model.PullRequest;
import com.pmease.gitplex.core.model.Repository;
import com.pmease.gitplex.core.model.User;
import com.pmease.gitplex.core.model.PullRequestVote;

@SuppressWarnings("serial")
@Editable(order=100, icon="fa-group-o", description=
		"This gate keeper will be passed if the commit is approved by specified number of users "
		+ "from specified team.")
public class IfApprovedBySpecifiedTeam extends TeamAwareGateKeeper {

    private int leastApprovals = 1;

    @Editable(name="Least Approvals Required", order=1000)
    @Min(value = 1, message = "Least approvals should not be less than 1.")
    public int getLeastApprovals() {
        return leastApprovals;
    }

    public void setLeastApprovals(int leastApprovals) {
        this.leastApprovals = leastApprovals;
    }

    @Override
    public CheckResult doCheckRequest(PullRequest request) {
        Collection<User> members = new HashSet<User>();
        for (Membership membership : getTeam().getMemberships())
            members.add(membership.getUser());

        int approvals = 0;
        int pendings = 0;
        for (User member : members) {
            PullRequestVote.Result result = member.checkVoteSince(request.getReferentialUpdate());
            if (result == null) {
                pendings++;
            } else if (result == PullRequestVote.Result.APPROVE) {
                approvals++;
            }
        }

        if (approvals >= getLeastApprovals()) {
            return approved("Already get at least " + getLeastApprovals() + " approvals from team '"
                    + getTeam().getName() + "'.");
        } else if (getLeastApprovals() - approvals > pendings) {
            return disapproved("Unable to get at least " + getLeastApprovals()
                    + " approvals from team '" + getTeam().getName() + "'.");
        } else {
            int lackApprovals = getLeastApprovals() - approvals;

            request.pickVoters(members, lackApprovals);

            return pending("To be approved by " + lackApprovals + " user(s) from team '"
                    + getTeam().getName() + "'.", new CanVoteBySpecifiedTeam(getTeam()));
        }
    }

	private CheckResult checkApproval(User user) {
        Collection<User> members = new HashSet<User>();
        for (Membership membership : getTeam().getMemberships())
            members.add(membership.getUser());

        int approvals = 0;
        int pendings = members.size();
        
        if (members.contains(user)) {
        	approvals ++;
        	pendings --;
        }

        if (approvals >= getLeastApprovals()) {
            return approved("Get at least " + leastApprovals + " approvals from team '"
                    + getTeam().getName() + "'.");
        } else if (getLeastApprovals() - approvals > pendings) {
            return disapproved("Can not get at least " + leastApprovals 
                    + " approvals from team '" + getTeam().getName() + "'.");
        } else {
            int lackApprovals = getLeastApprovals() - approvals;

            return pending("Lack " + lackApprovals + " approvals from team '"
                    + getTeam().getName() + "'.", new CanVoteBySpecifiedTeam(getTeam()));
        }
	}

	@Override
	protected CheckResult doCheckCommit(User user, Branch branch, String commit) {
		return checkApproval(user);
	}

	@Override
	protected CheckResult doCheckFile(User user, Branch branch, String file) {
		return checkApproval(user);
	}

	@Override
	protected CheckResult doCheckRef(User user, Repository repository, String refName) {
		return checkApproval(user);
	}

}
