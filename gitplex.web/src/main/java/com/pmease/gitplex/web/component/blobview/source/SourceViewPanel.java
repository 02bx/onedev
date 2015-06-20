package com.pmease.gitplex.web.component.blobview.source;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.CallbackParameter;
import org.apache.wicket.ajax.markup.html.AjaxLink;
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
import org.apache.wicket.model.AbstractReadOnlyModel;
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
import com.pmease.commons.git.BlobIdent;
import com.pmease.commons.git.GitUtils;
import com.pmease.commons.lang.ExtractException;
import com.pmease.commons.lang.Extractor;
import com.pmease.commons.lang.Extractors;
import com.pmease.commons.lang.Symbol;
import com.pmease.commons.lang.TokenPosition;
import com.pmease.commons.wicket.assets.codemirror.CodeMirrorResourceReference;
import com.pmease.commons.wicket.assets.cookies.CookiesResourceReference;
import com.pmease.commons.wicket.behavior.RunTaskBehavior;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.search.SearchManager;
import com.pmease.gitplex.search.hit.QueryHit;
import com.pmease.gitplex.search.query.BlobQuery;
import com.pmease.gitplex.search.query.SymbolQuery;
import com.pmease.gitplex.search.query.TextQuery;
import com.pmease.gitplex.web.component.blobview.BlobViewContext;
import com.pmease.gitplex.web.component.blobview.BlobViewPanel;
import com.pmease.gitplex.web.page.repository.commit.RepoCommitPage;
import com.pmease.gitplex.web.page.repository.file.SearchResultPanel;
import com.pmease.gitplex.web.util.DateUtils;

@SuppressWarnings("serial")
public class SourceViewPanel extends BlobViewPanel {

	private static final int QUERY_ENTRIES = 20;
	
	private static final Logger logger = LoggerFactory.getLogger(SourceViewPanel.class);
	
	private Component codeContainer;
	
	private OutlinePanel outlinePanel;
	
	private WebMarkupContainer symbolsContainer;
	
	private String symbol = "";
	
	private boolean blamed;
	
	private List<QueryHit> symbolHits = new ArrayList<>();
	
	private final List<Symbol> symbols = new ArrayList<>();
	
	public SourceViewPanel(String id, BlobViewContext context, boolean blamed) {
		this(id, context);
		
		this.blamed = blamed;
	}
	
	public SourceViewPanel(String id, BlobViewContext context) {
		super(id, context);
		
		Preconditions.checkArgument(context.getBlob().getText() != null);
		
		Extractor extractor = GitPlex.getInstance(Extractors.class).getExtractor(context.getBlobIdent().path);
		if (extractor != null) {
			try {
				symbols.addAll(extractor.extract(context.getBlob().getText().getContent()));
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

	public void highlightToken(AjaxRequestTarget target, TokenPosition tokenPos) {
		try {
			String json = GitPlex.getInstance(ObjectMapper.class).writeValueAsString(tokenPos);
			String script = String.format("gitplex.sourceview.highlightToken('%s', %s);", 
					codeContainer.getMarkupId(), json);
			target.appendJavaScript(script);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
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
									null, null, SearchResultPanel.QUERY_ENTRIES);
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
					highlightToken = GitPlex.getInstance(ObjectMapper.class).writeValueAsString(context.getTokenPosition());
				} catch (JsonProcessingException e) {
					throw new RuntimeException(e);
				} 
				ResourceReference ajaxIndicator =  new PackageResourceReference(SourceViewPanel.class, "ajax-indicator.gif");
				String script = String.format("gitplex.sourceview.init('%s', '%s', '%s', %s, '%s', %s, %s);", 
						codeContainer.getMarkupId(), 
						StringEscapeUtils.escapeEcmaScript(context.getBlob().getText().getContent()),
						context.getBlobIdent().path, 
						highlightToken,
						RequestCycle.get().urlFor(ajaxIndicator, new PageParameters()), 
						getCallbackFunction(CallbackParameter.explicit("symbol")), 
						getBlameBlocks());
				response.render(OnDomReadyHeaderItem.forScript(script));
			}
			
		});		
		
		setOutputMarkupId(true);
	}

	private String getBlameBlocks() {
		if (blamed) {
			int fromLine = 0;
			List<BlameBlock> blocks = new ArrayList<>();
			for (Blame blame: context.getRepository().git().blame(
					context.getBlobIdent().path, context.getBlobIdent().revision, -1, -1)) {
				BlameBlock block = new BlameBlock();
				block.authorDate = DateUtils.formatDate(blame.getCommit().getAuthor().getWhen());
				block.authorName = StringEscapeUtils.escapeHtml4(blame.getCommit().getAuthor().getName());
				block.commitHash = GitUtils.abbreviateSHA(blame.getCommit().getHash(), 7);
				block.commitMessage = blame.getCommit().getSubject();
				PageParameters params = RepoCommitPage.paramsOf(context.getRepository(), blame.getCommit().getHash());
				block.commitUrl = RequestCycle.get().urlFor(RepoCommitPage.class, params).toString();
				block.fromLine = fromLine;
				block.toLine = fromLine + blame.getLines().size();
				fromLine = block.toLine;
				blocks.add(block);
			}
			try {
				return GitPlex.getInstance(ObjectMapper.class).writeValueAsString(blocks);
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		} else {
			return "undefined";
		}
	}
	
	public void blame(AjaxRequestTarget target, boolean blamed) {
		this.blamed = blamed;
		String script = String.format("gitplex.sourceview.blame('%s', %s);", 
				codeContainer.getMarkupId(), getBlameBlocks());
		target.appendJavaScript(script);
	}
	
	public boolean isBlamed() {
		return blamed;
	}

	@SuppressWarnings("unused")
	private static class BlameBlock {
		
		String commitMessage;
		
		String commitHash;
		
		String commitUrl;
		
		String authorName;
		
		String authorDate;
		
		int fromLine;
		
		int toLine;
	}
	
}
