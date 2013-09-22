package com.pmease.gitop.core.gatekeeper;

import com.pmease.commons.editable.annotation.Editable;
import com.pmease.gitop.core.model.MergeRequest;

@SuppressWarnings("serial")
@Editable
public class HasSourceBranch extends AbstractGateKeeper {

	@Override
	public CheckResult check(MergeRequest request) {
		if (request.getSource() != null)
			return accept("Associated with source branch.");
		else
			return reject("Not associated with source branch.");
	}

}
