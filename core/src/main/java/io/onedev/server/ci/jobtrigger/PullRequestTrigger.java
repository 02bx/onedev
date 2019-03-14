package io.onedev.server.ci.jobtrigger;

import io.onedev.commons.utils.PathUtils;
import io.onedev.server.ci.CISpec;
import io.onedev.server.ci.Job;
import io.onedev.server.event.ProjectEvent;
import io.onedev.server.event.pullrequest.PullRequestOpened;
import io.onedev.server.event.pullrequest.PullRequestUpdated;
import io.onedev.server.web.editable.annotation.BranchPatterns;
import io.onedev.server.web.editable.annotation.Editable;

@Editable(order=300, name="When pull requests are created/updated")
public class PullRequestTrigger extends JobTrigger {

	private static final long serialVersionUID = 1L;

	private String targetBranch;
	
	@Editable(order=100, description="Optionally specify target branch of the pull request to match. "
			+ "Wildcard character * and ? may be used")
	@BranchPatterns
	public String getTargetBranch() {
		return targetBranch;
	}

	public void setTargetBranch(String targetBranch) {
		this.targetBranch = targetBranch;
	}
	
	@Override
	protected boolean matches(ProjectEvent event, CISpec ciSpec, Job job) {
		if (event instanceof PullRequestOpened) {
			PullRequestOpened pullRequestOpened = (PullRequestOpened) event;
			if (getTargetBranch() == null 
					|| PathUtils.matchChildAware(getTargetBranch(), pullRequestOpened.getRequest().getTargetBranch())) { 
				return true;
			}
		} 
		if (event instanceof PullRequestUpdated) {
			PullRequestUpdated pullRequestUpdated = (PullRequestUpdated) event;
			if (getTargetBranch() == null 
					|| PathUtils.matchChildAware(getTargetBranch(), pullRequestUpdated.getRequest().getTargetBranch())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String getDescription() {
		String condition;
		if (getTargetBranch() != null)
			condition = String.format("when pull requests to branch %s are created/updated", getTargetBranch());
		else
			condition = "when pull requests are created/updated";
		if (isIgnore())
			return "Do not trigger " + condition;
		else
			return "Trigger " + condition;
	}

}
