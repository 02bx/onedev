package io.onedev.server.web.page.project.savedquery;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import de.agilecoders.wicket.core.markup.html.bootstrap.common.NotificationPanel;
import io.onedev.server.model.Project;
import io.onedev.server.model.support.NamedQuery;
import io.onedev.server.model.support.QuerySetting;
import io.onedev.server.model.support.WatchStatus;
import io.onedev.server.security.SecurityUtils;
import io.onedev.server.web.component.modal.ModalLink;
import io.onedev.server.web.component.modal.ModalPanel;
import io.onedev.server.web.component.tabbable.AjaxActionTab;
import io.onedev.server.web.component.tabbable.Tab;
import io.onedev.server.web.component.tabbable.Tabbable;
import io.onedev.server.web.component.watchstatus.WatchStatusLink;
import io.onedev.server.web.editable.BeanContext;
import io.onedev.server.web.page.project.ProjectPage;
import io.onedev.server.web.util.ajaxlistener.ConfirmLeaveListener;

@SuppressWarnings("serial")
public abstract class SavedQueriesPanel<T extends NamedQuery> extends Panel {

	public SavedQueriesPanel(String id) {
		super(id);
	}

	private ArrayList<T> getUserQueries() {
		QuerySetting<T> querySetting = getQuerySetting();
		if (querySetting != null)
			return querySetting.getUserQueries();
		else
			return new ArrayList<>();
	}	
	
	private WatchStatus getProjectWatchStatus(T namedQuery) {
		QuerySetting<T> querySetting = getQuerySetting();
		if (querySetting != null)
			return querySetting.getQueryWatchSupport().getProjectWatchStatus(namedQuery);
		else
			return WatchStatus.DEFAULT;
	}
	
	private WatchStatus getUserWatchStatus(T namedQuery) {
		QuerySetting<T> querySetting = getQuerySetting();
		if (querySetting != null)
			return querySetting.getQueryWatchSupport().getUserWatchStatus(namedQuery);
		else
			return WatchStatus.DEFAULT;
	}
	
	private Project getProject() {
		ProjectPage page = (ProjectPage) getPage();
		return page.getProject();
	}
	
	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new SavedQueriesCssResourceReference()));
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(new ModalLink("edit") {

			private static final String TAB_PANEL_ID = "tabPanel";
			
			@Override
			protected void onConfigure() {
				super.onConfigure();
				if (SecurityUtils.canManage(getProject())) {
					setVisible(!getUserQueries().isEmpty() || !getProjectQueries().isEmpty());
				} else {
					setVisible(SecurityUtils.getUser() != null && !getUserQueries().isEmpty());
				}
			}

			private Component newUserQueriesEditor(String componentId, ModalPanel modal, ArrayList<T> userQueries) {
				return new NamedQueriesEditor(componentId, userQueries) {
					
					@Override
					protected void onSave(AjaxRequestTarget target, ArrayList<T> queries) {
						target.add(SavedQueriesPanel.this);
						modal.close();
						
						QuerySetting<T> querySetting = getQuerySetting();
						querySetting.setUserQueries(queries);
						onSaveQuerySetting(querySetting);
					}
					
					@Override
					protected void onCancel(AjaxRequestTarget target) {
						modal.close();
					}
				};
			}
			
			private Component newProjectQueriesEditor(String componentId, ModalPanel modal, ArrayList<T> projectQueries) {
				return new NamedQueriesEditor(componentId, projectQueries) {
					
					@Override
					protected void onSave(AjaxRequestTarget target, ArrayList<T> queries) {
						target.add(SavedQueriesPanel.this);
						modal.close();
						onSaveProjectQueries(queries);
					}
					
					@Override
					protected void onCancel(AjaxRequestTarget target) {
						modal.close();
					}
					
				};
			}
			
			@Override
			protected Component newContent(String id, ModalPanel modal) {
				Fragment fragment = new Fragment(id, "editSavedQueriesFrag", SavedQueriesPanel.this);
				List<Tab> tabs = new ArrayList<>();

				ArrayList<T> userQueries = getUserQueries();
				if (!userQueries.isEmpty()) {
					tabs.add(new AjaxActionTab(Model.of("For Mine")) {

						@Override
						protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
							super.updateAjaxAttributes(attributes);
							attributes.getAjaxCallListeners().add(new ConfirmLeaveListener());
						}

						@Override
						protected void onSelect(AjaxRequestTarget target, Component tabLink) {
							Component editor = newUserQueriesEditor(TAB_PANEL_ID, modal, userQueries);
							fragment.replace(editor);
							target.add(editor);
						}
						
					});
					fragment.add(newUserQueriesEditor(TAB_PANEL_ID, modal, userQueries));
				}
				
				ArrayList<T> projectQueries = getProjectQueries();
				if (SecurityUtils.canManage(getProject()) && !projectQueries.isEmpty()) {
					tabs.add(new AjaxActionTab(Model.of("For All Users")) {

						@Override
						protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
							super.updateAjaxAttributes(attributes);
							attributes.getAjaxCallListeners().add(new ConfirmLeaveListener());
						}

						@Override
						protected void onSelect(AjaxRequestTarget target, Component tabLink) {
							Component editor = newProjectQueriesEditor(TAB_PANEL_ID, modal, projectQueries);
							fragment.replace(editor);
							target.add(editor);
						}
						
					});
					if (userQueries.isEmpty())
						fragment.add(newProjectQueriesEditor(TAB_PANEL_ID, modal, projectQueries));
				}
				
				fragment.add(new Tabbable("tab", tabs));
				
				fragment.add(new AjaxLink<Void>("close") {

					@Override
					protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
						super.updateAjaxAttributes(attributes);
						attributes.getAjaxCallListeners().add(new ConfirmLeaveListener());
					}

					@Override
					public void onClick(AjaxRequestTarget target) {
						modal.close();
					}
					
				});
				return fragment;
			}
			
		});
		
		add(new ListView<T>("userQueries", new LoadableDetachableModel<List<T>>() {

			@Override
			protected List<T> load() {
				return getUserQueries();
			}
			
		}) {

			@Override
			protected void populateItem(ListItem<T> item) {
				T namedQuery = item.getModelObject();
				Link<Void> link = newQueryLink("link", namedQuery);
				link.add(new Label("label", namedQuery.getName()));
				item.add(link);
				
				item.add(new WatchStatusLink("watchStatus") {
					
					@Override
					protected void onWatchStatusChange(AjaxRequestTarget target, WatchStatus watchStatus) {
						target.add(this);
						QuerySetting<T> querySetting = getQuerySetting();
						querySetting.getQueryWatchSupport().setUserWatchStatus(namedQuery, watchStatus);
						onSaveQuerySetting(querySetting);
						
					}
					
					@Override
					protected WatchStatus getWatchStatus() {
						return getUserWatchStatus(namedQuery);
					}
					
					@Override
					protected void onConfigure() {
						super.onConfigure();
						setVisible(getQuerySetting().getQueryWatchSupport() != null);
					}
					
				});
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(!getModelObject().isEmpty());
			}

		});
		
		add(new ListView<T>("projectQueries", new LoadableDetachableModel<List<T>>() {

			@Override
			protected List<T> load() {
				List<T> namedQueries = new ArrayList<>();
				for (T namedQuery: getProjectQueries()) {
					try {
						if (SecurityUtils.getUser() != null || !needsLogin(namedQuery))
							namedQueries.add(namedQuery);
					} catch (Exception e) {
						namedQueries.add(namedQuery);
					}
				}
				return namedQueries;
			}
			
		}) {

			@Override
			protected void populateItem(ListItem<T> item) {
				T namedQuery = item.getModelObject();
				Link<Void> link = newQueryLink("link", namedQuery);
				link.add(new Label("label", namedQuery.getName()));
				item.add(link);
				
				item.add(new WatchStatusLink("watchStatus") {
					
					@Override
					protected void onWatchStatusChange(AjaxRequestTarget target, WatchStatus watchStatus) {
						target.add(this);

						QuerySetting<T> querySetting = getQuerySetting();
						querySetting.getQueryWatchSupport().setProjectWatchStatus(namedQuery, watchStatus);
						onSaveQuerySetting(querySetting);
					}
					
					@Override
					protected WatchStatus getWatchStatus() {
						return getProjectWatchStatus(namedQuery);
					}

					@Override
					protected void onConfigure() {
						super.onConfigure();
						setVisible(SecurityUtils.getUser() != null && getQuerySetting().getQueryWatchSupport() != null);
					}
					
				});
				
			}
			
			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(!getModelObject().isEmpty());
			}
			
		});		
		
		add(new WebMarkupContainer("watchHint") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(getQuerySetting() != null && getQuerySetting().getQueryWatchSupport() != null);
			}
			
		});
		setOutputMarkupId(true);
	}
	
	private abstract class NamedQueriesEditor extends Fragment {

		private final NamedQueriesBean<T> bean;
		
		public NamedQueriesEditor(String id, ArrayList<T> queries) {
			super(id, "editSavedQueriesContentFrag", SavedQueriesPanel.this);
			bean = newNamedQueriesBean();
			bean.getQueries().addAll(queries);
		}

		@Override
		protected void onInitialize() {
			super.onInitialize();
			
			Form<?> form = new Form<Void>("form");
			form.setOutputMarkupId(true);
			
			form.add(new NotificationPanel("feedback", form));
			form.add(BeanContext.editBean("editor", bean));
			form.add(new AjaxButton("save") {

				@Override
				protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
					super.onSubmit(target, form);
					
					Set<String> names = new HashSet<>();
					for (NamedQuery namedQuery: bean.getQueries()) {
						if (names.contains(namedQuery.getName())) {
							form.error("Duplicate name found: " + namedQuery.getName());
							return;
						} else {
							names.add(namedQuery.getName());
						}
					}
					onSave(target, (ArrayList<T>)bean.getQueries());
				}

				@Override
				protected void onError(AjaxRequestTarget target, Form<?> form) {
					super.onError(target, form);
					target.add(form);
				}
				
			});
			form.add(new AjaxLink<Void>("cancel") {

				@Override
				protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
					super.updateAjaxAttributes(attributes);
					attributes.getAjaxCallListeners().add(new ConfirmLeaveListener());
				}

				@Override
				public void onClick(AjaxRequestTarget target) {
					onCancel(target);
				}
				
			});
			add(form);
			setOutputMarkupId(true);
		}
		
		protected abstract void onSave(AjaxRequestTarget target, ArrayList<T> queries);
		
		protected abstract void onCancel(AjaxRequestTarget target);
	}
	
	protected abstract NamedQueriesBean<T> newNamedQueriesBean();
	
	protected abstract boolean needsLogin(T namedQuery);
	
	protected abstract Link<Void> newQueryLink(String componentId, T namedQuery);
	
	@Nullable
	protected abstract QuerySetting<T> getQuerySetting();
	
	protected abstract ArrayList<T> getProjectQueries();

	protected abstract void onSaveProjectQueries(ArrayList<T> projectQueries);
	
	protected abstract void onSaveQuerySetting(QuerySetting<T> querySetting);
	
}
