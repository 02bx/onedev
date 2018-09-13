package io.onedev.server.web.behavior;

import static io.onedev.server.model.support.issue.IssueConstants.FIELD_COMMENT;
import static io.onedev.server.model.support.issue.IssueConstants.FIELD_COMMENT_COUNT;
import static io.onedev.server.model.support.issue.IssueConstants.FIELD_DESCRIPTION;
import static io.onedev.server.model.support.issue.IssueConstants.FIELD_MILESTONE;
import static io.onedev.server.model.support.issue.IssueConstants.FIELD_NUMBER;
import static io.onedev.server.model.support.issue.IssueConstants.FIELD_STATE;
import static io.onedev.server.model.support.issue.IssueConstants.FIELD_SUBMIT_DATE;
import static io.onedev.server.model.support.issue.IssueConstants.FIELD_TITLE;
import static io.onedev.server.model.support.issue.IssueConstants.FIELD_UPDATE_DATE;
import static io.onedev.server.model.support.issue.IssueConstants.FIELD_VOTE_COUNT;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import io.onedev.codeassist.FenceAware;
import io.onedev.codeassist.InputSuggestion;
import io.onedev.codeassist.grammar.LexerRuleRefElementSpec;
import io.onedev.codeassist.parser.Element;
import io.onedev.codeassist.parser.ParseExpect;
import io.onedev.codeassist.parser.TerminalExpect;
import io.onedev.server.OneDev;
import io.onedev.server.exception.OneException;
import io.onedev.server.manager.BuildManager;
import io.onedev.server.manager.CacheManager;
import io.onedev.server.manager.IssueManager;
import io.onedev.server.manager.PullRequestManager;
import io.onedev.server.manager.TeamManager;
import io.onedev.server.model.Build;
import io.onedev.server.model.Issue;
import io.onedev.server.model.Project;
import io.onedev.server.model.PullRequest;
import io.onedev.server.model.support.issue.IssueConstants;
import io.onedev.server.search.entity.issue.IssueQuery;
import io.onedev.server.search.entity.issue.IssueQueryLexer;
import io.onedev.server.search.entity.issue.IssueQueryParser;
import io.onedev.server.security.SecurityUtils;
import io.onedev.server.util.DateUtils;
import io.onedev.server.util.EditContext;
import io.onedev.server.util.OneContext;
import io.onedev.server.util.facade.UserFacade;
import io.onedev.server.util.inputspec.BuildChoiceInput;
import io.onedev.server.util.inputspec.InputContext;
import io.onedev.server.util.inputspec.InputSpec;
import io.onedev.server.util.inputspec.IssueChoiceInput;
import io.onedev.server.util.inputspec.PullRequestChoiceInput;
import io.onedev.server.util.inputspec.booleaninput.BooleanInput;
import io.onedev.server.util.inputspec.choiceinput.ChoiceInput;
import io.onedev.server.util.inputspec.dateinput.DateInput;
import io.onedev.server.util.inputspec.numberinput.NumberInput;
import io.onedev.server.util.inputspec.teamchoiceinput.TeamChoiceInput;
import io.onedev.server.util.inputspec.textinput.TextInput;
import io.onedev.server.util.inputspec.userchoiceinput.UserChoiceInput;
import io.onedev.server.web.behavior.inputassist.ANTLRAssistBehavior;
import io.onedev.server.web.behavior.inputassist.InputAssistBehavior;
import io.onedev.server.web.util.SuggestionUtils;
import io.onedev.utils.Range;

@SuppressWarnings("serial")
public class IssueQueryBehavior extends ANTLRAssistBehavior {

	private final IModel<Project> projectModel;
	
	private static final String VALUE_OPEN = "\"";
	
	private static final String VALUE_CLOSE = "\"";

	private static final String ESCAPE_CHARS = "\"\\";
	
	private static final int MAX_ISSUE_TITLE_LEN = 75;
	
	public IssueQueryBehavior(IModel<Project> projectModel) {
		super(IssueQueryParser.class, "query", false);
		this.projectModel = projectModel;
	}

	@Override
	public void detach(Component component) {
		super.detach(component);
		projectModel.detach();
	}
	
	private Project getProject() {
		return projectModel.getObject();
	}

	@Override
	protected List<InputSuggestion> suggest(TerminalExpect terminalExpect) {
		if (terminalExpect.getElementSpec() instanceof LexerRuleRefElementSpec) {
			LexerRuleRefElementSpec spec = (LexerRuleRefElementSpec) terminalExpect.getElementSpec();
			if (spec.getRuleName().equals("Quoted")) {
				return new FenceAware(codeAssist.getGrammar(), VALUE_OPEN, VALUE_CLOSE) {

					private List<InputSuggestion> getUserSuggestions(String matchWith) {
						List<InputSuggestion> suggestions = new ArrayList<>();
						for (UserFacade user: OneDev.getInstance(CacheManager.class).getUsers().values()) {
							Range match = Range.match(user.getName(), matchWith, true, false, true);
							if (match != null) {
								String description;
								if (!user.getDisplayName().equals(user.getName()))
									description = user.getDisplayName();
								else
									description = null;
								suggestions.add(new InputSuggestion(user.getName(), description, match).escape("\"\\"));
							}
						}
						return suggestions;
					}
					
					private List<InputSuggestion> getBuildSuggestions(String matchWith) {
						List<InputSuggestion> suggestions = new ArrayList<>();
						for (Build build: OneDev.getInstance(BuildManager.class).query(getProject(), matchWith, InputAssistBehavior.MAX_SUGGESTIONS)) {
							if (matchWith.contains(Build.FQN_SEPARATOR)) 
								matchWith = StringUtils.substringAfter(matchWith, Build.FQN_SEPARATOR);
							Range match = Range.match(build.getName(), matchWith, true, false, true);
							if (match != null) {
								int offset = build.getConfiguration().getName().length()+1;
								match = new Range(match.getFrom() + offset, match.getTo() + offset);
							}
							suggestions.add(new InputSuggestion(build.getFQN(), null, match).escape("\"\\"));
						}
						return suggestions;
					}
					
					@Override
					protected List<InputSuggestion> match(String unfencedMatchWith) {
						String unfencedLowerCaseMatchWith = unfencedMatchWith.toLowerCase();
						List<InputSuggestion> suggestions = new ArrayList<>();
						Project project = getProject();

						if ("criteriaField".equals(spec.getLabel())) {
							List<String> candidates = new ArrayList<>(IssueConstants.QUERY_FIELDS);
							for (InputSpec field: project.getIssueWorkflow().getFieldSpecs())
								candidates.add(field.getName());
							suggestions.addAll(getSuggestions(candidates, unfencedLowerCaseMatchWith, ESCAPE_CHARS));
						} else if ("orderField".equals(spec.getLabel())) {
							List<String> candidates = new ArrayList<>(IssueConstants.ORDER_FIELDS.keySet());
							for (InputSpec field: project.getIssueWorkflow().getFieldSpecs()) {
								if (field instanceof NumberInput || field instanceof ChoiceInput || field instanceof DateInput) 
									candidates.add(field.getName());
							}
							suggestions.addAll(getSuggestions(candidates, unfencedLowerCaseMatchWith, ESCAPE_CHARS));
						} else if ("revisionValue".equals(spec.getLabel())) {
							String revisionType = terminalExpect.getState()
									.findMatchedElementsByLabel("revisionType", true).iterator().next().getMatchedText();
							switch (revisionType) {
							case "branch":
								suggestions.addAll(SuggestionUtils.suggestBranch(project, unfencedLowerCaseMatchWith, ESCAPE_CHARS));
								break;
							case "tag":
								suggestions.addAll(SuggestionUtils.suggestTag(project, unfencedLowerCaseMatchWith, ESCAPE_CHARS));
								break;
							case "build":
								suggestions.addAll(getBuildSuggestions(unfencedLowerCaseMatchWith));
								break;
							}
						} else if ("criteriaValue".equals(spec.getLabel())) {
							List<Element> fieldElements = terminalExpect.getState().findMatchedElementsByLabel("criteriaField", true);
							List<Element> operatorElements = terminalExpect.getState().findMatchedElementsByLabel("operator", true);
							Preconditions.checkState(operatorElements.size() == 1);
							String operatorName = StringUtils.normalizeSpace(operatorElements.get(0).getMatchedText());
							int operator = IssueQuery.getOperator(operatorName);							
							if (fieldElements.isEmpty()) {
								if (operator == IssueQueryLexer.SubmittedBy)
									suggestions.addAll(getUserSuggestions(unfencedLowerCaseMatchWith));
								else
									suggestions.addAll(getBuildSuggestions(unfencedLowerCaseMatchWith));
							} else {
								String fieldName = IssueQuery.getValue(fieldElements.get(0).getMatchedText());
								
								try {
									IssueQuery.checkField(project, fieldName, operator);
									InputSpec fieldSpec = project.getIssueWorkflow().getFieldSpec(fieldName);
									if (fieldSpec instanceof DateInput || fieldName.equals(FIELD_SUBMIT_DATE) 
											|| fieldName.equals(FIELD_UPDATE_DATE)) {
										suggestions.addAll(getSuggestions(DateUtils.RELAX_DATE_EXAMPLES, unfencedLowerCaseMatchWith, null));
										CollectionUtils.addIgnoreNull(suggestions, suggestToFence(terminalExpect, unfencedMatchWith));
									} else if (fieldSpec instanceof UserChoiceInput) {
										suggestions.addAll(getUserSuggestions(unfencedLowerCaseMatchWith));
									} else if (fieldSpec instanceof IssueChoiceInput) {
										List<Issue> issues = OneDev.getInstance(IssueManager.class).query(project, unfencedLowerCaseMatchWith, InputAssistBehavior.MAX_SUGGESTIONS);		
										for (Issue issue: issues) {
											InputSuggestion suggestion = new InputSuggestion("#" + issue.getNumber(), StringUtils.abbreviate(issue.getTitle(), MAX_ISSUE_TITLE_LEN), null);
											suggestions.add(suggestion);
										}
									} else if (fieldSpec instanceof BuildChoiceInput) {
										List<Build> builds = OneDev.getInstance(BuildManager.class).query(project, unfencedLowerCaseMatchWith, InputAssistBehavior.MAX_SUGGESTIONS);		
										for (Build build: builds) {
											InputSuggestion suggestion = new InputSuggestion(build.getName(), null, null);
											suggestions.add(suggestion);
										}
									} else if (fieldSpec instanceof PullRequestChoiceInput) {
										List<PullRequest> requests = OneDev.getInstance(PullRequestManager.class).query(project, unfencedLowerCaseMatchWith, InputAssistBehavior.MAX_SUGGESTIONS);		
										for (PullRequest request: requests) {
											InputSuggestion suggestion = new InputSuggestion("#" + request.getNumber(), StringUtils.abbreviate(request.getTitle(), MAX_ISSUE_TITLE_LEN), null);
											suggestions.add(suggestion);
										}
									} else if (fieldSpec instanceof BooleanInput) {
										suggestions.addAll(getSuggestions(Lists.newArrayList("true", "false"), unfencedLowerCaseMatchWith, null));
									} else if (fieldSpec instanceof TeamChoiceInput) {
										List<String> candidates = OneDev.getInstance(TeamManager.class).query().stream().map(it->it.getName()).collect(Collectors.toList());
										suggestions.addAll(getSuggestions(candidates, unfencedLowerCaseMatchWith, ESCAPE_CHARS));
									} else if (fieldName.equals(FIELD_STATE)) {
										List<String> candidates = project.getIssueWorkflow().getStateSpecs().stream().map(it->it.getName()).collect(Collectors.toList());
										suggestions.addAll(getSuggestions(candidates, unfencedLowerCaseMatchWith, ESCAPE_CHARS));
									} else if (fieldSpec instanceof ChoiceInput) {
										OneContext.push(newOneContext());
										try {
											List<String> candidates = new ArrayList<>(((ChoiceInput)fieldSpec).getChoiceProvider().getChoices(true).keySet());
											suggestions.addAll(getSuggestions(candidates, unfencedLowerCaseMatchWith, ESCAPE_CHARS));
										} finally {
											OneContext.pop();
										}			
									} else if (fieldName.equals(FIELD_MILESTONE)) {
										List<String> candidates = project.getMilestones().stream().map(it->it.getName()).collect(Collectors.toList());
										suggestions.addAll(getSuggestions(candidates, unfencedLowerCaseMatchWith, ESCAPE_CHARS));
									} else if (fieldName.equals(FIELD_TITLE) || fieldName.equals(FIELD_DESCRIPTION) 
											|| fieldName.equals(FIELD_COMMENT) || fieldName.equals(FIELD_VOTE_COUNT) 
											|| fieldName.equals(FIELD_COMMENT_COUNT) || fieldName.equals(FIELD_NUMBER) 
											|| fieldSpec instanceof NumberInput || fieldSpec instanceof TextInput) {
										return null;
									}
								} catch (OneException ex) {
								}
							}
						}
						return suggestions;
					}
					
					private OneContext newOneContext() {
						return new OneContext() {

							@Override
							public Project getProject() {
								return IssueQueryBehavior.this.getProject();
							}

							@Override
							public EditContext getEditContext(int level) {
								return new EditContext() {

									@Override
									public Object getInputValue(String name) {
										return null;
									}
									
								};
							}

							@Override
							public InputContext getInputContext() {
								throw new UnsupportedOperationException();
							}
							
						};
					}

					@Override
					protected String getFencingDescription() {
						return "quote as literal value";
					}
					
				}.suggest(terminalExpect);
			}
		} 
		return null;
	}
	
	@Override
	protected Optional<String> describe(ParseExpect parseExpect, String suggestedLiteral) {
		if (suggestedLiteral.equals("mine")) {
			if (SecurityUtils.getUser() != null)
				return Optional.of("issues relevant to me");
			else
				return null;
		} 

		if ((suggestedLiteral.equals("is me") || suggestedLiteral.equals("is not me")) && SecurityUtils.getUser() == null)
			return null;	
		
		if (suggestedLiteral.equals("open"))
			return Optional.of("issues with state in open category");
		else if (suggestedLiteral.equals("closed"))
			return Optional.of("issues with state in closed category");
		
		parseExpect = parseExpect.findExpectByLabel("operator");
		if (parseExpect != null) {
			List<Element> fieldElements = parseExpect.getState().findMatchedElementsByLabel("criteriaField", false);
			if (!fieldElements.isEmpty()) {
				String fieldName = IssueQuery.getValue(fieldElements.iterator().next().getMatchedText());
				try {
					IssueQuery.checkField(getProject(), fieldName, IssueQuery.getOperator(suggestedLiteral));
				} catch (OneException e) {
					return null;
				}
			}
		}
		return super.describe(parseExpect, suggestedLiteral);
	}

}
