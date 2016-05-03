package com.pmease.gitplex.web.page.depot.commit;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import com.google.common.base.Preconditions;
import com.pmease.commons.git.GitUtils;
import com.pmease.commons.git.WhitespaceOption;
import com.pmease.commons.wicket.assets.oneline.OnelineResourceReference;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.entity.Depot;
import com.pmease.gitplex.core.entity.PullRequest;
import com.pmease.gitplex.core.manager.AuxiliaryManager;
import com.pmease.gitplex.web.component.avatar.ContributorAvatars;
import com.pmease.gitplex.web.component.contributorpanel.ContributorPanel;
import com.pmease.gitplex.web.component.createbranch.CreateBranchLink;
import com.pmease.gitplex.web.component.createtag.CreateTagLink;
import com.pmease.gitplex.web.component.diff.revision.RevisionDiffPanel;
import com.pmease.gitplex.web.component.hashandcode.HashAndCodePanel;
import com.pmease.gitplex.web.page.depot.DepotPage;
import com.pmease.gitplex.web.page.depot.branches.DepotBranchesPage;
import com.pmease.gitplex.web.page.depot.file.DepotFilePage;
import com.pmease.gitplex.web.page.depot.tags.DepotTagsPage;

@SuppressWarnings("serial")
public class CommitDetailPage extends DepotPage {

	private static final String PARAM_REVISION = "revision";
	
	// make sure to use a different value from wicket:id according to wicket bug:
	// https://issues.apache.org/jira/browse/WICKET-6069
	private static final String PARAM_COMPARE_WITH = "compare-with";
	
	private static final String PARAM_WHITESPACE_OPTION = "whitespace-option";
	
	private static final String PARAM_PATH_FILTER = "path-filter";
	
	protected String revision;
	
	private HistoryState state;
	
	private IModel<RevCommit> commitModel = new LoadableDetachableModel<RevCommit>() {

		@Override
		protected RevCommit load() {
			try(RevWalk revWalk = new RevWalk(getDepot().getRepository())) {
				ObjectId objectId;
				if (GitUtils.isHash(revision))
					objectId = ObjectId.fromString(revision);
				else
					objectId = getDepot().getRepository().resolve(revision);
				return revWalk.parseCommit(objectId);
			} catch (RevisionSyntaxException | IOException e) {
				throw new RuntimeException(e);
			}
		}
		
	};
	
	private RevisionDiffPanel revisionDiff;
	
	public CommitDetailPage(PageParameters params) {
		super(params);
		
		revision = GitUtils.normalizePath(params.get(PARAM_REVISION).toString());
		state = new HistoryState(params);
	}

	private RevCommit getCommit() {
		return commitModel.getObject();
	}
	
	private List<String> getParents() {
		List<String> parents = new ArrayList<>();
		for (RevCommit parent: getCommit().getParents())
			parents.add(parent.name());
		return parents;
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(new Label("text", GitUtils.getShortMessage(getCommit())));
		
		add(new HashAndCodePanel("hashAndCode", depotModel, getCommit().getId().name()));
		
		add(new CreateBranchLink("createBranch", depotModel, revision) {

			@Override
			protected void onCreate(AjaxRequestTarget target, String branch) {
				setResponsePage(DepotBranchesPage.class, DepotBranchesPage.paramsOf(getDepot()));
			}
			
		});
		
		add(new CreateTagLink("createTag", depotModel, revision) {

			@Override
			protected void onCreate(AjaxRequestTarget target, String tag) {
				setResponsePage(DepotTagsPage.class, DepotTagsPage.paramsOf(getDepot()));
			}
			
		});
		
		String message = GitUtils.getDetailMessage(getCommit());
		if (message != null) {
			add(new Label("detail", message));
		} else {
			add(new WebMarkupContainer("detail").setVisible(false));
		}
		
		add(new AjaxLazyLoadPanel("refs") {

			@Override
			public Component getLazyLoadComponent(String markupId) {
				Fragment fragment = new Fragment(markupId, "refsFrag", CommitDetailPage.this) {

					@Override
					public void renderHead(IHeaderResponse response) {
						super.renderHead(response);
						String script = String.format("gitplex.commitdetail.initRefs('%s');", getMarkupId());
						response.render(OnDomReadyHeaderItem.forScript(script));
					}
					
				};
				fragment.add(new ListView<Ref>("refs", new LoadableDetachableModel<List<Ref>>() {

					@Override
					protected List<Ref> load() {
						Set<ObjectId> descendants = GitPlex.getInstance(AuxiliaryManager.class)
								.getDescendants(getDepot(), getCommit().getId());
						descendants.add(getCommit().getId());
						
						List<Ref> branchRefs = new ArrayList<>();
						for (Ref ref: getDepot().getBranchRefs()) {
							if (descendants.contains(ref.getObjectId())) {
								branchRefs.add(ref);
							}
						}
						
						List<Ref> tagRefs = new ArrayList<>();
						for (Ref ref: getDepot().getTagRefs()) {
							RevCommit taggedCommit = getDepot().getRevCommit(ref.getObjectId());
							if (descendants.contains(taggedCommit.getId()))
								tagRefs.add(ref);
						}

						List<Ref> refs = new ArrayList<>();
						refs.addAll(branchRefs);
						refs.addAll(tagRefs);
						
						return refs;
					}
					
				}) {

					@Override
					protected void populateItem(ListItem<Ref> item) {
						String ref = item.getModelObject().getName();
						String branch = GitUtils.ref2branch(ref); 
						if (branch != null) {
							DepotFilePage.HistoryState state = new DepotFilePage.HistoryState();
							state.blobIdent.revision = branch;
							Link<Void> link = new BookmarkablePageLink<Void>("link", DepotFilePage.class, 
									DepotFilePage.paramsOf(depotModel.getObject(), state));
							link.add(new Label("label", branch));
							item.add(link);
							item.add(AttributeAppender.append("class", "branch"));
						} else {
							String tag = Preconditions.checkNotNull(GitUtils.ref2tag(ref));
							DepotFilePage.HistoryState state = new DepotFilePage.HistoryState();
							state.blobIdent.revision = tag;
							Link<Void> link = new BookmarkablePageLink<Void>("link", DepotFilePage.class, 
									DepotFilePage.paramsOf(depotModel.getObject(), state));
							link.add(new Label("label", tag));
							item.add(link);
							item.add(AttributeAppender.append("class", "tag"));
						}
					}
					
				});
				return fragment;
			}
			
		});
		
		add(new ContributorAvatars("contributorAvatars", getCommit().getAuthorIdent(), getCommit().getCommitterIdent()));
		add(new ContributorPanel("contribution", getCommit().getAuthorIdent(), getCommit().getCommitterIdent()));

		final WebMarkupContainer parentsContainer = new WebMarkupContainer("parents");
		parentsContainer.setOutputMarkupId(true);
		add(parentsContainer);
		if (getParents().size() == 1) {
			String parent = getParents().get(0);
			Link<Void> link = new BookmarkablePageLink<Void>("parent", CommitDetailPage.class, 
					paramsOf(depotModel.getObject(), parent));
			link.add(new Label("label", GitUtils.abbreviateSHA(parent)));
			parentsContainer.add(link);
			parentsContainer.add(new WebMarkupContainer("parents").setVisible(false));
		} else {
			parentsContainer.add(new WebMarkupContainer("parent").setVisible(false));
			parentsContainer.add(new Label("count", getParents().size() + " parents"));
			parentsContainer.add(new ListView<String>("parents", new LoadableDetachableModel<List<String>>() {

				@Override
				protected List<String> load() {
					return getParents();
				}
				
			}) {

				@Override
				protected void populateItem(ListItem<String> item) {
					final String parent = item.getModelObject();
					Link<Void> link = new BookmarkablePageLink<Void>("link", CommitDetailPage.class, 
							paramsOf(depotModel.getObject(), parent));
					link.add(new Label("label", GitUtils.abbreviateSHA(parent)));
					item.add(link);
					
					item.add(new AjaxLink<Void>("diff") {

						@Override
						public void onClick(AjaxRequestTarget target) {
							setCompareWith(parent); 
							target.add(parentsContainer);
							newRevisionDiff(target);
							pushState(target);
						}

						@Override
						protected void onInitialize() {
							super.onInitialize();
							if (parent.equals(getCompareWith())) 
								add(AttributeAppender.append("class", "active"));
						}	
						
					});
				}
				
			});
		}

		if (getCommit().getParentCount() != 0) {
			newRevisionDiff(null);
		} else {
			add(new WebMarkupContainer("revisionDiff").setVisible(false));
		}
	}
	
	private String getCompareWith() {
		if (state.compareWith == null)
			state.compareWith = getParents().get(0);
		return state.compareWith;
	}
	
	private void setCompareWith(String compareWith) {
		state.compareWith = compareWith;
	}
	
	private void newRevisionDiff(@Nullable AjaxRequestTarget target) {
		revisionDiff = new RevisionDiffPanel("revisionDiff", depotModel,  
				Model.of((PullRequest)null), getCompareWith(), 
				getCommit().name(), state.pathFilter, state.whitespaceOption) {

			@Override
			protected void onPathFilterChange(AjaxRequestTarget target, String pathFilter) {
				state.pathFilter = pathFilter;
				pushState(target);
			}

			@Override
			protected void onWhitespaceOptionChange(AjaxRequestTarget target,
					WhitespaceOption whitespaceOption) {
				state.whitespaceOption = whitespaceOption;
				pushState(target);
			}
			
		};
		revisionDiff.setOutputMarkupId(true);
		if (target != null) {
			replace(revisionDiff);
			target.add(revisionDiff);
		} else {
			add(revisionDiff);
		}
	}
	
	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(JavaScriptHeaderItem.forReference(OnelineResourceReference.INSTANCE));
		response.render(JavaScriptHeaderItem.forReference(new JavaScriptResourceReference(
				CommitDetailPage.class, "commit-detail.js")));
		response.render(CssHeaderItem.forReference(new CssResourceReference(
				CommitDetailPage.class, "commit-detail.css")));
	}

	public static PageParameters paramsOf(Depot depot, String revision) {
		return paramsOf(depot, revision, new HistoryState());
	}
	
	public static PageParameters paramsOf(Depot depot, String revision, HistoryState state) {
		PageParameters params = paramsOf(depot);
		params.set(PARAM_REVISION, revision);
		if (state.compareWith != null)
			params.set(PARAM_COMPARE_WITH, state.compareWith);
		if (state.whitespaceOption != WhitespaceOption.DEFAULT)
			params.set(PARAM_WHITESPACE_OPTION, state.whitespaceOption.name());
		if (state.pathFilter != null)
			params.set(PARAM_PATH_FILTER, state.pathFilter);
		return params;
	}
	
	@Override
	protected void onSelect(AjaxRequestTarget target, Depot depot) {
		setResponsePage(DepotCommitsPage.class, paramsOf(depot));
	}
	
	@Override
	protected void onDetach() {
		commitModel.detach();
		
		super.onDetach();
	}
	
	private void pushState(AjaxRequestTarget target) {
		PageParameters params = paramsOf(getDepot(), getCommit().name(), state);
		CharSequence url = RequestCycle.get().urlFor(CommitDetailPage.class, params);
		pushState(target, url.toString(), state);
	}
	
	@Override
	protected void onPopState(AjaxRequestTarget target, Serializable data) {
		super.onPopState(target, data);
		
		state = (HistoryState) data;
		newRevisionDiff(target);
	}
	
	public static class HistoryState implements Serializable {

		private static final long serialVersionUID = 1L;
		
		public String compareWith;
		
		public WhitespaceOption whitespaceOption = WhitespaceOption.DEFAULT;
		
		public String pathFilter;
		
		public HistoryState() {
		}
		
		public HistoryState(PageParameters params) {
			compareWith = params.get(PARAM_COMPARE_WITH).toString();
			whitespaceOption = WhitespaceOption.of(params.get(PARAM_WHITESPACE_OPTION).toString());
			pathFilter = params.get(PARAM_PATH_FILTER).toString();
		}
		
	}
}
