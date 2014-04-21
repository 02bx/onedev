package com.pmease.gitop.web.page.repository.source.blob;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.EnclosureContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import com.google.common.collect.Lists;
import com.pmease.commons.git.GitPerson;
import com.pmease.commons.wicket.behavior.dropdown.DropdownBehavior;
import com.pmease.commons.wicket.behavior.dropdown.DropdownPanel;
import com.pmease.gitop.web.component.link.PersonLink;
import com.pmease.gitop.web.component.link.PersonLink.Mode;

import de.agilecoders.wicket.core.markup.html.bootstrap.components.TooltipConfig;
import de.agilecoders.wicket.jquery.JQuery;

@SuppressWarnings("serial")
public class ContributorsPanel extends Panel {

	private final static int MAX_DISPLAYED_COMMITTERS = 20;
	
	public ContributorsPanel(String id, IModel<List<GitPerson>> model) {
		super(id, model);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(new Label("contributorStat", new AbstractReadOnlyModel<Integer>() {

			@Override
			public Integer getObject() {
				return getContributors().size();
			}
			
		}));
		
		ListView<GitPerson> contributorsView = new ListView<GitPerson>("contributors", 
				new AbstractReadOnlyModel<List<GitPerson>>() {

			@Override
			public List<GitPerson> getObject() {
				List<GitPerson> committers = getContributors();
				if (committers.size() > MAX_DISPLAYED_COMMITTERS) {
					return Lists.newArrayList(committers.subList(0, MAX_DISPLAYED_COMMITTERS));
				} else {
					return committers;
				}
			}
			
		}) {

			@Override
			protected void populateItem(ListItem<GitPerson> item) {
				GitPerson person = item.getModelObject();
				item.add(new PersonLink("link", person, Mode.AVATAR).withTooltipConfig(new TooltipConfig()));
			}
		};
		
		add(contributorsView);
		
		WebMarkupContainer more = new WebMarkupContainer("more") {
			@Override
			protected void onConfigure() {
				super.onConfigure();
				
				this.setVisibilityAllowed(getContributors().size() > MAX_DISPLAYED_COMMITTERS);
			}
		};
		
		DropdownPanel dropdownPanel = new DropdownPanel("moreDropdown", false) {

			@SuppressWarnings("unchecked")
			@Override
			protected Component newContent(String id) {
				Fragment frag = new Fragment(id, "committers-dropdown", ContributorsPanel.this);
				frag.add(new ListView<GitPerson>("committers", 
						(IModel<List<GitPerson>>) ContributorsPanel.this.getDefaultModel()) {

					@Override
					protected void populateItem(ListItem<GitPerson> item) {
						item.add(new PersonLink("committer", item.getModelObject(), Mode.NAME_AND_AVATAR));
					}
				});
				
				return frag;
			}
		};
		more.add(new DropdownBehavior(dropdownPanel).clickMode(true));
		
		EnclosureContainer moreContainer = new EnclosureContainer("morecontainer", more);
		moreContainer.add(more);
		moreContainer.add(dropdownPanel);
		
		add(moreContainer);
	}
	
	@SuppressWarnings("unchecked")
	private List<GitPerson> getContributors() {
		return (List<GitPerson>) getDefaultModelObject();
	}
	
	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		
		response.render(OnDomReadyHeaderItem.forScript(JQuery.$(this, ".has-tip").chain("tooltip").get()));
	}
}
