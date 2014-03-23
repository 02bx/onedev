package com.pmease.gitop.web.page.project.settings;

import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.bean.validation.PropertyValidator;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.time.Duration;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.pmease.commons.git.Git;
import com.pmease.gitop.core.Gitop;
import com.pmease.gitop.core.manager.ProjectManager;
import com.pmease.gitop.model.Project;
import com.pmease.gitop.web.common.quantity.Data;
import com.pmease.gitop.web.common.wicket.component.vex.AjaxConfirmLink;
import com.pmease.gitop.web.common.wicket.form.BaseForm;
import com.pmease.gitop.web.common.wicket.form.checkbox.CheckBoxElement;
import com.pmease.gitop.web.common.wicket.form.select.DropDownChoiceElement;
import com.pmease.gitop.web.common.wicket.form.textfield.TextFieldElement;
import com.pmease.gitop.web.git.GitUtils;
import com.pmease.gitop.web.page.PageSpec;
import com.pmease.gitop.web.page.account.home.AccountHomePage;

import de.agilecoders.wicket.core.markup.html.bootstrap.common.NotificationPanel;

@SuppressWarnings("serial")
public class RepositoryOptionsPage extends AbstractRepositorySettingPage {

	public RepositoryOptionsPage(PageParameters params) {
		super(params);
	}

	private String projectName;
	private String defaultBranchName;
	
	@Override
	protected void onPageInitialize() {
		super.onPageInitialize();
		
		add(new Label("location", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return getProject().code().repoDir().toString();
			}
			
		}));
		
		add(new Label("size", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				long size = FileUtils.sizeOf(getProject().code().repoDir());
				return Data.formatBytes(size);
			}
			
		}));
		
		projectName = getProject().getName();
		
		Form<?> form = new BaseForm<Void>("form");
		add(form);
		form.add(new NotificationPanel("feedback",
									   new ComponentFeedbackMessageFilter(form))
					.hideAfter(Duration.seconds(15)));

		form.add(new TextFieldElement<String>("name", "Project Name", 
				new PropertyModel<String>(this, "projectName"))
				.add(new PropertyValidator<String>())
				.add(new IValidator<String>() {

					@Override
					public void validate(IValidatable<String> validatable) {
						Project project = getProject();
						
						String name = validatable.getValue();
						if (Objects.equal(name, project.getName())) {
							return;
						}
						
						ProjectManager pm = Gitop.getInstance(ProjectManager.class);
						if (pm.findBy(project.getOwner(), name) != null) {
							validatable.error(new ValidationError().setMessage("Project name is already exist"));
						}
					}
				}));
		form.add(new TextFieldElement<String>("description", "Description", 
				new PropertyModel<String>(projectModel, "description"))
				.add(new PropertyValidator<String>())
				.setRequired(false));
		
		form.add(new CheckBoxElement("forkable", "Allow Forks",
				new PropertyModel<Boolean>(projectModel, "forkable"),
				Model.of("Enable/Disable whether this repository can be forked by others")));
		
		// Default branch is recorded in HEAD ref of the repository, since no any branches exist in 
		// project when it is created, it might be more appropriate to assign default branch directly 
		// via branches page.
		IModel<List<? extends String>> branchesModel = new AbstractReadOnlyModel<List<? extends String>>() {

			@Override
			public List<String> getObject() {
				Project project = getProject();
				Git git = project.code();
				if (git.hasCommits()) {
					return Lists.newArrayList(git.listBranches());
				} else {
					return Collections.emptyList();
				}
			}
		};
		
		defaultBranchName = GitUtils.getDefaultBranch(getProject().code());
		form.add(new DropDownChoiceElement<String>(
				"defaultBranch", 
				"Default Branch",
				new PropertyModel<String>(this, "defaultBranchName"),
				branchesModel)
				.setHelp("Set default branch for browsing. By default, it is \"master\"." ));
		
		form.add(new AjaxButton("submit", form) {
			@Override
			protected void onError(AjaxRequestTarget target, Form<?> form) {
				target.add(form);
			}
			
			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				Project project = getProject();
				boolean nameChanged = !Objects.equal(project.getName(), projectName);
				if (nameChanged) {
					project.setName(projectName);
				}
//				project.setDefaultBranchName(defaultBranch);
				
				if (!Strings.isNullOrEmpty(defaultBranchName)) {
					project.code().updateDefaultBranch(defaultBranchName);
				}
				
				Gitop.getInstance(ProjectManager.class).save(project);
				
				if (nameChanged) {
					setResponsePage(RepositoryOptionsPage.class, PageSpec.forProject(project));
				} else {
					form.success("Project " + project + " has been updated.");
					target.add(form);
				}
			}
		});
		
		add(new AjaxConfirmLink<Void>("deletelink", Model.of(
				"<p>Are you sure you want to delete project: " 
						+ getProject() 
						+ "?</p><b>NOTE:</b> Once you delete this project, there is no going back")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				Gitop.getInstance(ProjectManager.class).delete(getProject());
				setResponsePage(AccountHomePage.class, PageSpec.forUser(getAccount()));
			}
		});
	}

	@Override
	protected String getPageTitle() {
		return "Options - " + getProject();
	}
}
