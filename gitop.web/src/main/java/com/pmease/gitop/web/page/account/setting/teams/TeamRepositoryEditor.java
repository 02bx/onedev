package com.pmease.gitop.web.page.account.setting.teams;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.util.WildcardListModel;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.pmease.gitop.core.Gitop;
import com.pmease.gitop.core.manager.AuthorizationManager;
import com.pmease.gitop.core.manager.RepositoryManager;
import com.pmease.gitop.model.Authorization;
import com.pmease.gitop.model.Repository;
import com.pmease.gitop.model.Team;
import com.pmease.gitop.web.common.wicket.form.FeedbackPanel;
import com.pmease.gitop.web.component.choice.ProjectMultiChoice;
import com.pmease.gitop.web.component.link.ProjectHomeLink;
import com.pmease.gitop.web.model.RepositoryModel;
import com.vaynberg.wicket.select2.ChoiceProvider;
import com.vaynberg.wicket.select2.Response;

@SuppressWarnings("serial")
public class TeamRepositoryEditor extends Panel {

	public TeamRepositoryEditor(String id, final IModel<Team> model) {
		super(id, model);
		
		setOutputMarkupId(true);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(newProjectsForm());
		
		WebMarkupContainer projectsDiv = new WebMarkupContainer("projects");
		projectsDiv.setOutputMarkupId(true);
		add(projectsDiv);
		
		final IModel<List<Authorization>> model = 
				new LoadableDetachableModel<List<Authorization>>() {

					@Override
					protected List<Authorization> load() {
						AuthorizationManager am = Gitop.getInstance(AuthorizationManager.class);
						return am.query(Restrictions.eq("team", getTeam()));
					}
		};
		
		projectsDiv.add(new Label("total", new AbstractReadOnlyModel<Integer>() {

			@Override
			public Integer getObject() {
				return model.getObject().size();
			}
			
		}));
		
		ListView<Authorization> projectsView = new ListView<Authorization>("repo", model) {

			@Override
			protected void populateItem(ListItem<Authorization> item) {
				Authorization a = item.getModelObject();
				item.add(new ProjectHomeLink("link", new RepositoryModel(a.getProject())));
				final Long id = a.getId();
				item.add(new AjaxLink<Void>("removelink") {

					@Override
					public void onClick(AjaxRequestTarget target) {
						AuthorizationManager am = Gitop.getInstance(AuthorizationManager.class);
						Authorization authorization = am.get(id);
						am.delete(authorization);
						// TODO: add notification
						//
//						Messenger.warn("Project [" + authorization.getProject() 
//								+ "] is removed from team <b>[" 
//								+ authorization.getTeam().getName() + "]</b>").run(target);
						onProjectsChanged(target);
					}
				});
			}
		};
		
		projectsDiv.add(projectsView);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Component newProjectsForm() {
		Form<?> form = new Form<Void>("reposForm");
		form.add(new FeedbackPanel("feedback", form));
		final IModel<Collection<Repository>> reposModel = new WildcardListModel(new ArrayList<Repository>());
		form.add(new ProjectMultiChoice("repochoice", reposModel, new ProjectChoiceProvider()));
		
		form.add(new AjaxButton("submit", form) {
			@Override
			protected void onError(AjaxRequestTarget target, Form<?> form) {
				target.add(form);
			}
			
			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				Collection<Repository> projects = reposModel.getObject();
				AuthorizationManager am = Gitop.getInstance(AuthorizationManager.class);
				for (Repository each : projects) {
					Authorization authorization = new Authorization();
					authorization.setTeam(getTeam());
					authorization.setProject(each);
					am.save(authorization);
				}
				
				reposModel.setObject(new ArrayList<Repository>());
				target.add(form);
				onProjectsChanged(target);
			}
		});
		
		return form;
	}

	class ProjectChoiceProvider extends ChoiceProvider<Repository> {
		
		@Override
		public void query(String term, int page, Response<Repository> response) {
			RepositoryManager pm = Gitop.getInstance(RepositoryManager.class);
			int first = page * 25;
			List<Repository> projects = 
					pm.query(
							new Criterion[] {
									Restrictions.eq("owner", getTeam().getOwner()),
									Restrictions.like("name", term, MatchMode.START).ignoreCase()
							}, new Order[] {
									Order.asc("name")
							}, first, 25);
			
			if (projects.isEmpty()) {
				response.addAll(projects);
				return;
			}
			
			List<Authorization> authorizations =
					Gitop.getInstance(AuthorizationManager.class)
					.query(Restrictions.eq("team", getTeam()));
			
			List<Repository> result = Lists.newArrayList();
			for (Repository each : projects) {
				if (!in(each, authorizations)) {
					result.add(each);
				}
			}
			
			response.addAll(result);
		}

		private boolean in(Repository project, List<Authorization> authorizations) {
			for (Authorization each : authorizations) {
				if (Objects.equal(project, each.getProject())) {
					return true;
				}
			}
			
			return false;
		}
		
		@Override
		public void toJson(Repository choice, JSONWriter writer) throws JSONException {
			writer.key("id").value(choice.getId())
				  .key("owner").value(choice.getOwner().getName())
				  .key("name").value(choice.getName());
		}

		@Override
		public Collection<Repository> toChoices(Collection<String> ids) {
			List<Repository> list = Lists.newArrayList();
			RepositoryManager pm = Gitop.getInstance(RepositoryManager.class);
			for (String each : ids) {
				Long id = Long.valueOf(each);
				list.add(pm.get(id));
			}
			
			return list;
		}
	}
	
	private void onProjectsChanged(AjaxRequestTarget target) {
		target.add(get("projects"));
	}
	
	private Team getTeam() {
		return (Team) getDefaultModelObject();
	}
}
