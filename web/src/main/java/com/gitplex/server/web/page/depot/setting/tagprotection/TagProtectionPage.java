package com.gitplex.server.web.page.depot.setting.tagprotection;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.gitplex.server.GitPlex;
import com.gitplex.server.manager.DepotManager;
import com.gitplex.server.model.Depot;
import com.gitplex.server.model.support.TagProtection;
import com.gitplex.server.web.behavior.sortable.SortBehavior;
import com.gitplex.server.web.behavior.sortable.SortPosition;
import com.gitplex.server.web.page.depot.setting.DepotSettingPage;

@SuppressWarnings("serial")
public class TagProtectionPage extends DepotSettingPage {

	private WebMarkupContainer container;
	
	public TagProtectionPage(PageParameters params) {
		super(params);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		container = new WebMarkupContainer("tagProtectionSetting");
		container.setOutputMarkupId(true);
		add(container);
		container.add(new ListView<TagProtection>("protections", new AbstractReadOnlyModel<List<TagProtection>>() {

			@Override
			public List<TagProtection> getObject() {
				return getDepot().getTagProtections();
			}
		}) {

			@Override
			protected void populateItem(final ListItem<TagProtection> item) {
				item.add(new TagProtectionPanel("protection", item.getModelObject()) {

					@Override
					protected void onDelete(AjaxRequestTarget target) {
						getDepot().getTagProtections().remove(item.getIndex());
						GitPlex.getInstance(DepotManager.class).save(getDepot(), null, null);
						target.add(container);
					}

					@Override
					protected void onSave(AjaxRequestTarget target, TagProtection protection) {
						getDepot().getTagProtections().set(item.getIndex(), protection);
						GitPlex.getInstance(DepotManager.class).save(getDepot(), null, null);
					}
					
				});
			}
			
		});
		
		container.add(new SortBehavior() {
			
			@Override
			protected void onSort(AjaxRequestTarget target, SortPosition from, SortPosition to) {
				List<TagProtection> protections = getDepot().getTagProtections();
				TagProtection protection = protections.get(from.getItemIndex());
				protections.set(from.getItemIndex(), protections.set(to.getItemIndex(), protection));
				GitPlex.getInstance(DepotManager.class).save(getDepot(), null, null);
				
				target.add(container);
			}
			
		}.handle(".drag-handle").items("li.protection"));
		
		container.add(newAddNewFrag());
	}

	private Component newAddNewFrag() {
		Fragment fragment = new Fragment("newProtection", "addNewLinkFrag", this); 
		fragment.add(new AjaxLink<Void>("link") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				Fragment fragment = new Fragment("newProtection", "editNewFrag", getPage());
				fragment.setOutputMarkupId(true);
				fragment.add(new TagProtectionEditor("editor", new TagProtection()) {

					@Override
					protected void onSave(AjaxRequestTarget target, TagProtection protection) {
						getDepot().getTagProtections().add(protection);
						GitPlex.getInstance(DepotManager.class).save(getDepot(), null, null);
						container.replace(newAddNewFrag());
						target.add(container);
					}

					@Override
					protected void onCancel(AjaxRequestTarget target) {
						Component newAddNewFrag = newAddNewFrag();
						container.replace(newAddNewFrag);
						target.add(newAddNewFrag);
					}
					
				});
				container.replace(fragment);
				target.add(fragment);
			}
			
		});
		fragment.setOutputMarkupId(true);
		return fragment;
	}
	
	@Override
	protected void onSelect(AjaxRequestTarget target, Depot depot) {
		setResponsePage(TagProtectionPage.class, TagProtectionPage.paramsOf(depot));
	}
	
}
