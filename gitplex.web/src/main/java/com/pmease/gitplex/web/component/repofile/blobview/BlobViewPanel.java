package com.pmease.gitplex.web.component.repofile.blobview;

import javax.annotation.Nullable;

import org.apache.commons.io.FileUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ResourceLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;

import com.google.common.base.Preconditions;
import com.pmease.commons.git.Blob;
import com.pmease.commons.git.BlobIdent;
import com.pmease.commons.wicket.assets.closestdescendant.ClosestDescendantResourceReference;
import com.pmease.commons.wicket.component.ClientStateAwareAjaxLink;
import com.pmease.gitplex.web.component.repofile.blobview.BlobViewContext.Mode;
import com.pmease.gitplex.web.page.repository.file.RepoFilePage;
import com.pmease.gitplex.web.resource.BlobResource;
import com.pmease.gitplex.web.resource.BlobResourceReference;

@SuppressWarnings("serial")
public abstract class BlobViewPanel extends Panel {

	protected final BlobViewContext context;
	
	public BlobViewPanel(String id, BlobViewContext context) {
		super(id);
		
		BlobIdent blobIdent = context.getBlobIdent();
		Preconditions.checkArgument(blobIdent.revision != null 
				&& blobIdent.path != null && blobIdent.mode != null);
		
		this.context = context;
	}
	
	private Blob getBlob() {
		return context.getRepository().getBlob(context.getBlobIdent());
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();

		add(new Label("lines", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return getBlob().getText().getLines().size() + " lines";
			}
			
		}) {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				
				setVisible(getBlob().getText() != null);
			}
			
		});
		
		add(new Label("charset", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return getBlob().getText().getCharset().displayName();
			}
			
		}) {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				
				setVisible(getBlob().getText() != null);
			}
			
		});
		
		add(new Label("size", FileUtils.byteCountToDisplaySize(getBlob().getSize())));

		add(new ResourceLink<Void>("raw", new BlobResourceReference(), 
				BlobResource.paramsOf(context.getRepository(), context.getBlobIdent())));
		
		add(new ClientStateAwareAjaxLink<Void>("blame") {

			@Override
			protected void onInitialize() {
				super.onInitialize();
				
				add(AttributeAppender.append("class", new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						if (context.getMode() == Mode.BLAME)
							return " active";
						else
							return " ";
					}
					
				}));
				
				PageParameters params = RepoFilePage.paramsOf(context.getRepository(), 
						context.getBlobIdent().revision, context.getBlobIdent().path, 
						context.getMode()==null?Mode.BLAME:null);
				CharSequence url = RequestCycle.get().urlFor(RepoFilePage.class, params);
				add(AttributeAppender.replace("href", url.toString()));
				
				setOutputMarkupId(true);
			}

			@Override
			public void onClick(AjaxRequestTarget target, @Nullable String clientState) {
				context.onBlameChange(target, clientState);
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				
				setVisible(getBlob().getText() != null);
			}
			
		});
		add(new AjaxLink<Void>("history") {

			@Override
			public void onClick(AjaxRequestTarget target) {
			}
			
		});
		
		add(newCustomActions("customActions"));
		
		WebMarkupContainer changeActions = new WebMarkupContainer("changeActions") {
			
			@Override
			protected void onComponentTag(ComponentTag tag) {
				super.onComponentTag(tag);
				
				if (!context.isOnBranch() && !context.isAtSourceBranchHead())
					tag.put("title", "Must on a branch to change or propose change of this file");
			}
			
		};
		add(changeActions);
		
		changeActions.add(new ClientStateAwareAjaxLink<Void>("edit") {

			@Override
			protected void onInitialize() {
				super.onInitialize();
				
				if (context.isAtSourceBranchHead())
					add(new Label("label", "Edit on source branch"));
				else
					add(new Label("label", "Edit"));
				
				PageParameters params;
				if (context.isOnBranch()) {
					params = RepoFilePage.paramsOf(context.getRepository(), 
							context.getBlobIdent().revision, context.getBlobIdent().path, Mode.EDIT);
				} else if (context.isAtSourceBranchHead()) {
					params = getEditSourceBranchParams(null);
				} else {
					params = null;
				}
				if (params != null) {
					CharSequence url = RequestCycle.get().urlFor(RepoFilePage.class, params);
					add(AttributeAppender.replace("href", url.toString()));
				}
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				
				setVisible(getBlob().getText() != null);
			}

			@Override
			protected void onComponentTag(ComponentTag tag) {
				super.onComponentTag(tag);
				
				if (!context.isOnBranch() && !context.isAtSourceBranchHead()) 
					tag.put("disabled", "disabled");
			}

			@Override
			public void onClick(AjaxRequestTarget target, String clientState) {
				if (context.isOnBranch()) {
					context.onEdit(target, clientState);
				} else {
					PageParameters params = getEditSourceBranchParams(clientState);
					setResponsePage(RepoFilePage.class, params);
				}
			}
			
			private PageParameters getEditSourceBranchParams(String clientState) {
				return RepoFilePage.paramsOf(context.getPullRequest().getSourceRepo(), 
						context.getPullRequest().getSourceBranch(), context.getBlobIdent().path, 
						null, null, null, Mode.EDIT, null, clientState);
			}
			
		});
		
		changeActions.add(new AjaxLink<Void>("delete") {

			@Override
			protected void onInitialize() {
				super.onInitialize();
				
				if (context.isAtSourceBranchHead())
					add(new Label("label", "Delete from source branch"));
				else
					add(new Label("label", "Delete"));
				
				PageParameters params;
				if (context.isOnBranch()) {
					params = RepoFilePage.paramsOf(context.getRepository(), 
							context.getBlobIdent().revision, context.getBlobIdent().path, Mode.EDIT);
				} else if (context.isAtSourceBranchHead()) {
					params = RepoFilePage.paramsOf(context.getRepository(), 
							context.getPullRequest().getSourceBranch(), context.getBlobIdent().path, Mode.DELETE);
				} else {
					params = null;
				}
				if (params != null) {
					CharSequence url = RequestCycle.get().urlFor(RepoFilePage.class, params);
					add(AttributeAppender.replace("href", url.toString()));
				}
			}
			
			@Override
			protected void onComponentTag(ComponentTag tag) {
				super.onComponentTag(tag);
				
				if (!context.isOnBranch() && !context.isAtSourceBranchHead())
					tag.put("disabled", "disabled");
			}

			@Override
			public void onClick(AjaxRequestTarget target) {
				if (context.isOnBranch()) {
					context.onDelete(target);
				} else {
					PageParameters params = RepoFilePage.paramsOf(context.getPullRequest().getSourceRepo(), 
							context.getPullRequest().getSourceBranch(), context.getBlobIdent().path, Mode.DELETE);
					setResponsePage(RepoFilePage.class, params);
				}
			}

		});

		setOutputMarkupId(true);
	}
	
	protected WebMarkupContainer newCustomActions(String id) {
		return new WebMarkupContainer(id);
	}
	
	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		
		response.render(JavaScriptHeaderItem.forReference(ClosestDescendantResourceReference.INSTANCE));
		response.render(JavaScriptHeaderItem.forReference(
				new JavaScriptResourceReference(BlobViewPanel.class, "blob-view.js")));
		response.render(CssHeaderItem.forReference(
				new CssResourceReference(BlobViewPanel.class, "blob-view.css")));
		
		response.render(OnDomReadyHeaderItem.forScript(String.format("gitplex.blobView('%s');", getMarkupId())));
	}

	public BlobViewContext getContext() {
		return context;
	}
	
}
