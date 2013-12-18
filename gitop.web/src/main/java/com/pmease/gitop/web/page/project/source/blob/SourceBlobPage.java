package com.pmease.gitop.web.page.project.source.blob;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.pmease.commons.git.Commit;
import com.pmease.commons.git.Git;
import com.pmease.commons.wicket.behavior.collapse.CollapseBehavior;
import com.pmease.gitop.web.component.label.AgeLabel;
import com.pmease.gitop.web.component.link.GitUserLink;
import com.pmease.gitop.web.page.project.api.GitPerson;
import com.pmease.gitop.web.page.project.source.AbstractFilePage;
import com.pmease.gitop.web.page.project.source.component.SourceBreadcrumbPanel;
import com.pmease.gitop.web.util.UrlUtils;

@SuppressWarnings("serial")
public class SourceBlobPage extends AbstractFilePage {

	private final IModel<Commit> lastCommitModel;
	private final IModel<List<GitPerson>> committersModel;
	
	
	public SourceBlobPage(PageParameters params) {
		super(params);
		
		lastCommitModel = new LoadableDetachableModel<Commit>() {

			@Override
			protected Commit load() {
				Git git = getProject().code();
				List<String> paths = getPaths();
				List<Commit> commits = git.log(null, getRevision(), Joiner.on("/").join(paths), 1);
				return Iterables.getFirst(commits, null);
			}
		};
		
		committersModel = new LoadableDetachableModel<List<GitPerson>>() {

			@Override
			protected List<GitPerson> load() {
				Git git = getProject().code();
				List<Commit> commits = git.log(null, getRevision(), getFilePath(), 0);
				Set<GitPerson> users = Sets.newHashSet();
				for (Commit each : commits) {
					GitPerson person = new GitPerson(each.getAuthor().getName(), each.getAuthor().getEmail());
					users.add(person);
				}
				
				return Lists.newArrayList(users);
			}
			
		};
		
		add(new SourceBlobPanel("source", projectModel, revisionModel, pathsModel));
	}

	@Override
	public void onPageInitialize() {
		super.onPageInitialize();
		
		add(new SourceBreadcrumbPanel("breadcrumb", projectModel, revisionModel, pathsModel));
		
		add(new Label("shortMessage", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return getLastCommit().getSubject();
			}
		}));
		
		Label detailedMsg = new Label("detailedMessage", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return getLastCommit().getMessage();
			}
		}) {
			@Override
			protected void onConfigure() {
				super.onConfigure();
				
				if (Objects.equal(getLastCommit().getSubject(), getLastCommit().getMessage())) {
					this.setVisibilityAllowed(false);
				}
			}
		};
		
		add(detailedMsg);
		
		WebMarkupContainer detailedToggle = new WebMarkupContainer("detailed-toggle");
		detailedToggle.add(new CollapseBehavior(detailedMsg));
		add(detailedToggle);
		
		add(new GitUserLink("author", new AbstractReadOnlyModel<GitPerson>() {

			@Override
			public GitPerson getObject() {
				return GitPerson.of(getLastCommit().getAuthor());
			}
		}) {
			@Override
			protected void onConfigure() {
				super.onConfigure();
			}
		});
		
		add(new AgeLabel("author-date", new AbstractReadOnlyModel<Date>() {

			@Override
			public Date getObject() {
				return getLastCommit().getAuthor().getDate();
			}
			
		}));
		
		add(new AjaxLazyLoadPanel("paticipants") {

			@Override
			public Component getLazyLoadComponent(String markupId) {
				return new ContributorsPanel(markupId, committersModel);
			}
		});
	}

	protected String getFilePath() {
		return UrlUtils.concatSegments(getPaths());
	}
	
	@Override
	protected String getPageTitle() {
		return getFilePath() + " at " + getRevision() + " " + getProject().getPathName();
	}
	
	protected Commit getLastCommit() {
		return lastCommitModel.getObject();
	}
	
	@Override
	public void onDetach() {
		if (lastCommitModel != null) {
			lastCommitModel.detach();
		}
		
		if (committersModel != null) {
			committersModel.detach();
		}
		
		super.onDetach();
	}
}
