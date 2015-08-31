package com.pmease.gitplex.web.component.repofile.blobview.source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.CallbackParameter;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.eclipse.jgit.lib.FileMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.pmease.commons.git.Blame;
import com.pmease.commons.git.Blob;
import com.pmease.commons.git.BlobIdent;
import com.pmease.commons.git.GitUtils;
import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.commons.lang.extractors.ExtractException;
import com.pmease.commons.lang.extractors.Extractor;
import com.pmease.commons.lang.extractors.Extractors;
import com.pmease.commons.lang.extractors.Symbol;
import com.pmease.commons.lang.extractors.TokenPosition;
import com.pmease.commons.wicket.assets.codemirror.CodeMirrorResourceReference;
import com.pmease.commons.wicket.assets.cookies.CookiesResourceReference;
import com.pmease.commons.wicket.behavior.RunTaskBehavior;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.manager.CommentManager;
import com.pmease.gitplex.core.model.Comment;
import com.pmease.gitplex.core.model.PullRequest;
import com.pmease.gitplex.search.SearchManager;
import com.pmease.gitplex.search.hit.QueryHit;
import com.pmease.gitplex.search.query.BlobQuery;
import com.pmease.gitplex.search.query.SymbolQuery;
import com.pmease.gitplex.search.query.TextQuery;
import com.pmease.gitplex.web.component.avatar.AvatarMode;
import com.pmease.gitplex.web.component.comment.CommentPanel;
import com.pmease.gitplex.web.component.comment.event.CommentRemoved;
import com.pmease.gitplex.web.component.comment.event.CommentResized;
import com.pmease.gitplex.web.component.repofile.blobsearch.result.SearchResultPanel;
import com.pmease.gitplex.web.component.repofile.blobview.BlobViewContext;
import com.pmease.gitplex.web.component.repofile.blobview.BlobViewPanel;
import com.pmease.gitplex.web.component.userlink.UserLink;
import com.pmease.gitplex.web.model.UserModel;
import com.pmease.gitplex.web.page.repository.commit.RepoCommitPage;
import com.pmease.gitplex.web.utils.DateUtils;

@SuppressWarnings("serial")
public class SourceViewPanel extends BlobViewPanel {

	private static final int QUERY_ENTRIES = 20;
	
	private static final Logger logger = LoggerFactory.getLogger(SourceViewPanel.class);
	
	private String symbol = "";
	
	private List<QueryHit> symbolHits = new ArrayList<>();
	
	private final List<Symbol> symbols = new ArrayList<>();
	
	private final IModel<List<Comment>> commentsModel = new LoadableDetachableModel<List<Comment>>() {

		@Override
		protected List<Comment> load() {
			List<Comment> comments = new ArrayList<>();
			PullRequest request = context.getPullRequest();
			if (request != null) {
				for (Comment comment: request.getComments()) {
					if (comment.getInlineInfo() != null) {
						GitPlex.getInstance(CommentManager.class).updateInlineInfo(comment);
						BlobIdent blobIdent = comment.getBlobIdent();
						if (blobIdent.equals(context.getBlobIdent()))
							comments.add(comment);
					}
				}
				Collections.sort(comments, new Comparator<Comment>() {
	
					@Override
					public int compare(Comment comment1, Comment comment2) {
						return comment1.getDate().compareTo(comment2.getDate());
					}
					
				});
			}
			return comments;
		}
		
	};	
	
	private Component codeContainer;
	
	private OutlinePanel outlinePanel;
	
	private WebMarkupContainer symbolsContainer;
	
	private RepeatingView commentRows;
	
	public SourceViewPanel(String id, BlobViewContext context) {
		super(id, context);
		
		Blob blob = context.getRepository().getBlob(context.getBlobIdent());
		Preconditions.checkArgument(blob.getText() != null);
		
		Extractor extractor = GitPlex.getInstance(Extractors.class).getExtractor(context.getBlobIdent().path);
		if (extractor != null) {
			try {
				symbols.addAll(extractor.extract(blob.getText().getContent()));
			} catch (ExtractException e) {
				logger.debug("Error extracting symbols from blob: " + context.getBlobIdent(), e);
			}
		}
	}
	
	@Override
	protected WebMarkupContainer newCustomActions(String id) {
		Fragment fragment = new Fragment(id, "actionsFrag", this);
		fragment.setVisible(!symbols.isEmpty());
		return fragment;
	}

	public void highlightToken(AjaxRequestTarget target, @Nullable TokenPosition tokenPos) {
		String json;
		if (tokenPos != null) {
			try {
				json = GitPlex.getInstance(ObjectMapper.class).writeValueAsString(tokenPos);
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		} else {
			json = "undefined";
		}
		String script = String.format("gitplex.sourceview.highlightToken('%s', %s);", 
				codeContainer.getMarkupId(), json);
		target.appendJavaScript(script);
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();

		add(codeContainer = new WebMarkupContainer("code"));
		codeContainer.setOutputMarkupId(true);
		
		add(outlinePanel = new OutlinePanel("outline", symbols) {

			@Override
			protected void onSelect(AjaxRequestTarget target, Symbol symbol) {
				highlightToken(target, symbol.getPos());
			}
			
		});
		outlinePanel.setVisible(!symbols.isEmpty());
		
		add(symbolsContainer = new WebMarkupContainer("symbols"));
		symbolsContainer.setOutputMarkupId(true);
		symbolsContainer.add(new ListView<QueryHit>("declarations", new AbstractReadOnlyModel<List<QueryHit>>() {

			@Override
			public List<QueryHit> getObject() {
				return symbolHits;
			}
			
		}) {

			@Override
			protected void populateItem(ListItem<QueryHit> item) {
				final QueryHit hit = item.getModelObject();
				item.add(new Image("icon", hit.getIcon()) {

					@Override
					protected boolean shouldAddAntiCacheParameter() {
						return false;
					}
					
				});
				AjaxLink<Void> link = new AjaxLink<Void>("link") {

					@Override
					public void onClick(AjaxRequestTarget target) {
						String script = String.format(
								"$('#%s .CodeMirror')[0].CodeMirror.hideTokenHover();", 
								codeContainer.getMarkupId());
						target.prependJavaScript(script);
						BlobIdent blobIdent = new BlobIdent(
								context.getBlobIdent().revision, 
								hit.getBlobPath(), 
								FileMode.REGULAR_FILE.getBits());
						context.onSelect(target, blobIdent, hit.getTokenPos());
					}
					
				};
				link.add(hit.render("label"));
				link.add(new Label("scope", hit.getScope()).setVisible(hit.getScope()!=null));
				
				item.add(link);
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(!symbolHits.isEmpty());
			}
			
		});
		
		symbolsContainer.add(new AjaxLink<Void>("findOccurrences") {

			private RunTaskBehavior runTaskBehavior;
			
			@Override
			protected void onInitialize() {
				super.onInitialize();
				
				add(runTaskBehavior = new RunTaskBehavior() {
					
					@Override
					protected void runTask(AjaxRequestTarget target) {
						BlobQuery query = new TextQuery(symbol, false, true, true, 
									null, null, SearchResultPanel.MAX_QUERY_ENTRIES);
						try {
							SearchManager searchManager = GitPlex.getInstance(SearchManager.class);
							List<QueryHit> hits = searchManager.search(context.getRepository(), 
									context.getBlobIdent().revision, query);
							String script = String.format(
									"$('#%s .CodeMirror')[0].CodeMirror.hideTokenHover();", 
									codeContainer.getMarkupId());
							target.prependJavaScript(script);
							context.onSearchComplete(target, hits);
						} catch (InterruptedException e) {
							throw new RuntimeException(e);
						}								
						
					}
					
				});
			}

			@Override
			public void onClick(AjaxRequestTarget target) {
				runTaskBehavior.requestRun(target);
			}
			
		});
		
		commentRows = new RepeatingView("comments");
		
		for (Comment comment: commentsModel.getObject())
			commentRows.add(newCommentRow(commentRows.newChildId(), comment));
		
		add(commentRows);
		
		add(new AbstractDefaultAjaxBehavior() {

			@Override
			protected void respond(AjaxRequestTarget target) {
				IRequestParameters params = RequestCycle.get().getRequest().getQueryParameters();
				symbol = params.getParameterValue("symbol").toString();
				if (symbol.startsWith("@"))
					symbol = symbol.substring(1);
				try {
					SymbolQuery query = new SymbolQuery(symbol, true, true, null, null, QUERY_ENTRIES);
					SearchManager searchManager = GitPlex.getInstance(SearchManager.class);
					symbolHits = searchManager.search(context.getRepository(), context.getBlobIdent().revision, query);
					if (symbolHits.size() < QUERY_ENTRIES) {
						query = new SymbolQuery(symbol, false, true, null, null, QUERY_ENTRIES - symbolHits.size());
						symbolHits.addAll(searchManager.search(context.getRepository(), 
								context.getBlobIdent().revision, query));
					}
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}								
				target.add(symbolsContainer);
				String script = String.format("gitplex.sourceview.symbolsQueried('%s', '%s');", 
						codeContainer.getMarkupId(), symbolsContainer.getMarkupId());
				target.appendJavaScript(script);
			}

			@Override
			public void renderHead(Component component, IHeaderResponse response) {
				super.renderHead(component, response);

				response.render(JavaScriptHeaderItem.forReference(CookiesResourceReference.INSTANCE));
				response.render(JavaScriptHeaderItem.forReference(CodeMirrorResourceReference.INSTANCE));
				
				response.render(JavaScriptHeaderItem.forReference(
						new JavaScriptResourceReference(SourceViewPanel.class, "source-view.js")));
				response.render(CssHeaderItem.forReference(
						new CssResourceReference(SourceViewPanel.class, "source-view.css")));
				
				String highlightToken;
				try {
					highlightToken = GitPlex.getInstance(ObjectMapper.class).writeValueAsString(context.getTokenPos());
				} catch (JsonProcessingException e) {
					throw new RuntimeException(e);
				} 
				ResourceReference ajaxIndicator =  new PackageResourceReference(SourceViewPanel.class, "ajax-indicator.gif");
				Blob blob = context.getRepository().getBlob(context.getBlobIdent());
				String script = String.format("gitplex.sourceview.init('%s', '%s', '%s', %s, '%s', %s, %s, %d, %d);", 
						codeContainer.getMarkupId(), 
						StringEscapeUtils.escapeEcmaScript(blob.getText().getContent()),
						context.getBlobIdent().path, 
						highlightToken,
						RequestCycle.get().urlFor(ajaxIndicator, new PageParameters()), 
						getCallbackFunction(CallbackParameter.explicit("symbol")), 
						getBlameCommits(), 
						context.getPullRequest()!=null?context.getPullRequest().getId():-1,
						context.getComment()!=null?context.getComment().getId():-1);
				response.render(OnDomReadyHeaderItem.forScript(script));
			}
			
		});		
		
		setOutputMarkupId(true);
	}
	
	public void onBlameChange(AjaxRequestTarget target) {
		String script = String.format("gitplex.sourceview.blame('%s', %s);", 
				codeContainer.getMarkupId(), getBlameCommits());
		target.appendJavaScript(script);
	}

	private String getBlameCommits() {
		if (context.isBlame()) {
			List<BlameCommit> commits = new ArrayList<>();
			
			String commitHash = context.getRepository().getObjectId(context.getBlobIdent().revision).name();
			
			for (Blame blame: context.getRepository().git().blame(commitHash, context.getBlobIdent().path).values()) {
				BlameCommit commit = new BlameCommit();
				commit.authorDate = DateUtils.formatDate(blame.getCommit().getAuthor().getWhen());
				commit.authorName = StringEscapeUtils.escapeHtml4(blame.getCommit().getAuthor().getName());
				commit.hash = GitUtils.abbreviateSHA(blame.getCommit().getHash(), 7);
				commit.message = blame.getCommit().getSubject();
				PageParameters params = RepoCommitPage.paramsOf(context.getRepository(), blame.getCommit().getHash());
				commit.url = RequestCycle.get().urlFor(RepoCommitPage.class, params).toString();
				commit.ranges = blame.getRanges();
				commits.add(commit);
			}
			try {
				return GitPlex.getInstance(ObjectMapper.class).writeValueAsString(commits);
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		} else {
			return "undefined";
		}
	}
	
	private Component newCommentRow(String id, Comment comment) {
		final Long commentId = comment.getId();
		WebMarkupContainer row = new WebMarkupContainer(id) {

			@Override
			public void onEvent(IEvent<?> event) {
				super.onEvent(event);
				
				if (event.getPayload() instanceof CommentRemoved) {
					CommentRemoved commentRemoved = (CommentRemoved) event.getPayload();
					commentRows.remove(this);
					String script = String.format("gitplex.sourceview.removeComment('%s');", getMarkupId());
					commentRemoved.getTarget().appendJavaScript(script);
					
					send(SourceViewPanel.this, Broadcast.BUBBLE, commentRemoved);
				} else if (event.getPayload() instanceof CommentResized) {
					CommentResized commentResized = (CommentResized) event.getPayload();
					String script = String.format("gitplex.sourceview.commentResized('%s');", getMarkupId());
					commentResized.getTarget().appendJavaScript(script);
				}
			}

		};
		row.add(new UserLink("avatar", new UserModel(comment.getUser()), AvatarMode.AVATAR));
		row.add(new CommentPanel("detail", new LoadableDetachableModel<Comment>() {

			@Override
			protected Comment load() {
				return GitPlex.getInstance(Dao.class).load(Comment.class, commentId);
			}
			
		}));
		
		row.add(AttributeAppender.append("data-lineNo", comment.getLine()));
		row.setMarkupId("pullrequest-comment-" + comment.getId());
		row.setOutputMarkupId(true);
		
		return row;
	}
	
	@Override
	protected void onDetach() {
		commentsModel.detach();
		super.onDetach();
	}

	@SuppressWarnings("unused")
	private static class BlameCommit {
		
		String hash;
		
		String message;
		
		String url;
		
		String authorName;
		
		String authorDate;
		
		List<Blame.Range> ranges;
	}
	
}
