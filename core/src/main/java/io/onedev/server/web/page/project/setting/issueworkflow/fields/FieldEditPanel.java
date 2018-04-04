package io.onedev.server.web.page.project.setting.issueworkflow.fields;

import java.util.ArrayList;
import java.util.List;

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
import io.onedev.server.util.inputspec.InputContext;
import io.onedev.server.util.inputspec.InputSpec;
import io.onedev.server.web.editable.BeanContext;
import io.onedev.server.web.editable.BeanEditor;
import io.onedev.server.web.editable.PathSegment;
import io.onedev.server.web.page.project.ProjectPage;
import io.onedev.server.web.util.ajaxlistener.ConfirmLeaveListener;

@SuppressWarnings("serial")
abstract class FieldEditPanel extends Panel implements InputContext {

	private final int fieldIndex;
	
	public FieldEditPanel(String id, int fieldIndex) {
		super(id);
	
		this.fieldIndex = fieldIndex;
	}
	
	private Project getProject() {
		ProjectPage page = (ProjectPage) getPage();
		return page.getProject();
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		FieldBean bean = new FieldBean();
		if (fieldIndex != -1)
			bean.setField(SerializationUtils.clone(getWorkflow().getFields().get(fieldIndex)));

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
				attributes.getAjaxCallListeners().add(new ConfirmLeaveListener(FieldEditPanel.this));
			}

			@Override
			public void onClick(AjaxRequestTarget target) {
				onCancel(target);
			}
			
		});
		
		BeanEditor editor = BeanContext.editBean("editor", bean);
		form.add(editor);
		form.add(new AjaxButton("save") {

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				super.onSubmit(target, form);

				InputSpec field = bean.getField();
				if (fieldIndex != -1) { 
					InputSpec oldField = getWorkflow().getFields().get(fieldIndex);
					if (!field.getName().equals(oldField.getName()) && getWorkflow().getField(field.getName()) != null) {
						editor.getErrorContext(new PathSegment.Property("field"))
								.getErrorContext(new PathSegment.Property("name"))
								.addError("This name has already been used by another field");
					}
				} else if (getWorkflow().getField(field.getName()) != null) {
					editor.getErrorContext(new PathSegment.Property("field"))
							.getErrorContext(new PathSegment.Property("name"))
							.addError("This name has already been used by another field");
				}

				if (!editor.hasErrors(true)) {
					if (fieldIndex != -1) {
						InputSpec oldField = getWorkflow().getFields().get(fieldIndex);
						if (!field.getName().equals(oldField.getName()))
							getWorkflow().onFieldRename(oldField.getName(), field.getName());
						getWorkflow().getFields().set(fieldIndex, bean.getField());
					} else {
						getWorkflow().getFields().add(bean.getField());
					}
					getProject().setIssueWorkflow(getWorkflow());
					OneDev.getInstance(ProjectManager.class).save(getProject());
					onSave(target);
				} else {
					target.add(form);
				}
			}
			
		});
		
		form.add(new AjaxLink<Void>("cancel") {

			@Override
			protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
				super.updateAjaxAttributes(attributes);
				attributes.getAjaxCallListeners().add(new ConfirmLeaveListener(FieldEditPanel.this));
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

	@Override
	public List<String> getInputNames() {
		List<String> inputNames = new ArrayList<>();
		int currentIndex = 0;
		for (InputSpec field: getWorkflow().getFields()) {
			if (currentIndex != fieldIndex)
				inputNames.add(field.getName());
			currentIndex++;
		}
		return inputNames;
	}
	
	@Override
	public InputSpec getInput(String inputName) {
		return getWorkflow().getField(inputName);
	}
}
