package com.pmease.gitplex.web.page.account.repositories;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PageableListView;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;

import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.commons.util.StringUtils;
import com.pmease.commons.wicket.behavior.OnTypingDoneBehavior;
import com.pmease.commons.wicket.component.MultilineLabel;
import com.pmease.commons.wicket.component.clearable.ClearableTextField;
import com.pmease.commons.wicket.component.navigator.PagingNavigator;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.model.Repository;
import com.pmease.gitplex.core.security.SecurityUtils;
import com.pmease.gitplex.web.Constants;
import com.pmease.gitplex.web.component.confirmdelete.ConfirmDeleteRepoModal;
import com.pmease.gitplex.web.component.confirmdelete.ConfirmDeleteRepoModalBehavior;
import com.pmease.gitplex.web.page.account.AccountPage;
import com.pmease.gitplex.web.page.repository.RepositoryPage;
import com.pmease.gitplex.web.page.repository.setting.general.GeneralSettingPage;
import com.pmease.gitplex.web.page.repository.tree.RepoTreePage;

@SuppressWarnings("serial")
public class AccountReposPage extends AccountPage {

	private PageableListView<Repository> reposView;
	
	private PagingNavigator pagingNavigator;
	
	private WebMarkupContainer reposContainer; 
	
	private TextField<String> searchInput;
	
	private String searchFor;
	
	public AccountReposPage(PageParameters params) {
		super(params);
	}

	@Override
	protected String getPageTitle() {
		return "Repositories - " + getAccount();
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(searchInput = new ClearableTextField<String>("searchRepos", Model.of("")));
		searchInput.add(new OnSearchingBehavior() {

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				// IE triggers "input" event when the focused on the search input even if nothing is 
				// input into search box yet. To work around this issue, we compare search string 
				// against previous value to only update the branches table if there is an actual 
				// change.
				String newSearchFor = searchInput.getInput();
				if (StringUtils.isNotBlank(newSearchFor))
					newSearchFor = newSearchFor.trim().toLowerCase();
				else
					newSearchFor = null;
				if (!ObjectUtils.equals(newSearchFor, searchFor))
					super.onUpdate(target);
			}
			
		});
		
		add(new Link<Void>("addNew") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(SecurityUtils.canManage(getAccount()));
			}

			@Override
			public void onClick() {
				Repository repository = new Repository();
				repository.setOwner(getAccount());
				setResponsePage(new NewAccountRepoPage(repository));
			}
			
		});
		
		reposContainer = new WebMarkupContainer("reposContainer");
		reposContainer.setOutputMarkupId(true);
		add(reposContainer);
		
		final ConfirmDeleteRepoModal confirmDeleteDlg = new ConfirmDeleteRepoModal("confirmDelete") {

			@Override
			protected void onDeleted(AjaxRequestTarget target) {
				setResponsePage(getPage());
			}
			
		};
		add(confirmDeleteDlg);
		
		reposContainer.add(reposView = new PageableListView<Repository>("repositories", new LoadableDetachableModel<List<Repository>>() {

			@Override
			protected List<Repository> load() {
				List<Repository> repositories = new ArrayList<Repository>(getAccount().getRepositories());
				
				searchFor = searchInput.getInput();
				if (StringUtils.isNotBlank(searchFor)) {
					searchFor = searchFor.trim().toLowerCase();
					for (Iterator<Repository> it = repositories.iterator(); it.hasNext();) {
						Repository repository = it.next();
						if (!repository.getName().toLowerCase().contains(searchFor))
							it.remove();
					}
				} else {
					searchFor = null;
				}
				Collections.sort(repositories, new Comparator<Repository>() {

					@Override
					public int compare(Repository repository1, Repository repository2) {
						return repository1.getName().compareTo(repository2.getName());
					}
					
				});
				return repositories;
			}
			
		}, Constants.DEFAULT_PAGE_SIZE) {

			@Override
			protected void populateItem(final ListItem<Repository> item) {
				Repository repository = item.getModelObject();

				Link<Void> link = new BookmarkablePageLink<>("repoLink", RepoTreePage.class, RepositoryPage.paramsOf(repository)); 
				link.add(new Label("repoName", repository.getName()));
				item.add(link);
						
				item.add(new MultilineLabel("description", repository.getDescription()));
				
				item.add(new Link<Void>("setting") {

					@Override
					public void onClick() {
						PageParameters params = RepositoryPage.paramsOf(item.getModelObject());
						addPrevPageParam(params);
						setResponsePage(GeneralSettingPage.class, params);
					}

					@Override
					protected void onConfigure() {
						super.onConfigure();
						setVisible(SecurityUtils.canManage(item.getModelObject()));
					}
					
				});
				
				final Long repositoryId = repository.getId();
				item.add(new WebMarkupContainer("deleteRepo") {

					@Override
					protected void onInitialize() {
						super.onInitialize();
						
						add(new ConfirmDeleteRepoModalBehavior(confirmDeleteDlg) {
							
							@Override
							protected Repository getRepository() {
								return GitPlex.getInstance(Dao.class).load(Repository.class, repositoryId);
							}
							
						});
					}

					@Override
					protected void onConfigure() {
						super.onConfigure();

						setVisible(SecurityUtils.canManage(item.getModelObject()));
					}
					
				});
			}
			
		});

		add(pagingNavigator = new PagingNavigator("reposPageNav", reposView) {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(reposView.getPageCount() > 1);
			}
			
		});
		pagingNavigator.setOutputMarkupPlaceholderTag(true);
	}
	
	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new CssResourceReference(AccountReposPage.class, "account-repos.css")));
	}

	private class OnSearchingBehavior extends OnTypingDoneBehavior {

		public OnSearchingBehavior() {
			super(100);
		}

		@Override
		protected void onUpdate(AjaxRequestTarget target) {
			target.add(reposContainer);
			target.add(pagingNavigator);
		}

	}
}
