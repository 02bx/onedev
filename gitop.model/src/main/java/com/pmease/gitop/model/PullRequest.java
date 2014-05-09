package com.pmease.gitop.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import com.pmease.gitop.model.gatekeeper.checkresult.CheckResult;
import com.pmease.gitop.model.gatekeeper.checkresult.Disapproved;
import com.pmease.gitop.model.gatekeeper.checkresult.Pending;
import com.pmease.gitop.model.gatekeeper.checkresult.PendingAndBlock;

@SuppressWarnings("serial")
@Entity
public class PullRequest extends AbstractEntity {

	public enum Status {
		PENDING_APPROVAL("Pending Approval"), PENDING_UPDATE("Pending Update"), 
		PENDING_INTEGRATE("Pending Integrate"), INTEGRATED("Integrated"), 
		DISCARDED("Discarded");

		private final String displayName;
		
		Status(String displayName) {
			this.displayName = displayName;
		}
		
		@Override
		public String toString() {
			return displayName;
		}
		
	}
	
	@Column(nullable = false)
	private String title;
	
	private String description;
	
	@Column(nullable=false)
	private String baseCommit;

	private boolean autoMerge;

	@ManyToOne
	private User submitter;
	
	@ManyToOne
	@JoinColumn(nullable = false)
	private Branch target;

	@ManyToOne
	private Branch source;
	
	@Transient
	private Git sandbox;
	
	@Lob
	private CheckResult checkResult;

	@Embedded
	private MergeInfo mergeInfo;
	
	@Embedded
	private CloseInfo closeInfo;

	@Column(nullable=false)
	private Date createDate = new Date();
	
	@Column(nullable=false)
	private Date updateDate = new Date();

	@OneToMany(mappedBy = "request", cascade = CascadeType.REMOVE)
	private Collection<PullRequestUpdate> updates = new ArrayList<PullRequestUpdate>();

	@OneToMany(mappedBy = "request", cascade = CascadeType.REMOVE)
	private Collection<VoteInvitation> voteInvitations = new ArrayList<VoteInvitation>();
	
	@OneToMany(mappedBy = "request", cascade = CascadeType.REMOVE)
	private Collection<PullRequestComment> comments = new ArrayList<PullRequestComment>();

	private transient List<PullRequestUpdate> sortedUpdates;

	private transient List<PullRequestUpdate> effectiveUpdates;

	private transient PullRequestUpdate baseUpdate;
	
	private transient Set<String> mergedCommits;
	
	private transient Set<String> pendingCommits;
	
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

	/**
	 * Get head commit of target branch at the time when pull request is created.
     *
	 * @return
	 * 			head commit of the target branch at the time creating the pull request
	 */
	public String getBaseCommit() {
		return baseCommit;
	}

	public void setBaseCommit(String baseCommit) {
		this.baseCommit = baseCommit;
	}

	public boolean isAutoMerge() {
		return autoMerge;
	}

	public void setAutoMerge(boolean autoMerge) {
		this.autoMerge = autoMerge;
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

	public Collection<PullRequestComment> getComments() {
		return comments;
	}

	public void setComments(Collection<PullRequestComment> comments) {
		this.comments = comments;
	}

	public Status getStatus() {
		if (closeInfo != null) {
			if (closeInfo.getCloseStatus() == CloseInfo.Status.INTEGRATED)
				return Status.INTEGRATED;
			else
				return Status.DISCARDED;
		} else if (checkResult instanceof Pending || checkResult instanceof PendingAndBlock) {
			return Status.PENDING_APPROVAL;
		} else if (checkResult instanceof Disapproved) {
			return Status.PENDING_UPDATE;
		} else {
			return Status.PENDING_INTEGRATE;
		}
	}

	public boolean isOpen() {
		return closeInfo == null;
	}
	
	public PullRequestUpdate getBaseUpdate() {
		if (baseUpdate != null) {
			return baseUpdate;
		} else {
			return getEffectiveUpdates().iterator().next();
		}
	}

	public void setBaseUpdate(PullRequestUpdate baseUpdate) {
		this.baseUpdate = baseUpdate;
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
	 * Get merge info of this pull request.
	 *  
	 * @return
	 * 			merge info of this pull request
	 */
	public MergeInfo getMergeInfo() {
		return mergeInfo;
	}
	
	public void setMergeInfo(MergeInfo mergeInfo) {
		this.mergeInfo = mergeInfo;
	}

	public @Nullable CloseInfo getCloseInfo() {
		return closeInfo;
	}

	public void setCloseInfo(CloseInfo closeInfo) {
		this.closeInfo = closeInfo;
	}

	/**
	 * Get list of sorted updates.
	 * <p>
	 * 
	 * @return list of sorted updates ordered by update id reversely
	 */
	public List<PullRequestUpdate> getSortedUpdates() {
		if (sortedUpdates == null) {
			Preconditions.checkState(!getUpdates().isEmpty());
			sortedUpdates = new ArrayList<PullRequestUpdate>(getUpdates());

			if (sortedUpdates.size() > 1) {
				Collections.sort(sortedUpdates);
				Collections.reverse(sortedUpdates);
			}
		}
		return sortedUpdates;
	}

	/**
	 * Get list of effective updates in reverse order. Update is considered
	 * effective if it is not ancestor of target branch head.
	 * 
	 * @return 
	 * 			list of effective updates in reverse order.
	 */
	public List<PullRequestUpdate> getEffectiveUpdates() {
		if (effectiveUpdates == null) {
			effectiveUpdates = new ArrayList<PullRequestUpdate>();

			Git git = getTarget().getRepository().git();
			for (PullRequestUpdate update : getSortedUpdates()) {
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
		return getSortedUpdates().iterator().next();
	}
	
	public PullRequestUpdate getInitialUpdate() {
		List<PullRequestUpdate> updates = getSortedUpdates();
		return updates.get(updates.size()-1);
	}
	
	public Collection<String> findTouchedFiles() {
		Git git = getTarget().getRepository().git();
		return git.listChangedFiles(getTarget().getHeadCommit(), getLatestUpdate().getHeadCommit());
	}

	public String getHeadRef() {
		Preconditions.checkNotNull(getId());
		return Repository.REFS_GITOP + "pulls/" + getId() + "/head";
	}
	
	public String getMergeRef() {
		Preconditions.checkNotNull(getId());
		return Repository.REFS_GITOP + "pulls/" + getId() + "/merge";
	};

	/**
	 * Delete refs of this pull request, without touching refs of its updates.
	 */
	public void deleteRefs() {
		Git git = getTarget().getRepository().git();
		git.deleteRef(getHeadRef());
		git.deleteRef(getMergeRef());
	}
	
	public String getLockName() {
		Preconditions.checkNotNull(getId());
		return "pull request: " + getId();
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
		for (Vote vote : getBaseUpdate().listVotesOnwards()) {
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
			return Restrictions.isNull("closeInfo");
		}
		
		public static Criterion ofClosed() {
			return Restrictions.isNotNull("closeInfo");
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

	public Set<String> getMergedCommits() {
		if (mergedCommits == null) {
			mergedCommits = new HashSet<>();
			Repository repo = getTarget().getRepository();
			for (Commit commit: repo.git().log(getBaseCommit(), getTarget().getHeadCommit(), null, 0, 0)) {
				mergedCommits.add(commit.getHash());
			}
		}
		return mergedCommits;
	}

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

}
