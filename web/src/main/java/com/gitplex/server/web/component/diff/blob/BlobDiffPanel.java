package com.gitplex.server.web.component.diff.blob;

import javax.annotation.Nullable;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;

import com.gitplex.server.GitPlex;
import com.gitplex.server.git.Blob;
import com.gitplex.server.git.BlobChange;
import com.gitplex.server.model.CodeComment;
import com.gitplex.server.model.Depot;
import com.gitplex.server.model.PullRequest;
import com.gitplex.server.model.support.CommentPos;
import com.gitplex.server.util.diff.DiffUtils;
import com.gitplex.server.web.WebConstants;
import com.gitplex.server.web.component.diff.DiffRenderer;
import com.gitplex.server.web.component.diff.blob.text.TextDiffPanel;
import com.gitplex.server.web.component.diff.difftitle.BlobDiffTitle;
import com.gitplex.server.web.component.diff.revision.BlobCommentSupport;
import com.gitplex.server.web.component.diff.revision.DiffViewMode;

@SuppressWarnings("serial")
public class BlobDiffPanel extends Panel implements SourceAware {

	private static final String CONTENT_ID = "content";
	
	private final IModel<Depot> depotModel;
	
	private final IModel<PullRequest> requestModel;
	
	private final BlobChange change;
	
	private final IModel<Boolean> blameModel;
	
	private final DiffViewMode diffMode;
	
	private final BlobCommentSupport markSupport;
	
	public BlobDiffPanel(String id, IModel<Depot> depotModel, IModel<PullRequest> requestModel, 
			BlobChange change, DiffViewMode diffMode, @Nullable IModel<Boolean> blameModel, 
			@Nullable BlobCommentSupport markSupport) {
		super(id);
		
		this.depotModel = depotModel;
		this.requestModel = requestModel;
		this.change = change;
		this.blameModel = blameModel;
		this.diffMode = diffMode;
		this.markSupport = markSupport;
	}
	
	private Fragment newFragment(String message, boolean warning) {
		Fragment fragment = new Fragment(CONTENT_ID, "noDiffFrag", this);
		fragment.add(new BlobDiffTitle("title", change));
		if (warning)
			fragment.add(new WebMarkupContainer("icon").add(AttributeAppender.append("class", "fa fa-warning")));
		else
			fragment.add(new WebMarkupContainer("icon").add(AttributeAppender.append("class", "fa fa-info-circle")));
		fragment.add(new Label("message", message));
		return fragment;
	}
	
	private void showBlob(Blob blob) {
		if (blob.getText() != null) {
			if (blob.getText().getLines().size() > DiffUtils.MAX_DIFF_SIZE) {
				add(newFragment("Unable to diff as the file is too large.", true));
			} else if (change.getAdditions()+change.getDeletions() > WebConstants.MAX_SINGLE_FILE_DIFF_LINES) {
				add(newFragment("Diff is too large to be displayed.", true));
			} else if (change.getDiffBlocks().isEmpty()) {
				if (change.getNewBlobIdent().path != null)
					add(newFragment("Empty file added.", false));
				else
					add(newFragment("Empty file removed.", false));
			} else {
				add(new TextDiffPanel(CONTENT_ID, depotModel, requestModel, change, diffMode, blameModel, markSupport));
			}
		} else if (blob.isPartial()) {
			add(newFragment("File is too large to be loaded.", true));
		} else {
			Panel diffPanel = null;
			for (DiffRenderer renderer: GitPlex.getExtensions(DiffRenderer.class)) {
				diffPanel = renderer.render(CONTENT_ID, blob.getMediaType(), change);
				if (diffPanel != null)
					break;
			}
			if (diffPanel != null)
				add(diffPanel);
			else
				add(newFragment("Binary file.", false));
		}
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		if (change.getType() == ChangeType.ADD || change.getType() == ChangeType.COPY) {
			showBlob(change.getNewBlob());
		} else if (change.getType() == ChangeType.DELETE) {
			showBlob(change.getOldBlob());
		} else {
			if (change.getOldText() != null && change.getNewText() != null) {
				if (change.getOldText().getLines().size() + change.getNewText().getLines().size() > DiffUtils.MAX_DIFF_SIZE) {
					add(newFragment("Unable to diff as the file is too large.", true));
				} else if (change.getAdditions() + change.getDeletions() > WebConstants.MAX_SINGLE_FILE_DIFF_LINES) {
					add(newFragment("Diff is too large to be displayed.", true));
				} else if (change.getAdditions() + change.getDeletions() == 0 
						&& (markSupport == null || markSupport.getComments().isEmpty())) {
					add(newFragment("Content is identical", false));
				} else {
					add(new TextDiffPanel(CONTENT_ID, depotModel, requestModel, change, diffMode, blameModel, markSupport));
				}
			} else if (change.getOldBlob().isPartial() || change.getNewBlob().isPartial()) {
				add(newFragment("File is too large to be loaded.", true));
			} else if (change.getOldBlob().getMediaType().equals(change.getNewBlob().getMediaType())) {
				Panel diffPanel = null;
				for (DiffRenderer renderer: GitPlex.getExtensions(DiffRenderer.class)) {
					diffPanel = renderer.render(CONTENT_ID, change.getNewBlob().getMediaType(), change);
					if (diffPanel != null)
						break;
				}
				if (diffPanel != null)
					add(diffPanel);
				else
					add(newFragment("Binary file.", false));
			} else {
				add(newFragment("Binary file.", false));
			}
		}
	}
	
	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new BlobDiffResourceReference()));
	}

	protected void onDetach() {
		depotModel.detach();
		requestModel.detach();
		
		if (blameModel != null)
			blameModel.detach();
		
		super.onDetach();
	}

	@Override
	public void onCommentDeleted(AjaxRequestTarget target, CodeComment comment) {
		Component content = get(CONTENT_ID);
		if (content instanceof SourceAware) {
			SourceAware sourceAware = (SourceAware) content;
			sourceAware.onCommentDeleted(target, comment);
		}
	}

	@Override
	public void onCommentClosed(AjaxRequestTarget target, CodeComment comment) {
		Component content = get(CONTENT_ID);
		if (content instanceof SourceAware) {
			SourceAware sourceAware = (SourceAware) content;
			sourceAware.onCommentClosed(target, comment);
		}
	}

	@Override
	public void onCommentAdded(AjaxRequestTarget target, CodeComment comment) {
		Component content = get(CONTENT_ID);
		if (content instanceof SourceAware) {
			SourceAware sourceAware = (SourceAware) content;
			sourceAware.onCommentAdded(target, comment);
		}
	}

	@Override
	public void mark(AjaxRequestTarget target, CommentPos mark) {
		Component content = get(CONTENT_ID);
		if (content instanceof SourceAware) {
			SourceAware sourceAware = (SourceAware) content;
			sourceAware.mark(target, mark);
		}
	}

	@Override
	public void onUnblame(AjaxRequestTarget target) {
		Component content = get(CONTENT_ID);
		if (content instanceof SourceAware) {
			SourceAware sourceAware = (SourceAware) content;
			sourceAware.onUnblame(target);
		}
	}

}
