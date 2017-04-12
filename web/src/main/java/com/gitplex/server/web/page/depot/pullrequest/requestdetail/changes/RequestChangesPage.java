package com.gitplex.server.web.page.depot.pullrequest.requestdetail.changes;

import static org.apache.wicket.ajax.attributes.CallbackParameter.explicit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.jgit.revwalk.RevCommit;

import com.google.common.base.Preconditions;
import com.gitplex.server.GitPlex;
import com.gitplex.server.git.GitUtils;
import com.gitplex.server.manager.CodeCommentManager;
import com.gitplex.server.model.CodeComment;
import com.gitplex.server.model.Depot;
import com.gitplex.server.model.PullRequest;
import com.gitplex.server.model.PullRequestUpdate;
import com.gitplex.server.model.support.CodeCommentActivity;
import com.gitplex.server.model.support.CommentPos;
import com.gitplex.server.util.diff.WhitespaceOption;
import com.gitplex.server.web.behavior.AbstractPostAjaxBehavior;
import com.gitplex.server.web.component.diff.revision.CommentSupport;
import com.gitplex.server.web.component.diff.revision.RevisionDiffPanel;
import com.gitplex.server.web.component.floating.FloatingPanel;
import com.gitplex.server.web.component.link.DropdownLink;
import com.gitplex.server.web.page.depot.pullrequest.requestdetail.RequestDetailPage;
import com.gitplex.server.web.page.depot.pullrequest.requestlist.RequestListPage;
import com.gitplex.server.web.websocket.CodeCommentChangedRegion;
import com.gitplex.server.web.websocket.PullRequestChanged;
import com.gitplex.server.web.websocket.WebSocketManager;
import com.gitplex.server.web.websocket.WebSocketRegion;

@SuppressWarnings("serial")
public class RequestChangesPage extends RequestDetailPage implements CommentSupport {

	public static final String PARAM_OLD_COMMIT = "old-commit";
	
	public static final String PARAM_NEW_COMMIT = "new-commit";
	
	private static final String PARAM_WHITESPACE_OPTION = "whitespace-option";
	
	private static final String PARAM_PATH_FILTER = "path-filter";
	
	private static final String PARAM_BLAME_FILE = "blame-file";
	
	public static final String PARAM_COMMENT = "comment";
	
	private static final String PARAM_MARK = "mark";
	
	private static final String PARAM_ANCHOR = "anchor";
	
	private State state = new State();
	
	private WebMarkupContainer head;
	
	private final IModel<List<RevCommit>> commitsModel = new LoadableDetachableModel<List<RevCommit>>() {

		@Override
		protected List<RevCommit> load() {
			List<RevCommit> commits = new ArrayList<>();
			for (PullRequestUpdate update: getPullRequest().getSortedUpdates()) {
				commits.addAll(update.getCommits());
			}
			return commits;
		}
		
	};
	
	public RequestChangesPage(PageParameters params) {
		super(params);

		state.oldCommit = params.get(PARAM_OLD_COMMIT).toString();
		if (state.oldCommit == null)
			state.oldCommit = getPullRequest().getBaseCommitHash();
		state.newCommit = params.get(PARAM_NEW_COMMIT).toString();
		if (state.newCommit == null)
			state.newCommit = getPullRequest().getHeadCommitHash();
		state.pathFilter = params.get(PARAM_PATH_FILTER).toString();
		state.blameFile = params.get(PARAM_BLAME_FILE).toString();
		state.whitespaceOption = WhitespaceOption.ofNullableName(params.get(PARAM_WHITESPACE_OPTION).toString());
		state.commentId = params.get(PARAM_COMMENT).toOptionalLong();
		state.mark = CommentPos.fromString(params.get(PARAM_MARK).toString());
		state.anchor = params.get(PARAM_ANCHOR).toString();
	}
	
	private int getCommitIndex(String commitHash) {
		int index = -1;
		for (int i=0; i<commitsModel.getObject().size(); i++) {
			RevCommit commit = commitsModel.getObject().get(i);
			if (commit.name().equals(commitHash)) {
				index = i;
				break;
			}
		}
		return index;
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(head = new WebMarkupContainer("changesHead") {

			@Override
			public void onEvent(IEvent<?> event) {
				super.onEvent(event);

				if (event.getPayload() instanceof PullRequestChanged) {
					PullRequestChanged pullRequestChanged = (PullRequestChanged) event.getPayload();
					pullRequestChanged.getPartialPageRequestHandler().add(this);
				}
			}
			
		});
		head.setOutputMarkupId(true);
		
		head.add(new AjaxLink<Void>("prevCommitLink") {

			@Override
			protected void onComponentTag(ComponentTag tag) {
				super.onComponentTag(tag);

				configure();
				if (!isEnabled()) {
					tag.put("disabled", "disabled");
				}
			}
			
			@Override
			protected void onConfigure() {
				super.onConfigure();
				setEnabled(getCommitIndex(state.oldCommit) != -1 && getCommitIndex(state.newCommit) != -1);
			}

			@Override
			public void onClick(AjaxRequestTarget target) {
				int index = getCommitIndex(state.oldCommit);
				if (index != -1) {
					state.newCommit = commitsModel.getObject().get(index).name();
					index--;
					if (index == -1) {
						state.oldCommit = getPullRequest().getBaseCommitHash();
					} else {
						state.oldCommit = commitsModel.getObject().get(index).name();
					}
					newRevisionDiff(target);
				}
				target.add(head);
				pushState(target);
			}
			
		});
		head.add(new AjaxLink<Void>("nextCommitLink") {

			@Override
			protected void onConfigure() {
				super.onConfigure();

				int oldIndex = getCommitIndex(state.oldCommit);
				int newIndex = getCommitIndex(state.newCommit);
				if (!state.oldCommit.equals(getPullRequest().getBaseCommitHash()) && oldIndex == -1 
						|| newIndex == -1 || newIndex == commitsModel.getObject().size()-1) {
					setEnabled(false);
				} else {
					setEnabled(true);
				}
				
			}

			@Override
			protected void onComponentTag(ComponentTag tag) {
				super.onComponentTag(tag);
				
				configure();
				if (!isEnabled()) {
					tag.put("disabled", "disabled");
				}
			}
			
			@Override
			public void onClick(AjaxRequestTarget target) {
				int index = getCommitIndex(state.newCommit);
				if (index != -1 && index != commitsModel.getObject().size()-1) {
					CodeComment comment = getOpenComment();
					
					// we will not move old commit if an opened comment points to it
					if (comment == null || comment.getCommentPos().getCommit().equals(state.newCommit)) {
						state.oldCommit = state.newCommit;
					}
					index++;
					state.newCommit = commitsModel.getObject().get(index).name();
					newRevisionDiff(target);
				} 
				target.add(head);
				pushState(target);
			}
			
		});
		
		DropdownLink selectedCommitsLink = new DropdownLink("comparingCommits") {
			
			@Override
			protected Component newContent(String id, FloatingPanel dropdown) {
				AbstractPostAjaxBehavior callbackBehavior = new AbstractPostAjaxBehavior() {
					
					@Override
					protected void respond(AjaxRequestTarget target) {
						IRequestParameters params = RequestCycle.get().getRequest().getPostParameters();
						state.oldCommit = params.getParameterValue("oldCommit").toString();
						state.newCommit = params.getParameterValue("newCommit").toString();
						target.add(head);
						newRevisionDiff(target);
						pushState(target);
						dropdown.close();
					}
					
				};
				Fragment fragment = new Fragment(id, "commitsFrag", RequestChangesPage.this) {

					@Override
					public void renderHead(IHeaderResponse response) {
						super.renderHead(response);
						
						String callback = callbackBehavior.getCallbackFunction(explicit("oldCommit"), explicit("newCommit")).toString();
						String script;
						int oldIndex = getCommitIndex(state.oldCommit);
						int newIndex = getCommitIndex(state.newCommit);
						if ((state.oldCommit.equals(getPullRequest().getBaseCommitHash()) || oldIndex != -1) 
								&& newIndex != -1) {
							script = String.format("gitplex.server.requestChanges.initCommitSelector(%s, '%s', %d, %d);", 
									callback, getPullRequest().getBaseCommitHash(), oldIndex+1, newIndex);
						} else {
							script = String.format("gitplex.server.requestChanges.initCommitSelector(%s, '%s');", 
									callback, getPullRequest().getBaseCommitHash());
						}
						response.render(OnDomReadyHeaderItem.forScript(script));
					}
					
				};
				fragment.add(callbackBehavior);
				fragment.add(new ListView<RevCommit>("commits", commitsModel) {

					@Override
					protected void populateItem(ListItem<RevCommit> item) {
						RevCommit commit = item.getModelObject();
						item.add(AttributeAppender.append("data-hash", commit.name()));
						item.add(new Label("hash", GitUtils.abbreviateSHA(commit.name())));
						item.add(new Label("subject", commit.getShortMessage()));
					}
					
				});
				return fragment;
			}
		};
		selectedCommitsLink.add(new Label("label", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				String oldName;
				if (state.oldCommit.equals(getPullRequest().getBaseCommitHash()))
					oldName = "base";
				else
					oldName = GitUtils.abbreviateSHA(state.oldCommit);

				String newName;
				if (state.newCommit.equals(getPullRequest().getHeadCommitHash())) {
					newName = "head";
				} else {
					newName = GitUtils.abbreviateSHA(state.newCommit);
				}
				
				return oldName + " ... " + newName;
			}
			
		}));
		
		head.add(selectedCommitsLink);
		
		head.add(new AjaxLink<Void>("fullChanges") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				state.oldCommit = getPullRequest().getBaseCommitHash();
				state.newCommit = getPullRequest().getHeadCommitHash();
				target.add(head);
				newRevisionDiff(target);
				pushState(target);
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(!state.oldCommit.equals(getPullRequest().getBaseCommitHash()) 
						|| !state.newCommit.equals(getPullRequest().getHeadCommitHash()));
			}
			
		});
		
		newRevisionDiff(null);
	}

	@Override
	public void onDetach() {
		commitsModel.detach();
		super.onDetach();
	}
	
	public static PageParameters paramsOf(PullRequest request, String oldCommit, String newCommit) {
		State state = new State();
		state.oldCommit = oldCommit;
		state.newCommit = newCommit;
		return paramsOf(request, state);
	}
	
	public static PageParameters paramsOf(PullRequest request, CodeComment comment, @Nullable String anchor) {
		PullRequest.ComparingInfo comparingInfo = null;
		for (int i=comment.getActivities().size()-1; i>=0; i--) {
			CodeCommentActivity activity = comment.getActivities().get(i);
			comparingInfo = request.getRequestComparingInfo(activity.getComparingInfo());
			if (comparingInfo != null)
				break;
		}
		if (comparingInfo == null) {
			comparingInfo = Preconditions.checkNotNull(request.getRequestComparingInfo(comment.getComparingInfo()));
		}
		RequestChangesPage.State state = new RequestChangesPage.State();
		state.commentId = comment.getId();
		state.mark = comment.getCommentPos();
		state.oldCommit = comparingInfo.getOldCommit();
		state.newCommit = comparingInfo.getNewCommit();
		state.pathFilter = comparingInfo.getPathFilter();
		state.whitespaceOption = comparingInfo.getWhitespaceOption();
		state.anchor = anchor;
		return paramsOf(request, state);
	}
	
	public static PageParameters paramsOf(PullRequest request, State state) {
		PageParameters params = RequestDetailPage.paramsOf(request);

		if (state.oldCommit != null)
			params.set(PARAM_OLD_COMMIT, state.oldCommit);
		if (state.newCommit != null)
			params.set(PARAM_NEW_COMMIT, state.newCommit);
		if (state.whitespaceOption != WhitespaceOption.DEFAULT)
			params.set(PARAM_WHITESPACE_OPTION, state.whitespaceOption.name());
		if (state.pathFilter != null)
			params.set(PARAM_PATH_FILTER, state.pathFilter);
		if (state.blameFile != null)
			params.set(PARAM_BLAME_FILE, state.blameFile);
		if (state.commentId != null)
			params.set(PARAM_COMMENT, state.commentId);
		if (state.mark != null)
			params.set(PARAM_MARK, state.mark.toString());
		if (state.anchor != null)
			params.set(PARAM_ANCHOR, state.anchor);
		return params;
	}
	
	@Override
	protected void onPopState(AjaxRequestTarget target, Serializable data) {
		super.onPopState(target, data);

		state = (State) data;
		GitPlex.getInstance(WebSocketManager.class).onRegionChange(this);
		
		newRevisionDiff(target);
	}
	
	@Override
	protected void onSelect(AjaxRequestTarget target, Depot depot) {
		setResponsePage(RequestListPage.class, paramsOf(depot));
	}

	private void pushState(IPartialPageRequestHandler partialPageRequestHandler) {
		PageParameters params = paramsOf(getPullRequest(), state);
		CharSequence url = RequestCycle.get().urlFor(RequestChangesPage.class, params);
		pushState(partialPageRequestHandler, url.toString(), state);
	}
	
	private void newRevisionDiff(@Nullable AjaxRequestTarget target) {
		IModel<String> blameModel = new IModel<String>() {

			@Override
			public void detach() {
			}

			@Override
			public String getObject() {
				return state.blameFile;
			}

			@Override
			public void setObject(String object) {
				state.blameFile = object;
				pushState(RequestCycle.get().find(AjaxRequestTarget.class));
			}
			
		};
		IModel<String> pathFilterModel = new IModel<String>() {

			@Override
			public void detach() {
			}

			@Override
			public String getObject() {
				return state.pathFilter;
			}

			@Override
			public void setObject(String object) {
				state.pathFilter = object;
				pushState(RequestCycle.get().find(AjaxRequestTarget.class));
			}
			
		};
		IModel<WhitespaceOption> whitespaceOptionModel = new IModel<WhitespaceOption>() {

			@Override
			public void detach() {
			}

			@Override
			public WhitespaceOption getObject() {
				return state.whitespaceOption;
			}

			@Override
			public void setObject(WhitespaceOption object) {
				state.whitespaceOption = object;
				pushState(RequestCycle.get().find(AjaxRequestTarget.class));
			}

		};
		
		Component revisionDiff = new RevisionDiffPanel("revisionDiff", depotModel,  
				requestModel, state.oldCommit, state.newCommit, pathFilterModel, 
				whitespaceOptionModel, blameModel, this);
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
		response.render(JavaScriptHeaderItem.forReference(new RequestChangesResourceReference()));
	}

	@Override
	public CommentPos getMark() {
		return state.mark;
	}

	@Override
	public String getAnchor() {
		return state.anchor;
	}

	@Override
	public String getMarkUrl(CommentPos mark) {
		State markState = new State();
		markState.mark = mark;
		markState.oldCommit = state.oldCommit;
		markState.newCommit = state.newCommit;
		markState.pathFilter = state.pathFilter;
		markState.whitespaceOption = state.whitespaceOption;
		return urlFor(RequestChangesPage.class, paramsOf(getPullRequest(), markState)).toString();
	}

	@Override
	public String getCommentUrl(CodeComment comment) {
		State commentState = new State();
		commentState.mark = comment.getCommentPos();
		commentState.commentId = comment.getId();
		commentState.oldCommit = state.oldCommit;
		commentState.newCommit = state.newCommit;
		commentState.pathFilter = state.pathFilter;
		commentState.whitespaceOption = state.whitespaceOption;
		return urlFor(RequestChangesPage.class, paramsOf(getPullRequest(), commentState)).toString();
	}
	
	@Override
	public CodeComment getOpenComment() {
		if (state.commentId != null)
			return GitPlex.getInstance(CodeCommentManager.class).load(state.commentId);
		else
			return null;
	}

	@Override
	public void onCommentOpened(AjaxRequestTarget target, CodeComment comment) {
		if (comment != null) {
			state.commentId = comment.getId();
			state.mark = comment.getCommentPos();
		} else {
			state.commentId = null;
			state.mark = null;
		}
		GitPlex.getInstance(WebSocketManager.class).onRegionChange(this);
		pushState(target);
	}

	@Override
	public void onMark(AjaxRequestTarget target, CommentPos mark) {
		state.mark = mark;
		pushState(target);
	}
	
	public State getState() {
		return state;
	}

	@Override
	public void onAddComment(AjaxRequestTarget target, CommentPos mark) {
		state.commentId = null;
		state.mark = mark;
		pushState(target);
		GitPlex.getInstance(WebSocketManager.class).onRegionChange(this);
	}

	@Override
	public Collection<WebSocketRegion> getWebSocketRegions() {
		Collection<WebSocketRegion> regions = super.getWebSocketRegions();
		if (state.commentId != null)
			regions.add(new CodeCommentChangedRegion(state.commentId));
		return regions;
	}

	public static class State implements Serializable {

		private static final long serialVersionUID = 1L;
		
		public String oldCommit;
		
		public String newCommit;
		
		public WhitespaceOption whitespaceOption = WhitespaceOption.DEFAULT;
		
		@Nullable
		public String pathFilter;
		
		@Nullable
		public String blameFile;
		
		@Nullable
		public Long commentId;
		
		@Nullable
		public CommentPos mark;
		
		@Nullable
		public String anchor;

	}

}
