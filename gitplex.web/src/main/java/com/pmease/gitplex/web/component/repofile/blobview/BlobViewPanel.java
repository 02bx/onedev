package com.pmease.gitplex.web.component.repofile.blobview;

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
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;

import com.google.common.base.Preconditions;
import com.pmease.commons.git.Git;
import com.pmease.commons.wicket.assets.closestdescendant.ClosestDescendantResourceReference;
import com.pmease.gitplex.web.page.repository.file.HistoryState;
import com.pmease.gitplex.web.resource.BlobResource;
import com.pmease.gitplex.web.resource.BlobResourceReference;

@SuppressWarnings("serial")
public abstract class BlobViewPanel extends Panel {

	protected final BlobViewContext context;
	
	public BlobViewPanel(String id, BlobViewContext context) {
		super(id);
		
		HistoryState state = context.getState();
		Preconditions.checkArgument(state.file.revision != null 
				&& state.file.path != null && state.file.mode != null);
		
		this.context = context;
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();

		add(new Label("lines", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return context.getBlob().getText().getLines().size() + " lines";
			}
			
		}) {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				
				setVisible(context.getBlob().getText() != null);
			}
			
		});
		
		add(new Label("charset", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return context.getBlob().getText().getCharset().displayName();
			}
			
		}) {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				
				setVisible(context.getBlob().getText() != null);
			}
			
		});
		
		add(new Label("size", FileUtils.byteCountToDisplaySize(context.getBlob().getSize())));

		add(new ResourceLink<Void>("raw", new BlobResourceReference(), 
				BlobResource.paramsOf(context.getRepository(), context.getState().file)));
		
		add(new AjaxLink<Void>("blame") {

			@Override
			protected void onInitialize() {
				super.onInitialize();
				
				add(AttributeAppender.append("class", new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						if (context.getState().blame)
							return " active";
						else
							return " ";
					}
					
				}));
				
				setOutputMarkupId(true);
			}

			@Override
			public void onClick(AjaxRequestTarget target) {
				context.getState().blame = !context.getState().blame;
				context.onBlameChange(target);
				
				// this blob view panel might be replaced with another panel
				if (findPage() != null) {
					target.add(this);
					target.focusComponent(null);
				}
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				
				setVisible(context.getBlob().getText() != null);
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
				
				if (!context.getRepository().getRefs(Git.REFS_HEADS).containsKey(context.getState().file.revision))
					tag.put("title", "Must on a branch to change or propose change of this file");
			}
			
		};
		add(changeActions);
		
		changeActions.add(new AjaxLink<Void>("edit") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				
				setVisible(context.getBlob().getText() != null);
			}

			@Override
			protected void onComponentTag(ComponentTag tag) {
				super.onComponentTag(tag);
				
				if (!context.getRepository().getRefs(Git.REFS_HEADS).containsKey(context.getState().file.revision))
					tag.put("disabled", "disabled");
			}

			@Override
			public void onClick(AjaxRequestTarget target) {
				context.onEdit(target);
			}
			
		});
		
		changeActions.add(new AjaxLink<Void>("delete") {

			@Override
			protected void onComponentTag(ComponentTag tag) {
				super.onComponentTag(tag);
				
				if (!context.getRepository().getRefs(Git.REFS_HEADS).containsKey(context.getState().file.revision))
					tag.put("disabled", "disabled");
			}

			@Override
			public void onClick(AjaxRequestTarget target) {
				context.onDelete(target);
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
