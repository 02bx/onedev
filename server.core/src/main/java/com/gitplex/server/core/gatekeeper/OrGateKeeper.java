package com.gitplex.server.core.gatekeeper;

import java.util.ArrayList;
import java.util.List;

import com.gitplex.commons.wicket.editable.annotation.Editable;
import com.gitplex.commons.wicket.editable.annotation.Horizontal;
import com.gitplex.server.core.gatekeeper.checkresult.Failed;
import com.gitplex.server.core.gatekeeper.checkresult.GateCheckResult;
import com.gitplex.server.core.gatekeeper.checkresult.Passed;
import com.gitplex.server.core.gatekeeper.checkresult.Pending;

@Editable(name="Or Composition", order=200, icon="fa-object-group", category=GateKeeper.CATEGORY_COMPOSITION,
		description="This gatekeeper will be passed if any of the contained gatekeepers is passed")
@Horizontal
public class OrGateKeeper extends AndOrGateKeeper {

	private static final long serialVersionUID = 1L;
	
	@Override
	protected GateCheckResult aggregate(Checker checker) {
		List<String> pendingReasons = new ArrayList<String>();
		List<String> rejectReasons = new ArrayList<String>();
		
		for (GateKeeper each: getGateKeepers()) {
			GateCheckResult result = checker.check(each);
			if (result instanceof Failed) {
				rejectReasons.addAll(result.getReasons());
			} else if (result instanceof Passed) {
				return result;
			} else if (result instanceof Pending) {
				pendingReasons.addAll(result.getReasons());
			}
		}
		
		if (!pendingReasons.isEmpty())
			return pending(pendingReasons);
		else
			return failed(rejectReasons);
	}

}
