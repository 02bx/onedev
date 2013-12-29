package com.pmease.gitop.core.gatekeeper;

import java.util.Collection;

import javax.validation.constraints.Min;

import com.google.common.base.Preconditions;
import com.pmease.commons.editable.annotation.Editable;
import com.pmease.commons.git.Commit;
import com.pmease.gitop.core.Gitop;
import com.pmease.gitop.core.manager.BuildResultManager;
import com.pmease.gitop.model.BuildResult;
import com.pmease.gitop.model.PullRequest;
import com.pmease.gitop.model.gatekeeper.CommonGateKeeper;
import com.pmease.gitop.model.gatekeeper.checkresult.CheckResult;
import com.pmease.gitop.model.gatekeeper.voteeligibility.NoneCanVote;

@Editable(icon="icon-checkbox-checked", order=1000, 
		description="This gate keeper will be satisfied if commit is verified successfully "
				+ "by specified number of builds. To make this working, your CI system has to "
				+ "be configured to build against Gitop pull requests.")
@SuppressWarnings("serial")
public class IfVerifiedByBuilds extends CommonGateKeeper {
	
	private int buildCount = 1;
	
	private boolean checkMerged = true;
	
	private boolean blockMode = true;
	
	@Editable(order=100, description="Specify number of passed builds required by this gate keeper.")
	@Min(1)
	public int getBuildCount() {
		return buildCount;
	}

	public void setBuildCount(int buildCount) {
		this.buildCount = buildCount;
	}

	@Editable(order=200, description="Enable this to check the merged commit instead of head commit.")
	public boolean isCheckMerged() {
		return checkMerged;
	}

	public void setCheckMerged(boolean checkMerged) {
		this.checkMerged = checkMerged;
	}

	@Editable(order=300, description="If this is checked, subsequent gate keepers will not be checked "
			+ "while waiting for the build results. This can be used to only notify relevant voters "
			+ "when the commit passes build.")
	public boolean isBlockMode() {
		return blockMode;
	}

	public void setBlockMode(boolean blockMode) {
		this.blockMode = blockMode;
	}

	public CheckResult doCheck(PullRequest request) {
		BuildResultManager BuildResultManager = Gitop.getInstance(BuildResultManager.class);

		Preconditions.checkNotNull(request.getMergePrediction());
		
		String commit;
		if (isCheckMerged()) {
			commit = request.getMergePrediction().getMerged();
			if (commit == null) 
				return rejected("Can not build against merged result due to conflicts.");
		} else {
			commit = request.getLatestUpdate().getHeadCommit();
		}
		if (commit.equals(Commit.ZERO_HASH)) { // delete branch
			return accepted("Build is no longer necessary.");
		} else if (commit.startsWith(Commit.ZERO_HASH)) { // check if a certain file can be touched
			if (blockMode)
				return blocked("Not verified by build.", new NoneCanVote());
			else
				return pending("Not verified by build.", new NoneCanVote());
		} else {
			Collection<BuildResult> buildResults = BuildResultManager.findBy(commit);
			for (BuildResult each: buildResults) {
				if (!each.isPassed())
					return rejected("At least one build is failed for the merged commit.");
			}
			int lacks = buildCount - buildResults.size();
			
			String prefix;
			if (request.getId() == null)
				prefix = "Not ";
			else
				prefix = "To be ";
			if (lacks > 0) {
				if (blockMode) {
					if (buildCount > 1)
						return blocked(prefix + "verified by " + lacks + " more build(s)", new NoneCanVote());
					else
						return blocked(prefix + "verified by build", new NoneCanVote());
				} else {
					if (buildCount > 1)
						return pending(prefix + "verified by " + lacks + " more build(s)", new NoneCanVote());
					else
						return pending(prefix + "verified by build", new NoneCanVote());
				}
			} else {
				return accepted("Builds passed");
			}
		}
	}

}
