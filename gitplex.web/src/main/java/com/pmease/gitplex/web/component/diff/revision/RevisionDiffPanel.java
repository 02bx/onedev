package com.pmease.gitplex.web.component.diff.revision;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

import javax.annotation.Nullable;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.util.lang.Objects;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;

import com.pmease.commons.git.Blob;
import com.pmease.commons.git.BlobChange;
import com.pmease.commons.git.BlobIdent;
import com.pmease.commons.git.LineProcessor;
import com.pmease.commons.lang.diff.DiffUtils;
import com.pmease.commons.wicket.ajaxlistener.ConfirmLeaveListener;
import com.pmease.commons.wicket.ajaxlistener.IndicateLoadingListener;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.model.Comment;
import com.pmease.gitplex.core.model.PullRequest;
import com.pmease.gitplex.core.model.Repository;
import com.pmease.gitplex.web.Constants;
import com.pmease.gitplex.web.component.diff.blob.BlobDiffPanel;
import com.pmease.gitplex.web.component.diff.diffstat.DiffStatBar;

@SuppressWarnings("serial")
public abstract class RevisionDiffPanel extends Panel {

	private final IModel<Repository> repoModel;
	
	private final IModel<PullRequest> requestModel;
	
	private final IModel<Comment> commentModel;
	
	private final String oldRev;
	
	private final String newRev;
	
	private final String path;
	
	private final String comparePath;
	
	private final LineProcessor lineProcessor;
	
	private final DiffMode diffMode;
	
	private IModel<ChangesAndCount> changesAndCountModel = new LoadableDetachableModel<ChangesAndCount>() {

		@Override
		protected ChangesAndCount load() {
			String oldCommitHash = repoModel.getObject().getObjectId(oldRev).name();
			String newCommitHash = repoModel.getObject().getObjectId(newRev).name();
			List<String> paths = new ArrayList<>();
			if (path != null)
				paths.add(path);
			if (comparePath != null)
				paths.add(comparePath);
			List<DiffEntry> diffEntries = repoModel.getObject().getDiffs(oldCommitHash, newCommitHash,
					true, paths.toArray(new String[paths.size()]));
			List<BlobChange> diffableChanges = new ArrayList<>();
	    	for (DiffEntry entry: diffEntries) {
	    		if (diffableChanges.size() < Constants.MAX_DIFF_FILES) {
		    		diffableChanges.add(new BlobChange(oldRev, newRev, entry) {

						@Override
						public Blob getBlob(BlobIdent blobIdent) {
							return repoModel.getObject().getBlob(blobIdent);
						}

						@Override
						public LineProcessor getLineProcessor() {
							return lineProcessor;
						}

		    		});
	    		} else {
	    			break;
	    		}
	    	}

	    	// Diff calculation can be slow, so we pre-load diffs of each change 
	    	// concurrently
	    	Collection<Callable<Void>> tasks = new ArrayList<>();
	    	for (final BlobChange change: diffableChanges) {
	    		tasks.add(new Callable<Void>() {

					@Override
					public Void call() throws Exception {
						change.getDiffBlocks();
						return null;
					}
	    			
	    		});
	    	}
	    	for (Future<Void> future: GitPlex.getInstance(ForkJoinPool.class).invokeAll(tasks)) {
	    		try {
	    			// call get in order to throw exception if there is any during task execution
					future.get();
				} catch (InterruptedException|ExecutionException e) {
					throw new RuntimeException(e);
				}
	    	}

	    	int totalChanges = diffEntries.size();
	    	if (diffableChanges.size() == totalChanges) { 
		    	// some changes should be removed if content is the same after line processing 
		    	for (Iterator<BlobChange> it = diffableChanges.iterator(); it.hasNext();) {
		    		BlobChange change = it.next();
		    		if (change.getType() == ChangeType.MODIFY 
		    				&& Objects.equal(change.getOldBlobIdent().mode, change.getNewBlobIdent().mode)
		    				&& change.getAdditions() + change.getDeletions() == 0) {
		    			Blob.Text oldText = change.getOldText();
		    			Blob.Text newText = change.getNewText();
		    			if (oldText != null && newText != null 
		    					&& (oldText.getLines().size() + newText.getLines().size()) <= DiffUtils.MAX_DIFF_SIZE) {
			    			it.remove();
		    			}
		    		}
		    	}
		    	totalChanges = diffableChanges.size();
	    	} 

	    	List<BlobChange> displayableChanges = new ArrayList<>();
	    	int totalChangedLines = 0;
	    	for (BlobChange change: diffableChanges) {
	    		int changedLines = change.getAdditions() + change.getDeletions(); 
	    		
	    		// we do not count large diff in a single file in order to 
	    		// display smaller diffs from different files as many as 
	    		// possible. 
	    		if (changedLines <= Constants.MAX_SINGLE_FILE_DIFF_LINES) {
		    		totalChangedLines += changedLines;
		    		if (totalChangedLines <= Constants.MAX_DIFF_LINES)
		    			displayableChanges.add(change);
		    		else
		    			break;
	    		} else {
	    			// large diff in a single file will not be displayed, so 
	    			// adding it to change list will do no harm, and can avoid 
	    			// displaying "too many changes" when some big text file 
	    			// is added/removed without touching too many files
	    			displayableChanges.add(change);
	    		}
	    	}
	    	return new ChangesAndCount(displayableChanges, totalChanges);
		}
	};
	
	public RevisionDiffPanel(String id, IModel<Repository> repoModel, IModel<PullRequest> requestModel, 
			IModel<Comment> commentModel, String oldRev, String newRev, @Nullable String path, 
			@Nullable String comparePath, LineProcessor lineProcessor, DiffMode diffMode) {
		super(id);
		
		this.repoModel = repoModel;
		this.requestModel = requestModel;
		this.commentModel = commentModel;
		this.oldRev = oldRev;
		this.newRev = newRev;
		this.path = path;
		this.comparePath = comparePath;
		this.lineProcessor = lineProcessor;
		this.diffMode = diffMode;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();

		add(new Label("totalChanged", new AbstractReadOnlyModel<Integer>() {

			@Override
			public Integer getObject() {
				return getChangesCount();
			}
			
		}));
		if (path != null) {
			add(new Label("filterPath", "filter by " + path));
			add(new AjaxLink<Void>("clearPath") {

				@Override
				protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
					super.updateAjaxAttributes(attributes);
					attributes.getAjaxCallListeners().add(new ConfirmLeaveListener(RevisionDiffPanel.this));
					attributes.getAjaxCallListeners().add(new IndicateLoadingListener());
				}

				@Override
				public void onClick(AjaxRequestTarget target) {
					onClearPath(target);
				}
				
			});
		} else {
			add(new WebMarkupContainer("filterPath").setVisible(false));
			add(new WebMarkupContainer("clearPath").setVisible(false));
		}

		add(new WebMarkupContainer("tooManyChanges") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(getChanges().size() < getChangesCount());
			}
			
		});
		
		add(new ListView<BlobChange>("diffStats", new AbstractReadOnlyModel<List<BlobChange>>() {

			@Override
			public List<BlobChange> getObject() {
				return getChanges();
			}
			
		}) {

			@Override
			protected void populateItem(ListItem<BlobChange> item) {
				BlobChange change = item.getModelObject();
				String iconClass;
				if (change.getType() == ChangeType.ADD)
					iconClass = " fa-ext fa-diff-added";
				else if (change.getType() == ChangeType.DELETE)
					iconClass = " fa-ext fa-diff-removed";
				else if (change.getType() == ChangeType.MODIFY)
					iconClass = " fa-ext fa-diff-modified";
				else
					iconClass = " fa-ext fa-diff-renamed";
				
				item.add(new WebMarkupContainer("icon").add(AttributeAppender.append("class", iconClass)));
				
				WebMarkupContainer pathLink = new WebMarkupContainer("path");
				pathLink.add(AttributeModifier.replace("href", "#diff-" + change.getPath()));
				pathLink.add(new Label("path", change.getPath()));
				
				item.add(pathLink);
				
				item.add(new Label("additions", "+" + change.getAdditions()));
				item.add(new Label("deletions", "-" + change.getDeletions()));
				
				boolean barVisible;
				if (change.getType() == ChangeType.ADD) {
					Blob.Text text = change.getNewText();
					barVisible = (text != null && text.getLines().size() <= DiffUtils.MAX_DIFF_SIZE);
				} else if (change.getType() == ChangeType.DELETE) {
					Blob.Text text = change.getOldText();
					barVisible = (text != null && text.getLines().size() <= DiffUtils.MAX_DIFF_SIZE);
				} else {
					Blob.Text oldText = change.getOldText();
					Blob.Text newText = change.getNewText();
					barVisible = (oldText != null && newText != null 
							&& oldText.getLines().size()+newText.getLines().size() <= DiffUtils.MAX_DIFF_SIZE);
				}
				item.add(new DiffStatBar("bar", change.getAdditions(), change.getDeletions(), false).setVisible(barVisible));
			}
			
		});
		
		add(new ListView<BlobChange>("changes", new AbstractReadOnlyModel<List<BlobChange>>() {

			@Override
			public List<BlobChange> getObject() {
				return getChanges();
			}
			
		}) {

			@Override
			protected void populateItem(ListItem<BlobChange> item) {
				BlobChange change = item.getModelObject();
				item.setMarkupId("diff-" + change.getPath());
				item.setOutputMarkupId(true);
				item.add(new BlobDiffPanel("change", repoModel, requestModel, commentModel, change, diffMode));
			}
			
		});
	}
	
	private List<BlobChange> getChanges() {
		return changesAndCountModel.getObject().getChanges();
	}
	
	private int getChangesCount() {
		return changesAndCountModel.getObject().getCount();
	}
	
	@Override
	protected void onDetach() {
		changesAndCountModel.detach();
		repoModel.detach();
		requestModel.detach();
		commentModel.detach();
		
		super.onDetach();
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new CssResourceReference(RevisionDiffPanel.class, "revision-diff.css")));
	}
	
	protected abstract void onClearPath(AjaxRequestTarget target);

	private static class ChangesAndCount {
		
		private final List<BlobChange> changes;
		
		private final int count;
		
		public ChangesAndCount(List<BlobChange> changes, int count) {
			this.changes = changes;
			this.count =  count;
		}

		public List<BlobChange> getChanges() {
			return changes;
		}

		public int getCount() {
			return count;
		}
		
	}
}
