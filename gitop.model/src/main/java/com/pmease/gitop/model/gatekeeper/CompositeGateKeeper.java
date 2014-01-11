package com.pmease.gitop.model.gatekeeper;

import com.pmease.commons.editable.annotation.Editable;
import com.pmease.gitop.model.Branch;
import com.pmease.gitop.model.Project;
import com.pmease.gitop.model.PullRequest;
import com.pmease.gitop.model.User;
import com.pmease.gitop.model.gatekeeper.checkresult.CheckResult;

@SuppressWarnings("serial")
@Editable(name="Composite Gate Keepers")
public abstract class CompositeGateKeeper extends AbstractGateKeeper {
	
	public static interface Checker {
		CheckResult check(GateKeeper gateKeeper);
	}
	
	protected abstract CheckResult aggregate(Checker checker);

	@Override
	public CheckResult doCheckRequest(final PullRequest request) {
		return aggregate(new Checker() {

			@Override
			public CheckResult check(GateKeeper gateKeeper) {
				return gateKeeper.checkRequest(request);
			}
			
		});
	}

	@Override
	protected CheckResult doCheckFile(final User user, final Branch branch, final String file) {
		return aggregate(new Checker() {

			@Override
			public CheckResult check(GateKeeper gateKeeper) {
				return gateKeeper.checkFile(user, branch, file);
			}
			
		});
	}
	
	@Override
	protected CheckResult doCheckCommit(final User user, final Branch branch, final String commit) {
		return aggregate(new Checker() {

			@Override
			public CheckResult check(GateKeeper gateKeeper) {
				return gateKeeper.checkCommit(user, branch, commit);
			}
			
		});
	}

	@Override
	protected CheckResult doCheckRef(final User user, final Project project, final String refName) {
		return aggregate(new Checker() {

			@Override
			public CheckResult check(GateKeeper gateKeeper) {
				return gateKeeper.checkRef(user, project, refName);
			}
			
		});
	}

}
