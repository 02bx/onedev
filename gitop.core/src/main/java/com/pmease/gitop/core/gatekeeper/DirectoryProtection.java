package com.pmease.gitop.core.gatekeeper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotEmpty;

import com.pmease.commons.editable.annotation.Editable;
import com.pmease.commons.editable.annotation.OmitName;
import com.pmease.commons.util.trimmable.TrimUtils;
import com.pmease.commons.util.trimmable.Trimmable;
import com.pmease.gitop.core.Gitop;
import com.pmease.gitop.core.editable.BranchChoice;
import com.pmease.gitop.core.editable.DirectoryChoice;
import com.pmease.gitop.core.editable.TeamChoice;
import com.pmease.gitop.core.manager.BranchManager;
import com.pmease.gitop.core.manager.TeamManager;
import com.pmease.gitop.model.Branch;
import com.pmease.gitop.model.Project;
import com.pmease.gitop.model.PullRequest;
import com.pmease.gitop.model.Team;
import com.pmease.gitop.model.User;
import com.pmease.gitop.model.gatekeeper.AndGateKeeper;
import com.pmease.gitop.model.gatekeeper.CommonGateKeeper;
import com.pmease.gitop.model.gatekeeper.GateKeeper;
import com.pmease.gitop.model.gatekeeper.IfThenGateKeeper;
import com.pmease.gitop.model.gatekeeper.checkresult.CheckResult;

@SuppressWarnings("serial")
@Editable(order=200, icon="icon-lock", description="By default, users with write permission of "
		+ "the project can write to all directories. Use this gate keeper to restrict write "
		+ "access of certain directories in specified branches to certain teams. Note that if "
		+ "branch is not specified, the directory restriction will apply to all branches.")
public class DirectoryProtection extends CommonGateKeeper {
	
	private List<Long> branchIds = new ArrayList<Long>();
	
	private List<Entry> entries = new ArrayList<Entry>();
	
	public DirectoryProtection() {
		Entry entry = new Entry();
		entries.add(entry);
	}

	@Editable(name="Branches To Apply (Optionally)", order=50)
	@BranchChoice
	@NotNull
	public List<Long> getBranchIds() {
		return branchIds;
	}

	public void setBranchIds(List<Long> branchIds) {
		this.branchIds = branchIds;
	}

	@Editable(name="Protected Directories", order=100)
	@Valid
	@Size(min=1, message="At least one entry has to be added.")
	@NotNull
	@OmitName(OmitName.Place.EDITOR)
	public List<Entry> getEntries() {
		return entries;
	}

	public void setEntries(List<Entry> entries) {
		this.entries = entries;
	}

	@Editable
	public static class Entry implements Trimmable, Serializable {
		
		private String directory;
		
		private Long teamId;

		@Editable(name="Directory to Protect", order=100)
		@DirectoryChoice
		@NotEmpty
		public String getDirectory() {
			return directory;
		}

		public void setDirectory(String directory) {
			this.directory = directory;
		}

		@Editable(name="Team Can Write", order=200)
		@TeamChoice(excludes={Team.ANONYMOUS, Team.LOGGEDIN})
		@NotNull
		public Long getTeamId() {
			return teamId;
		}

		public void setTeamId(Long teamId) {
			this.teamId = teamId;
		}

		@Override
		public Object trim(Object context) {
			if (Gitop.getInstance(TeamManager.class).get(teamId) == null)
				return null;
			else
				return this;
		}

	}

	private GateKeeper getGateKeeper() {
		AndGateKeeper andGate = new AndGateKeeper();
		for (Entry entry: entries) {
			IfThenGateKeeper ifThenGate = new IfThenGateKeeper();
			IfTouchesSpecifiedDirectories ifGate = new IfTouchesSpecifiedDirectories();
			ifGate.getDirectories().add(entry.getDirectory());
			ifThenGate.setIfGate(ifGate);
			
			IfApprovedBySpecifiedTeam thenGate = new IfApprovedBySpecifiedTeam();
			thenGate.setTeamId(entry.getTeamId());
			ifThenGate.setThenGate(thenGate);
			
			andGate.getGateKeepers().add(ifThenGate);
		}
		if (branchIds.isEmpty()) {
			return andGate;
		} else {
			IfThenGateKeeper ifThenGate = new IfThenGateKeeper();
			IfSubmittedToSpecifiedBranches branchGate = new IfSubmittedToSpecifiedBranches();
			branchGate.getBranchIds().addAll(branchIds);
			ifThenGate.setIfGate(branchGate);
			ifThenGate.setThenGate(andGate);
			return ifThenGate;
		}
	}

	@Override
	protected GateKeeper trim(Project project) {
		if (!branchIds.isEmpty()) {
			for (Iterator<Long> it = branchIds.iterator(); it.hasNext();) {
				if (Gitop.getInstance(BranchManager.class).get(it.next()) == null)
					it.remove();
			}
			if (branchIds.isEmpty())
				return null;
		}
		TrimUtils.trim(entries, project);

		if (entries.isEmpty())
			return null;
		else
			return this;
	}

	@Override
	protected CheckResult doCheckRequest(PullRequest request) {
		return getGateKeeper().checkRequest(request);
	}

	@Override
	protected CheckResult doCheckFile(User user, Branch branch, @Nullable String file) {
		return getGateKeeper().checkFile(user, branch, file);
	}

	@Override
	protected CheckResult doCheckCommit(User user, Branch branch, String commit) {
		return getGateKeeper().checkCommit(user, branch, commit);
	}

}
