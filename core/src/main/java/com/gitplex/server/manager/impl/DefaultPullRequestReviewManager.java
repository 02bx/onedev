package com.gitplex.server.manager.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gitplex.server.manager.AccountManager;
import com.gitplex.server.manager.PullRequestManager;
import com.gitplex.server.manager.PullRequestReviewManager;
import com.gitplex.server.manager.PullRequestStatusChangeManager;
import com.gitplex.server.model.Account;
import com.gitplex.server.model.Depot;
import com.gitplex.server.model.PullRequest;
import com.gitplex.server.model.PullRequestReview;
import com.gitplex.server.model.PullRequestStatusChange;
import com.gitplex.server.model.PullRequestStatusChange.Type;
import com.gitplex.server.model.PullRequestUpdate;
import com.gitplex.server.model.ReviewInvitation;
import com.gitplex.server.model.Team;
import com.gitplex.server.model.support.BranchProtection;
import com.gitplex.server.model.support.FileProtection;
import com.gitplex.server.persistence.annotation.Sessional;
import com.gitplex.server.persistence.annotation.Transactional;
import com.gitplex.server.persistence.dao.AbstractEntityManager;
import com.gitplex.server.persistence.dao.Dao;
import com.gitplex.server.persistence.dao.EntityCriteria;
import com.gitplex.server.security.ObjectPermission;
import com.gitplex.server.security.SecurityUtils;
import com.gitplex.server.security.privilege.DepotPrivilege;
import com.gitplex.server.util.ReviewStatus;
import com.gitplex.server.util.reviewappointment.ReviewAppointment;
import com.google.common.base.Preconditions;

@Singleton
public class DefaultPullRequestReviewManager extends AbstractEntityManager<PullRequestReview> 
		implements PullRequestReviewManager {

	private static final Logger logger = LoggerFactory.getLogger(DefaultPullRequestReviewManager.class);
	
	private final AccountManager accountManager;

	private final PullRequestManager pullRequestManager;
	
	private final PullRequestStatusChangeManager pullRequestStatusChangeManager;
	
	@Inject
	public DefaultPullRequestReviewManager(Dao dao, AccountManager accountManager, PullRequestManager pullRequestManager, 
			PullRequestStatusChangeManager pullRequestStatusChangeManager) {
		super(dao);
		
		this.accountManager = accountManager;
		this.pullRequestManager = pullRequestManager;
		this.pullRequestStatusChangeManager = pullRequestStatusChangeManager;
	}

	@Transactional
	@Override
	public ReviewStatus checkRequest(PullRequest request) {
		return new ReviewStatusImpl(request);
	}

	@Transactional
	@Override
	public void save(PullRequestReview review) {
		super.save(review);	

		PullRequestStatusChange statusChange = new PullRequestStatusChange();
		statusChange.setDate(new Date());
		statusChange.setNote(review.getNote());
		statusChange.setRequest(review.getRequest());
		if (review.isApproved()) {
			statusChange.setType(Type.APPROVED);
		} else {
			statusChange.setType(Type.DISAPPROVED);
		}
		statusChange.setUser(review.getUser());
		pullRequestStatusChangeManager.save(statusChange);
		
		review.getRequest().setLastEvent(statusChange);
		pullRequestManager.save(review.getRequest());
	}

	@Sessional
	@Override
	public List<PullRequestReview> findAll(PullRequest request) {
		EntityCriteria<PullRequestReview> criteria = EntityCriteria.of(PullRequestReview.class);
		criteria.createCriteria("update").add(Restrictions.eq("request", request));
		criteria.addOrder(Order.asc("date"));
		return findAll(criteria);
	}

	@Transactional
	@Override
	public void delete(Account user, PullRequest request) {
		for (Iterator<PullRequestReview> it = request.getReviews().iterator(); it.hasNext();) {
			PullRequestReview review = it.next();
			if (review.getUser().equals(user)) {
				delete(review);
				it.remove();
			}
		}
		
		PullRequestStatusChange statusChange = new PullRequestStatusChange();
		statusChange.setDate(new Date());
		statusChange.setRequest(request);
		statusChange.setType(Type.WITHDRAWED_REVIEW);
		statusChange.setNote("Review of user '" + user.getDisplayName() + "' is withdrawed");
		statusChange.setUser(accountManager.getCurrent());
		pullRequestStatusChangeManager.save(statusChange);
		
		request.setLastEvent(statusChange);
		pullRequestManager.save(request);
	}

	@Override
	public boolean canModify(Account user, Depot depot, String branch, String file) {
		BranchProtection branchProtection = depot.getBranchProtection(branch);
		if (branchProtection != null) {
			if (branchProtection.getReviewAppointment(depot) != null 
					&& !branchProtection.getReviewAppointment(depot).matches(user)) {
				return false;
			}
			FileProtection fileProtection = branchProtection.getFileProtection(file);
			if (fileProtection != null && !fileProtection.getReviewAppointment(depot).matches(user))
				return false;
		}			
		return true;
	}

	private Set<String> getChangedFiles(Depot depot, ObjectId oldObjectId, ObjectId newObjectId) {
		Set<String> changedFiles = new HashSet<>();
		try (TreeWalk treeWalk = new TreeWalk(depot.getRepository())) {
			treeWalk.setFilter(TreeFilter.ANY_DIFF);
			treeWalk.setRecursive(true);
			RevCommit oldCommit = depot.getRevCommit(oldObjectId);
			RevCommit newCommit = depot.getRevCommit(newObjectId);
			treeWalk.addTree(oldCommit.getTree());
			treeWalk.addTree(newCommit.getTree());
			while (treeWalk.next()) {
				changedFiles.add(treeWalk.getPathString());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return changedFiles;
	}
	
	@Override
	public boolean canPush(Account user, Depot depot, String branch, ObjectId oldObjectId, ObjectId newObjectId) {
		BranchProtection branchProtection = depot.getBranchProtection(branch);
		if (branchProtection != null) {
			if (branchProtection.getReviewAppointment(depot) != null 
					&& !branchProtection.getReviewAppointment(depot).matches(user)) {
				return false;
			}
			
			for (String changedFile: getChangedFiles(depot, oldObjectId, newObjectId)) {
				FileProtection fileProtection = branchProtection.getFileProtection(changedFile);
				if (fileProtection != null && !fileProtection.getReviewAppointment(depot).matches(user))
					return false;
			}
		}
		return true;
	}

	private static class ReviewStatusImpl implements ReviewStatus {

		private final PullRequest request;
		
		private final List<Account> awaitingReviewers = new ArrayList<>();
		
		private final Map<Account, PullRequestReview> effectiveReviews = new HashMap<>();
		
		@Override
		public List<Account> getAwaitingReviewers() {
			return awaitingReviewers;
		}

		@Override
		public Map<Account, PullRequestReview> getEffectiveReviews() {
			return effectiveReviews;
		}

		@Nullable
		private ReviewInvitation getInvitation(Account user) {
			for (ReviewInvitation invitation: request.getReviewInvitations()) {
				if (invitation.getUser().equals(user))
					return invitation;
			}
			return null;
		}
		
		public ReviewStatusImpl(PullRequest request) {
			this.request = request;
			
			for (ReviewInvitation invitation: request.getReviewInvitations()) {
				if (!invitation.getUser().equals(request.getSubmitter()) 
						&& invitation.getType() == ReviewInvitation.Type.MANUAL) {
					PullRequestReview effectiveReview = invitation.getUser().getReviewAfter(request.getLatestUpdate());
					if (effectiveReview == null) 
						awaitingReviewers.add(invitation.getUser());
					else
						effectiveReviews.put(effectiveReview.getUser(), effectiveReview);
				}
			}

			BranchProtection branchProtection = request.getTargetDepot().getBranchProtection(request.getTargetBranch());
			if (branchProtection != null) {
				ReviewAppointment appointment = branchProtection.getReviewAppointment(request.getTargetDepot());
				if (appointment != null) 
					checkReviews(appointment, request.getLatestUpdate());

				Set<FileProtection> checkedFileProtections = new HashSet<>();
				for (int i=request.getSortedUpdates().size()-1; i>=0; i--) {
					if (checkedFileProtections.containsAll(branchProtection.getFileProtections()))
						break;
					PullRequestUpdate update = request.getSortedUpdates().get(i);
					for (String file: update.getChangedFiles()) {
						FileProtection fileProtection = branchProtection.getFileProtection(file);
						if (fileProtection != null && !checkedFileProtections.contains(fileProtection)) {
							checkedFileProtections.add(fileProtection);
							checkReviews(fileProtection.getReviewAppointment(request.getTargetDepot()), update);
						}
					}
				}
			}
			
			ObjectPermission writePermission = ObjectPermission.ofDepotWrite(request.getTargetDepot());
			if (request.getSubmitter() == null || !request.getSubmitter().asSubject().isPermitted(writePermission)) {
				Collection<Account> writers = SecurityUtils.findUsersCan(request.getTargetDepot(), 
						DepotPrivilege.WRITE);
				checkReviews(writers, 1, request.getLatestUpdate(), true);
			}
			
			for (Iterator<Map.Entry<Account, PullRequestReview>> it = effectiveReviews.entrySet().iterator(); it.hasNext();) {
				PullRequestReview review = it.next().getValue();
				if (review.isCheckMerged()) {
					if (request.getMergePreview() == null 
							|| !review.getCommit().equals(request.getMergePreview().getMerged())) {
						if (!awaitingReviewers.contains(review.getUser()))
							awaitingReviewers.add(review.getUser());
						it.remove();
					}
				}
			}
			
		}
		
		private void checkReviews(ReviewAppointment appointment, PullRequestUpdate update) {
			for (Account user: appointment.getUsers()) {
				if (!user.equals(request.getSubmitter()) && !awaitingReviewers.contains(user)) {
					PullRequestReview effectiveReview = user.getReviewAfter(update);
					if (effectiveReview == null) {
						awaitingReviewers.add(user);
						recordReviewer(user);
					} else {
						effectiveReviews.put(effectiveReview.getUser(), effectiveReview);
					}
				}
			}
			
			for (Map.Entry<Team, Integer> entry: appointment.getTeams().entrySet()) {
				Team team = entry.getKey();
				int requiredCount = entry.getValue();
				checkReviews(team.getMembers(), requiredCount, update, false);
			}
		}
		
		private void checkReviews(Collection<Account> users, int requiredCount, PullRequestUpdate update, 
				boolean excludeAutoReviews) {
			if (requiredCount == 0)
				requiredCount = users.size();
			
			int effectiveCount = 0;
			Set<Account> potentialReviewers = new HashSet<>();
			for (Account user: users) {
				if (user.equals(request.getSubmitter())) {
					effectiveCount++;
				} else if (awaitingReviewers.contains(user)) {
					requiredCount--;
				} else {
					PullRequestReview effectiveReview = user.getReviewAfter(update);
					if (effectiveReview != null) {
						if (!excludeAutoReviews || !effectiveReview.isAutoCheck()) {						
							effectiveCount++;
						} else {
							logger.warn("Auto review made by {} is ignored while checking reviews of pull request #{}", 
									user.getDisplayName(), update.getRequest().getNumber());
						}
					} else {
						potentialReviewers.add(user);
					}
				}
				if (effectiveCount >= requiredCount)
					break;
			}

			int missingCount = requiredCount - effectiveCount;
			
			Set<Account> candidateReviewers = new HashSet<>();
			for (Account user: potentialReviewers) {
				ReviewInvitation invitation = getInvitation(user);
				if (invitation != null && invitation.getType() != ReviewInvitation.Type.EXCLUDE)
					candidateReviewers.add(user);
			}
			if (candidateReviewers.size() < missingCount) {
				for (Account user: potentialReviewers) {
					ReviewInvitation invitation = getInvitation(user);
					if (invitation == null) {
						candidateReviewers.add(user);
						if (candidateReviewers.size() == missingCount)
							break;
					}
				}
			}
			if (candidateReviewers.size() < missingCount) {
				List<ReviewInvitation> excludedInvitations = new ArrayList<>();
				for (Account user: potentialReviewers) {
					ReviewInvitation invitation = getInvitation(user);
					if (invitation != null && invitation.getType() == ReviewInvitation.Type.EXCLUDE)
						excludedInvitations.add(invitation);
				}
				excludedInvitations.sort(Comparator.comparing(ReviewInvitation::getDate));
				
				for (ReviewInvitation invitation: excludedInvitations) {
					candidateReviewers.add(invitation.getUser());
					if (candidateReviewers.size() == missingCount)
						break;
				}
			}
			if (candidateReviewers.size() < missingCount) {
				String errorMessage = String.format("Impossible to provide required number of reviewers "
						+ "(candidates: %s, required number of reviewers: %d, pull request: #%d)", 
						candidateReviewers, missingCount, update.getRequest().getNumber());
				throw new RuntimeException(errorMessage);
			}

			Set<Account> selectedReviewers = selectReviewers(candidateReviewers, missingCount);
			for (Account user: selectedReviewers) {
				awaitingReviewers.add(user);
				recordReviewer(user);
			}
		}
		
		private Set<Account> selectReviewers(Set<Account> candidateReviewers, int count) {
			Preconditions.checkArgument(candidateReviewers.size() >= count);
			Set<Account> selectedReviewers = new HashSet<>();
			if (count > 0) {
				for (Account user: candidateReviewers) {
					selectedReviewers.add(user);
					if (selectedReviewers.size() == count)
						break;
				}
			}
			return selectedReviewers;
		}
		
		private void recordReviewer(Account user) {
			ReviewInvitation invitation = getInvitation(user);
			if (invitation != null) {
				invitation.setType(ReviewInvitation.Type.RULE);
			} else {
				invitation = new ReviewInvitation();
				invitation.setRequest(request);
				invitation.setUser(user);
				invitation.setType(ReviewInvitation.Type.RULE);
				request.getReviewInvitations().add(invitation);
			}
		}
		
	}	
}
