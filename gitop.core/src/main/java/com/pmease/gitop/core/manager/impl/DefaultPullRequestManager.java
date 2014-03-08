package com.pmease.gitop.core.manager.impl;

import static com.pmease.gitop.model.PullRequest.CriterionHelper.ofOpen;
import static com.pmease.gitop.model.PullRequest.CriterionHelper.ofSource;
import static com.pmease.gitop.model.PullRequest.CriterionHelper.ofTarget;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.pmease.commons.git.Git;
import com.pmease.commons.hibernate.Sessional;
import com.pmease.commons.hibernate.Transactional;
import com.pmease.commons.hibernate.UnitOfWork;
import com.pmease.commons.hibernate.dao.AbstractGenericDao;
import com.pmease.commons.hibernate.dao.GeneralDao;
import com.pmease.commons.util.FileUtils;
import com.pmease.commons.util.LockUtils;
import com.pmease.gitop.core.manager.BranchManager;
import com.pmease.gitop.core.manager.PullRequestCommentManager;
import com.pmease.gitop.core.manager.PullRequestManager;
import com.pmease.gitop.core.manager.PullRequestUpdateManager;
import com.pmease.gitop.core.manager.VoteInvitationManager;
import com.pmease.gitop.model.Branch;
import com.pmease.gitop.model.CloseInfo;
import com.pmease.gitop.model.MergeInfo;
import com.pmease.gitop.model.PullRequest;
import com.pmease.gitop.model.PullRequest.Status;
import com.pmease.gitop.model.PullRequestComment;
import com.pmease.gitop.model.PullRequestUpdate;
import com.pmease.gitop.model.User;
import com.pmease.gitop.model.VoteInvitation;
import com.pmease.gitop.model.gatekeeper.checkresult.Approved;

@Singleton
public class DefaultPullRequestManager extends AbstractGenericDao<PullRequest> 
		implements PullRequestManager {

	private final VoteInvitationManager voteInvitationManager;
	
	private final PullRequestUpdateManager pullRequestUpdateManager;
	
	private final PullRequestCommentManager pullRequestCommentManager;
	
	private final BranchManager branchManager;
	
	private final UnitOfWork unitOfWork;
	
	private final Executor executor;
	
	@Inject
	public DefaultPullRequestManager(GeneralDao generalDao, 
			VoteInvitationManager voteInvitationManager,
			PullRequestUpdateManager pullRequestUpdateManager, 
			PullRequestCommentManager pullRequestCommentManager,
			BranchManager branchManager, 
			UnitOfWork unitOfWork, Executor executor) {
		super(generalDao);
		this.voteInvitationManager = voteInvitationManager;
		this.pullRequestUpdateManager = pullRequestUpdateManager;
		this.pullRequestCommentManager = pullRequestCommentManager;
		this.branchManager = branchManager;
		this.unitOfWork = unitOfWork;
		this.executor = executor;
	}

	@Sessional
	@Override
	public PullRequest findOpen(Branch target, Branch source) {
		Criterion[] criterions = new Criterion[]{ofOpen(), ofTarget(target), ofSource(source)};
		return find(criterions, new Order[]{Order.desc("id")});
	}

	@Transactional
	@Override
	public void delete(final PullRequest request) {
		deleteRefs(request);
		
		super.delete(request);
	}

	@Sessional
	@Override
	public void deleteRefs(PullRequest request) {
		for (PullRequestUpdate update : request.getUpdates())
			update.deleteRefs();
		
		request.deleteRefs();
	}
	
	/**
	 * Refresh internal state of this pull request. Pull request should be refreshed when:
	 * 
	 * <li> It is updated 
	 * <li> Head of target branch changes
	 * <li> Some one vote against it
	 * <li> CI system reports completion of build against relevant commits
	 */
	@Sessional
	public void refresh(final PullRequest request) {
		if (request.isNew()) {
	    	Git git = new Git(FileUtils.createTempDir());
	    	try {
		    	String targetHead = request.getTarget().getHeadCommit();
				String sourceHead = request.getSource().getHeadCommit();
				
	    		git.clone(request.getTarget().getProject().code().repoDir().getAbsolutePath(), 
	    				false, true, true, request.getTarget().getName());
	    		
	    		git.reset(null, null);
			
		    	request.getTarget().getProject().setCodeSandbox(git);

				if (git.isAncestor(sourceHead, targetHead)) {
					request.setCloseInfo(new CloseInfo(CloseInfo.Status.INTEGRATED, null));
					request.setCheckResult(new Approved("Already integrated."));
					request.setMergeInfo(new MergeInfo(targetHead, sourceHead, sourceHead, targetHead));
				} else {
					if (git.isAncestor(targetHead, sourceHead)) {
						request.setMergeInfo(new MergeInfo(targetHead, sourceHead, targetHead, sourceHead));
					} else {
						git.fetch(request.getSource().getProject().code().repoDir().getAbsolutePath(), sourceHead);
						String mergeBase = git.calcMergeBase(targetHead, sourceHead);
						if (git.merge(sourceHead, null, null, null))
							request.setMergeInfo(new MergeInfo(targetHead, sourceHead, mergeBase, git.parseRevision("HEAD", true)));
						else
							request.setMergeInfo(new MergeInfo(targetHead, sourceHead, mergeBase, null));
					}
					
					request.setCheckResult(request.getTarget().getProject().getGateKeeper().checkRequest(request));
				}
	    	} finally {
	    		request.getTarget().getProject().setCodeSandbox(null);
	    		FileUtils.deleteDir(git.repoDir());
	    	}
		} else {
			LockUtils.call(request.getLockName(), new Callable<Void>() {

				@Override
				public Void call() throws Exception {
					Git git = request.getTarget().getProject().code();
					String branchHead = request.getTarget().getHeadCommit();
					String requestHead = request.getLatestUpdate().getHeadCommit();
					
					String mergeRef = request.getMergeRef();
					
					if (git.isAncestor(requestHead, branchHead)) {
						request.setCloseInfo(new CloseInfo(CloseInfo.Status.INTEGRATED, null));
						request.setCheckResult(new Approved("Already integrated."));
						request.setMergeInfo(new MergeInfo(branchHead, requestHead, requestHead, branchHead));
					} else {
						// Update head ref so that it can be pulled by build system
						git.updateRef(request.getHeadRef(), requestHead, null, null);
						
						if (git.isAncestor(branchHead, requestHead)) {
							request.setMergeInfo(new MergeInfo(branchHead, requestHead, branchHead, requestHead));
							git.updateRef(mergeRef, requestHead, null, null);
						} else {
							if (request.getMergeInfo() != null 
									&& (!request.getMergeInfo().getBranchHead().equals(branchHead) 
											|| !request.getMergeInfo().getRequestHead().equals(requestHead))) {
								 // Commits for merging have been changed since last merge, we have to
								 // re-merge 
								request.setMergeInfo(null);
							}
							if (request.getMergeInfo() != null && request.getMergeInfo().getMergeHead() != null 
									&& !request.getMergeInfo().getMergeHead().equals(git.parseRevision(mergeRef, false))) {
								 // Commits for merging have not been changed since last merge, but recorded 
								 // merge is incorrect in repository, so we have to re-merge 
								request.setMergeInfo(null);
							}
							if (request.getMergeInfo() == null) {
								String mergeBase = git.calcMergeBase(branchHead, requestHead);
								
								File tempDir = FileUtils.createTempDir();
								try {
									Git tempGit = new Git(tempDir);
									
									// Branch name here is not significant, we just use an existing branch
									// in cloned repository to hold mergeBase, so that we can merge with 
									// previousUpdate 
									String branchName = request.getTarget().getName();
									tempGit.clone(git.repoDir().getAbsolutePath(), false, true, true, branchName);
									tempGit.updateRef("HEAD", requestHead, null, null);
									tempGit.reset(null, null);
									
									if (tempGit.merge(branchHead, null, null, null)) {
										git.fetch(tempGit.repoDir().getAbsolutePath(), "+HEAD:" + mergeRef);
										request.setMergeInfo(new MergeInfo(branchHead, requestHead, 
												mergeBase, git.parseRevision(mergeRef, true)));
									} else {
										request.setMergeInfo(new MergeInfo(branchHead, requestHead, mergeBase, null));
									}
								} finally {
									FileUtils.deleteDir(tempDir);
								}
							}
						}
						
						request.setCheckResult(request.getTarget().getProject().getGateKeeper().checkRequest(request));
				
						for (VoteInvitation invitation : request.getVoteInvitations()) {
							if (!request.getCheckResult().canVote(invitation.getVoter(), request))
								voteInvitationManager.delete(invitation);
						}
					}
			
					save(request);
					
					if (request.isAutoMerge())
						merge(request, null, null);
					
					return null;
				}
				
			});
		}
	}

	@Transactional
	public void discard(final PullRequest request, final User user, final String comment) {
		LockUtils.call(request.getLockName(), new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				request.setCloseInfo(new CloseInfo(CloseInfo.Status.DISCARDED, user));
				save(request);
				
				if (comment != null) {
					PullRequestComment requestComment = new PullRequestComment();
					requestComment.setRequest(request);
					requestComment.setUser(user);
					requestComment.setDate(new Date());
					requestComment.setContent(comment);
					
					pullRequestCommentManager.save(requestComment);
				}
				
				return null;
			}
			
		});
	}
	
	/**
	 * Merge specified request if possible.
	 * 
	 * @param request
	 * 			request to be merged
	 * @return
	 * 			<tt>true</tt> if successful, <tt>false</tt> otherwise. Reason of unsuccessful
	 * 			merge can be:
	 * 			<li> request is not in PENDING_MERGE status.
	 * 			<li> branch ref has just been updated in some other threads and this thread 
	 * 				is unable to lock the reference.
	 */
	@Transactional
	public boolean merge(final PullRequest request, final User user, 
			final String comment) {
		return LockUtils.call(request.getLockName(), new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				if (request.getStatus() == Status.PENDING_INTEGRATE) {
					Git git = request.getTarget().getProject().code();
					if (git.updateRef(request.getTarget().getHeadRef(), 
							request.getMergeInfo().getMergeHead(), 
							request.getMergeInfo().getBranchHead(), 
							comment!=null?comment:"merge pull request")) {
						request.setCloseInfo(new CloseInfo(CloseInfo.Status.INTEGRATED, user));
						save(request);

						final Long branchId = request.getTarget().getId();
						executor.execute(new Runnable() {

							@Override
							public void run() {
								unitOfWork.call(new Callable<Void>() {

									@Override
									public Void call() throws Exception {
										Branch branch = branchManager.load(branchId);
										branchManager.onBranchRefUpdate(branch);
										return null;
									}
									
								});
							}
							
						});
						return true;
					}
					
					if (user != null && comment != null) {
						PullRequestComment requestComment = new PullRequestComment();
						requestComment.setRequest(request);
						requestComment.setUser(user);
						requestComment.setDate(new Date());
						requestComment.setContent(comment);
						
						pullRequestCommentManager.save(requestComment);
					}
					
				}
				return false;
			}
			
		});
	}

	@Sessional
	@Override
	public List<PullRequest> findByCommit(String commit) {
		return query(Restrictions.or(
				Restrictions.eq("mergeInfo.requestHead", commit), 
				Restrictions.eq("mergeInfo.merged", commit)));
	}

	@Transactional
	@Override
	public PullRequest create(Branch target, Branch source, User submitter, String title, 
			@Nullable String description, boolean autoMerge) {
		PullRequest request = new PullRequest();
		request.setAutoMerge(autoMerge);
		request.setSource(source);
		request.setTarget(target);
		request.setTitle(title);
		request.setDescription(description);
		request.setSubmittedBy(submitter);
		save(request);
		
		pullRequestUpdateManager.update(request);
		
		refresh(request);
		
		return request;
	}

}
