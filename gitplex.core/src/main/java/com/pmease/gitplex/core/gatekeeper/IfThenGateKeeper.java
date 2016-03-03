package com.pmease.gitplex.core.gatekeeper;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.pmease.commons.wicket.editable.annotation.Editable;
import com.pmease.commons.wicket.editable.annotation.Horizontal;
import com.pmease.gitplex.core.entity.Account;
import com.pmease.gitplex.core.entity.Depot;
import com.pmease.gitplex.core.gatekeeper.checkresult.Blocking;
import com.pmease.gitplex.core.gatekeeper.checkresult.CheckResult;
import com.pmease.gitplex.core.gatekeeper.checkresult.Failed;
import com.pmease.gitplex.core.gatekeeper.checkresult.Passed;
import com.pmease.gitplex.core.gatekeeper.checkresult.Pending;

@Editable(name="If...Then... Composition", order=300, icon="fa-object-group",  
		category=GateKeeper.CATEGORY_COMPOSITION, 
		description="If first gate keeper is passed, then go ahead to check second gate keeper")
@Horizontal
public class IfThenGateKeeper extends CompositeGateKeeper {

	private static final long serialVersionUID = 1L;
	
	private GateKeeper ifGate = new DefaultGateKeeper();
	
	private GateKeeper thenGate = new DefaultGateKeeper();
	
	@Valid
	@NotNull
	public GateKeeper getIfGate() {
		return ifGate;
	}

	public void setIfGate(GateKeeper ifGate) {
		this.ifGate = ifGate;
	}

	@Valid
	@NotNull
	public GateKeeper getThenGate() {
		return thenGate;
	}

	public void setThenGate(GateKeeper thenGate) {
		this.thenGate = thenGate;
	}

	@Override
	protected CheckResult aggregate(Checker checker) {
		CheckResult ifResult = checker.check(getIfGate());
		if (ifResult instanceof Passed) {
			return checker.check(getThenGate());
		} else if (ifResult instanceof Failed) {
			return passed(ifResult.getReasons());
		} else if (ifResult instanceof Blocking) {
			return ifResult;
		} else if (ifResult instanceof Pending) {
			CheckResult thenResult = checker.check(getThenGate());
			if (thenResult instanceof Passed)
				return thenResult;
			else 
				return ifResult;
		} else {
			return ifResult;
		}
	}

	@Override
	public void onDepotRename(Depot renamedDepot, String oldName) {
		ifGate.onDepotRename(renamedDepot, oldName);
		thenGate.onDepotRename(renamedDepot, oldName);
	}

	@Override
	public void onAccountRename(String oldName, String newName) {
		ifGate.onAccountRename(oldName, newName);
		thenGate.onAccountRename(oldName, newName);
	}

	@Override
	public boolean onDepotTransfer(Depot depotDefiningGateKeeper, Depot transferredDepot, 
			Account originalOwner) {
		return ifGate.onDepotTransfer(depotDefiningGateKeeper, transferredDepot, originalOwner) 
				|| thenGate.onDepotTransfer(depotDefiningGateKeeper, transferredDepot, originalOwner);
	}
	
	@Override
	public boolean onAccountDelete(String accountName) {
		return ifGate.onAccountDelete(accountName) || thenGate.onAccountDelete(accountName);
	}

	@Override
	public boolean onDepotDelete(Depot depot) {
		return ifGate.onDepotDelete(depot) || thenGate.onDepotDelete(depot);
	}

	@Override
	public void onTeamRename(String oldName, String newName) {
		ifGate.onTeamRename(oldName, newName);
		thenGate.onTeamRename(oldName, newName);
	}

	@Override
	public boolean onTeamDelete(String teamName) {
		return ifGate.onTeamDelete(teamName) || thenGate.onTeamDelete(teamName);
	}

	@Override
	public boolean onRefDelete(String refName) {
		return ifGate.onRefDelete(refName) || thenGate.onRefDelete(refName);
	}

}
