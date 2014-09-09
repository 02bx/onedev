package com.pmease.gitplex.core.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import com.google.common.base.Preconditions;
import com.pmease.commons.git.Commit;
import com.pmease.commons.git.Git;
import com.pmease.commons.hibernate.AbstractEntity;
import com.pmease.commons.util.LockUtils;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.gatekeeper.checkresult.Approved;
import com.pmease.gitplex.core.gatekeeper.checkresult.CheckResult;
import com.pmease.gitplex.core.gatekeeper.checkresult.Disapproved;
import com.pmease.gitplex.core.gatekeeper.checkresult.Ignored;
import com.pmease.gitplex.core.gatekeeper.checkresult.Pending;
import com.pmease.gitplex.core.gatekeeper.checkresult.PendingAndBlock;
import com.pmease.gitplex.core.manager.CommentVisitManager;
import com.pmease.gitplex.core.manager.CommitCommentManager;
import com.pmease.gitplex.core.manager.UserManager;

@SuppressWarnings("serial")
@Entity
public class PullRequest extends AbstractEntity {

	public enum CloseStatus {INTEGRATED, DISCARDED};
	
	public enum Status {
		PENDING_REFRESH("Pending Check"), PENDING_APPROVAL("Pending Approval"), 
		PENDING_UPDATE("Pending Update"), PENDING_INTEGRATE("Pending Integration"), 
		INTEGRATED("Integrated"), DISCARDED("Discarded");

		private final String displayName;
		
		Status(String displayName) {
			this.displayName = displayName;
		}
		
		@Override
		public String toString() {
			return displayName;
		}
		
	}
	
	@Column
	private CloseStatus closeStatus;

	@Column(nullable=false)
	private String title;
	
	private String description;
	
	private boolean autoIntegrate;

	@ManyToOne
	private User submitter;
	
	@ManyToOne
	@JoinColumn(nullable = false)
	private Branch target;

	@ManyToOne
	private Branch source;
	
	@Column(nullable=false)
	private String baseCommit;
	
	@Transient
	private Git sandbox;
	
	@Lob
	private CheckResult checkResult;

	@Embedded
	private IntegrationInfo integrationInfo;
	
	@Column(nullable=false)
	private Date createDate = new Date();
	
	@Column(nullable=false)
	private Date updateDate = new Date();

	@OneToMany(mappedBy = "request", cascade = CascadeType.REMOVE)
	private Collection<PullRequestUpdate> updates = new ArrayList<>();

	@OneToMany(mappedBy = "request", cascade = CascadeType.REMOVE)
	private Collection<VoteInvitation> voteInvitations = new ArrayList<>();
	
	@OneToMany(mappedBy = "request", cascade = CascadeType.REMOVE)
	private Collection<Verification> verifications = new ArrayList<>();

	@OneToMany(mappedBy = "request", cascade = CascadeType.REMOVE)
	private Collection<PullRequestComment> requestComments = new ArrayList<>();

	@OneToMany(mappedBy = "request", cascade = CascadeType.REMOVE)
	private Collection<InlineComment> inlineComments = new ArrayList<>();

	@OneToMany(mappedBy = "request", cascade = CascadeType.REMOVE)
	private Collection<PullRequestAudit> audits = new ArrayList<>();

	private transient List<PullRequestUpdate> sortedUpdates;
	
	private transient List<PullRequestUpdate> effectiveUpdates;

	private transient PullRequestUpdate referentialUpdate;
	
	private transient Set<String> pendingCommits;
	
	private transient Map<String, List<CommitComment>> commitComments;
	
	private transient Map<String, Map<CommentPosition, Date>> commentVisits;
	
	private transient List<String> commits;
	
	/**
	 * Get title of this merge request.
	 * 
	 * @return user specified title of this merge request, <tt>null</tt> for
	 *         auto-created merge request.
	 */
	public @Nullable String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isAutoIntegrate() {
		return autoIntegrate;
	}

	public void setAutoIntegrate(boolean autoIntegrate) {
		this.autoIntegrate = autoIntegrate;
	}

	/**
	 * Get the user submitting the pull request.
	 * 
	 * @return
	 * 			the user submitting the pull request, or <tt>null</tt> if the user 
	 * 			submitting the request is removed.
	 */
	@Nullable
	public User getSubmitter() {
		return submitter;
	}

	public void setSubmitter(@Nullable User submitter) {
		this.submitter = submitter;
	}

	/**
	 * Get target branch of this request.
	 * 
	 * @return
	 * 			target branch of this request
	 */
	public Branch getTarget() {
		return target;
	}

	public void setTarget(@Nullable Branch target) {
		this.target = target;
	}

	/**
	 * Get source branch of this request.
	 * 
	 * @return
	 * 			source branch of this request, or <tt>null</tt> if source branch 
	 * 			is deleted. In case of source branch being deleted, the pull request 
	 * 			may still in opened state for review, but can not be updated with 
	 * 			new commits
	 */
	@Nullable
	public Branch getSource() {
		return source;
	}

	public void setSource(@Nullable Branch source) {
		this.source = source;
	}

	public String getBaseCommit() {
		return baseCommit;
	}

	public void setBaseCommit(String baseCommit) {
		this.baseCommit = baseCommit;
	}

	public Git git() {
		if (sandbox == null)
			return getTarget().getRepository().git();
		else
			return sandbox;
	}

	public Git getSandbox() {
		return sandbox;
	}

	public void setSandbox(Git sandbox) {
		this.sandbox = sandbox;
	}

	public Collection<PullRequestUpdate> getUpdates() {
		return updates;
	}

	public void setUpdates(Collection<PullRequestUpdate> updates) {
		this.updates = updates;
	}

	public Collection<VoteInvitation> getVoteInvitations() {
		return voteInvitations;
	}

	public void setVoteInvitations(Collection<VoteInvitation> voteInvitations) {
		this.voteInvitations = voteInvitations;
	}

	public Collection<Verification> getVerifications() {
		return verifications;
	}

	public void setVerifications(Collection<Verification> verifications) {
		this.verifications = verifications;
	}

	public Collection<PullRequestComment> getRequestComments() {
		return requestComments;
	}

	public void setRequestComments(Collection<PullRequestComment> requestComments) {
		this.requestComments = requestComments;
	}

	public Collection<InlineComment> getInlineComments() {
		return inlineComments;
	}

	public void setInlineComments(Collection<InlineComment> inlineComments) {
		this.inlineComments = inlineComments;
	}

	public Collection<PullRequestAudit> getAudits() {
		return audits;
	}

	public void setAudits(Collection<PullRequestAudit> audits) {
		this.audits = audits;
	}

	public Status getStatus() {
		if (closeStatus == CloseStatus.INTEGRATED) 
			return Status.INTEGRATED;
		else if (closeStatus == CloseStatus.DISCARDED) 
			return Status.DISCARDED;
		else if (checkResult instanceof Pending || checkResult instanceof PendingAndBlock) 
			return Status.PENDING_APPROVAL;
		else if (checkResult instanceof Disapproved) 
			return Status.PENDING_UPDATE;
		else if (checkResult instanceof Ignored || checkResult instanceof Approved) 
			return Status.PENDING_INTEGRATE;
		else 
			return Status.PENDING_REFRESH;
	}

	@Nullable
	public CloseStatus getCloseStatus() {
		return closeStatus;
	}

	public void setCloseStatus(@Nullable CloseStatus closeStatus) {
		this.closeStatus = closeStatus;
	}

	public boolean isOpen() {
		return closeStatus == null;
	}
	
	public PullRequestUpdate getReferentialUpdate() {
		if (referentialUpdate != null) {
			return referentialUpdate;
		} else {
			return getEffectiveUpdates().get(0);
		}
	}

	public void setReferentialUpdate(PullRequestUpdate referentiralUpdate) {
		this.referentialUpdate = referentiralUpdate;
	}

	/**
	 * Get gate keeper check result.
	 *  
	 * @return
	 * 			check result of this pull request has not been refreshed yet
	 */
	public CheckResult getCheckResult() {
		return checkResult;
	}
	
	public void setCheckResult(CheckResult checkResult) {
		this.checkResult = checkResult;
	}
	
	/**
	 * Get integration info of this pull request.
	 *  
	 * @return
	 * 			integration info of this pull request, or <tt>null</tt> if integration 
	 * 			info has not been calculated yet
	 */
	public @Nullable IntegrationInfo getIntegrationInfo() {
		return integrationInfo;
	}
	
	public void setIntegrationInfo(IntegrationInfo integrationInfo) {
		this.integrationInfo = integrationInfo;
	}

	/**
	 * Get list of reversely sorted updates.
	 * 
	 * @return 
	 * 			list of sorted updates ordered by id reversely
	 */
	public List<PullRequestUpdate> getSortedUpdates() {
		if (sortedUpdates == null) {
			Preconditions.checkState(getUpdates().size() >= 1);
			sortedUpdates = new ArrayList<PullRequestUpdate>(getUpdates());
			Collections.sort(sortedUpdates);
			Collections.reverse(sortedUpdates);
		}
		return sortedUpdates;
	}

	/**
	 * Get list of effective updates reversely sorted by id. Update is considered effective if it is 
	 * not ancestor of target branch head.
	 * 
	 * @return 
	 * 			list of effective updates reversely sorted by id
	 */
	public List<PullRequestUpdate> getEffectiveUpdates() {
		if (effectiveUpdates == null) {
			effectiveUpdates = new ArrayList<PullRequestUpdate>();

			Git git = getTarget().getRepository().git();
			for (PullRequestUpdate update: getSortedUpdates()) {
				if (!git.isAncestor(update.getHeadCommit(), getTarget().getHeadCommit())) {
					effectiveUpdates.add(update);
				} else {
					break;
				}
			}
			
			Preconditions.checkState(!effectiveUpdates.isEmpty());
		}
		return effectiveUpdates;
	}

	public PullRequestUpdate getLatestUpdate() {
		return getSortedUpdates().get(0);
	}
	
	public Collection<String> findTouchedFiles() {
		Git git = getTarget().getRepository().git();
		return git.listChangedFiles(getTarget().getHeadCommit(), getLatestUpdate().getHeadCommit(), null);
	}

	public String getBaseRef() {
		Preconditions.checkNotNull(getId());
		return Repository.REFS_GITPLEX + "pulls/" + getId() + "/base";
	}

	public String getHeadRef() {
		Preconditions.checkNotNull(getId());
		return Repository.REFS_GITPLEX + "pulls/" + getId() + "/head";
	}
	
	public String getIntegrateRef() {
		Preconditions.checkNotNull(getId());
		return Repository.REFS_GITPLEX + "pulls/" + getId() + "/integrate";
	};

	/**
	 * Delete refs of this pull request, without touching refs of its updates.
	 */
	public void deleteRefs() {
		Git git = getTarget().getRepository().git();
		git.deleteRef(getBaseRef(), null, null);
		git.deleteRef(getHeadRef(), null, null);
		git.deleteRef(getIntegrateRef(), null, null);
	}
	
    public <T> T lockAndCall(Callable<T> callable) {
		Preconditions.checkNotNull(getId());
		
    	return LockUtils.call("pull request: " + getId(), callable);
    }
	
	/**
	 * Invite specified number of users in candidates to vote for this request.
	 * <p>
	 * 
	 * @param candidates 
	 * 			a collection of users to invite users from
	 * @param count 
	 * 			number of users to invite
	 */
	public void pickVoters(Collection<User> candidates, int count) {
		Collection<User> copyOfCandidates = new HashSet<User>(candidates);

		// submitter is not allowed to vote for this request
		if (getSubmitter() != null)
			copyOfCandidates.remove(getSubmitter());

		/*
		 * users already voted since base update should be excluded from
		 * invitation list as their votes are still valid
		 */
		for (Vote vote : getReferentialUpdate().listVotesOnwards()) {
			copyOfCandidates.remove(vote.getVoter());
		}

		Set<User> invited = new HashSet<User>();
		for (VoteInvitation each : getVoteInvitations())
			invited.add(each.getVoter());

		invited.retainAll(copyOfCandidates);

		for (int i = 0; i < count - invited.size(); i++) {
			User selected = Collections.min(copyOfCandidates, new Comparator<User>() {

				@Override
				public int compare(User user1, User user2) {
					return user1.getVotes().size() - user2.getVotes().size();
				}

			});

			copyOfCandidates.remove(selected);

			VoteInvitation invitation = new VoteInvitation();
			invitation.setRequest(this);
			invitation.setVoter(selected);
			invitation.getRequest().getVoteInvitations().add(invitation);
			invitation.getVoter().getVoteInvitations().add(invitation);
		}

	}
	
	public static class CriterionHelper {
		public static Criterion ofOpen() {
			return Restrictions.isNull("closeStatus");
		}
		
		public static Criterion ofClosed() {
			return Restrictions.isNotNull("closeStatus");
		}
		
		public static Criterion ofTarget(Branch target) {
			return Restrictions.eq("target", target);
		}

		public static Criterion ofSource(Branch source) {
			return Restrictions.eq("source", source);
		}
		
		public static Criterion ofSubmitter(User submitter) {
			return Restrictions.eq("submitter", submitter);
		}
		
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public Date getUpdateDate() {
		return updateDate;
	}

	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	/**
	 * Get commits pending integration.
	 * 
	 * @return
	 * 			commits pending integration
	 */
	public Set<String> getPendingCommits() {
		if (pendingCommits == null) {
			pendingCommits = new HashSet<>();
			Repository repo = getTarget().getRepository();
			for (Commit commit: repo.git().log(getTarget().getHeadCommit(), getLatestUpdate().getHeadCommit(), null, 0, 0)) {
				pendingCommits.add(commit.getHash());
			}
		}
		return pendingCommits;
	}

	public boolean canIntegrate() {
		return getStatus() == Status.PENDING_INTEGRATE 
				&& getIntegrationInfo() != null 
				&& getIntegrationInfo().getIntegrationHead() != null;
	}

	public Map<String, List<CommitComment>> getCommitComments() {
		if (commitComments == null) {
			commitComments = new HashMap<>();
			Map<String, Date> commits = new HashMap<>();
			for (PullRequestUpdate update: updates) {
				for (Commit commit: update.getCommits())
					commits.put(commit.getHash(), commit.getCommitter().getWhen());
			}
			CommitCommentManager manager = GitPlex.getInstance(CommitCommentManager.class);
			for (CommitComment comment: manager.findByCommitOrDates(getTarget().getRepository(), 
					getBaseCommit(), Collections.min(commits.values()), Collections.max(commits.values()))) {
				if (comment.getCommit().equals(getBaseCommit()) || commits.containsKey(comment.getCommit())) {
					List<CommitComment> comments = commitComments.get(comment.getCommit());
					if (comments == null) {
						comments = new ArrayList<>();
						commitComments.put(comment.getCommit(), comments);
					}
					comments.add(comment);
				}
			}
		}
		return commitComments;
	}

	public Map<String, Map<CommentPosition, Date>> getCommentVisits() {
		if (commentVisits == null) {
			commentVisits = new HashMap<>();
			
			User currentUser = GitPlex.getInstance(UserManager.class).getCurrent();
			if (currentUser != null) {
				Map<String, Date> commits = new HashMap<>();
				for (PullRequestUpdate update: updates) {
					for (Commit commit: update.getCommits())
						commits.put(commit.getHash(), commit.getCommitter().getWhen());
				}
				CommentVisitManager manager = GitPlex.getInstance(CommentVisitManager.class);
				for (CommentVisit visit: manager.findByCommitOrDates(getTarget().getRepository(), currentUser, 
						getBaseCommit(), Collections.min(commits.values()), Collections.max(commits.values()))) {
					if (visit.getCommit().equals(getBaseCommit()) || commits.containsKey(visit.getCommit())) {
						Map<CommentPosition, Date> visits = commentVisits.get(visit.getCommit());
						if (visits == null) {
							visits = new HashMap<>();
							commentVisits.put(visit.getCommit(), visits);
						}
						visits.put(visit.getPosition(), visit.getVisitDate());
					}
				}
			}
		}
		return commentVisits;
	}

	public List<String> getCommits() {
		if (commits == null) {
			commits = new ArrayList<>();
			commits.add(getBaseCommit());
			for (int i=getSortedUpdates().size()-1; i>=0; i--) {
				for (Commit commit: getSortedUpdates().get(i).getCommits())
					commits.add(commit.getHash());
			}
		}
		return commits;
	}
	
}
