package com.pmease.gitplex.web.page.depot.setting.gatekeeper;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.pmease.commons.util.ReflectionUtils;
import com.pmease.commons.wicket.component.modal.ModalPanel;
import com.pmease.commons.wicket.editable.EditableUtils;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.entity.Depot;
import com.pmease.gitplex.core.gatekeeper.GateKeeper;
import com.pmease.gitplex.core.manager.DepotManager;
import com.pmease.gitplex.web.page.depot.setting.DepotSettingPage;

@SuppressWarnings("serial")
public class GateKeeperPage extends DepotSettingPage {

	private static final String CONTAINER_ID = "gateKeeperSetting";
	
	public GateKeeperPage(PageParameters params) {
		super(params);
	}

	private WebMarkupContainer newContent() {
		WebMarkupContainer content = new WebMarkupContainer(CONTAINER_ID);
		content.setOutputMarkupId(true);
		
		content.add(new ListView<GateKeeper>("gateKeepers", getDepot().getGateKeepers()) {

			@Override
			protected void populateItem(final ListItem<GateKeeper> item) {
				item.add(new GateKeeperPanel("gateKeeper", item.getModelObject()) {

					@Override
					protected void onDelete(AjaxRequestTarget target) {
						getDepot().getGateKeepers().remove(item.getIndex());
						onGateKeeperChanged(target);
					}

					@Override
					protected void onChange(AjaxRequestTarget target, GateKeeper gateKeeper) {
						getDepot().getGateKeepers().set(item.getIndex(), gateKeeper);
						onGateKeeperChanged(target);
					}
					
				});
			}
			
		});
		content.add(new GateKeeperLink("gateKeeperDropdownTrigger") {

			@Override
			protected void onSelect(AjaxRequestTarget target, Class<? extends GateKeeper> gateKeeperClass) {
				final GateKeeper gateKeeper = ReflectionUtils.instantiateClass(gateKeeperClass);
				if (EditableUtils.isDefaultInstanceValid(gateKeeperClass)) {
					getDepot().getGateKeepers().add(gateKeeper);
					onGateKeeperChanged(target);
				} else {
					new ModalPanel(target) {

						@Override
						protected Component newContent(String id) {
							return new GateKeeperEditor(id, gateKeeper) {

								@Override
								protected void onCancel(AjaxRequestTarget target) {
									close(target);
								}

								@Override
								protected void onSave(AjaxRequestTarget target, GateKeeper gateKeeper) {
									close(target);
									getDepot().getGateKeepers().add(gateKeeper);
									onGateKeeperChanged(target);
								}
								
							};
						}
						
					};
				}
			}
			
		});
		content.add(new WebMarkupContainer("gateKeeperPanel").setVisible(false));
		content.add(new WebMarkupContainer("gateKeeperModal").setOutputMarkupPlaceholderTag(true).setVisible(false));
		return content;
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(newContent());
	}
	
	private void onGateKeeperChanged(AjaxRequestTarget target) {
		GitPlex.getInstance(DepotManager.class).save(getDepot(), null, null);
		replace(newContent());
		target.add(get(CONTAINER_ID));
	}

	@Override
	protected String getPageTitle() {
		return "Gate Keepers - " + getDepot();
	}

	@Override
	protected void onSelect(AjaxRequestTarget target, Depot depot) {
		setResponsePage(GateKeeperPage.class, GateKeeperPage.paramsOf(depot));
	}

}
