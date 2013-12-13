package com.pmease.gitop.web.page.project.settings;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.pmease.commons.editable.EditableUtils;
import com.pmease.commons.util.ReflectionUtils;
import com.pmease.commons.wicket.behavior.dropdown.DropdownBehavior;
import com.pmease.commons.wicket.behavior.modal.ModalPanel;
import com.pmease.gitop.core.Gitop;
import com.pmease.gitop.core.manager.ProjectManager;
import com.pmease.gitop.model.gatekeeper.DefaultGateKeeper;
import com.pmease.gitop.model.gatekeeper.GateKeeper;
import com.pmease.gitop.web.page.project.settings.gatekeeper.GateKeeperDropdown;
import com.pmease.gitop.web.page.project.settings.gatekeeper.GateKeeperEditor;
import com.pmease.gitop.web.page.project.settings.gatekeeper.GateKeeperPanel;

@SuppressWarnings("serial")
public class GateKeeperSettingPage extends AbstractProjectSettingPage {

	public GateKeeperSettingPage(PageParameters params) {
		super(params);
	}

	private WebMarkupContainer newContent() {
		WebMarkupContainer content = new WebMarkupContainer("content");
		content.setOutputMarkupId(true);
		
		if (getProject().getGateKeeper() instanceof DefaultGateKeeper) {
			GateKeeperDropdown gateKeeperDropdown = new GateKeeperDropdown("gateKeeperDropdown") {

				@Override
				protected void onSelect(AjaxRequestTarget target, Class<? extends GateKeeper> gateKeeperClass) {
					final GateKeeper gateKeeper = ReflectionUtils.instantiateClass(gateKeeperClass);
					if (EditableUtils.isDefaultInstanceValid(gateKeeperClass)) {
						getProject().setGateKeeper(gateKeeper);
						onGateKeeperChange(target);
					} else {
						ModalPanel modalPanel = new ModalPanel("gateKeeperModal") {

							@Override
							protected Component newContent(String id) {
								return new GateKeeperEditor(id, gateKeeper) {

									@Override
									protected void onCancel(AjaxRequestTarget target) {
										close(target);
									}

									@Override
									protected void onSave(AjaxRequestTarget target) {
										close(target);
										getProject().setGateKeeper(gateKeeper);
										onGateKeeperChange(target);
									}
									
								};
							}
							
						};
						((WebMarkupContainer)GateKeeperSettingPage.this.get("content")).replace(modalPanel);
						target.add(modalPanel);
					}
				}
				
			};
			content.add(gateKeeperDropdown);
			DropdownBehavior behavior = new DropdownBehavior(gateKeeperDropdown);
			behavior.alignWithCursor(10, 10);
			content.add(new WebMarkupContainer("gateKeeperDropdownTrigger").add(behavior));
			content.add(new WebMarkupContainer("gateKeeperPanel").setVisible(false));
		} else {
			content.add(new WebMarkupContainer("gateKeeperDropdown").setVisible(false));
			content.add(new WebMarkupContainer("gateKeeperDropdownTrigger").setVisible(false));
			content.add(new GateKeeperPanel("gateKeeperPanel", getProject().getGateKeeper()) {

				@Override
				protected void onDelete(AjaxRequestTarget target) {
					getProject().setGateKeeper(new DefaultGateKeeper());
					onGateKeeperChange(target);
				}

				@Override
				protected void onSave(AjaxRequestTarget target, GateKeeper gateKeeper) {
					getProject().setGateKeeper(gateKeeper);
					onGateKeeperChange(target);
				}
				
			});
		}
		content.add(new WebMarkupContainer("gateKeeperModal").setOutputMarkupPlaceholderTag(true).setVisible(false));
		return content;
	}
	
	@Override
	protected void onPageInitialize() {
		super.onPageInitialize();
		
		add(newContent());
	}
	
	private void onGateKeeperChange(AjaxRequestTarget target) {
		Gitop.getInstance(ProjectManager.class).save(getProject());
		replace(newContent());
		target.add(get("content"));
	}

	@Override
	protected Category getCategory() {
		return Category.GATE_KEEPER;
	}

}
