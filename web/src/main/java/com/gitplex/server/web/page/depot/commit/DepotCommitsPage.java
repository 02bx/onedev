package com.gitplex.server.web.page.depot.commit;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unbescape.java.JavaEscape;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.gitplex.server.GitPlex;
import com.gitplex.server.git.BlobIdent;
import com.gitplex.server.git.GitUtils;
import com.gitplex.server.git.command.RevListCommand;
import com.gitplex.server.manager.WorkExecutor;
import com.gitplex.server.model.Depot;
import com.gitplex.server.model.support.DepotAndRevision;
import com.gitplex.server.util.StringUtils;
import com.gitplex.server.util.concurrent.PrioritizedCallable;
import com.gitplex.server.web.WebConstants;
import com.gitplex.server.web.behavior.clipboard.CopyClipboardBehavior;
import com.gitplex.server.web.component.avatar.ContributorAvatars;
import com.gitplex.server.web.component.commitgraph.CommitGraphResourceReference;
import com.gitplex.server.web.component.commitgraph.CommitGraphUtils;
import com.gitplex.server.web.component.commitmessage.ExpandableCommitMessagePanel;
import com.gitplex.server.web.component.contributorpanel.ContributorPanel;
import com.gitplex.server.web.page.depot.DepotPage;
import com.gitplex.server.web.page.depot.compare.RevisionComparePage;
import com.gitplex.server.web.page.depot.file.DepotFilePage;
import com.gitplex.server.web.util.ajaxlistener.IndicateLoadingListener;
import com.gitplex.server.web.util.model.CommitRefsModel;
import com.gitplex.server.web.page.depot.commit.CommitQueryLexer;
import com.gitplex.server.web.page.depot.commit.CommitQueryParser;
import com.gitplex.server.web.page.depot.commit.CommitQueryParser.CriteriaContext;
import com.gitplex.server.web.page.depot.commit.CommitQueryParser.QueryContext;

import de.agilecoders.wicket.core.markup.html.bootstrap.common.NotificationPanel;
import jersey.repackaged.com.google.common.collect.Lists;

@SuppressWarnings("serial")
public class DepotCommitsPage extends DepotPage {

	private static final Logger logger = LoggerFactory.getLogger(DepotCommitsPage.class);
	
	private static final int LOG_PRIORITY = 1;
	
	private static final String GIT_ERROR_START = "Command error output: ";
	
	private static final int COUNT = 50;
	
	private static final int MAX_STEPS = 50;
	
	private static final String PARAM_COMPARE_WITH = "compareWith";
	
	private static final String PARAM_QUERY = "query";
	
	private static final String PARAM_STEP = "step";
	
	private State state = new State();
	
	private boolean hasMore;
	
	private WebMarkupContainer body;
	
	private Form<?> queryForm;
	
	private RepeatingView commitsView;
	
	private NotificationPanel feedback;
	
	private WebMarkupContainer foot;
	
	private IModel<Commits> commitsModel = new LoadableDetachableModel<Commits>() {

		@Override
		protected Commits load() {
			Commits commits = new Commits();
			
			List<String> commitHashes;
			try {
				RevListCommand command = new RevListCommand(getDepot().getDirectory());
				command.ignoreCase(true);
				state.applyTo(command);
				commitHashes = GitPlex.getInstance(WorkExecutor.class).submit(new PrioritizedCallable<List<String>>(LOG_PRIORITY) {

					@Override
					public List<String> call() throws Exception {
						return command.call();
					}
					
				}).get();
			} catch (Exception e) {
				if (e.getMessage() != null && e.getMessage().contains(GIT_ERROR_START)) {
					queryForm.error(StringUtils.substringAfter(e.getMessage(), GIT_ERROR_START));
					commitHashes = new ArrayList<>();
				} else {
					throw Throwables.propagate(e);
				}
			}
			
			hasMore = commitHashes.size() == state.getStep()*COUNT;
			
			try (RevWalk revWalk = new RevWalk(getDepot().getRepository())) {
				int lastMaxCount = (state.getStep()-1)*COUNT;

				commits.last = new ArrayList<>();
				
				for (int i=0; i<lastMaxCount; i++) { 
					commits.last.add(revWalk.parseCommit(ObjectId.fromString(commitHashes.get(i))));
				}
				
				CommitGraphUtils.sort(commits.last, 0);
				
				commits.current = new ArrayList<>(commits.last);
				for (int i=lastMaxCount; i<commitHashes.size(); i++)
					commits.current.add(revWalk.parseCommit(ObjectId.fromString(commitHashes.get(i))));
				
				CommitGraphUtils.sort(commits.current, lastMaxCount);

				commits.last = separateByDate(commits.last);
				commits.current = separateByDate(commits.current);
				
				return commits;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
	};
	
	private CommitRefsModel labelsModel = new CommitRefsModel(depotModel);
	
	public DepotCommitsPage(PageParameters params) {
		super(params);
		
		state = new State(params);
	}
	
	@SuppressWarnings("deprecation")
	private Component replaceItem(AjaxRequestTarget target, int index) {
		Component item = commitsView.get(index);
		Component newItem = newCommitItem(item.getId(), index);
		item.replaceWith(newItem);
		target.add(newItem);
		return newItem;
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();

		queryForm = new Form<Void>("query") {

			@Override
			protected void onSubmit() {
				super.onSubmit();

				AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
				try {
					state.getParseTree(); // validate query
					updateCommits(target);
				} catch (Exception e) {
					logger.error("Error parsing commit query string: " + state.getQuery(), e);
					if (StringUtils.isNotBlank(e.getMessage()))
						error(e.getMessage());
					else
						error("Syntax error");
					target.add(feedback);
				}
			}

			@Override
			protected void onError() {
				super.onError();
				
				RequestCycle.get().find(AjaxRequestTarget.class).add(feedback);
			}

		};
		queryForm.add(new TextField<String>("input", new IModel<String>() {

			@Override
			public void detach() {
			}

			@Override
			public String getObject() {
				return state.getQuery();
			}

			@Override
			public void setObject(String object) {
				state.setQuery(object);
			}
			
		}).add(new QueryAssistBehavior(depotModel)));
		
		queryForm.add(new AjaxButton("submit") {});
		queryForm.setOutputMarkupId(true);
		add(queryForm);
		
		add(feedback = new NotificationPanel("feedback", queryForm));
		feedback.setOutputMarkupPlaceholderTag(true);
		
		body = new WebMarkupContainer("body");
		body.setOutputMarkupId(true);
		add(body);
		body.add(commitsView = newCommitsView());
		body.add(new WebMarkupContainer("noCommits") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(!queryForm.hasErrorMessage() && commitsModel.getObject().current.isEmpty());
			}
			
		});

		foot = new WebMarkupContainer("foot");
		foot.setOutputMarkupId(true);
		
		foot.add(new AjaxLink<Void>("more") {

			@Override
			protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
				super.updateAjaxAttributes(attributes);
				attributes.getAjaxCallListeners().add(new IndicateLoadingListener());
			}

			@Override
			public void onClick(AjaxRequestTarget target) {
				state.setStep(state.getStep()+1);
				
				Commits commits = commitsModel.getObject();
				int commitIndex = 0;
				int lastCommitIndex = 0;
				for (int i=0; i<commits.last.size(); i++) {
					RevCommit lastCommit = commits.last.get(i);
					RevCommit currentCommit = commits.current.get(i);
					if (lastCommit == null) {
						if (currentCommit == null) {
							if (!commits.last.get(i+1).name().equals(commits.current.get(i+1).name())) 
								replaceItem(target, i);
						} else {
							addCommitClass(replaceItem(target, i), commitIndex);
						}
					} else {
						if (currentCommit == null) {
							replaceItem(target, i);
						} else if (commitIndex != lastCommitIndex 
								|| !lastCommit.name().equals(currentCommit.name())){
							addCommitClass(replaceItem(target, i), commitIndex);
						}						
					}
					if (lastCommit != null)
						lastCommitIndex++;
					if (currentCommit != null)
						commitIndex++;
				}

				StringBuilder builder = new StringBuilder();
				for (int i=commits.last.size(); i<commits.current.size(); i++) {
					Component item = newCommitItem(commitsView.newChildId(), i);
					if (commits.current.get(i) != null)
						addCommitClass(item, commitIndex++);
					commitsView.add(item);
					target.add(item);
					builder.append(String.format("$('#repo-commits>.body>.list').append(\"<li id='%s'></li>\");", 
							item.getMarkupId()));
				}
				target.prependJavaScript(builder);
				target.add(feedback);
				target.add(foot);
				target.appendJavaScript(renderCommitGraph());
				pushState(target);
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(hasMore && state.getStep() < MAX_STEPS);
			}
			
		});
		foot.add(new WebMarkupContainer("tooMany") {
			
			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(state.getStep() == MAX_STEPS);
			}
			
		});
		add(foot);
	}
	
	private void updateCommits(AjaxRequestTarget target) {
		state.setStep(1);

		target.add(feedback);
		body.replace(commitsView = newCommitsView());
		target.add(body);
		target.add(foot);
		target.appendJavaScript(renderCommitGraph());
		pushState(target);
	}
	
	private void pushState(AjaxRequestTarget target) {
		PageParameters params = paramsOf(getDepot(), state);
		CharSequence url = RequestCycle.get().urlFor(DepotCommitsPage.class, params);
		pushState(target, url.toString(), state);
	}
	
	private RepeatingView newCommitsView() {
		RepeatingView commitsView = new RepeatingView("commits");
		commitsView.setOutputMarkupId(true);
		
		int commitIndex = 0;
		List<RevCommit> commits = commitsModel.getObject().current;
		for (int i=0; i<commits.size(); i++) {
			Component item = newCommitItem(commitsView.newChildId(), i);
			if (commits.get(i) != null)
				addCommitClass(item, commitIndex++);
			commitsView.add(item);
		}
		
		return commitsView;
	}
	
	private void addCommitClass(Component item, int commitIndex) {
		item.add(AttributeAppender.append("class", " commit-item-" + commitIndex));
	}
	
	private Component newCommitItem(String itemId, int index) {
		List<RevCommit> current = commitsModel.getObject().current;
		RevCommit commit = current.get(index);
		
		Fragment item;
		if (commit != null) {
			item = new Fragment(itemId, "commitFrag", this);
			item.add(new ContributorAvatars("avatar", commit.getAuthorIdent(), commit.getCommitterIdent()));

			item.add(new ExpandableCommitMessagePanel("message", depotModel, new LoadableDetachableModel<RevCommit>() {

				@Override
				protected RevCommit load() {
					return commitsModel.getObject().current.get(index);
				}
				
			}, new LoadableDetachableModel<List<Pattern>>() {

				@Override
				protected List<Pattern> load() {
					List<Pattern> patterns =  new ArrayList<>();
					QueryContext parseTree = state.getParseTree();
					if (parseTree != null) {
						for (CriteriaContext criteria: parseTree.criteria()) {
							if (criteria.message() != null) {
								String message = criteria.message().Value().getText();
								message = message.substring(1);
								message = message.substring(0, message.length()-1);
								message = JavaEscape.unescapeJava(message);
								patterns.add(Pattern.compile(message, Pattern.CASE_INSENSITIVE));
							}
						}
					}
					return patterns;
				}
				
			}));

			RepeatingView labelsView = new RepeatingView("labels");

			List<String> commitLabels = labelsModel.getObject().get(commit.name());
			if (commitLabels == null)
				commitLabels = new ArrayList<>();
			for (String label: commitLabels) 
				labelsView.add(new Label(labelsView.newChildId(), label));
			item.add(labelsView);
			
			item.add(new ContributorPanel("contribution", 
					commit.getAuthorIdent(), commit.getCommitterIdent(), true));
			
			/*
			 * If we query a single definitive path, let's record it to be used for 
			 * diff comparison and code browsing  
			 */
			String path = null;
			
			QueryContext parseTree = state.getParseTree();
			if (parseTree != null) {
				for (CriteriaContext criteria: parseTree.criteria()) {
					if (criteria.path() != null) {
						String value = criteria.path().Value().getText();
						value = value.substring(1);
						value = value.substring(0, value.length()-1);
						if (value.contains("*") || path != null) {
							path = null;
							break;
						} else {
							path = value;
						}
					}
				}
			}

			DepotFilePage.State browseState = new DepotFilePage.State();
			browseState.blobIdent = new BlobIdent(commit.name(), null, FileMode.TYPE_TREE);
			browseState.blobIdent.path = path;
			PageParameters params = DepotFilePage.paramsOf(depotModel.getObject(), browseState);
			item.add(new BookmarkablePageLink<Void>("browseCode", DepotFilePage.class, params));
			
			if (state.getCompareWith() != null) {
				RevisionComparePage.State compareState = new RevisionComparePage.State();
				compareState.leftSide = new DepotAndRevision(getDepot(), commit.name());
				compareState.rightSide = new DepotAndRevision(getDepot(), DepotCommitsPage.this.state.getCompareWith());
				compareState.pathFilter = path;
				compareState.tabPanel = RevisionComparePage.TabPanel.CHANGES;
				
				params = RevisionComparePage.paramsOf(getDepot(), compareState);
				item.add(new BookmarkablePageLink<Void>("compare", RevisionComparePage.class, params));
			} else {
				item.add(new WebMarkupContainer("compare").setVisible(false));
			}

			CommitDetailPage.State commitState = new CommitDetailPage.State();
			commitState.revision = commit.name();
			params = CommitDetailPage.paramsOf(depotModel.getObject(), commitState);
			Link<Void> hashLink = new BookmarkablePageLink<Void>("hashLink", CommitDetailPage.class, params);
			item.add(hashLink);
			hashLink.add(new Label("hash", GitUtils.abbreviateSHA(commit.name())));
			item.add(new WebMarkupContainer("copyHash").add(new CopyClipboardBehavior(Model.of(commit.name()))));

			item.add(AttributeAppender.append("class", "commit clearfix"));
		} else {
			item = new Fragment(itemId, "dateFrag", this);
			DateTime dateTime = new DateTime(current.get(index+1).getCommitterIdent().getWhen());
			item.add(new Label("date", WebConstants.DATE_FORMATTER.print(dateTime)));
			item.add(AttributeAppender.append("class", "date"));
		}
		item.setOutputMarkupId(true);
		
		return item;
	}
	
	public static PageParameters paramsOf(Depot depot, State state) {
		PageParameters params = paramsOf(depot);
		if (state.getCompareWith() != null)
			params.set(PARAM_COMPARE_WITH, state.getCompareWith());
		if (state.getQuery() != null)
			params.set(PARAM_QUERY, state.getQuery());
		if (state.getStep() != 1)
			params.set(PARAM_STEP, state.getStep());
		return params;
	}
	
	@Override
	protected void onSelect(AjaxRequestTarget target, Depot depot) {
		setResponsePage(DepotCommitsPage.class, paramsOf(depot));
	}

	@Override
	protected void onPopState(AjaxRequestTarget target, Serializable data) {
		super.onPopState(target, data);
		
		state = (State) data;

		target.add(queryForm);
		
		body.replace(commitsView = newCommitsView());
		target.add(body);
		target.add(foot);
		
		target.appendJavaScript(renderCommitGraph());
	}
	
	private String renderCommitGraph() {
		String jsonOfCommits = CommitGraphUtils.asJSON(commitsModel.getObject().current);
		return String.format("gitplex.server.commitgraph.render('%s', %s);", body.getMarkupId(), jsonOfCommits);
	}

	@Override
	protected void onDetach() {
		commitsModel.detach();
		labelsModel.detach();
		
		super.onDetach();
	}

	private List<RevCommit> separateByDate(List<RevCommit> commits) {
		List<RevCommit> separated = new ArrayList<>();
		DateTime groupTime = null;
		for (RevCommit commit: commits) {
			DateTime commitTime = new DateTime(commit.getCommitterIdent().getWhen());
			if (groupTime == null || commitTime.getYear() != groupTime.getYear() 
					|| commitTime.getDayOfYear() != groupTime.getDayOfYear()) {
				groupTime = commitTime;
				separated.add(null);
			} 
			separated.add(commit);
		}
		return separated;
	}
	
	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		
		response.render(JavaScriptHeaderItem.forReference(new CommitGraphResourceReference()));
		response.render(CssHeaderItem.forReference(new DepotCommitsResourceReference()));
		response.render(OnDomReadyHeaderItem.forScript(renderCommitGraph()));
	}
	
	/*
	 * We do not use "--date-order", "--topo-order" or "--author-order" option of git log to 
	 * retrieve commits as they can be slow. However use the default log ordering has the 
	 * possibility of returning parent before child which can corrupt the commit lane. To 
	 * solve this issue, we sort the commits in memory so that parent always comes after 
	 * child. When more commits are loaded via "more" button, it is possible that some 
	 * commits displayed previously can be parent of commits loaded lately and should be 
	 * moved. To work around this issue, we calculate exact order of commits being displayed 
	 * as "last", and calculate commits will be displayed as "current". Then we compare them 
	 * to see which commit item in the page should be replaced, and which should be added. 
	 *  
	 * @author robin
	 *
	 */
	private static class Commits {
		List<RevCommit> last;
		
		List<RevCommit> current;
	}
	
	public static class State implements Serializable {

		private static final long serialVersionUID = 1L;

		private String compareWith;
		
		private String query;
		
		private int step = 1;
		
		private transient Optional<QueryContext> parseTree;
		
		public State() {
		}
		
		public State(PageParameters params) {
			compareWith = params.get(PARAM_COMPARE_WITH).toString();
			query = params.get(PARAM_QUERY).toString();
			
			Integer step = params.get(PARAM_STEP).toOptionalInteger();
			if (step != null)
				this.step = step.intValue();		
		}

		public String getCompareWith() {
			return compareWith;
		}

		public void setCompareWith(String compareWith) {
			this.compareWith = compareWith;
		}

		public String getQuery() {
			return query;
		}

		public void setQuery(String query) {
			this.query = query;
			parseTree = null;
		}

		public int getStep() {
			return step;
		}

		public void setStep(int step) {
			this.step = step;
		}
		
		@Nullable
		public QueryContext getParseTree() {
			if (parseTree == null) {
				if (query != null) {
					ANTLRInputStream is = new ANTLRInputStream(query); 
					CommitQueryLexer lexer = new CommitQueryLexer(is);
					lexer.removeErrorListeners();
					CommonTokenStream tokens = new CommonTokenStream(lexer);
					CommitQueryParser parser = new CommitQueryParser(tokens);
					parser.removeErrorListeners();
					parser.setErrorHandler(new BailErrorStrategy());
					parseTree = Optional.of(parser.query());
				} else {
					parseTree = Optional.fromNullable(null);
				}
			}
			return parseTree.orNull();
		}
		
		public void applyTo(RevListCommand command) {
			if (step > MAX_STEPS)
				throw new RuntimeException("Step should be no more than " + MAX_STEPS);
			
			command.count(step*COUNT);

			QueryContext parseTree = getParseTree();
			if (parseTree != null) { 
				new ParseTreeWalker().walk(new RevListCommandFiller(command), parseTree);
				if (command.revisions().isEmpty() && compareWith != null)
					command.revisions(Lists.newArrayList(compareWith));
			} else if (compareWith != null) {
				command.revisions(Lists.newArrayList(compareWith));
			}
		}
		
	}

}