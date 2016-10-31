package com.gitplex.core.entity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gitplex.core.manager.impl.DefaultCodeCommentRelationManager;
import com.google.common.base.Preconditions;
import com.gitplex.commons.git.GitUtils;
import com.gitplex.commons.hibernate.AbstractEntity;

@Entity
@Table(indexes={@Index(columnList="g_request_id"), @Index(columnList="uuid"), @Index(columnList="date")})
public class PullRequestUpdate extends AbstractEntity {

	private static final long serialVersionUID = 1L;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(nullable=false)
	private PullRequest request;
	
	@Column(nullable=false)
	private String headCommitHash;
	
	@Column(nullable=false)
	private String mergeCommitHash;

	@Column(nullable=false)
	private Date date = new Date();
	
	@OneToMany(mappedBy="update", cascade=CascadeType.REMOVE)
	private Collection<PullRequestReview> reviews = new ArrayList<PullRequestReview>();
	
	@Column(nullable=false)
	private String uuid = UUID.randomUUID().toString();
	
	private transient List<RevCommit> commits;
	
	private transient Collection<String> changedFiles;

	public PullRequest getRequest() {
		return request;
	}

	public void setRequest(PullRequest request) {
		this.request = request;
	}

	public String getHeadCommitHash() {
		return headCommitHash;
	}
	
	public void setHeadCommitHash(String headCommitHash) {
		this.headCommitHash = headCommitHash;
	}
	
	public String getMergeCommitHash() {
		return mergeCommitHash;
	}

	public void setMergeCommitHash(String mergeCommitHash) {
		this.mergeCommitHash = mergeCommitHash;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

    public Collection<PullRequestReview> getReviews() {
		return getRequest().getReviews(this);
	}

	@JsonProperty
	public String getHeadRef() {
		Preconditions.checkNotNull(getId());
		return Depot.REFS_GITPLEX + "updates/" + getId();
	}
	
	/**
	 * List reviews against this update and all subsequent updates.
	 * <p>
	 * @return
	 * 			list of found reviews, ordered by associated updates reversely
	 */
	public List<PullRequestReview> listReviewsOnwards() {
		List<PullRequestReview> reviews = new ArrayList<PullRequestReview>();
		Set<Account> excludedReviewers = new HashSet<>();
		for (PullRequestReviewInvitation invitation: getRequest().getReviewInvitations()) {
			if (invitation.getStatus() == PullRequestReviewInvitation.Status.EXCLUDED)
				excludedReviewers.add(invitation.getUser());
		}
		for (PullRequestUpdate update: getRequest().getEffectiveUpdates()) {
			for (PullRequestReview review: update.getReviews()) {
				if (!excludedReviewers.contains(review.getUser()))
					reviews.add(review);
			}
			if (update.equals(this))
				break;
		}
		
		return reviews;
	}

	public void deleteRefs() {
		GitUtils.deleteRef(getRequest().getTargetDepot().updateRef(getHeadRef()));
	}	
	
	/**
	 * Get changed files of this update since previous update. This calculation 
	 * excludes changes introduced by commits from target branch (this will 
	 * happen if some commits were merged from target branch to source branch)
	 */
	public Collection<String> getChangedFiles() {
		if (changedFiles == null) {
			changedFiles = new HashSet<>();
			
			Repository repository = getRequest().getWorkDepot().getRepository();
			try (	RevWalk revWalk = new RevWalk(repository);
					TreeWalk treeWalk = new TreeWalk(repository)) {
				RevCommit mergeCommit = revWalk.parseCommit(ObjectId.fromString(getMergeCommitHash()));
				RevCommit baseCommit = revWalk.parseCommit(ObjectId.fromString(getBaseCommitHash()));
				RevCommit headCommit = revWalk.parseCommit(ObjectId.fromString(getHeadCommitHash()));
				revWalk.markStart(mergeCommit);
				revWalk.markStart(baseCommit);
				revWalk.setRevFilter(RevFilter.MERGE_BASE);
				RevCommit mergeBase = Preconditions.checkNotNull(revWalk.next());
				treeWalk.setRecursive(true);
				if (mergeBase.equals(baseCommit)) {
					treeWalk.addTree(mergeCommit.getTree());
					treeWalk.addTree(headCommit.getTree());
					treeWalk.setFilter(TreeFilter.ANY_DIFF);
					while (treeWalk.next())
						changedFiles.add(treeWalk.getPathString());
				} else if (mergeBase.equals(mergeCommit)) {
					treeWalk.addTree(baseCommit.getTree());
					treeWalk.addTree(headCommit.getTree());
					treeWalk.setFilter(TreeFilter.ANY_DIFF);
					while (treeWalk.next())
						changedFiles.add(treeWalk.getPathString());
				} else {
					treeWalk.addTree(headCommit.getTree());
					treeWalk.addTree(baseCommit.getTree());
					treeWalk.addTree(mergeCommit.getTree());
					treeWalk.setFilter(new TreeFilter() {

						@Override
						public boolean include(TreeWalk walker)
								throws MissingObjectException, IncorrectObjectTypeException, IOException {
							int m0 = walker.getRawMode(0);
							
							// only include the path if the file is modified in head commit 
							// compared to base commit and that modification is not introduced
							// by commit from target branch
							return (walker.getRawMode(1) != m0 || !walker.idEqual(1, 0)) 
									&& (walker.getRawMode(2) != m0 || !walker.idEqual(2, 0));
						}

						@Override
						public boolean shouldBeRecursive() {
							return false;
						}

						@Override
						public TreeFilter clone() {
							return this;
						}
						
					});
					while (treeWalk.next()) {
						changedFiles.add(treeWalk.getPathString());
					}
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return changedFiles;
	}
	
	public String getBaseCommitHash() {
		PullRequest request = getRequest();

		int index = request.getSortedUpdates().indexOf(this);
		if (index > 0)
			return request.getSortedUpdates().get(index-1).getHeadCommitHash();
		else
			return request.getBaseCommitHash();
	}
	
	/**
	 * Base commit represents head commit of last update, or base commit of the request 
	 * for the first update. Base commit is used to calculate commits belonging to 
	 * current update.
	 * 
	 * @return
	 * 			base commit of this update
	 */
	public RevCommit getBaseCommit() {
		PullRequest request = getRequest();

		int index = request.getSortedUpdates().indexOf(this);
		if (index > 0)
			return request.getSortedUpdates().get(index-1).getHeadCommit();
		else
			return request.getBaseCommit();
	}
	
	public String getUUID() {
		return uuid;
	}

	public void setUUID(String uuid) {
		this.uuid = uuid;
	}

	/**
	 * Get commits belonging to this update, reversely ordered by commit traversing. The list of commit will remain 
	 * unchanged even if tip of target branch changes. This assumption is key to our caching of code comment to
	 * to pull request relations. Check {@link DefaultCodeCommentRelationManager#findCodeComments(PullRequest)} for
	 * details
	 * 
	 * @return
	 * 			commits belonging to this update ordered by commit id
	 */
	public List<RevCommit> getCommits() {
		if (commits == null) {
			commits = new ArrayList<>();
			
			try (RevWalk revWalk = new RevWalk(getRequest().getWorkDepot().getRepository())) {
				revWalk.markStart(revWalk.parseCommit(ObjectId.fromString(getHeadCommitHash())));
				revWalk.markUninteresting(revWalk.parseCommit(ObjectId.fromString(getBaseCommitHash())));
				
				/*
				 * Instead of excluding commits reachable from target branch, we exclude commits reachable
				 * from the merge commit to achieve two purposes:
				 * 1. commits merged back into target branch after this update can still be included in this
				 * update
				 * 2. commits of this update will remain unchanged even if tip of the target branch changes     
				 */
				revWalk.markUninteresting(revWalk.parseCommit(ObjectId.fromString(getMergeCommitHash())));
				
				revWalk.forEach(c->commits.add(c));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			Collections.reverse(commits);
		}
		return commits;
	}
	
	public RevCommit getHeadCommit() {
		return request.getWorkDepot().getRevCommit(ObjectId.fromString(getHeadCommitHash()));
	}
	
}
