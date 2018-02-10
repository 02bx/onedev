package com.turbodev.server.web.page.user;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.turbodev.server.TurboDev;
import com.turbodev.server.manager.UserManager;
import com.turbodev.server.model.User;
import com.turbodev.server.persistence.dao.EntityCriteria;
import com.turbodev.server.security.SecurityUtils;
import com.turbodev.server.web.ComponentRenderer;
import com.turbodev.server.web.WebConstants;
import com.turbodev.server.web.behavior.OnTypingDoneBehavior;
import com.turbodev.server.web.component.avatar.AvatarLink;
import com.turbodev.server.web.component.datatable.HistoryAwareDataTable;
import com.turbodev.server.web.component.link.UserLink;
import com.turbodev.server.web.component.link.ViewStateAwarePageLink;
import com.turbodev.server.web.page.layout.LayoutPage;
import com.turbodev.server.web.util.ConfirmOnClick;
import com.turbodev.server.web.util.PagingHistorySupport;

@SuppressWarnings("serial")
public class UserListPage extends LayoutPage {

	private static final String PARAM_CURRENT_PAGE = "currentPage";
	
	private DataTable<User, Void> usersTable;
	
	private String searchInput;
	
	private EntityCriteria<User> getCriteria() {
		EntityCriteria<User> criteria = EntityCriteria.of(User.class);
		if (searchInput != null) {
			criteria.add(Restrictions.or(
					Restrictions.ilike("name", searchInput, MatchMode.ANYWHERE), 
					Restrictions.ilike("fullName", searchInput, MatchMode.ANYWHERE)));
		}
		return criteria;
	}
	
	@Override
	protected boolean isPermitted() {
		return SecurityUtils.isAdministrator();
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		TextField<String> searchField;
		add(searchField = new TextField<String>("filterUsers", Model.of("")));
		searchField.add(new OnTypingDoneBehavior(100) {

			@Override
			protected void onTypingDone(AjaxRequestTarget target) {
				searchInput = searchField.getInput();
				target.add(usersTable);
			}

		});
		
		add(new Link<Void>("addNew") {

			@Override
			public void onClick() {
				setResponsePage(NewUserPage.class);
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(SecurityUtils.isAdministrator());
			}
			
		});
		
		List<IColumn<User, Void>> columns = new ArrayList<>();
		
		columns.add(new AbstractColumn<User, Void>(Model.of("Name")) {

			@Override
			public void populateItem(Item<ICellPopulator<User>> cellItem, String componentId,
					IModel<User> rowModel) {
				User user = rowModel.getObject();
				Fragment fragment = new Fragment(componentId, "nameFrag", UserListPage.this);
				fragment.add(new AvatarLink("avatarLink", user));
				fragment.add(new UserLink("nameLink", user));
				cellItem.add(fragment);
			}
		});
		
		columns.add(new AbstractColumn<User, Void>(Model.of("Email")) {

			@Override
			public void populateItem(Item<ICellPopulator<User>> cellItem, String componentId,
					IModel<User> rowModel) {
				cellItem.add(new Label(componentId, rowModel.getObject().getEmail()));
			}
		});
		
		columns.add(new AbstractColumn<User, Void>(Model.of("Actions")) {

			@Override
			public void populateItem(Item<ICellPopulator<User>> cellItem, String componentId, IModel<User> rowModel) {
				Fragment fragment = new Fragment(componentId, "actionFrag", UserListPage.this);
				fragment.add(AttributeAppender.append("class", "actions"));
				
				fragment.add(new Link<Void>("profile") {

					@Override
					public void onClick() {
						PageParameters params = UserPage.paramsOf(rowModel.getObject());
						setResponsePage(UserProfilePage.class, params);
					}

				});
				
				User user = rowModel.getObject();
				
				fragment.add(new Link<Void>("delete") {

					@Override
					public void onClick() {
						TurboDev.getInstance(UserManager.class).delete(rowModel.getObject());
					}

					@Override
					protected void onConfigure() {
						super.onConfigure();
						User user = rowModel.getObject();
						setVisible(SecurityUtils.isAdministrator() && !user.isRoot() && !user.equals(getLoginUser()));
					}

				}.add(new ConfirmOnClick("Do you really want to delete user '" + user.getDisplayName() + "'?")));
				
				cellItem.add(fragment);
			}
		});
		
		SortableDataProvider<User, Void> dataProvider = new SortableDataProvider<User, Void>() {

			@Override
			public Iterator<? extends User> iterator(long first, long count) {
				EntityCriteria<User> criteria = getCriteria();
				criteria.addOrder(Order.asc("name"));
				return TurboDev.getInstance(UserManager.class).findRange(criteria, (int)first, (int)count).iterator();
			}

			@Override
			public long size() {
				return TurboDev.getInstance(UserManager.class).count(getCriteria());
			}

			@Override
			public IModel<User> model(User object) {
				Long id = object.getId();
				return new LoadableDetachableModel<User>() {

					@Override
					protected User load() {
						return TurboDev.getInstance(UserManager.class).load(id);
					}
					
				};
			}
		};
		
		PagingHistorySupport pagingHistorySupport = new PagingHistorySupport() {
			
			@Override
			public PageParameters newPageParameters(int currentPage) {
				PageParameters params = new PageParameters();
				params.add(PARAM_CURRENT_PAGE, currentPage+1);
				return params;
			}
			
			@Override
			public int getCurrentPage() {
				return getPageParameters().get(PARAM_CURRENT_PAGE).toInt(1)-1;
			}
			
		};
		
		add(usersTable = new HistoryAwareDataTable<User, Void>("users", columns, dataProvider, 
				WebConstants.PAGE_SIZE, pagingHistorySupport));
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new UserResourceReference()));
	}

	@Override
	protected List<ComponentRenderer> getBreadcrumbs() {
		List<ComponentRenderer> breadcrumbs = super.getBreadcrumbs();
		
		breadcrumbs.add(new ComponentRenderer() {

			@Override
			public Component render(String componentId) {
				return new ViewStateAwarePageLink<Void>(componentId, UserListPage.class) {

					@Override
					public IModel<?> getBody() {
						return Model.of("Users");
					}
					
				};
			}
			
		});

		return breadcrumbs;
	}
	
}
