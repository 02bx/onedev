package com.gitplex.server.web.page.project.moreinfo;

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

import com.gitplex.server.GitPlex;
import com.gitplex.server.manager.CommitInfoManager;
import com.gitplex.server.manager.UrlManager;
import com.gitplex.server.model.Project;
import com.gitplex.server.security.SecurityUtils;
import com.gitplex.server.web.behavior.clipboard.CopyClipboardBehavior;
import com.gitplex.server.web.component.projectselector.ProjectSelector;
import com.gitplex.server.web.component.floating.FloatingPanel;
import com.gitplex.server.web.component.link.DropdownLink;
import com.gitplex.server.web.component.link.ViewStateAwarePageLink;
import com.gitplex.server.web.component.markdown.MarkdownViewer;
import com.gitplex.server.web.component.modal.ModalLink;
import com.gitplex.server.web.component.modal.ModalPanel;
import com.gitplex.server.web.page.project.blob.ProjectBlobPage;
import com.gitplex.server.web.page.project.branches.ProjectBranchesPage;
import com.gitplex.server.web.page.project.commit.ProjectCommitsPage;
import com.gitplex.server.web.page.project.tags.ProjectTagsPage;

@SuppressWarnings("serial")
public abstract class MoreInfoPanel extends Panel {

	private final IModel<Project> projectModel;
	
	public MoreInfoPanel(String id, IModel<Project> projectModel) {
		super(id);
		this.projectModel = projectModel;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		if (getProject().getForkedFrom() != null) {
			Link<Void> link = new ViewStateAwarePageLink<Void>("forkedFromLink", 
					ProjectBlobPage.class, ProjectBlobPage.paramsOf(getProject().getForkedFrom()));
			link.add(new Label("name", getProject().getForkedFrom().getName()));
			add(link);
		} else {
			WebMarkupContainer link = new WebMarkupContainer("forkedFromLink");
			link.add(new Label("name"));
			link.setVisible(false);
			add(link);
		}
		add(new Label("id", getProject().getId()));
		
		UrlManager urlManager = GitPlex.getInstance(UrlManager.class);
		Model<String> cloneUrlModel = Model.of(urlManager.urlFor(getProject()));
		add(new TextField<String>("cloneUrl", cloneUrlModel));
		add(new WebMarkupContainer("copyUrl").add(new CopyClipboardBehavior(cloneUrlModel)));
		
		if (getProject().getDescription() != null) {
			add(new MarkdownViewer("description", Model.of(getProject().getDescription()), null));
		} else {
			add(new WebMarkupContainer("description").setVisible(false));
		}
		
		add(new ViewStateAwarePageLink<Void>("commitsLink", 
				ProjectCommitsPage.class, ProjectCommitsPage.paramsOf(getProject())) {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(GitPlex.getInstance(CommitInfoManager.class).getCommitCount(getProject()) != 0);
			}

			@Override
			protected void onInitialize() {
				super.onInitialize();
				CommitInfoManager commitInfoManager = GitPlex.getInstance(CommitInfoManager.class);
				add(new Label("count", commitInfoManager.getCommitCount(getProject()) + " commits"));
			}
			
		});
		
		add(new ViewStateAwarePageLink<Void>("branchesLink", 
				ProjectBranchesPage.class, ProjectBranchesPage.paramsOf(getProject())) {

			@Override
			protected void onInitialize() {
				super.onInitialize();
				add(new Label("count", getProject().getRefs(Constants.R_HEADS).size() + " branches"));
			}
			
		});
		
		add(new ViewStateAwarePageLink<Void>("tagsLink", 
				ProjectTagsPage.class, ProjectTagsPage.paramsOf(getProject())) {

			@Override
			protected void onInitialize() {
				super.onInitialize();
				add(new Label("count", getProject().getRefs(Constants.R_TAGS).size() + " tags"));
			}
			
		});
		
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
			
		}.setVisible(SecurityUtils.getUser() != null));
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
		response.render(CssHeaderItem.forReference(new MoreInfoResourceReference()));
	}

	protected abstract void onPromptForkOption(AjaxRequestTarget target);
	
}
