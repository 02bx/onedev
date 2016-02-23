package com.pmease.gitplex.core.gatekeeper;

import java.util.Collection;
import java.util.HashSet;

import javax.validation.constraints.Min;

import org.eclipse.jgit.lib.ObjectId;
import org.hibernate.validator.constraints.NotEmpty;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.pmease.commons.wicket.editable.annotation.Editable;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.gatekeeper.checkresult.CheckResult;
import com.pmease.gitplex.core.manager.TeamManager;
import com.pmease.gitplex.core.model.Depot;
import com.pmease.gitplex.core.model.Membership;
import com.pmease.gitplex.core.model.PullRequest;
import com.pmease.gitplex.core.model.Review;
import com.pmease.gitplex.core.model.Team;
import com.pmease.gitplex.core.model.User;

@SuppressWarnings("serial")
@Editable(order=100, icon="fa-group", category=GateKeeper.CATEGORY_USER, description=
		"This gate keeper will be passed if the commit is approved by specified number of users "
		+ "from specified team.")
public class IfApprovedBySpecifiedTeam extends AbstractGateKeeper {

	private String teamName;
	
    private int leastApprovals = 1;

    @Editable(name="Team", order=100)
    @NotEmpty
    public String getTeamName() {
		return teamName;
	}

	public void setTeamName(String teamName) {
		this.teamName = teamName;
	}

	@Editable(name="Least Approvals Required", order=200)
    @Min(value = 1, message = "Least approvals should not be less than 1.")
    public int getLeastApprovals() {
        return leastApprovals;
    }

    public void setLeastApprovals(int leastApprovals) {
        this.leastApprovals = leastApprovals;
    }

    private Team getTeam(User owner) {
    	return Preconditions.checkNotNull(GitPlex.getInstance(TeamManager.class).findBy(owner, teamName));
    }
    
    @Override
    public CheckResult doCheckRequest(PullRequest request) {
        Collection<User> members = new HashSet<User>();
        for (Membership membership : getTeam(request.getTargetDepot().getOwner()).getMemberships())
            members.add(membership.getUser());

        int approvals = 0;
        int pendings = 0;
        for (User member : members) {
            Review.Result result = member.checkReviewSince(request.getReferentialUpdate());
            if (result == null) {
                pendings++;
            } else if (result == Review.Result.APPROVE) {
                approvals++;
            }
        }

        if (approvals >= getLeastApprovals()) {
            return passed(Lists.newArrayList("Already get at least " + getLeastApprovals() + " approvals from team '"
                    + teamName + "'."));
        } else if (getLeastApprovals() - approvals > pendings) {
            return failed(Lists.newArrayList("Unable to get at least " + getLeastApprovals()
                    + " approvals from team '" + teamName + "'."));
        } else {
            int lackApprovals = getLeastApprovals() - approvals;

            request.pickReviewers(members, lackApprovals);

            return pending(Lists.newArrayList("To be approved by " + lackApprovals + " user(s) from team '"
                    + teamName + "'."));
        }
    }

	private CheckResult check(User user, User owner) {
        Collection<User> members = new HashSet<User>();
        for (Membership membership : getTeam(owner).getMemberships())
            members.add(membership.getUser());

        int approvals = 0;
        int pendings = members.size();
        
        if (members.contains(user)) {
        	approvals ++;
        	pendings --;
        }

        if (approvals >= getLeastApprovals()) {
            return passed(Lists.newArrayList("Get at least " + leastApprovals + " approvals from team '"
                    + teamName + "'."));
        } else if (getLeastApprovals() - approvals > pendings) {
            return failed(Lists.newArrayList("Can not get at least " + leastApprovals 
                    + " approvals from team '" + teamName + "'."));
        } else {
            int lackApprovals = getLeastApprovals() - approvals;

            return pending(Lists.newArrayList("Lack " + lackApprovals + " approvals from team '"
                    + teamName + "'."));
        }
	}

	@Override
	protected CheckResult doCheckPush(User user, Depot depot, String refName, ObjectId oldCommit, ObjectId newCommit) {
		return check(user, depot.getOwner());
	}

	@Override
	protected CheckResult doCheckFile(User user, Depot depot, String branch, String file) {
		return check(user, depot.getOwner());
	}

}
