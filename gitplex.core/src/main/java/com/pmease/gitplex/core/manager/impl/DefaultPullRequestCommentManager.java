package com.pmease.gitplex.core.manager.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.base.Preconditions;
import com.pmease.commons.git.BlobIdent;
import com.pmease.commons.git.BlobText;
import com.pmease.commons.git.Change;
import com.pmease.commons.hibernate.Transactional;
import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.commons.markdown.MarkdownManager;
import com.pmease.commons.util.diff.AroundContext;
import com.pmease.commons.util.diff.DiffLine;
import com.pmease.commons.util.diff.DiffUtils;
import com.pmease.commons.util.diff.WordSplitter;
import com.pmease.gitplex.core.comment.InlineComment;
import com.pmease.gitplex.core.comment.MentionParser;
import com.pmease.gitplex.core.listeners.PullRequestListener;
import com.pmease.gitplex.core.manager.PullRequestCommentManager;
import com.pmease.gitplex.core.model.PullRequestComment;
import com.pmease.gitplex.core.model.User;

@Singleton
public class DefaultPullRequestCommentManager implements PullRequestCommentManager {

	private final Dao dao;
	
	private final Set<PullRequestListener> pullRequestListeners;
	
	private final MarkdownManager markdownManager;
	
	@Inject
	public DefaultPullRequestCommentManager(Dao dao, MarkdownManager markdownManager, 
			Set<PullRequestListener> pullRequestListeners) {
		this.dao = dao;
		this.markdownManager = markdownManager;
		this.pullRequestListeners = pullRequestListeners;
	}

	@Transactional
	@Override
	public void updateInline(PullRequestComment comment) {
		Preconditions.checkNotNull(comment.getInlineInfo());
		
		String latestCommitHash = comment.getRequest().getLatestUpdate().getHeadCommitHash();
		if (!comment.getNewCommitHash().equals(latestCommitHash)) {
			List<Change> changes = comment.getRepository().getChanges(comment.getNewCommitHash(), latestCommitHash);
			String oldCommitHash = comment.getOldCommitHash();
			if (oldCommitHash.equals(comment.getBlobIdent().revision)) {
				BlobIdent newBlobInfo = null;
				if (comment.getCompareWith().path != null) {
					for (Change change: changes) {
						if (comment.getCompareWith().path.equals(change.getOldBlobIdent().path)) {
							newBlobInfo = new BlobIdent(latestCommitHash, change.getNewBlobIdent().path, change.getNewBlobIdent().mode);
							break;
						}
					}
				} else {
					for (Change change: changes) {
						if (comment.getBlobIdent().path.equals(change.getNewBlobIdent().path)) {
							newBlobInfo = new BlobIdent(latestCommitHash, change.getNewBlobIdent().path, change.getNewBlobIdent().mode);
							break;
						}
					}
				}
				if (newBlobInfo != null) {
					BlobText oldText = comment.getRepository().getBlobText(comment.getBlobIdent());
					Preconditions.checkNotNull(oldText);
					List<String> newLines;
					if (newBlobInfo.path != null) {
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
					comment.getCompareWith().revision = latestCommitHash;
				}
			} else {
				BlobIdent newBlobInfo = null;
				for (Change change: changes) {
					if (comment.getBlobIdent().path.equals(change.getOldBlobIdent().path)) {
						newBlobInfo = new BlobIdent(latestCommitHash, change.getNewBlobIdent().path, change.getNewBlobIdent().mode);
						break;
					}
				}
				if (newBlobInfo != null) {
					BlobText oldText = comment.getRepository().getBlobText(comment.getBlobIdent());
					Preconditions.checkNotNull(oldText);
					List<String> newLines;
					if (newBlobInfo.path != null) {
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
							comment.setBlobIdent(newBlobInfo);
							comment.setLine(newLineNo);
							
							List<String> oldLines;
							if (comment.getCompareWith().path != null) {
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
					comment.getBlobIdent().revision = latestCommitHash;
				}
			}
			dao.persist(comment);
		}
	}
	
	@Transactional
	@Override
	public void save(PullRequestComment comment, boolean notify) {
		boolean isNew = comment.isNew();
		dao.persist(comment);
		
		if (isNew) {
			String rawHtml = markdownManager.parse(comment.getContent());
			Collection<User> mentions = new MentionParser().parseMentions(rawHtml);
			for (User user: mentions) {
				for (PullRequestListener listener: pullRequestListeners)
					listener.onMentioned(comment, user);
			}
		}
		
		if (notify) {
			for (PullRequestListener listener: pullRequestListeners)
				listener.onCommented(comment);
		}
	}

}
