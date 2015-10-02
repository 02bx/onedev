package com.pmease.gitplex.core.gatekeeper;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.pmease.commons.wicket.editable.annotation.Editable;
import com.pmease.gitplex.core.gatekeeper.checkresult.CheckResult;
import com.pmease.gitplex.core.gatekeeper.helper.branchselection.SpecifyTargetBranchesByNames;
import com.pmease.gitplex.core.gatekeeper.helper.branchselection.TargetBranchSelection;
import com.pmease.gitplex.core.model.PullRequest;
import com.pmease.gitplex.core.model.Repository;
import com.pmease.gitplex.core.model.User;

@SuppressWarnings("serial")
@Editable(order=400, icon="fa-lock", category=GateKeeper.CATEGORY_COMMONLY_USED, description=
		"Use this gate keeper to prevent history of certain branches from being rewritten via forced push.")
public class HistoryProtection extends AbstractGateKeeper {

	private TargetBranchSelection branchSelection = new SpecifyTargetBranchesByNames();
	
	@Editable(name="Branches to Be Protected", order=100)
	@Valid
	@NotNull
	public TargetBranchSelection getBranchSelection() {
		return branchSelection;
	}

	public void setBranchSelection(TargetBranchSelection branchSelection) {
		this.branchSelection = branchSelection;
	}

	@Override
	protected GateKeeper trim(Repository repository) {
		return this;
	}

	private GateKeeper getGateKeeper() {
		IfThenGateKeeper ifThenGate = new IfThenGateKeeper();
		ifThenGate.setIfGate(branchSelection.getGateKeeper());
		ifThenGate.setThenGate(new IfPushWithoutRewritingHistory());
		
		return ifThenGate;
	}

	@Override
	protected CheckResult doCheckRequest(PullRequest request) {
		return getGateKeeper().checkRequest(request);
	}

	@Override
	protected CheckResult doCheckFile(User user, Repository repository, String branch, String file) {
		return getGateKeeper().checkFile(user, repository, branch, file);
	}

	@Override
	protected CheckResult doCheckCommit(User user, Repository repository, String branch, String commit) {
		return getGateKeeper().checkCommit(user, repository, branch, commit);
	}

	@Override
	protected CheckResult doCheckRef(User user, Repository repository, String refName) {
		return getGateKeeper().checkRef(user, repository, refName);
	}

}
