package io.onedev.server.web.page.project.setting.issuecustomfields;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

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
import io.onedev.server.util.input.Input;
import io.onedev.server.util.input.InputContext;
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

	@Nullable
	private Input getField(String fieldName) {
		for (Input field: getFields()) {
			if (field.getName().equals(fieldName))
				return field;
		}
		return null;
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		FieldSpecBean bean = new FieldSpecBean();
		if (fieldIndex != -1)
			bean.setFieldSpec(SerializationUtils.clone(getFields().get(fieldIndex)));

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

				Input field = bean.getFieldSpec();
				if (fieldIndex != -1) { 
					Input oldField = getFields().get(fieldIndex);
					if (!field.getName().equals(oldField.getName()) && getField(field.getName()) != null) {
						editor.getErrorContext(new PathSegment.Property("fieldSpec"))
								.getErrorContext(new PathSegment.Property("name"))
								.addError("This name has already been used by another field");
					}
				} else if (getField(field.getName()) != null) {
					editor.getErrorContext(new PathSegment.Property("fieldSpec"))
							.getErrorContext(new PathSegment.Property("name"))
							.addError("This name has already been used by another field");
				}

				if (!editor.hasErrors(true)) {
					if (fieldIndex != -1) {
						Input oldField = getFields().get(fieldIndex);
						if (!field.getName().equals(oldField.getName())) {
							for (Input eachField: getFields())
								eachField.onInputRename(oldField.getName(), field.getName());
						}
						getFields().set(fieldIndex, bean.getFieldSpec());
					} else {
						getFields().add(bean.getFieldSpec());
					}
					getProject().getIssueWorkflow().setFieldSpecs(getFields());
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

	protected abstract List<Input> getFields();
	
	protected abstract void onSave(AjaxRequestTarget target);
	
	protected abstract void onCancel(AjaxRequestTarget target);

	@Override
	public Map<String, Input> getInputs() {
		Map<String, Input> inputs = new LinkedHashMap<>();
		int currentIndex = 0;
		for (Input field: getFields()) {
			if (currentIndex != fieldIndex)
				inputs.put(field.getName(), field);
			currentIndex++;
		}
		return inputs;
	}
	
	@Override
	public List<String> getScenarios() {
		return getProject().getIssueWorkflow().getScenarios();
	}

}
