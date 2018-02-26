package com.turbodev.server.web.page.project.tags;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PageableListView;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTag;

import com.turbodev.utils.StringUtils;
import com.turbodev.server.TurboDev;
import com.turbodev.server.git.BlobIdent;
import com.turbodev.server.git.GitUtils;
import com.turbodev.server.git.RefInfo;
import com.turbodev.server.manager.VerificationManager;
import com.turbodev.server.model.Project;
import com.turbodev.server.model.support.TagProtection;
import com.turbodev.server.security.SecurityUtils;
import com.turbodev.server.util.Verification;
import com.turbodev.server.web.behavior.OnTypingDoneBehavior;
import com.turbodev.server.web.component.contributorpanel.ContributorPanel;
import com.turbodev.server.web.component.datatable.HistoryAwarePagingNavigator;
import com.turbodev.server.web.component.link.ArchiveMenuLink;
import com.turbodev.server.web.component.link.UserLink;
import com.turbodev.server.web.component.link.ViewStateAwarePageLink;
import com.turbodev.server.web.component.modal.ModalLink;
import com.turbodev.server.web.component.modal.ModalPanel;
import com.turbodev.server.web.component.revisionpicker.RevisionPicker;
import com.turbodev.server.web.component.verification.VerificationStatusPanel;
import com.turbodev.server.web.page.project.NoCommitsPage;
import com.turbodev.server.web.page.project.ProjectPage;
import com.turbodev.server.web.page.project.blob.ProjectBlobPage;
import com.turbodev.server.web.page.project.commit.CommitDetailPage;
import com.turbodev.server.web.util.PagingHistorySupport;
import com.turbodev.server.web.util.ajaxlistener.ConfirmListener;

import de.agilecoders.wicket.core.markup.html.bootstrap.common.NotificationPanel;

@SuppressWarnings("serial")
public class ProjectTagsPage extends ProjectPage {

	private static final String PARAM_CURRENT_PAGE = "currentPage";
	
	private WebMarkupContainer tagsContainer;
	
	private PagingNavigator pagingNavigator;
	
	private WebMarkupContainer noTagsContainer;
	
	public ProjectTagsPage(PageParameters params) {
		super(params);
		
		if (getProject().getDefaultBranch() == null) 
			throw new RestartResponseException(NoCommitsPage.class, paramsOf(getProject()));
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		TextField<String> searchField;
		add(searchField = new TextField<String>("searchTags", Model.of("")));
		searchField.add(new OnTypingDoneBehavior(200) {

			@Override
			protected void onTypingDone(AjaxRequestTarget target) {
				target.add(tagsContainer);
				target.add(pagingNavigator);
				target.add(noTagsContainer);
			}
			
		});
		
		add(new ModalLink("createTag") {

			private String tagName;
			
			private String tagMessage;
			
			private String tagRevision = getProject().getDefaultBranch();
			
			@Override
			protected void onConfigure() {
				super.onConfigure();
				
				setVisible(SecurityUtils.canCreateTag(getProject(), Constants.R_TAGS));
			}

			private RevisionPicker newRevisionPicker() {
				return new RevisionPicker("revision", projectModel, tagRevision) {

					@Override
					protected void onSelect(AjaxRequestTarget target, String revision) {
						tagRevision = revision; 
						RevisionPicker revisionPicker = newRevisionPicker();
						getParent().replace(revisionPicker);
						target.add(revisionPicker);
					}
					
				};
			}
			
			@Override
			protected Component newContent(String id, ModalPanel modal) {
				Fragment fragment = new Fragment(id, "createTagFrag", ProjectTagsPage.this);
				Form<?> form = new Form<Void>("form");
				form.setOutputMarkupId(true);
				form.add(new NotificationPanel("feedback", form));
				tagName = null;
				final TextField<String> nameInput;
				form.add(nameInput = new TextField<String>("name", new IModel<String>() {

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
					
				}));
				nameInput.setOutputMarkupId(true);
				
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
				form.add(newRevisionPicker());
				form.add(new AjaxButton("create") {

					@Override
					protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
						super.onSubmit(target, form);
						
						if (tagName == null) {
							form.error("Tag name is required.");
							target.focusComponent(nameInput);
							target.add(form);
						} else if (!Repository.isValidRefName(Constants.R_HEADS + tagName)) {
							form.error("Invalid tag name.");
							target.focusComponent(nameInput);
							target.add(form);
						} else if (getProject().getObjectId(GitUtils.tag2ref(tagName), false) != null) {
							form.error("Tag '" + tagName + "' already exists, please choose a different name.");
							target.focusComponent(nameInput);
							target.add(form);
						} else {
							Project project = getProject();
							TagProtection protection = project.getTagProtection(tagName, getLoginUser());
							if (protection != null && protection.isNoCreation()) {
								form.error("Unable to create protected tag");
								target.focusComponent(nameInput);
								target.add(form);
							} else {
								getProject().tag(tagName, tagRevision, getLoginUser().asPerson(), tagMessage);
								modal.close();
								target.add(tagsContainer);
								target.add(pagingNavigator);
								target.add(noTagsContainer);
								searchField.setModelObject(null);
								target.add(searchField);
							}
						}
					}

				});
				form.add(new AjaxLink<Void>("cancel") {

					@Override
					public void onClick(AjaxRequestTarget target) {
						modal.close();
					}
					
				});
				fragment.add(form);
				return fragment;
			}
			
		});
		
		IModel<List<RefInfo>> tagsModel = new LoadableDetachableModel<List<RefInfo>>() {

			@Override
			protected List<RefInfo> load() {
				List<RefInfo> refs = getProject().getTags();
				String searchFor = searchField.getModelObject();
				if (StringUtils.isNotBlank(searchFor)) {
					searchFor = searchFor.trim().toLowerCase();
					for (Iterator<RefInfo> it = refs.iterator(); it.hasNext();) {
						String tag = GitUtils.ref2tag(it.next().getRef().getName());
						if (!tag.toLowerCase().contains(searchFor))
							it.remove();
					}
				}
				return refs;
			}
			
		}; 
		
		add(tagsContainer = new WebMarkupContainer("tagsContainer") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(!tagsModel.getObject().isEmpty());
			}
			
		});
		tagsContainer.setOutputMarkupPlaceholderTag(true);
		
		PageableListView<RefInfo> tagsView;

		tagsContainer.add(tagsView = new PageableListView<RefInfo>("tags", tagsModel, 
				com.turbodev.server.web.WebConstants.PAGE_SIZE) {

			@Override
			protected void populateItem(ListItem<RefInfo> item) {
				RefInfo ref = item.getModelObject();
				String tagName = GitUtils.ref2tag(ref.getRef().getName());
				
				BlobIdent blobIdent = new BlobIdent(tagName, null, FileMode.TREE.getBits());
				ProjectBlobPage.State state = new ProjectBlobPage.State(blobIdent);
				AbstractLink link = new ViewStateAwarePageLink<Void>("tagLink", 
						ProjectBlobPage.class, ProjectBlobPage.paramsOf(getProject(), state));
				link.add(new Label("name", tagName));
				item.add(link);
				
				String commitHash = ref.getPeeledObj().name();
				item.add(new VerificationStatusPanel("verificationStatus", 
						new LoadableDetachableModel<Map<String, Verification>>() {

					@Override
					protected Map<String, Verification> load() {
						return TurboDev.getInstance(VerificationManager.class).getVerifications(getProject(), commitHash);
					}
					
				}));

				if (ref.getObj() instanceof RevTag) {
					RevTag revTag = (RevTag) ref.getObj();
					Fragment fragment = new Fragment("annotated", "annotatedFrag", ProjectTagsPage.this);
					if (revTag.getTaggerIdent() != null) {
						fragment.add(new UserLink("author", revTag.getTaggerIdent()));
					} else {
						fragment.add(new WebMarkupContainer("author").setVisible(false));
					}
					Label message = new Label("message", revTag.getFullMessage());
					message.setOutputMarkupId(true);
					fragment.add(message);
					String toggleScript = String.format("$('#%s').toggle();", message.getMarkupId());
					WebMarkupContainer messageToggle = new WebMarkupContainer("messageToggle"); 
					messageToggle.add(AttributeAppender.append("onclick", toggleScript));
					messageToggle.setVisible(StringUtils.isNotBlank(revTag.getFullMessage()));
					fragment.add(messageToggle);
					item.add(fragment);
				} else {
					item.add(new WebMarkupContainer("annotated").setVisible(false));
				}

				RevCommit commit = (RevCommit) ref.getPeeledObj();
				PageParameters params = CommitDetailPage.paramsOf(getProject(), commit.name());
				
				link = new ViewStateAwarePageLink<Void>("messageLink", CommitDetailPage.class, params);
				link.add(new Label("message", commit.getShortMessage()));
				item.add(link);
				
				item.add(new ContributorPanel("contributor", commit.getAuthorIdent(), commit.getCommitterIdent(), true));
				
				item.add(new ArchiveMenuLink("download", projectModel) {

					@Override
					protected String getRevision() {
						return tagName;
					}
					
				});
				
				item.add(new AjaxLink<Void>("delete") {

					@Override
					protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
						super.updateAjaxAttributes(attributes);
						attributes.getAjaxCallListeners().add(new ConfirmListener("Do you really want to delete tag " + tagName + "?"));
					}

					@Override
					protected void disableLink(ComponentTag tag) {
						super.disableLink(tag);
						tag.append("class", "disabled", " ");
						tag.put("title", "Deletion not allowed due to branch protection rule");
					}

					@Override
					public void onClick(AjaxRequestTarget target) {
						getProject().deleteTag(tagName);
						target.add(tagsContainer);
						target.add(pagingNavigator);
						target.add(noTagsContainer);
					}

					@Override
					protected void onConfigure() {
						super.onConfigure();

						Project project = getProject();
						if (SecurityUtils.canWrite(project)) {
							TagProtection protection = project.getTagProtection(tagName, getLoginUser());
							setEnabled(protection == null || !protection.isNoDeletion());
						} else {
							setVisible(false);
						}
					}
					
				});
			}
			
		});

		PagingHistorySupport pagingHistorySupport = new PagingHistorySupport() {
			
			@Override
			public PageParameters newPageParameters(int currentPage) {
				PageParameters params = paramsOf(getProject());
				params.add(PARAM_CURRENT_PAGE, currentPage+1);
				return params;
			}
			
			@Override
			public int getCurrentPage() {
				return getPageParameters().get(PARAM_CURRENT_PAGE).toInt(1)-1;
			}
			
		};
		
		tagsView.setCurrentPage(pagingHistorySupport.getCurrentPage());
		
		add(pagingNavigator = new HistoryAwarePagingNavigator("tagsPageNav", tagsView, pagingHistorySupport) {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(tagsView.getPageCount() > 1);
			}
			
		});
		pagingNavigator.setOutputMarkupPlaceholderTag(true);
		
		add(noTagsContainer = new WebMarkupContainer("noTags") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(tagsModel.getObject().isEmpty());
			}
			
		});
		noTagsContainer.setOutputMarkupPlaceholderTag(true);
	}
	
	@Override
	protected String getRobotsMeta() {
		return "noindex,nofollow";
	}
	
	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new ProjectTagsResourceReference()));
	}

}
