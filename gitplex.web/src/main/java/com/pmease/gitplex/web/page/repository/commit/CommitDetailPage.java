package com.pmease.gitplex.web.page.repository.commit;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
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
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TagCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import com.google.common.base.Preconditions;
import com.pmease.commons.git.GitUtils;
import com.pmease.commons.wicket.assets.oneline.OnelineResourceReference;
import com.pmease.commons.wicket.component.modal.ModalLink;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.manager.AuxiliaryManager;
import com.pmease.gitplex.core.model.Comment;
import com.pmease.gitplex.core.model.PullRequest;
import com.pmease.gitplex.core.model.Repository;
import com.pmease.gitplex.core.security.SecurityUtils;
import com.pmease.gitplex.web.component.avatar.ContributorAvatars;
import com.pmease.gitplex.web.component.contributionpanel.ContributionPanel;
import com.pmease.gitplex.web.component.diff.revision.RevisionDiffPanel;
import com.pmease.gitplex.web.component.diff.revision.option.DiffOptionPanel;
import com.pmease.gitplex.web.component.hashandcode.HashAndCodePanel;
import com.pmease.gitplex.web.page.repository.RepositoryPage;
import com.pmease.gitplex.web.page.repository.branches.RepoBranchesPage;
import com.pmease.gitplex.web.page.repository.file.RepoFilePage;
import com.pmease.gitplex.web.page.repository.file.RepoFileState;
import com.pmease.gitplex.web.page.repository.tags.RepoTagsPage;

import de.agilecoders.wicket.core.markup.html.bootstrap.common.NotificationPanel;

@SuppressWarnings("serial")
public class CommitDetailPage extends RepositoryPage {

	private static final String PARAM_REVISION = "revision";
	
	// make sure to use a different value from wicket:id according to wicket bug:
	// https://issues.apache.org/jira/browse/WICKET-6069
	private static final String PARAM_COMPARE_WITH = "compareWith";
	
	private static final String PARAM_PATH = "path";
	
	protected String revision;
	
	private HistoryState state;
	
	private IModel<RevCommit> commitModel = new LoadableDetachableModel<RevCommit>() {

		@Override
		protected RevCommit load() {
			try(	FileRepository jgitRepo = getRepository().openAsJGitRepo();
					RevWalk revWalk = new RevWalk(jgitRepo);) {
				ObjectId objectId;
				if (GitUtils.isHash(revision))
					objectId = ObjectId.fromString(revision);
				else
					objectId = jgitRepo.resolve(revision);
				return revWalk.parseCommit(objectId);
			} catch (RevisionSyntaxException | IOException e) {
				throw new RuntimeException(e);
			}
		}
		
	};
	
	private DiffOptionPanel diffOption;
	
	private RevisionDiffPanel compareResult;
	
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
		
		add(new HashAndCodePanel("hashAndCode", repoModel, getCommit().getId().name()));
		
		add(new ModalLink("createBranch") {

			private String branchName;
			
			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(SecurityUtils.canCreate(getRepository(), UUID.randomUUID().toString()));
			}

			@Override
			protected Component newContent(String id) {
				Fragment fragment = new Fragment(id, "createBranchFrag", CommitDetailPage.this);
				Form<?> form = new Form<Void>("form");
				form.setOutputMarkupId(true);
				form.add(new NotificationPanel("feedback", form));
				branchName = null;
				form.add(new TextField<String>("name", new IModel<String>() {

					@Override
					public void detach() {
					}

					@Override
					public String getObject() {
						return branchName;
					}

					@Override
					public void setObject(String object) {
						branchName = object;
					}
					
				}).setOutputMarkupId(true));

				form.add(new AjaxButton("create") {

					@Override
					protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
						super.onSubmit(target, form);
						
						if (branchName == null) {
							form.error("Branch name is required.");
							target.focusComponent(form.get("name"));
							target.add(form);
						} else {
							String branchRef = GitUtils.branch2ref(branchName);
							if (getRepository().getObjectId(branchRef, false) != null) {
								form.error("Branch '" + branchName + "' already exists, please choose a different name.");
								target.add(form);
							} else {
								getRepository().git().createBranch(branchName, revision);
								setResponsePage(RepoBranchesPage.class, RepoBranchesPage.paramsOf(getRepository()));
							}
						}
					}

				});
				form.add(new AjaxLink<Void>("cancel") {

					@Override
					public void onClick(AjaxRequestTarget target) {
						close(target);
					}
					
				});
				fragment.add(form);
				return fragment;
			}
			
		});
		
		add(new ModalLink("createTag") {

			private String tagName;
			
			private String tagMessage;
			
			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(SecurityUtils.canModify(getRepository(), Constants.R_TAGS + UUID.randomUUID().toString()));
			}

			@Override
			protected Component newContent(String id) {
				Fragment fragment = new Fragment(id, "createTagFrag", CommitDetailPage.this);
				Form<?> form = new Form<Void>("form");
				form.setOutputMarkupId(true);
				form.add(new NotificationPanel("feedback", form));
				tagName = null;
				form.add(new TextField<String>("name", new IModel<String>() {

					@Override
					public void detach() {
					}

					@Override
					public String getObject() {
						return tagName;
					}

					@Override
					public void setObject(String object) {
						tagName = object;
					}
					
				}).setOutputMarkupId(true));
				
				tagMessage = null;
				form.add(new TextArea<String>("message", new IModel<String>() {

					@Override
					public void detach() {
					}

					@Override
					public String getObject() {
						return tagMessage;
					}

					@Override
					public void setObject(String object) {
						tagMessage = object;
					}
					
				}));
				form.add(new AjaxButton("create") {

					@Override
					protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
						super.onSubmit(target, form);
						
						if (tagName == null) {
							form.error("Tag name is required.");
							target.focusComponent(form.get("name"));
							target.add(form);
						} else {
							String tagRef = GitUtils.tag2ref(tagName);
							if (getRepository().getObjectId(tagRef, false) != null) {
								form.error("Tag '" + tagName + "' already exists, please choose a different name.");
								target.add(form);
							} else {
								try (FileRepository jgitRepo = getRepository().openAsJGitRepo();) {
									Git git = Git.wrap(jgitRepo);
									TagCommand tag = git.tag();
									tag.setName(tagName);
									if (tagMessage != null)
										tag.setMessage(tagMessage);
									tag.setTagger(getCurrentUser().asPerson());
									tag.setObjectId(getRepository().getRevCommit(revision));
									tag.call();
								} catch (GitAPIException e) {
									throw new RuntimeException(e);
								}
								setResponsePage(RepoTagsPage.class, RepoTagsPage.paramsOf(getRepository()));
							}
						}
					}

				});
				form.add(new AjaxLink<Void>("cancel") {

					@Override
					public void onClick(AjaxRequestTarget target) {
						close(target);
					}
					
				});
				fragment.add(form);
				return fragment;
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
								.getDescendants(getRepository(), getCommit().getId());
						descendants.add(getCommit().getId());
						
						List<Ref> branchRefs = new ArrayList<>();
						for (Ref ref: getRepository().getBranchRefs()) {
							if (descendants.contains(ref.getObjectId())) {
								branchRefs.add(ref);
							}
						}
						
						List<Ref> tagRefs = new ArrayList<>();
						for (Ref ref: getRepository().getTagRefs()) {
							RevCommit taggedCommit = getRepository().getRevCommit(ref.getObjectId());
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
							RepoFileState state = new RepoFileState();
							state.blobIdent.revision = branch;
							Link<Void> link = new BookmarkablePageLink<Void>("link", RepoFilePage.class, 
									RepoFilePage.paramsOf(repoModel.getObject(), state));
							link.add(new Label("label", branch));
							item.add(link);
							item.add(AttributeAppender.append("class", "branch"));
						} else {
							String tag = Preconditions.checkNotNull(GitUtils.ref2tag(ref));
							RepoFileState state = new RepoFileState();
							state.blobIdent.revision = tag;
							Link<Void> link = new BookmarkablePageLink<Void>("link", RepoFilePage.class, 
									RepoFilePage.paramsOf(repoModel.getObject(), state));
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
		add(new ContributionPanel("contribution", getCommit().getAuthorIdent(), getCommit().getCommitterIdent()));

		final WebMarkupContainer parentsContainer = new WebMarkupContainer("parents");
		parentsContainer.setOutputMarkupId(true);
		add(parentsContainer);
		if (getParents().size() == 1) {
			String parent = getParents().get(0);
			Link<Void> link = new BookmarkablePageLink<Void>("parent", CommitDetailPage.class, 
					paramsOf(repoModel.getObject(), parent));
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
							paramsOf(repoModel.getObject(), parent));
					link.add(new Label("label", GitUtils.abbreviateSHA(parent)));
					item.add(link);
					
					item.add(new AjaxLink<Void>("diff") {

						@Override
						public void onClick(AjaxRequestTarget target) {
							setCompareWith(parent); 
							target.add(parentsContainer);
							newCompareResult(target);
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
			add(diffOption = new DiffOptionPanel("compareOption", repoModel, getCommit().name()) {
	
				@Override
				protected void onSelectPath(AjaxRequestTarget target, String path) {
					CommitDetailPage.this.state.path = path;
					newCompareResult(target);
					pushState(target);
				}
	
				@Override
				protected void onLineProcessorChange(AjaxRequestTarget target) {
					newCompareResult(target);
				}
	
				@Override
				protected void onDiffModeChange(AjaxRequestTarget target) {
					newCompareResult(target);
				}
	
			});
			newCompareResult(null);
		} else {
			add(new WebMarkupContainer("compareOption").setVisible(false));
			add(new WebMarkupContainer("compareResult").setVisible(false));
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
	
	private void newCompareResult(@Nullable AjaxRequestTarget target) {
		compareResult = new RevisionDiffPanel("compareResult", repoModel,  
				Model.of((PullRequest)null), Model.of((Comment)null), getCompareWith(), 
				getCommit().name(), state.path, state.path, diffOption.getLineProcessor(), 
				diffOption.getDiffMode()) {

			@Override
			protected void onClearPath(AjaxRequestTarget target) {
				state.path = null;
				newCompareResult(target);
				pushState(target);
			}
			
		};
		compareResult.setOutputMarkupId(true);
		if (target != null) {
			replace(compareResult);
			target.add(compareResult);
		} else {
			add(compareResult);
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

	public static PageParameters paramsOf(Repository repository, String revision) {
		return paramsOf(repository, revision, new HistoryState());
	}
	
	public static PageParameters paramsOf(Repository repository, String revision, HistoryState state) {
		PageParameters params = paramsOf(repository);
		params.set(PARAM_REVISION, revision);
		if (state.compareWith != null)
			params.set(PARAM_COMPARE_WITH, state.compareWith);
		if (state.path != null)
			params.set(PARAM_PATH, state.path);
		return params;
	}
	
	@Override
	protected void onSelect(AjaxRequestTarget target, Repository repository) {
		setResponsePage(RepoCommitsPage.class, paramsOf(repository));
	}
	
	@Override
	protected void onDetach() {
		commitModel.detach();
		
		super.onDetach();
	}
	
	private void pushState(AjaxRequestTarget target) {
		PageParameters params = paramsOf(getRepository(), getCommit().name(), state);
		CharSequence url = RequestCycle.get().urlFor(CommitDetailPage.class, params);
		pushState(target, url.toString(), state);
	}
	
	@Override
	protected void onPopState(AjaxRequestTarget target, Serializable data) {
		super.onPopState(target, data);
		
		state = (HistoryState) data;
		newCompareResult(target);
	}
	
	public static class HistoryState implements Serializable {

		private static final long serialVersionUID = 1L;
		
		public String compareWith;
		
		public String path;
		
		public HistoryState() {
		}
		
		public HistoryState(PageParameters params) {
			compareWith = params.get(PARAM_COMPARE_WITH).toString();
			path = params.get(PARAM_PATH).toString();
		}
		
	}
}
