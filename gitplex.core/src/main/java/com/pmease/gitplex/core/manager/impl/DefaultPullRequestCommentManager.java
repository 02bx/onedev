package com.pmease.gitplex.core.manager.impl;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.base.Preconditions;
import com.pmease.commons.git.BlobInfo;
import com.pmease.commons.git.BlobText;
import com.pmease.commons.git.Change;
import com.pmease.commons.hibernate.Transactional;
import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.commons.util.diff.AroundContext;
import com.pmease.commons.util.diff.DiffLine;
import com.pmease.commons.util.diff.DiffUtils;
import com.pmease.commons.util.diff.WordSplitter;
import com.pmease.gitplex.core.comment.InlineComment;
import com.pmease.gitplex.core.extensionpoint.PullRequestListener;
import com.pmease.gitplex.core.extensionpoint.PullRequestListeners;
import com.pmease.gitplex.core.manager.PullRequestCommentManager;
import com.pmease.gitplex.core.model.PullRequest;
import com.pmease.gitplex.core.model.PullRequestComment;

@Singleton
public class DefaultPullRequestCommentManager implements PullRequestCommentManager {

	private final Dao dao;
	
	private final PullRequestListeners pullRequestListeners;
	
	@Inject
	public DefaultPullRequestCommentManager(Dao dao, PullRequestListeners pullRequestListeners) {
		this.dao = dao;
		this.pullRequestListeners = pullRequestListeners;
	}

	@Override
	public void updateInline(PullRequestComment comment) {
		Preconditions.checkNotNull(comment.getInlineInfo());
		
		String latestCommitHash = comment.getRequest().getLatestUpdate().getHeadCommitHash();
		if (!comment.getNewCommitHash().equals(latestCommitHash)) {
			List<Change> changes = comment.getRepository().getChanges(comment.getNewCommitHash(), latestCommitHash);
			String oldCommitHash = comment.getOldCommitHash();
			if (oldCommitHash.equals(comment.getBlobInfo().getRevision())) {
				BlobInfo newBlobInfo = null;
				if (comment.getCompareWith().getPath() != null) {
					for (Change change: changes) {
						if (comment.getCompareWith().getPath().equals(change.getOldPath())) {
							newBlobInfo = new BlobInfo(latestCommitHash, change.getNewPath(), change.getNewMode());
							break;
						}
					}
				} else {
					for (Change change: changes) {
						if (comment.getBlobInfo().getPath().equals(change.getNewPath())) {
							newBlobInfo = new BlobInfo(latestCommitHash, change.getNewPath(), change.getNewMode());
							break;
						}
					}
				}
				if (newBlobInfo != null) {
					BlobText oldText = comment.getRepository().getBlobText(comment.getBlobInfo());
					Preconditions.checkNotNull(oldText);
					List<String> newLines;
					if (newBlobInfo.getPath() != null) {
						BlobText newText = comment.getRepository().getBlobText(newBlobInfo);
						if (newText != null)
							newLines = newText.getLines();
						else
							newLines = null;
					} else {
						newLines = new ArrayList<>();
					}
					if (newLines != null) {
						List<DiffLine> diffs = DiffUtils.diff(oldText.getLines(), newLines, null);					
						AroundContext context = DiffUtils.around(
								diffs, comment.getLine(), -1, InlineComment.CONTEXT_SIZE);
						context.setDiffs(DiffUtils.diffTokens(context.getDiffs(), new WordSplitter()));
						comment.setContext(context);
					} else {
						comment.setContext(null);
					}
					comment.setCompareWith(newBlobInfo);
				} else {
					comment.getCompareWith().setRevision(latestCommitHash);
				}
			} else {
				BlobInfo newBlobInfo = null;
				for (Change change: changes) {
					if (comment.getBlobInfo().getPath().equals(change.getOldPath())) {
						newBlobInfo = new BlobInfo(latestCommitHash, change.getNewPath(), change.getNewMode());
						break;
					}
				}
				if (newBlobInfo != null) {
					BlobText oldText = comment.getRepository().getBlobText(comment.getBlobInfo());
					Preconditions.checkNotNull(oldText);
					List<String> newLines;
					if (newBlobInfo.getPath() != null) {
						BlobText newText = comment.getRepository().getBlobText(newBlobInfo);
						if (newText != null)
							newLines = newText.getLines();
						else
							newLines = null;
					} else {
						newLines = new ArrayList<>();
					}
					if (newLines != null) {
						List<DiffLine> diffs = DiffUtils.diff(oldText.getLines(), newLines, null);
						Integer newLineNo = DiffUtils.mapLines(diffs).get(comment.getLine());
						if (newLineNo != null) {
							comment.setBlobInfo(newBlobInfo);
							comment.setLine(newLineNo);
							
							List<String> oldLines;
							if (comment.getCompareWith().getPath() != null) {
								oldText = comment.getRepository().getBlobText(comment.getCompareWith());
								if (oldText != null)
									oldLines = oldText.getLines();
								else
									oldLines = null;
							} else {
								oldLines = new ArrayList<>();
							}
							if (oldLines != null) {
								diffs = DiffUtils.diff(oldLines, newLines, null);					
								AroundContext context = DiffUtils.around(
										diffs, -1, newLineNo, InlineComment.CONTEXT_SIZE);
								context.setDiffs(DiffUtils.diffTokens(context.getDiffs(), new WordSplitter()));
								comment.setContext(context);
							} else {
								comment.setContext(null);
							}
						} else {
							comment.setCompareWith(newBlobInfo);
							
							AroundContext context = DiffUtils.around(
									diffs, comment.getLine(), -1, InlineComment.CONTEXT_SIZE);
							context.setDiffs(DiffUtils.diffTokens(context.getDiffs(), new WordSplitter()));
							comment.setContext(context);
						}
					} else {
						comment.setCompareWith(newBlobInfo);
						comment.setContext(null);
					}
				} else {
					comment.getBlobInfo().setRevision(latestCommitHash);
				}
			}
			dao.persist(comment);
		}
	}
	
	@Transactional
	@Override
	public void save(PullRequestComment comment) {
		dao.persist(comment);

		final Long requestId = comment.getRequest().getId();
		
		dao.afterCommit(new Runnable() {

			@Override
			public void run() {
				pullRequestListeners.asyncCall(requestId, new PullRequestListeners.Callback() {
					
					@Override
					protected void call(PullRequestListener listener, PullRequest request) {
						listener.onCommented(request);
					}
					
				});
			}
			
		});
	}

}
