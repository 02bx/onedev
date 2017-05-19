package com.gitplex.server.model;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import com.fasterxml.jackson.annotation.JsonView;
import com.gitplex.server.GitPlex;
import com.gitplex.server.event.pullrequest.PullRequestCodeCommentEvent;
import com.gitplex.server.git.GitUtils;
import com.gitplex.server.manager.PullRequestManager;
import com.gitplex.server.manager.ReviewManager;
import com.gitplex.server.manager.VisitInfoManager;
import com.gitplex.server.model.support.CloseInfo;
import com.gitplex.server.model.support.DepotAndBranch;
import com.gitplex.server.model.support.LastEvent;
import com.gitplex.server.model.support.MergePreview;
import com.gitplex.server.model.support.MergeStrategy;
import com.gitplex.server.security.SecurityUtils;
import com.gitplex.server.security.privilege.DepotPrivilege;
import com.gitplex.server.util.ReviewStatus;
import com.gitplex.server.util.diff.WhitespaceOption;
import com.gitplex.server.util.editable.EditableUtils;
import com.gitplex.server.util.jackson.ExternalView;
import com.google.common.base.Preconditions;

@Entity
/*
 * @DynamicUpdate annotation here along with various @OptimisticLock annotations
 * on certain fields tell Hibernate not to perform version check on those fields
 * which can be updated from background thread.
 */
@DynamicUpdate 
@Table(
		indexes={
				@Index(columnList="title"), @Index(columnList="uuid"), 
				@Index(columnList="numberStr"), @Index(columnList="noSpaceTitle"), 
				@Index(columnList="number"), @Index(columnList="g_targetDepot_id"), 
				@Index(columnList="g_sourceDepot_id"), @Index(columnList="g_submitter_id")},
		uniqueConstraints={@UniqueConstraint(columnNames={"g_targetDepot_id", "number"})})
public class PullRequest extends AbstractEntity {

	private static final long serialVersionUID = 1L;

	public static final String REFS_PREFIX = "refs/pulls/";
	
	public static final int MAX_CODE_COMMENTS = 1000;
	 
	@Embedded
	private CloseInfo closeInfo;

	@Column(nullable=false)
	private String title;
	
	@Lob
	@Column(length=65535)
	private String description;
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn
	private Account submitter;
	
	private String submitterName;
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(nullable=false)
	private Depot targetDepot;
	
	@Column(nullable=false)
	private String targetBranch;

	@ManyToOne(fetch=FetchType.LAZY)
	private Depot sourceDepot;
	
	@Column(nullable=false)
	private String sourceBranch;
	
	@Column(nullable=false)
	private String baseCommitHash;
	
	@Embedded
	private LastEvent lastEvent;
	
	@Column(nullable=true)
	private Date lastCodeCommentEventDate;

	// used for number search in markdown editor
	@Column(nullable=false)
	private String numberStr;
	
	// used for title search in markdown editor
	@Column(nullable=false)
	private String noSpaceTitle;
	
	@Version
	private long version;
	
	@OptimisticLock(excluded=true)
	@Embedded
	private MergePreview lastMergePreview;
	
	@Column(nullable=false)
	private Date submitDate = new Date();
	
	@Column(nullable=false)
	private MergeStrategy mergeStrategy;
	
	@Column(nullable=false)
	private String uuid = UUID.randomUUID().toString();
	
	private long number;

	@OneToMany(mappedBy="request", cascade=CascadeType.REMOVE)
	private Collection<PullRequestUpdate> updates = new ArrayList<>();

	@OneToMany(mappedBy="request", cascade=CascadeType.REMOVE)
	private Collection<ReviewInvitation> reviewInvitations = new ArrayList<>();
	
	@OneToMany(mappedBy="request", cascade=CascadeType.REMOVE)
	private Collection<Review> reviews = new ArrayList<>();
	
	@OneToMany(mappedBy="request", cascade=CascadeType.REMOVE)
	private Collection<CodeComment> codeComments = new ArrayList<>();
	
	@OneToMany(mappedBy="referenced", cascade=CascadeType.REMOVE)
	private Collection<PullRequestReference> referencedBy = new ArrayList<>();
	
	@OneToMany(mappedBy="referencedBy",cascade=CascadeType.REMOVE)
	private Collection<PullRequestReference> referenced = new ArrayList<>();
	
	@OneToMany(mappedBy="request", cascade=CascadeType.REMOVE)
	private Collection<PullRequestComment> comments = new ArrayList<>();

	@OneToMany(mappedBy="request", cascade=CascadeType.REMOVE)
	private Collection<PullRequestStatusChange> statusChanges = new ArrayList<>();
	
	@OneToMany(mappedBy="request", cascade=CascadeType.REMOVE)
	private Collection<PullRequestTask> tasks = new ArrayList<>();
	
	@OneToMany(mappedBy="request", cascade=CascadeType.REMOVE)
	private Collection<PullRequestWatch> watches = new ArrayList<>();
	
	private transient ReviewStatus reviewStatus;
	
	private transient Boolean mergedIntoTarget;

	private transient List<PullRequestUpdate> sortedUpdates;
	
	private transient Collection<RevCommit> pendingCommits;
	
	private transient Collection<RevCommit> mergedCommits;
	
	private transient Optional<MergePreview> mergePreviewOpt;
	
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
		noSpaceTitle = StringUtils.deleteWhitespace(title);
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
	 * 			the user submitting the pull request
	 */
	@Nullable
	public Account getSubmitter() {
		return submitter;
	}

	public void setSubmitter(Account submitter) {
		this.submitter = submitter;
	}

	@Nullable
	public String getSubmitterName() {
		return submitterName;
	}

	public void setSubmitterName(String submitterName) {
		this.submitterName = submitterName;
	}

	public Depot getTargetDepot() {
		return targetDepot;
	}
	
	public DepotAndBranch getTarget() {
		return new DepotAndBranch(getTargetDepot(), getTargetBranch());
	}
	
	public void setTarget(DepotAndBranch target) {
		setTargetDepot(target.getDepot());
		setTargetBranch(target.getBranch());
	}
	
	public void setSource(DepotAndBranch source) {
		setSourceDepot(source.getDepot());
		setSourceBranch(source.getBranch());
	}
	
	public void setTargetDepot(Depot targetDepot) {
		this.targetDepot = targetDepot;
	}

	public String getTargetBranch() {
		return targetBranch;
	}

	public void setTargetBranch(String targetBranch) {
		this.targetBranch = targetBranch;
	}

	@Nullable
	public Depot getSourceDepot() {
		return sourceDepot;
	}

	public void setSourceDepot(Depot sourceDepot) {
		this.sourceDepot = sourceDepot;
	}

	public String getSourceBranch() {
		return sourceBranch;
	}

	public void setSourceBranch(String sourceBranch) {
		this.sourceBranch = sourceBranch;
	}

	@Nullable
	public DepotAndBranch getSource() {
		Depot sourceDepot = getSourceDepot();
		if (sourceDepot != null)
			return new DepotAndBranch(sourceDepot, getSourceBranch());
		else
			return null;
	}
	
	public String getTargetRef() {
		return GitUtils.branch2ref(getTargetBranch());
	}
	
	public String getSourceRef() {
		return GitUtils.branch2ref(getSourceBranch());
	}
	
	public Depot getWorkDepot() {
		if (isNew()) 
			return getSourceDepot();
		else
			return getTargetDepot();
	}
	
	public String getBaseCommitHash() {
		return baseCommitHash;
	}

	public void setBaseCommitHash(String baseCommitHash) {
		this.baseCommitHash = baseCommitHash;
	}
	
	public String getHeadCommitHash() {
		return getLatestUpdate().getHeadCommitHash();
	}

	public RevCommit getBaseCommit() {
		return getTargetDepot().getRevCommit(ObjectId.fromString(getBaseCommitHash()));
	}
	
	public RevCommit getHeadCommit() {
		return getTargetDepot().getRevCommit(ObjectId.fromString(getHeadCommitHash()));
	}
	
	/**
	 * Get unmodifiable collection of updates of this pull request. To add update 
	 * to the pull request, call {@link this#addUpdate(PullRequestUpdate)} instead.
	 * 
	 * @return
	 * 			unmodifiable collection of updates
	 */
	public Collection<PullRequestUpdate> getUpdates() {
		return Collections.unmodifiableCollection(updates);
	}
	
	public void setUpdates(Collection<PullRequestUpdate> updates) {
		this.updates = updates;
		sortedUpdates = null;
	}

	public void addUpdate(PullRequestUpdate update) {
		updates.add(update);
		sortedUpdates = null;
	}

	public Collection<ReviewInvitation> getReviewInvitations() {
		return reviewInvitations;
	}

	public void setReviewInvitations(Collection<ReviewInvitation> reviewInvitations) {
		this.reviewInvitations = reviewInvitations;
	}

	public Collection<PullRequestReference> getReferencedBy() {
		return referencedBy;
	}

	public void setReferencedBy(Collection<PullRequestReference> referencedBy) {
		this.referencedBy = referencedBy;
	}

	public Collection<PullRequestReference> getReferenced() {
		return referenced;
	}

	public void setReferenced(Collection<PullRequestReference> referenced) {
		this.referenced = referenced;
	}

	public Collection<PullRequestComment> getComments() {
		return comments;
	}

	public void setComments(Collection<PullRequestComment> comments) {
		this.comments = comments;
	}

	public Collection<CodeComment> getCodeComments() {
		return codeComments;
	}

	public void setCodeComments(Collection<CodeComment> codeComments) {
		this.codeComments = codeComments;
	}

	public Collection<PullRequestStatusChange> getStatusChanges() {
		return statusChanges;
	}

	public void setStatusChanges(Collection<PullRequestStatusChange> statusChanges) {
		this.statusChanges = statusChanges;
	}

	public Collection<PullRequestTask> getTasks() {
		return tasks;
	}

	public void setTasks(Collection<PullRequestTask> tasks) {
		this.tasks = tasks;
	}

	public Collection<PullRequestWatch> getWatches() {
		return watches;
	}

	public void setWatches(Collection<PullRequestWatch> watches) {
		this.watches = watches;
	}

	public ReviewStatus getReviewStatus() {
		if (reviewStatus == null)
			reviewStatus = GitPlex.getInstance(ReviewManager.class).checkRequest(this);
		return reviewStatus;
	}
	
	public void clearReviewStatus() {
		reviewStatus = null;
	}

	@Nullable
	public CloseInfo getCloseInfo() {
		return closeInfo;
	}

	public void setCloseInfo(CloseInfo closeInfo) {
		this.closeInfo = closeInfo;
	}

	public boolean isOpen() {
		return closeInfo == null;
	}
	
	/**
	 * Get last merge preview of this pull request. Note that this method may return an 
	 * out dated merge preview. Refer to {@link this#getIntegrationPreview()}
	 * if you'd like to get an update-to-date merge preview
	 *  
	 * @return
	 * 			merge preview of this pull request, or <tt>null</tt> if merge 
	 * 			preview has not been calculated yet. 
	 */
	@Nullable
	public MergePreview getLastMergePreview() {
		return lastMergePreview;
	}
	
	public void setLastMergePreview(MergePreview lastIntegrationPreview) {
		this.lastMergePreview = lastIntegrationPreview;
	}

	/**
	 * Get effective merge preview of this pull request.
	 * 
	 * @return
	 * 			update to date merge preview of this pull request, or <tt>null</tt> if 
	 * 			the merge preview has not been calculated or out dated. In both cases, 
	 * 			it will trigger a re-calculation, and client should call this method later 
	 * 			to get the calculated result 
	 */
	@JsonView(ExternalView.class)
	@Nullable
	public MergePreview getMergePreview() {
		if (mergePreviewOpt == null)
			mergePreviewOpt = Optional.ofNullable(GitPlex.getInstance(PullRequestManager.class).previewMerge(this));
		return mergePreviewOpt.orElse(null);
	}
	
	/**
	 * Get list of sorted updates.
	 * 
	 * @return 
	 * 			list of sorted updates ordered by id
	 */
	public List<PullRequestUpdate> getSortedUpdates() {
		if (sortedUpdates == null) {
			Preconditions.checkState(updates.size() >= 1);
			sortedUpdates = new ArrayList<PullRequestUpdate>(updates);
			Collections.sort(sortedUpdates);
		}
		return sortedUpdates;
	}

	public MergeStrategy getMergeStrategy() {
		return mergeStrategy;
	}

	public void setMergeStrategy(MergeStrategy mergeStrategy) {
		this.mergeStrategy = mergeStrategy;
	}

	@JsonView(ExternalView.class)
	public PullRequestUpdate getLatestUpdate() {
		return getSortedUpdates().get(getSortedUpdates().size()-1);
	}
	
	@JsonView(ExternalView.class)
	public String getBaseRef() {
		Preconditions.checkNotNull(getId());
		return REFS_PREFIX + getNumber() + "/base";
	}

	@JsonView(ExternalView.class)
	public String getMergeRef() {
		Preconditions.checkNotNull(getId());
		return REFS_PREFIX + getNumber() + "/merge";
	}

	@JsonView(ExternalView.class)
	public String getHeadRef() {
		Preconditions.checkNotNull(getId());
		return REFS_PREFIX + getNumber() + "/head";
	}
	
	/**
	 * Delete refs of this pull request, without touching refs of its updates.
	 */
	public void deleteRefs() {
		GitUtils.deleteRef(getTargetDepot().updateRef(getBaseRef()));
		GitUtils.deleteRef(getTargetDepot().updateRef(getMergeRef()));
		GitUtils.deleteRef(getTargetDepot().updateRef(getHeadRef()));
	}
	
	public static class CriterionHelper {
		public static Criterion ofOpen() {
			return Restrictions.isNull("closeInfo");
		}
		
		public static Criterion ofClosed() {
			return Restrictions.isNotNull("closeInfo");
		}
		
		public static Criterion ofTarget(DepotAndBranch target) {
			return Restrictions.and(
					Restrictions.eq("targetDepot", target.getDepot()),
					Restrictions.eq("targetBranch", target.getBranch()));
		}

		public static Criterion ofTargetDepot(Depot target) {
			return Restrictions.eq("targetDepot", target);
		}
		
		public static Criterion ofSource(DepotAndBranch source) {
			return Restrictions.and(
					Restrictions.eq("sourceDepot", source.getDepot()),
					Restrictions.eq("sourceBranch", source.getBranch()));
		}
		
		public static Criterion ofSourceDepot(Depot source) {
			return Restrictions.eq("sourceDepot", source);
		}
		
		public static Criterion ofSubmitter(Account submitter) {
			return Restrictions.eq("submitter", submitter);
		}
		
	}

	public Date getSubmitDate() {
		return submitDate;
	}

	public void setSubmitDate(Date submitDate) {
		this.submitDate = submitDate;
	}

	/**
	 * Get commits pending merge.
	 * 
	 * @return
	 * 			commits pending merge
	 */
	public Collection<RevCommit> getPendingCommits() {
		if (pendingCommits == null) {
			pendingCommits = new HashSet<>();
			Depot depot = getTargetDepot();
			try (RevWalk revWalk = new RevWalk(depot.getRepository())) {
				revWalk.markStart(revWalk.parseCommit(ObjectId.fromString(getHeadCommitHash())));
				revWalk.markUninteresting(revWalk.parseCommit(getTarget().getObjectId()));
				revWalk.forEach(c->pendingCommits.add(c));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return pendingCommits;
	}

	/**
	 * Merged commits represent commits already merged to target branch since base commit.
	 * 
	 * @return
	 * 			commits already merged to target branch since base commit
	 */
	public Collection<RevCommit> getMergedCommits() {
		if (mergedCommits == null) {
			mergedCommits = new HashSet<>();
			Depot depot = getTargetDepot();
			try (RevWalk revWalk = new RevWalk(depot.getRepository())) {
				revWalk.markStart(revWalk.parseCommit(getTarget().getObjectId(false)));
				revWalk.markUninteresting(revWalk.parseCommit(ObjectId.fromString(getBaseCommitHash())));
				revWalk.forEach(c->mergedCommits.add(c));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return mergedCommits;
	}
	
	public Collection<Review> getReviews() {
		return reviews;
	}
	
	public void setReviews(Collection<Review> reviews) {
		this.reviews = reviews;
	}
	
	@Nullable
	public PullRequestWatch getWatch(Account user) {
		for (PullRequestWatch watch: getWatches()) {
			if (watch.getUser().equals(user))
				return watch;
		}
		return null;
	}

	public String getNumberStr() {
		return numberStr;
	}

	public String getNoSpaceTitle() {
		return noSpaceTitle;
	}

	public long getVersion() {
		return version;
	}

	public String getUUID() {
		return uuid;
	}

	public void setUUID(String uuid) {
		this.uuid = uuid;
	}

	public long getNumber() {
		return number;
	}

	public void setNumber(long number) {
		this.number = number;
		numberStr = String.valueOf(number);
	}

	public List<RevCommit> getCommits() {
		List<RevCommit> commits = new ArrayList<>();
		getSortedUpdates().forEach(update->commits.addAll(update.getCommits()));
		return commits;
	}
	
	public String getCommitMessage() {
		String commitMessage = getTitle() + "\n\n";
		if (getDescription() != null)
			commitMessage += getDescription() + "\n\n";
		commitMessage += "This commit is created as result of accepting pull request #" + getNumber();
		return commitMessage;
	}
	
	public LastEvent getLastEvent() {
		return lastEvent;
	}

	public void setLastEvent(LastEvent lastEvent) {
		this.lastEvent = lastEvent;
	}

	public Date getLastCodeCommentEventDate() {
		return lastCodeCommentEventDate;
	}

	public void setLastCodeCommentEventDate(Date lastCodeCommentEventDate) {
		this.lastCodeCommentEventDate = lastCodeCommentEventDate;
	}

	public boolean isVisitedAfter(Date date) {
		Account user = SecurityUtils.getAccount();
		if (user != null) {
			Date visitDate = GitPlex.getInstance(VisitInfoManager.class).getVisitDate(user, this);
			return visitDate != null && visitDate.getTime()>date.getTime();
		} else {
			return true;
		}
	}
	
	public boolean isMerged() {
		return closeInfo != null && closeInfo.getCloseStatus() == CloseInfo.Status.MERGED;
	}
	
	public boolean isDiscarded() {
		return closeInfo != null && closeInfo.getCloseStatus() == CloseInfo.Status.DISCARDED;
	}
	
	public boolean isMergeIntoTarget() {
		if (mergedIntoTarget == null) { 
			mergedIntoTarget = GitUtils.isMergedInto(getTargetDepot().getRepository(), ObjectId.fromString(getHeadCommitHash()), 
					getTarget().getObjectId());
		}
		return mergedIntoTarget;
	}
	
	public void setLastEvent(PullRequestStatusChange statusChange) {
		LastEvent lastEvent = new LastEvent();
		lastEvent.setDate(statusChange.getDate());
		lastEvent.setType(statusChange.getType().getName());
		lastEvent.setUser(statusChange.getUser());
		setLastEvent(lastEvent);
	}
	
	public void setLastEvent(PullRequestCodeCommentEvent event) {
		LastEvent lastEvent = new LastEvent();
		lastEvent.setDate(event.getDate());
		lastEvent.setType(EditableUtils.getName(event.getClass()));
		lastEvent.setUser(event.getUser());
		setLastEvent(lastEvent);
		setLastCodeCommentEventDate(event.getDate());
	}

	@Nullable
	public ObjectId getSourceHead() {
		DepotAndBranch depotAndBranch = getSource();
		if (depotAndBranch != null)
			return depotAndBranch.getObjectId(false);
		else
			return null;
	}
	
	public List<Account> getRemainingReviewers() {
		ReviewStatus checkStatus = getReviewStatus();
		Collection<Account> users = SecurityUtils.findUsersCan(getTargetDepot(), DepotPrivilege.READ);
		users.removeAll(checkStatus.getAwaitingReviewers());
		for (Review review: checkStatus.getEffectiveReviews().values())
			users.remove(review.getUser());
		users.remove(SecurityUtils.getAccount());
		List<Account> userList = new ArrayList<>(users);
		userList.sort(Comparator.comparing(Account::getName));
		return userList;
	}
	
	public static class ComparingInfo implements Serializable {
		
		private static final long serialVersionUID = 1L;

		private final String oldCommit; 
		
		private final String newCommit;
		
		private final String pathFilter;
		
		private final WhitespaceOption whitespaceOption;
		
		public ComparingInfo(String oldCommit, String newCommit, WhitespaceOption whitespaceOption, @Nullable String pathFilter) {
			this.oldCommit = oldCommit;
			this.newCommit = newCommit;
			this.whitespaceOption = whitespaceOption;
			this.pathFilter = pathFilter;
		}

		public String getOldCommit() {
			return oldCommit;
		}

		public String getNewCommit() {
			return newCommit;
		}

		public String getPathFilter() {
			return pathFilter;
		}

		public WhitespaceOption getWhitespaceOption() {
			return whitespaceOption;
		}

	}
}
