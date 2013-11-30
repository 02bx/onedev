package com.pmease.gitop.model.gatekeeper.checkresult;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;
import com.pmease.gitop.model.PullRequest;
import com.pmease.gitop.model.User;
import com.pmease.gitop.model.gatekeeper.voteeligibility.NoneCanVote;
import com.pmease.gitop.model.gatekeeper.voteeligibility.VoteEligibility;

@SuppressWarnings("serial")
public abstract class CheckResult implements Serializable {
	
	private final List<String> reasons;

	private final Collection<VoteEligibility> voteEligibilities;
	
    public CheckResult(List<String> reasons, Collection<VoteEligibility> voteEligibilities) {
        this.reasons = reasons;
        this.voteEligibilities = voteEligibilities;
    }

    public CheckResult(List<String> reasons) {
        this.reasons = reasons;
        voteEligibilities = Lists.newArrayList((VoteEligibility) new NoneCanVote());
    }

    public CheckResult(String reason, VoteEligibility voteEligibility) {
        this.reasons = Lists.newArrayList(reason);
        
        this.voteEligibilities = Lists.newArrayList(voteEligibility);
    }

    public CheckResult(String reason) {
        this.reasons = Lists.newArrayList(reason);
        voteEligibilities = Lists.newArrayList((VoteEligibility) new NoneCanVote());
    }

    public List<String> getReasons() {
		return reasons;
	}
	
    public Collection<VoteEligibility> getVoteEligibilities() {
        return voteEligibilities;
    }

    public boolean canVote(User user, PullRequest request) {
        if (user.equals(request.getSubmitter()))
            return false;
        
        for (VoteEligibility each: voteEligibilities) {
            if (each.canVote(user, request))
                return true;
        }
        return false;
    }
    
}