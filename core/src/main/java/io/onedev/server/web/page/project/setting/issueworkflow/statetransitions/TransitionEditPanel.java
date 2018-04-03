package io.onedev.server.web.page.project.setting.issueworkflow.statetransitions;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.cycle.RequestCycle;

import io.onedev.server.OneDev;
import io.onedev.server.manager.ProjectManager;
import io.onedev.server.model.Project;
import io.onedev.server.model.support.issueworkflow.IssueWorkflow;
import io.onedev.server.model.support.issueworkflow.StateTransition;
import io.onedev.server.web.editable.BeanContext;
import io.onedev.server.web.editable.BeanEditor;
import io.onedev.server.web.editable.PathSegment;
import io.onedev.server.web.page.project.ProjectPage;
import io.onedev.server.web.util.ajaxlistener.ConfirmLeaveListener;

@SuppressWarnings("serial")
abstract class TransitionEditPanel extends Panel {

	private final int transitionIndex;
	
	public TransitionEditPanel(String id, int transitionIndex) {
		super(id);
	
		this.transitionIndex = transitionIndex;
	}
	
	private Project getProject() {
		ProjectPage page = (ProjectPage) getPage();
		return page.getProject();
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		StateTransition transition;
		if (transitionIndex != -1)
			transition = SerializationUtils.clone(getWorkflow().getStateTransitions().get(transitionIndex));
		else
			transition = new StateTransition();

		Form<?> form = new Form<Void>("form") {

			@Override
			protected void onError() {
				super.onError();
				RequestCycle.get().find(AjaxRequestTarget.class).add(this);
			}
			
		};
		
		form.add(new AjaxLink<Void>("close") {

			@Override
			protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
				super.updateAjaxAttributes(attributes);
				attributes.getAjaxCallListeners().add(new ConfirmLeaveListener(TransitionEditPanel.this));
			}

			@Override
			public void onClick(AjaxRequestTarget target) {
				onCancel(target);
			}
			
		});
		
		BeanEditor editor = BeanContext.editBean("editor", transition);
		form.add(editor);
		form.add(new AjaxButton("save") {

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				super.onSubmit(target, form);

				Collection<String> states = new ArrayList<>(transition.getFromStates());
				states.retainAll(transition.getToStates());
				if (!states.isEmpty()) {
					String error = "Can not do transition between same states";
					editor.getErrorContext(new PathSegment.Property("fromStates")).addError(error);
					editor.getErrorContext(new PathSegment.Property("toStates")).addError(error);
					target.add(form);
				} else {
					if (transitionIndex != -1)
						getWorkflow().getStateTransitions().set(transitionIndex, transition);
					else 
						getWorkflow().getStateTransitions().add(transition);
					getProject().setIssueWorkflow(getWorkflow());
					OneDev.getInstance(ProjectManager.class).save(getProject());
					onSave(target);
				}
			}
			
		});
		
		form.add(new AjaxLink<Void>("cancel") {

			@Override
			protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
				super.updateAjaxAttributes(attributes);
				attributes.getAjaxCallListeners().add(new ConfirmLeaveListener(TransitionEditPanel.this));
			}

			@Override
			public void onClick(AjaxRequestTarget target) {
				onCancel(target);
			}
			
		});
		form.setOutputMarkupId(true);
		
		add(form);
	}

	protected abstract IssueWorkflow getWorkflow();
	
	protected abstract void onSave(AjaxRequestTarget target);
	
	protected abstract void onCancel(AjaxRequestTarget target);

}
