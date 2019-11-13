package io.onedev.server.web.page.project.info;

import java.util.Collection;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.eclipse.jgit.lib.Constants;

import io.onedev.server.OneDev;
import io.onedev.server.cache.CommitInfoManager;
import io.onedev.server.entitymanager.UrlManager;
import io.onedev.server.model.Project;
import io.onedev.server.util.SecurityUtils;
import io.onedev.server.web.behavior.clipboard.CopyClipboardBehavior;
import io.onedev.server.web.component.floating.FloatingPanel;
import io.onedev.server.web.component.link.DropdownLink;
import io.onedev.server.web.component.link.ViewStateAwarePageLink;
import io.onedev.server.web.component.markdown.MarkdownViewer;
import io.onedev.server.web.component.modal.ModalLink;
import io.onedev.server.web.component.modal.ModalPanel;
import io.onedev.server.web.component.project.selector.ProjectSelector;
import io.onedev.server.web.page.project.blob.ProjectBlobPage;
import io.onedev.server.web.page.project.dashboard.ProjectDashboardPage;

@SuppressWarnings("serial")
public abstract class ProjectInfoPanel extends Panel {

	private final IModel<Project> projectModel;
	
	public ProjectInfoPanel(String id, IModel<Project> projectModel) {
		super(id);
		this.projectModel = projectModel;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		if (getProject().getForkedFrom() != null) {
			Link<Void> link = new ViewStateAwarePageLink<Void>("forkedFromLink", 
					ProjectDashboardPage.class, ProjectBlobPage.paramsOf(getProject().getForkedFrom())) {

				@Override
				protected void onComponentTag(ComponentTag tag) {
					super.onComponentTag(tag);
					if (!isEnabled())
						tag.setName("span");
				}
				
			};
			link.add(new Label("name", getProject().getForkedFrom().getName()));
			link.setEnabled(SecurityUtils.canAccess(getProject().getForkedFrom()));
			add(link);
		} else {
			WebMarkupContainer link = new WebMarkupContainer("forkedFromLink");
			link.add(new Label("name"));
			link.setVisible(false);
			add(link);
		}
		add(new Label("name", getProject().getName()));
		add(new Label("id", getProject().getId()));
		
		UrlManager urlManager = OneDev.getInstance(UrlManager.class);
		Model<String> cloneUrlModel = Model.of(urlManager.urlFor(getProject()));
		add(new TextField<String>("cloneUrl", cloneUrlModel)
				.setVisible(SecurityUtils.canReadCode(getProject())));
		add(new WebMarkupContainer("copyUrl").add(new CopyClipboardBehavior(cloneUrlModel)));
		
		if (getProject().getDescription() != null) {
			add(new MarkdownViewer("description", Model.of(getProject().getDescription()), null));
		} else {
			add(new WebMarkupContainer("description").setVisible(false));
		}
		
		CommitInfoManager commitInfoManager = OneDev.getInstance(CommitInfoManager.class);
		add(new Label("commitCount", commitInfoManager.getCommitCount(getProject()) + " commits"));
		add(new Label("branchCount", getProject().getRefs(Constants.R_HEADS).size() + " branches"));
		add(new Label("tagCount", getProject().getRefs(Constants.R_TAGS).size() + " tags"));
		
		if (getProject().getForks().isEmpty()) {
			add(new WebMarkupContainer("forks") {

				@Override
				protected void onInitialize() {
					super.onInitialize();
					add(new Label("label", "0 forks"));
				}

				@Override
				protected void onComponentTag(ComponentTag tag) {
					super.onComponentTag(tag);
					tag.setName("span");
				}
				
			});
		} else {
			add(new DropdownLink("forks") {

				@Override
				protected void onInitialize() {
					super.onInitialize();
					add(new Label("label", getProject().getForks().size() + " forks <i class='fa fa-caret-down'></i>").setEscapeModelStrings(false));
				}

				@Override
				protected Component newContent(String id, FloatingPanel dropdown) {
					return new ProjectSelector(id, new LoadableDetachableModel<Collection<Project>>() {

						@Override
						protected Collection<Project> load() {
							return getProject().getForks();
						}
						
					}, Project.idOf(getProject())) {

						@Override
						protected void onSelect(AjaxRequestTarget target, Project project) {
							setResponsePage(ProjectBlobPage.class, ProjectBlobPage.paramsOf(project));
						}

					};
				}
				
			});
		}
		
		add(new ModalLink("forkNow") {
			
			@Override
			public void onClick(AjaxRequestTarget target) {
				super.onClick(target);
				onPromptForkOption(target);
			}

			@Override
			protected Component newContent(String id, ModalPanel modal) {
				return new ForkOptionPanel(id, projectModel) {

					@Override
					protected void onClose(AjaxRequestTarget target) {
						modal.close();
					}
					
				};
			}
			
		}.setVisible(SecurityUtils.canCreateProjects() 
				&& SecurityUtils.getUser() != null 
				&&  SecurityUtils.canReadCode(getProject())));
	}
	
	private Project getProject() {
		return projectModel.getObject();
	}

	@Override
	protected void onDetach() {
		projectModel.detach();
		super.onDetach();
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new ProjectInfoResourceReference()));
	}

	protected abstract void onPromptForkOption(AjaxRequestTarget target);
	
}
