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
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.base.Preconditions;
import com.pmease.commons.git.Change;
import com.pmease.commons.git.Commit;
import com.pmease.commons.git.Git;
import com.pmease.commons.hibernate.AbstractEntity;
import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.commons.hibernate.dao.EntityCriteria;
import com.pmease.commons.jackson.ExternalView;
import com.pmease.commons.util.LockUtils;
import com.pmease.commons.util.Triple;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.comment.ChangeComments;
import com.pmease.gitplex.core.gatekeeper.checkresult.CheckResult;
import com.pmease.gitplex.core.gatekeeper.checkresult.Disapproved;
import com.pmease.gitplex.core.gatekeeper.checkresult.Pending;
import com.pmease.gitplex.core.gatekeeper.checkresult.PendingAndBlock;
import com.pmease.gitplex.core.manager.PullRequestManager;

@SuppressWarnings("serial")
@Entity
public class PullRequest extends AbstractEntity {

	public enum CloseStatus {INTEGRATED, DISCARDED};
	
	public enum Status {
		PENDING_APPROVAL("Pending Approval"), 
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
	
	public enum IntegrationStrategy {
		MERGE_ALWAYS("Merge always", "Always create merge commit when integrate into target branch"), 
		MERGE_IF_NECESSARY("Merge if necessary", "Create merge commit only if target branch can not be fast-forwarded to the pull request"), 
		MERGE_WITH_SQUASH("Merge with squash", "Squash all commits in the pull request and then merge with target branch"),
		REBASE_SOURCE_ONTO_TARGET("Rebase source onto target", "Rebase source branch onto target branch and then fast-forward target branch to source branch"), 
		REBASE_TARGET_ONTO_SOURCE("Rebase target onto source", "Rebase target branch onto source branch");

		private final String displayName;
		
		private final String description;
		
		IntegrationStrategy(String displayName, String description) {
			this.displayName = displayName;
			this.description = description;
		}
		
		public String getDisplayName() {
			return displayName;
		}

		public String getDescription() {
			return description;
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
	
	@ManyToOne(fetch=FetchType.LAZY)
	private User submitter;
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(nullable = false)
	private Branch target;

	@ManyToOne(fetch=FetchType.LAZY)
	private Branch source;
	
	@Column(nullable=false)
	private String baseCommitHash;

	@ManyToOne(fetch=FetchType.LAZY)
	private User assignee;
	
	@Transient
	private Git sandbox;
	
	@Embedded
	private IntegrationPreview lastIntegrationPreview;
	
	@Column(nullable=false)
	private Date createDate = new Date();
	
	@Column(nullable=false)
	private Date updateDate = new Date();
	
	@Column(nullable=false)
	private IntegrationStrategy integrationStrategy;

	@OneToMany(mappedBy="request", cascade=CascadeType.REMOVE)
	private Collection<PullRequestUpdate> updates = new ArrayList<>();

	@OneToMany(mappedBy="request", cascade=CascadeType.REMOVE)
	private Collection<ReviewInvitation> reviewInvitations = new ArrayList<>();
	
	@OneToMany(mappedBy="request", cascade=CascadeType.REMOVE)
	private Collection<PullRequestVerification> verifications = new ArrayList<>();

	@OneToMany(mappedBy="request", cascade=CascadeType.REMOVE)
	private Collection<PullRequestComment> comments = new ArrayList<>();

	@OneToMany(mappedBy="request", cascade=CascadeType.REMOVE)
	private Collection<PullRequestAudit> audits = new ArrayList<>();
	
	@OneToMany(mappedBy="request", cascade=CascadeType.REMOVE)
	private Collection<PullRequestNotification> notifications = new ArrayList<>();

	private transient CheckResult checkResult;

	private transient List<PullRequestUpdate> sortedUpdates;
	
	private transient List<PullRequestUpdate> effectiveUpdates;

	private transient PullRequestUpdate referentialUpdate;
	
	private transient Set<String> pendingCommits;
	
	private transient Collection<Commit> mergedCommits;
	
	private transient Map<ChangeKey, ChangeComments> commentsCache = new HashMap<>();
	
	private transient Collection<PullRequestCommentReply> commentReplies;
	
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
	 * Get the user responsible for integration of the pull request.
	 * 
	 * @return
	 * 			the user responsible for integration of this pull request, or <tt>null</tt> to have the 
	 * 			system integrate the pull request automatically
	 */
	@Nullable
	public User getAssignee() {
		return assignee;
	}

	public void setAssignee(User assignee) {
		this.assignee = assignee;
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

	public String getBaseCommitHash() {
		return baseCommitHash;
	}

	public void setBaseCommitHash(String baseCommitHash) {
		this.baseCommitHash = baseCommitHash;
	}
	
	public Commit getBaseCommit() {
		return getTarget().getRepository().getCommit(getBaseCommitHash());
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

	public Collection<ReviewInvitation> getReviewInvitations() {
		return reviewInvitations;
	}

	public void setReviewInvitations(Collection<ReviewInvitation> reviewInvitations) {
		this.reviewInvitations = reviewInvitations;
	}

	public Collection<PullRequestVerification> getVerifications() {
		return verifications;
	}

	public void setVerifications(Collection<PullRequestVerification> verifications) {
		this.verifications = verifications;
	}

	public Collection<PullRequestComment> getComments() {
		return comments;
	}

	public void setComments(Collection<PullRequestComment> comments) {
		this.comments = comments;
	}

	public Collection<PullRequestAudit> getAudits() {
		return audits;
	}

	public void setAudits(Collection<PullRequestAudit> audits) {
		this.audits = audits;
	}

	public Collection<PullRequestNotification> getNotifications() {
		return notifications;
	}

	public void setNotifications(Collection<PullRequestNotification> notifications) {
		this.notifications = notifications;
	}

	public Status getStatus() {
		if (closeStatus == CloseStatus.INTEGRATED) 
			return Status.INTEGRATED;
		else if (closeStatus == CloseStatus.DISCARDED) 
			return Status.DISCARDED;
		else if (getCheckResult() instanceof Pending || getCheckResult() instanceof PendingAndBlock) 
			return Status.PENDING_APPROVAL;
		else if (getCheckResult() instanceof Disapproved) 
			return Status.PENDING_UPDATE;
		else  
			return Status.PENDING_INTEGRATE;
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
		if (checkResult == null) 
			checkResult = getTarget().getRepository().getGateKeeper().checkRequest(this);
		return checkResult;
	}
	
	/**
	 * Get last integration preview of this pull request. Note that this method may return an 
	 * out dated integration preview. Refer to {@link this#getIntegrationPreview()}
	 * if you'd like to get an update-to-date integration preview
	 *  
	 * @return
	 * 			integration preview of this pull request, or <tt>null</tt> if integration 
	 * 			preview has not been calculated yet. 
	 */
	@Nullable
	public IntegrationPreview getLastIntegrationPreview() {
		return lastIntegrationPreview;
	}
	
	public void setLastIntegrationPreview(IntegrationPreview lastIntegrationPreview) {
		this.lastIntegrationPreview = lastIntegrationPreview;
	}

	/**
	 * Get effective integration preview of this pull request.
	 * 
	 * @return
	 * 			update to date integration preview of this pull request, or <tt>null</tt> if 
	 * 			the integration preview has not been calculated or out dated. In both cases, 
	 * 			it will trigger a re-calculation, and client should call this method later 
	 * 			to get the calculated result 
	 */
	@JsonView(ExternalView.class)
	@Nullable
	public IntegrationPreview getIntegrationPreview() {
		return GitPlex.getInstance(PullRequestManager.class).previewIntegration(this);
	}
	
	/**
	 * Get list of sorted updates.
	 * 
	 * @return 
	 * 			list of sorted updates ordered by id
	 */
	public List<PullRequestUpdate> getSortedUpdates() {
		if (sortedUpdates == null) {
			Preconditions.checkState(getUpdates().size() >= 1);
			sortedUpdates = new ArrayList<PullRequestUpdate>(getUpdates());
			Collections.sort(sortedUpdates);
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
			for (int i=getSortedUpdates().size()-1; i>=0; i--) {
				PullRequestUpdate update = getSortedUpdates().get(i);
				if (!git.isAncestor(update.getHeadCommitHash(), getTarget().getHeadCommitHash()))
					effectiveUpdates.add(update);
				else 
					break;
			}
			
			Preconditions.checkState(!effectiveUpdates.isEmpty());
		}
		return effectiveUpdates;
	}

	public IntegrationStrategy getIntegrationStrategy() {
		return integrationStrategy;
	}

	public void setIntegrationStrategy(IntegrationStrategy integrationStrategy) {
		this.integrationStrategy = integrationStrategy;
	}

	@JsonView(ExternalView.class)
	public PullRequestUpdate getLatestUpdate() {
		return getSortedUpdates().get(getSortedUpdates().size()-1);
	}
	
	public Collection<String> findTouchedFiles() {
		Git git = getTarget().getRepository().git();
		return git.listChangedFiles(getTarget().getHeadCommitHash(), getLatestUpdate().getHeadCommitHash(), null);
	}

	@JsonView(ExternalView.class)
	public String getBaseRef() {
		Preconditions.checkNotNull(getId());
		return Repository.REFS_GITPLEX + "pulls/" + getId() + "/base";
	}

	@JsonView(ExternalView.class)
	public String getIntegrateRef() {
		Preconditions.checkNotNull(getId());
		return Repository.REFS_GITPLEX + "pulls/" + getId() + "/integrate";
	}

	/**
	 * Delete refs of this pull request, without touching refs of its updates.
	 */
	public void deleteRefs() {
		Git git = getTarget().getRepository().git();
		git.deleteRef(getBaseRef(), null, null);
		git.deleteRef(getIntegrateRef(), null, null);
	}
	
    public <T> T lockAndCall(Callable<T> callable) {
		Preconditions.checkNotNull(getId());
		
    	return LockUtils.call("pull request: " + getId(), callable);
    }
	
	/**
	 * Invite specified number of users in candidates to review this request.
	 * <p>
	 * 
	 * @param candidates 
	 * 			a collection of users to invite users from
	 * @param count 
	 * 			number of users to invite
	 */
	public void pickReviewers(Collection<User> candidates, int count) {
		List<User> pickList = new ArrayList<User>(candidates);

		// submitter is not allowed to review this request
		if (getSubmitter() != null)
			pickList.remove(getSubmitter());

		/*
		 * users already reviewed since base update should be excluded from
		 * invitation list as their reviews are still valid
		 */
		for (Review review: getReferentialUpdate().listReviewsOnwards())
			pickList.remove(review.getReviewer());

		final Set<User> invited = new HashSet<>();
		final Set<User> inviteExcluded = new HashSet<>();
		
		for (ReviewInvitation invitation: getReviewInvitations()) {
			if (!invitation.isExcluded())
				invited.add(invitation.getReviewer());
			else
				inviteExcluded.add(invitation.getReviewer());
		}

		/* Follow below rules to pick reviewers:
		 * 1. If user is excluded previously, it will be considered last.
		 * 2. If user is already a reviewer, it will be considered first.
		 * 3. Otherwise pick user with least reviews.
		 */
		Collections.sort(pickList, new Comparator<User>() {

			@Override
			public int compare(User user1, User user2) {
				if (invited.contains(user1)) {
					if (invited.contains(user2))
						return user1.getReviewEffort() - user2.getReviewEffort();
					else
						return -1;
				} else if (invited.contains(user2)) {
					return 1;
				} else if (inviteExcluded.contains(user1)) {
					if (inviteExcluded.contains(user2)) 
						return user1.getReviewEffort() - user2.getReviewEffort();
					else
						return 1;
				} else if (inviteExcluded.contains(user2)) {
					return -1;
				} else {
					return user1.getReviewEffort() - user2.getReviewEffort();
				}
			}
			
		});

		List<User> picked;
		if (count <= pickList.size())
			picked = pickList.subList(0, count);
		else
			picked = pickList;

		Set<User> notified = new HashSet<>();
		for (PullRequestNotification notification: getNotifications()) {
			if (notification.getType() == PullRequestNotification.Type.REVIEW)
				notified.add(notification.getUser());
		}
		for (User each: picked) {
			boolean found = false;
			for (ReviewInvitation invitation: getReviewInvitations()) {
				if (invitation.getReviewer().equals(each)) {
					invitation.setExcluded(false);
					found = true;
				}
			}
			if (!found) {
				ReviewInvitation invitation = new ReviewInvitation();
				invitation.setRequest(this);
				invitation.setReviewer(each);
				getReviewInvitations().add(invitation);
			}
			
			if (!notified.contains(each)) {
				PullRequestNotification notification = new PullRequestNotification();
				notification.setRequest(this);
				notification.setUser(each);
				notification.setType(PullRequestNotification.Type.REVIEW);
				notification.setDate(new Date());
				getNotifications().add(notification);
			}
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
			for (Commit commit: repo.git().log(getTarget().getHeadCommitHash(), getLatestUpdate().getHeadCommitHash(), null, 0, 0))
				pendingCommits.add(commit.getHash());
		}
		return pendingCommits;
	}

	public ChangeComments getChangeComments(Change change) {
		ChangeKey key = new ChangeKey(change.getOldRev(), change.getNewRev(), change.getPath());
		ChangeComments comments = commentsCache.get(key);
		if (comments == null) {
			comments = new ChangeComments(this, change);
			commentsCache.put(key, comments);
		}
		return comments;
	}
	
	public List<String> getCommentables() {
		List<String> commentables = new ArrayList<>();
		commentables.add(getBaseCommitHash());
		for (PullRequestUpdate update: getSortedUpdates())
			commentables.add(update.getHeadCommitHash());
		return commentables;
	}
	
	/**
	 * Merged commits represent commits already merged to target branch since base commit.
	 * 
	 * @return
	 * 			commits already merged to target branch since base commit
	 */
	public Collection<Commit> getMergedCommits() {
		if (mergedCommits == null) {
			mergedCommits = new HashSet<>();

			Branch target = getTarget();
			Repository repo = target.getRepository();
			for (Commit commit: repo.git().log(getBaseCommitHash(), target.getHeadCommitHash(), null, 0, 0))
				mergedCommits.add(commit);
		}
		return mergedCommits;
	}
	
	private Collection<PullRequestCommentReply> getCommentReplies() {
		if (commentReplies == null) {
			EntityCriteria<PullRequestCommentReply> criteria = EntityCriteria.of(PullRequestCommentReply.class);
			criteria.createCriteria("comment").add(Restrictions.eq("request", this));
			commentReplies = GitPlex.getInstance(Dao.class).query(criteria);
		}
		return commentReplies;
	}
	
	public Collection<PullRequestCommentReply> getCommentReplies(PullRequestComment comment) {
		List<PullRequestCommentReply> replies = new ArrayList<>();
		for (PullRequestCommentReply reply: getCommentReplies()) {
			if (reply.getComment().equals(comment))
				replies.add(reply);
		}
		return replies;
	}
	
	private static class ChangeKey extends Triple<String, String, String> {

		public ChangeKey(String oldCommit, String newCommit, String file) {
			super(oldCommit, newCommit, file);
		}
		
	}
	
}
