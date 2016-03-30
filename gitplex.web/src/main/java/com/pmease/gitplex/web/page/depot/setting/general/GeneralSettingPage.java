package com.pmease.gitplex.web.page.depot.setting.general;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.feedback.FencedFeedbackPanel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.pmease.commons.wicket.component.modal.ModalPanel;
import com.pmease.commons.wicket.component.select2.ResponseFiller;
import com.pmease.commons.wicket.editable.BeanContext;
import com.pmease.commons.wicket.editable.BeanEditor;
import com.pmease.commons.wicket.editable.PathSegment;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.entity.Account;
import com.pmease.gitplex.core.entity.Depot;
import com.pmease.gitplex.core.manager.AccountManager;
import com.pmease.gitplex.core.manager.DepotManager;
import com.pmease.gitplex.core.security.SecurityUtils;
import com.pmease.gitplex.web.Constants;
import com.pmease.gitplex.web.component.accountchoice.AbstractAccountChoiceProvider;
import com.pmease.gitplex.web.component.accountchoice.AccountSingleChoice;
import com.pmease.gitplex.web.component.confirmdelete.ConfirmDeleteDepotModal;
import com.pmease.gitplex.web.page.account.depots.DepotListPage;
import com.pmease.gitplex.web.page.depot.setting.DepotSettingPage;
import com.vaynberg.wicket.select2.Response;

@SuppressWarnings("serial")
public class GeneralSettingPage extends DepotSettingPage {

	private String oldName;
	
	private BeanEditor<?> editor;
	
	public GeneralSettingPage(PageParameters params) {
		super(params);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		editor = BeanContext.editModel("editor", new IModel<Serializable>() {

			@Override
			public void detach() {
			}

			@Override
			public Serializable getObject() {
				return getDepot();
			}

			@Override
			public void setObject(Serializable object) {
				// check contract of DepotManager.save on why we assign oldName here
				oldName = getDepot().getName();
				editor.getBeanDescriptor().copyProperties(object, getDepot());
			}
			
		});
		
		Form<?> form = new Form<Void>("form") {

			@Override
			protected void onError() {
				super.onError();
			}

			@Override
			protected void onSubmit() {
				super.onSubmit();
				
				Depot depot = getDepot();
				DepotManager depotManager = GitPlex.getInstance(DepotManager.class);
				Depot depotWithSameName = depotManager.findBy(getAccount(), depot.getName());
				if (depotWithSameName != null && !depotWithSameName.equals(depot)) {
					String errorMessage = "This name has already been used by another repository in account " 
							+ getAccount().getName() + "."; 
					editor.getErrorContext(new PathSegment.Property("name")).addError(errorMessage);
				} else {
					depotManager.save(depot, null, oldName);
					Session.get().success("General setting has been updated");
					setResponsePage(GeneralSettingPage.class, paramsOf(depot));
				}
			}
			
		};
		form.add(editor);

		form.add(new AjaxLink<Void>("transfer") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				new ModalPanel(target) {

					private Account account;
					
					@Override
					protected Component newContent(String id) {
						Fragment fragment = new Fragment(id, "transferFrag", GeneralSettingPage.this);
						Form<?> form = new Form<Void>("form");
						form.setOutputMarkupId(true);
						fragment.add(form);
						form.add(new AccountSingleChoice("account", new IModel<Account>() {

							@Override
							public void detach() {
							}

							@Override
							public Account getObject() {
								return account;
							}

							@Override
							public void setObject(Account object) {
								account = object;
							}
							
						}, new AbstractAccountChoiceProvider() {
							
							@Override
							public void query(String term, int page, Response<Account> response) {
								List<Account> accounts = new ArrayList<>();
								for (Account account: GitPlex.getInstance(AccountManager.class).all()) {
									if (!account.equals(getAccount()) && SecurityUtils.canManage(account)) {
										accounts.add(account);
									}
								}
								new ResponseFiller<Account>(response).fill(accounts, page, Constants.DEFAULT_PAGE_SIZE);
							}
							
						}).setRequired(true));
						
						form.add(new FencedFeedbackPanel("feedback", form));
						
						form.add(new AjaxButton("transfer") {

							@Override
							protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
								super.onSubmit(target, form);
								DepotManager depotManager = GitPlex.getInstance(DepotManager.class);
								if (depotManager.findBy(account, getDepot().getName()) != null) {
									form.error("A repository with name '" + getDepot().getName() + "' already exist "
											+ "under this account, please rename the repository and try transfer "
											+ "again");
									target.add(form);
								} else {
									getDepot().setAccount(account);
									depotManager.save(getDepot(), getAccount().getId(), null);
									setResponsePage(GeneralSettingPage.class, GeneralSettingPage.paramsOf(getDepot()));
									Session.get().success("Repository has been transferred successfully");
								}
							}

							@Override
							protected void onError(AjaxRequestTarget target, Form<?> form) {
								super.onError(target, form);
								target.add(form);
							}

						});
						form.add(new AjaxLink<Void>("cancel") {

							@Override
							public void onClick(AjaxRequestTarget target) {
								close(target);
							}
							
						});		
						return fragment;
					}
					
				};
			}
			
		});
		
		form.add(new AjaxLink<Void>("delete") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				new ConfirmDeleteDepotModal(target) {
					
					@Override
					protected void onDeleted(AjaxRequestTarget target) {
						setResponsePage(DepotListPage.class, DepotListPage.paramsOf(getAccount()));						
					}
					
					@Override
					protected Depot getDepot() {
						return GeneralSettingPage.this.getDepot();
					}
				};
			}
			
		});
		
		add(form);
	}

	@Override
	protected void onSelect(AjaxRequestTarget target, Depot depot) {
		setResponsePage(GeneralSettingPage.class, GeneralSettingPage.paramsOf(depot));
	}
	
}
