package com.pmease.gitop.web.page.project.source.commits;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.pmease.commons.git.Commit;
import com.pmease.gitop.core.Gitop;
import com.pmease.gitop.core.manager.CommitCommentManager;
import com.pmease.gitop.model.Project;
import com.pmease.gitop.web.common.datatype.DataTypes;
import com.pmease.gitop.web.component.commit.CommitMessagePanel;
import com.pmease.gitop.web.component.commit.CommitMetaPanel;
import com.pmease.gitop.web.component.link.GitPersonLink;
import com.pmease.gitop.web.component.link.GitPersonLink.Mode;
import com.pmease.gitop.web.git.GitUtils;
import com.pmease.gitop.web.page.project.api.GitPerson;
import com.pmease.gitop.web.page.project.source.commit.SourceCommitPage;
import com.pmease.gitop.web.page.project.source.tree.SourceTreePage;

@SuppressWarnings("serial")
public class CommitsPanel extends Panel {
	
	private final IModel<Project> projectModel;
	private final IModel<Multimap<Date, Commit>> groupsModel;
	private final IModel<Map<String, Integer>> commentStatsModel;
	
	public CommitsPanel(String id, 
			IModel<List<Commit>> commitsModel, 
			IModel<Project> projectModel) {
		super(id, commitsModel);
		
		this.projectModel = projectModel;
		
		groupsModel = new LoadableDetachableModel<Multimap<Date, Commit>>() {

			@Override
			protected Multimap<Date, Commit> load() {
				List<Commit> commits = getCommits();
				
				LinkedListMultimap<Date, Commit> groups = LinkedListMultimap.<Date, Commit>create();
				for (Commit commit : commits) {
					Date date = commit.getAuthor().getDate();
					date = DateUtils.round(date, Calendar.DAY_OF_MONTH);
					groups.put(date, commit);
				}
				
				return groups;
			}
		};
		
		commentStatsModel = new LoadableDetachableModel<Map<String, Integer>>() {

			@Override
			protected Map<String, Integer> load() {
				// XXX: this may slow, performance tuning later
				return Gitop.getInstance(CommitCommentManager.class).getCommitCommentStats(getProject());
			}
			
		};
	}

	private Multimap<Date, Commit> getGroups() {
		return groupsModel.getObject();
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(new ListView<Date>("groups", new AbstractReadOnlyModel<List<Date>>() {

			@Override
			public List<Date> getObject() {
				List<Date> dates = Lists.newArrayList(getGroups().keySet());
				Collections.sort(dates);
				Collections.reverse(dates);
				return dates;
			}
		}) {

			@Override
			protected void populateItem(ListItem<Date> item) {
				final Date date = item.getModelObject();
				item.add(new Label("date", DataTypes.DATE.asString(date, "MMM dd, yyyy")));
				item.add(new ListView<Commit>("rows", new AbstractReadOnlyModel<List<Commit>>() {

					@Override
					public List<Commit> getObject() {
						Multimap<Date, Commit> groups = getGroups();
						return Lists.newArrayList(groups.get(date));
					}
				}) {

					@Override
					protected void populateItem(ListItem<Commit> item) {
						Commit commit = item.getModelObject();
						
						item.add(new GitPersonLink("avatar", 
													Model.of(GitPerson.of(commit.getAuthor())), 
													Mode.AVATAR)
									.enableTooltip("right"));
						
						item.add(new CommitMessagePanel("message", item.getModel(), projectModel));
						item.add(new CommitMetaPanel("meta", item.getModel()));
						
						AbstractLink shalink =new BookmarkablePageLink<Void>(
								"shalink", SourceCommitPage.class, 
								SourceCommitPage.newParams(getProject(), commit.getHash()));
						shalink.add(new Label("sha", GitUtils.abbreviateSHA(commit.getHash())));
						int commentsCount = getCommentCount(commit.getHash());
						shalink.add(new Label("commentsCount", commentsCount).setVisibilityAllowed(commentsCount > 0));
						
						item.add(shalink);
						item.add(new BookmarkablePageLink<Void>("treelink", 
								SourceTreePage.class, 
								SourceTreePage.newParams(getProject(), 
														 commit.getHash())));
					}
					
				});
			}
			
		});
	}
	
	private int getCommentCount(String sha) {
		Map<String, Integer> map = commentStatsModel.getObject();
		if (map.containsKey(sha)) {
			return map.get(sha);
		} else {
			return 0;
		}
	}
	
	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		
		response.render(OnDomReadyHeaderItem.forScript("gitop.commitMessage.toggle('.title-message .detailed-toggle');"));
				
	}
	
	private Project getProject() {
		return projectModel.getObject();
	}
	
	@SuppressWarnings("unchecked")
	private List<Commit> getCommits() {
		return (List<Commit>) getDefaultModelObject();
	}
	
	@Override
	public void onDetach() {
		if (projectModel != null) {
			projectModel.detach();
		}
		
		if (groupsModel != null) {
			groupsModel.detach();
		}
		
		if (commentStatsModel != null) {
			commentStatsModel.detach();
		}
		
		super.onDetach();
	}
}
