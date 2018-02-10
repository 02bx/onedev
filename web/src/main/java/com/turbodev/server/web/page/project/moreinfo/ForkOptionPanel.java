package com.turbodev.server.web.page.project.moreinfo;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.turbodev.server.TurboDev;
import com.turbodev.server.manager.ProjectManager;
import com.turbodev.server.model.Project;
import com.turbodev.server.security.SecurityUtils;
import com.turbodev.server.web.editable.BeanContext;
import com.turbodev.server.web.editable.BeanEditor;
import com.turbodev.server.web.editable.PathSegment;
import com.turbodev.server.web.page.project.blob.ProjectBlobPage;

@SuppressWarnings("serial")
abstract class ForkOptionPanel extends Panel {

	private final IModel<Project> projectModel;
	
	public ForkOptionPanel(String id, IModel<Project> projectModel) {
		super(id);
		this.projectModel = projectModel;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		Project project = new Project();
		project.setForkedFrom(getProject());
		project.setName(getProject().getName() + "." + SecurityUtils.getUser().getName());
		
		BeanEditor<?> editor = BeanContext.editBean("editor", project);
		
		Form<?> form = new Form<Void>("form");
		form.setOutputMarkupId(true);
		form.add(editor);
		
		form.add(new AjaxButton("save") {

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				super.onSubmit(target, form);
				ProjectManager projectManager = TurboDev.getInstance(ProjectManager.class);
				Project projectWithSameName = projectManager.find(project.getName());
				if (projectWithSameName != null) {
					editor.getErrorContext(new PathSegment.Property("name"))
							.addError("This name has already been used by another project");
					target.add(form);
				} else {
					projectManager.fork(getProject(), project);
					Session.get().success("Repository forked");
					setResponsePage(ProjectBlobPage.class, ProjectBlobPage.paramsOf(project));
				}
			}

			@Override
			protected void onError(AjaxRequestTarget target, Form<?> form) {
				super.onError(target, form);
				target.add(form);
			}
			
		});
		form.add(new AjaxLink<Void>("cancel") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				onClose(target);
			}
			
		});
		form.add(new AjaxLink<Void>("close") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				onClose(target);
			}
			
		});
		
		add(form);
	}
	
	private Project getProject() {
		return projectModel.getObject();
	}

	@Override
	protected void onDetach() {
		projectModel.detach();
		super.onDetach();
	}

	protected abstract void onClose(AjaxRequestTarget target);
	
}
