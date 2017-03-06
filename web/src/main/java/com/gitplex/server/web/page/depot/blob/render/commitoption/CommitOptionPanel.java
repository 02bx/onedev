package com.gitplex.server.web.page.depot.blob.render.commitoption;

import java.io.IOException;
import java.util.Arrays;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes.Method;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.unbescape.javascript.JavaScriptEscape;

import com.gitplex.server.GitPlex;
import com.gitplex.server.git.Blob;
import com.gitplex.server.git.BlobChange;
import com.gitplex.server.git.BlobEdit;
import com.gitplex.server.git.BlobIdent;
import com.gitplex.server.git.GitUtils;
import com.gitplex.server.git.PathAndContent;
import com.gitplex.server.git.exception.NotTreeException;
import com.gitplex.server.git.exception.ObjectAlreadyExistsException;
import com.gitplex.server.git.exception.ObsoleteCommitException;
import com.gitplex.server.manager.AccountManager;
import com.gitplex.server.model.Account;
import com.gitplex.server.model.Depot;
import com.gitplex.server.model.PullRequest;
import com.gitplex.server.util.Provider;
import com.gitplex.server.util.diff.WhitespaceOption;
import com.gitplex.server.web.component.diff.blob.BlobDiffPanel;
import com.gitplex.server.web.component.diff.revision.DiffViewMode;
import com.gitplex.server.web.component.link.ViewStateAwareAjaxLink;
import com.gitplex.server.web.page.depot.blob.navigator.BlobNameChanging;
import com.gitplex.server.web.page.depot.blob.render.BlobRenderContext;
import com.gitplex.server.web.page.depot.blob.render.BlobRenderContext.Mode;
import com.gitplex.server.web.util.ajaxlistener.ConfirmLeaveListener;
import com.gitplex.server.web.util.ajaxlistener.TrackViewStateListener;
import com.google.common.base.Preconditions;

import de.agilecoders.wicket.core.markup.html.bootstrap.common.NotificationPanel;
import jersey.repackaged.com.google.common.base.Objects;

@SuppressWarnings("serial")
public class CommitOptionPanel extends Panel {

	private final BlobRenderContext context;
	
	private BlobEdit blobEdit;
	
	private ObjectId currentCommitId;
	
	private String summaryCommitMessage;
	
	private String detailCommitMessage;
	
	private BlobChange change;
	
	private boolean contentModified;
	
	public CommitOptionPanel(String id, BlobRenderContext context, @Nullable Provider<byte[]> newContentProvider) {
		super(id);

		this.context = context;

		PathAndContent newBlob;
		if (newContentProvider != null) {
			newBlob = new PathAndContent() {

				@Override
				public String getPath() {
					return context.getNewPath();
				}

				@Override
				public byte[] getContent() {
					return newContentProvider.get();
				}

			};
		} else {
			newBlob = null;
		}
		
		String oldPath = context.getBlobIdent().isFile()?context.getBlobIdent().path:null;
		this.blobEdit = new BlobEdit(oldPath, newBlob);
	}
	
	private String getDefaultCommitMessage() {
		String oldPath = blobEdit.getOldPath();
		String oldName;
		if (oldPath != null && oldPath.contains("/"))
			oldName = StringUtils.substringAfterLast(oldPath, "/");
		else
			oldName = oldPath;
		
		PathAndContent newBlob = blobEdit.getNewBlob();
		if (newBlob == null) { 
			return "Delete " + oldName;
		} else {
			String newPath = newBlob.getPath();

			String newName;
			if (newPath != null && newPath.contains("/"))
				newName = StringUtils.substringAfterLast(newPath, "/");
			else
				newName = newPath;
			
			if (oldPath == null) {
				if (newName != null)
					return "Add " + newName;
				else
					return "Add new file";
			} else if (oldPath.equals(newPath)) {
				return "Edit " + oldName;
			} else {
				return "Rename " + oldName;
			}
		}
			
	}
	
	private void newChangedContainer(@Nullable AjaxRequestTarget target) {
		WebMarkupContainer changedContainer = new WebMarkupContainer("changed");
		changedContainer.setVisible(change != null);
		changedContainer.setOutputMarkupPlaceholderTag(true);
		if (change != null) {
			changedContainer.add(new BlobDiffPanel("changes", new AbstractReadOnlyModel<Depot>() {

				@Override
				public Depot getObject() {
					return context.getDepot();
				}
				
			}, new Model<PullRequest>(null), change, DiffViewMode.UNIFIED, null, null));
		} else {
			changedContainer.add(new WebMarkupContainer("changes"));
		}
		if (target != null) {
			replace(changedContainer);
			target.add(changedContainer);
			if (change != null) {
				String script = String.format("$('#%s .commit-option input[type=submit]').val('Commit and overwrite');", 
						getMarkupId());
				target.appendJavaScript(script);
			}
		} else {
			add(changedContainer);		
		}
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		FeedbackPanel feedback = new NotificationPanel("feedback", this);
		feedback.setOutputMarkupPlaceholderTag(true);
		add(feedback);
				
		newChangedContainer(null);
		
		Form<?> form = new Form<Void>("form");
		add(form);

		form.add(new TextField<String>("summaryCommitMessage", new IModel<String>() {

			@Override
			public void detach() {
			}

			@Override
			public String getObject() {
				return summaryCommitMessage;
			}

			@Override
			public void setObject(String object) {
				summaryCommitMessage = object;
			}
			
		}) {

			@Override
			protected void onComponentTag(ComponentTag tag) {
				super.onComponentTag(tag);
				tag.put("placeholder", getDefaultCommitMessage());
			}
			
		});
		
		form.add(new TextArea<String>("detailCommitMessage", new IModel<String>() {

			@Override
			public void detach() {
			}

			@Override
			public String getObject() {
				return detailCommitMessage;
			}

			@Override
			public void setObject(String object) {
				detailCommitMessage = object;
			}
			
		}));
		
		AjaxButton saveButton = new AjaxButton("save") {

			@Override
			protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
				super.updateAjaxAttributes(attributes);
				attributes.setMethod(Method.POST);

				attributes.getAjaxCallListeners().add(new TrackViewStateListener(true));
			}

			@Override
			protected void onComponentTag(ComponentTag tag) {
				super.onComponentTag(tag);

				if (!isBlobModified())
					tag.put("disabled", "disabled");
			}

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				super.onSubmit(target, form);
				if (save(target, feedback)) {
					String script = String.format(""
							+ "$('#%s').attr('disabled', 'disabled').val('Please wait...');"
							+ "gitplex.server.form.markClean($('form'));", getMarkupId());
					target.appendJavaScript(script);
				}
			}
			
		};
		saveButton.setOutputMarkupId(true);
		form.add(saveButton);
		
		form.add(new ViewStateAwareAjaxLink<Void>("cancel", true) {

			@Override
			protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
				super.updateAjaxAttributes(attributes);
				attributes.getAjaxCallListeners().add(new ConfirmLeaveListener());
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(blobEdit.getNewBlob() == null);
			}

			@Override
			public void onClick(AjaxRequestTarget target) {
				context.onModeChange(target, Mode.VIEW);
			}
			
		});

		setOutputMarkupId(true);
	}
	
	private boolean isBlobModified() {
		return !context.getBlobIdent().isFile() 
				|| context.getMode() == Mode.DELETE
				|| contentModified 
				|| !Objects.equal(context.getBlobIdent().path, context.getNewPath());
	}
	
	private boolean save(AjaxRequestTarget target, FeedbackPanel feedback) {
		change = null;
		
		PathAndContent newBlob = blobEdit.getNewBlob();
		if (newBlob != null && StringUtils.isBlank(newBlob.getPath())) {
			CommitOptionPanel.this.error("Please specify file name.");
			target.add(feedback);
			return false;
		} else {
			String commitMessage = summaryCommitMessage;
			if (StringUtils.isBlank(commitMessage))
				commitMessage = getDefaultCommitMessage();
			if (StringUtils.isNotBlank(detailCommitMessage))
				commitMessage += "\n\n" + detailCommitMessage;
			Account user = Preconditions.checkNotNull(GitPlex.getInstance(AccountManager.class).getCurrent());

			String refName = GitUtils.branch2ref(context.getBlobIdent().revision);
			ObjectId prevCommitId = context.getDepot().getObjectId(context.getBlobIdent().revision);
			Repository repository = context.getDepot().getRepository();
			ObjectId newCommitId = null;
			while(newCommitId == null) {
				try {
					newCommitId = blobEdit.commit(repository, refName, 
							prevCommitId, prevCommitId, user.asPerson(), commitMessage);
				} catch (ObsoleteCommitException e) {
					currentCommitId = e.getOldCommitId();
					try (RevWalk revWalk = new RevWalk(repository)) {
						RevCommit prevCommit = revWalk.parseCommit(prevCommitId);
						RevCommit currentCommit = revWalk.parseCommit(currentCommitId);
						prevCommitId = currentCommitId;

						String oldPath = blobEdit.getOldPath();
						if (oldPath != null) {
							TreeWalk treeWalk = TreeWalk.forPath(repository, oldPath, 
									prevCommit.getTree().getId(), currentCommit.getTree().getId());
							if (treeWalk != null) {
								if (!treeWalk.getObjectId(0).equals(treeWalk.getObjectId(1)) 
										|| !treeWalk.getFileMode(0).equals(treeWalk.getFileMode(1))) {
									// mark changed if original file exists and content or mode has been modified
									// by others
									if (treeWalk.getObjectId(1).equals(ObjectId.zeroId())) {
										if (newBlob != null) {
											blobEdit = new BlobEdit(null, newBlob);
											change = getChange(treeWalk, prevCommit, currentCommit);
											break;
										} else {
											newCommitId = currentCommitId;
											break;
										}
									} else {
										change = getChange(treeWalk, prevCommit, currentCommit);
										break;
									}
								}
							}
						}
						if (newBlob != null && !newBlob.getPath().equals(oldPath)) { 
							TreeWalk treeWalk = TreeWalk.forPath(repository, newBlob.getPath(), 
									prevCommit.getTree().getId(), currentCommit.getTree().getId());
							if (treeWalk != null) {
								if (!treeWalk.getObjectId(0).equals(treeWalk.getObjectId(1)) 
										|| !treeWalk.getFileMode(0).equals(treeWalk.getFileMode(1))) {
									// if added/renamed file exists and content or mode has been modified 
									// by others
									change = getChange(treeWalk, prevCommit, currentCommit);
									break;
								}
							}
						} 
					} catch (IOException e2) {
						throw new RuntimeException(e2);
					}
				} catch (ObjectAlreadyExistsException e) {
					CommitOptionPanel.this.error("A file with same name already exists. "
							+ "Please choose a different name and try again.");
					target.add(feedback);
					break;
				} catch (NotTreeException e) {
					CommitOptionPanel.this.error("A file exists where you’re trying to create a subdirectory. "
							+ "Choose a new path and try again..");
					target.add(feedback);
					break;
				}
			}
			if (newCommitId != null) {
				context.onCommitted(target, prevCommitId, newCommitId);
				return true;
			} else {
				newChangedContainer(target);
				return false;
			}
		}
	}
	
	private BlobChange getChange(TreeWalk treeWalk, RevCommit oldCommit, RevCommit newCommit) {
		DiffEntry.ChangeType changeType = DiffEntry.ChangeType.MODIFY;
		BlobIdent oldBlobIdent = new BlobIdent();
		oldBlobIdent.revision = oldCommit.name();
		if (!treeWalk.getObjectId(0).equals(ObjectId.zeroId())) {
			oldBlobIdent.path = treeWalk.getPathString();
			oldBlobIdent.mode = treeWalk.getRawMode(0);
		} else {
			changeType = DiffEntry.ChangeType.ADD;
		}
		
		BlobIdent newBlobIdent = new BlobIdent();
		newBlobIdent.revision = newCommit.name();
		if (!treeWalk.getObjectId(1).equals(ObjectId.zeroId())) {
			newBlobIdent.path = treeWalk.getPathString();
			newBlobIdent.mode = treeWalk.getRawMode(1);
		} else {
			changeType = DiffEntry.ChangeType.DELETE;
		}
		
		return new BlobChange(changeType, oldBlobIdent, newBlobIdent, WhitespaceOption.DEFAULT) {

			@Override
			public Blob getBlob(BlobIdent blobIdent) {
				return context.getDepot().getBlob(blobIdent);
			}

		};
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(JavaScriptHeaderItem.forReference(new CommitOptionResourceReference()));
	}
	
	public void onContentChange(IPartialPageRequestHandler partialPageRequestHandler) {
		Preconditions.checkNotNull(blobEdit.getNewBlob());
		
		if (context.getMode() == Mode.EDIT) {
			contentModified = !Arrays.equals(
					blobEdit.getNewBlob().getContent(), 
					context.getDepot().getBlob(context.getBlobIdent()).getBytes());
		} else {
			contentModified = blobEdit.getNewBlob().getContent().length != 0;
		}
		onBlobChange(partialPageRequestHandler);
	}
	
	@Override
	public void onEvent(IEvent<?> event) {
		super.onEvent(event);
		if (event.getPayload() instanceof BlobNameChanging) {
			BlobNameChanging payload = (BlobNameChanging) event.getPayload();
			onBlobChange(payload.getPartialPageRequestHandler());
		}
	}

	private void onBlobChange(IPartialPageRequestHandler partialPageRequestHandler) {
		String script = String.format("gitplex.server.commitOption.onBlobChange('%s', '%s', %b);", getMarkupId(), 
				JavaScriptEscape.escapeJavaScript(getDefaultCommitMessage()), isBlobModified());
		partialPageRequestHandler.appendJavaScript(script);
	}

}
