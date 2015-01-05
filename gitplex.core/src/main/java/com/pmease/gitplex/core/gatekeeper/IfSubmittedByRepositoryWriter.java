package com.pmease.gitplex.core.gatekeeper;

import com.pmease.commons.editable.annotation.Editable;
import com.pmease.gitplex.core.gatekeeper.checkresult.CheckResult;
import com.pmease.gitplex.core.model.Branch;
import com.pmease.gitplex.core.model.PullRequest;
import com.pmease.gitplex.core.model.Repository;
import com.pmease.gitplex.core.model.User;
import com.pmease.gitplex.core.permission.ObjectPermission;

@SuppressWarnings("serial")
@Editable(order=50, icon="pa-group-o", description=
		"This gate keeper will be passed if the commit is submitted by an user with "
		+ "write permission against the repository.")
public class IfSubmittedByRepositoryWriter extends ApprovalGateKeeper {

	@Override
	public CheckResult doCheckRequest(PullRequest request) {
		return check(request.getSubmitter(), request.getTarget().getRepository());
	}
	
	private CheckResult check(User user, Repository repository) {
		if (user.asSubject().isPermitted(ObjectPermission.ofRepositoryWrite(repository)))
			return passed("Submitted by repository writer");
		else
			return failed("Not submitted by repository writer");
	}
	
	@Override
	protected CheckResult doCheckFile(User user, Branch branch, String file) {
		return check(user, branch.getRepository());
	}

	@Override
	protected CheckResult doCheckCommit(User user, Branch branch, String commit) {
		return check(user, branch.getRepository());
	}

	@Override
	protected CheckResult doCheckRef(User user, Repository repository, String refName) {
		return check(user, repository);
	}

}
