package io.onedev.server.web.page.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseAtInterceptPageException;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import de.agilecoders.wicket.core.markup.html.bootstrap.components.TooltipConfig;
import de.agilecoders.wicket.core.markup.html.bootstrap.components.TooltipConfig.Placement;
import io.onedev.launcher.loader.AppLoader;
import io.onedev.launcher.loader.Plugin;
import io.onedev.server.OneDev;
import io.onedev.server.manager.SettingManager;
import io.onedev.server.manager.UserManager;
import io.onedev.server.model.User;
import io.onedev.server.persistence.dao.EntityCriteria;
import io.onedev.server.security.SecurityUtils;
import io.onedev.server.web.ComponentRenderer;
import io.onedev.server.web.behavior.TooltipBehavior;
import io.onedev.server.web.component.avatar.AvatarLink;
import io.onedev.server.web.component.floating.AlignPlacement;
import io.onedev.server.web.component.floating.FloatingPanel;
import io.onedev.server.web.component.link.DropdownLink;
import io.onedev.server.web.component.link.ViewStateAwarePageLink;
import io.onedev.server.web.page.admin.systemsetting.SystemSettingPage;
import io.onedev.server.web.page.base.BasePage;
import io.onedev.server.web.page.group.GroupListPage;
import io.onedev.server.web.page.project.ProjectListPage;
import io.onedev.server.web.page.security.LoginPage;
import io.onedev.server.web.page.security.LogoutPage;
import io.onedev.server.web.page.security.RegisterPage;
import io.onedev.server.web.page.user.UserListPage;
import io.onedev.server.web.page.user.UserProfilePage;
import io.onedev.utils.license.LicenseDetail;

@SuppressWarnings("serial")
public abstract class LayoutPage extends BasePage {
	
	private WebMarkupContainer head;
	
	public LayoutPage() {
	}
	
	public LayoutPage(PageParameters params) {
		super(params);
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		head = new WebMarkupContainer("head");
		add(head);
		
		head.add(new DropdownLink("nav") {

			@Override
			protected Component newContent(String id, FloatingPanel dropdown) {
				Fragment fragment = new Fragment(id, "navFrag", LayoutPage.this);
				fragment.add(new ViewStateAwarePageLink<Void>("projects", ProjectListPage.class));
				fragment.add(new ViewStateAwarePageLink<Void>("users", UserListPage.class).setVisible(SecurityUtils.isAdministrator()));
				fragment.add(new ViewStateAwarePageLink<Void>("groups", GroupListPage.class).setVisible(SecurityUtils.isAdministrator()));
				
				fragment.add(new ListView<LayoutMenuContribution>("extensions", new LoadableDetachableModel<List<LayoutMenuContribution>>() {

					@Override
					protected List<LayoutMenuContribution> load() {
						List<LayoutMenuContribution> extensions = new ArrayList<>();
						for (LayoutMenuContribution contribution: OneDev.getExtensions(LayoutMenuContribution.class)) {
							if (contribution.isAuthorized())
								extensions.add(contribution);
						}
						Collections.sort(extensions, new Comparator<LayoutMenuContribution>() {

							@Override
							public int compare(LayoutMenuContribution o1, LayoutMenuContribution o2) {
								return o1.getOrder() - o2.getOrder();
							}
							
						});
						return extensions;
					}
					
				}) {

					@Override
					protected void populateItem(ListItem<LayoutMenuContribution> item) {
						LayoutMenuContribution contribution = item.getModelObject();
						Link<Void> link = new BookmarkablePageLink<Void>("link", contribution.getPageClass());
						link.add(new Label("label", contribution.getLabel()));
						item.add(link);
					}
					
				});
				
				fragment.add(new ViewStateAwarePageLink<Void>("administration", SystemSettingPage.class).setVisible(SecurityUtils.isAdministrator()));
				
				Plugin product = AppLoader.getProduct();
				fragment.add(new Label("productVersion", product.getVersion()));
				fragment.add(new ExternalLink("docLink", OneDev.getInstance().getDocLink() + "/readme.md"));
				return fragment;
			}
			
		});

		head.add(new ListView<ComponentRenderer>("breadcrumbs", getBreadcrumbs()) {

			@Override
			protected void populateItem(ListItem<ComponentRenderer> item) {
				item.add(item.getModelObject().render("nav"));
				item.add(new WebMarkupContainer("separator").setVisible(item.getIndex() != getList().size()-1));
			}
			
		});

		int userCount = OneDev.getInstance(UserManager.class).count(EntityCriteria.of(User.class));
		int licenseLimit = LicenseDetail.FREE_LICENSE_USERS;
		LicenseDetail license = OneDev.getInstance(SettingManager.class).getLicense();
		if (license != null && license.getRemainingDays()>=0)
			licenseLimit += license.getLicensedUsers();
		if (userCount > licenseLimit) {
			String tooltip = String.format(""
					+ "Git push is disabled as number of users (%d) in system exceeds license limit (%d).", 
					userCount, licenseLimit);
			TooltipBehavior tooltipBehavior = new TooltipBehavior(Model.of(tooltip), 
					new TooltipConfig().withPlacement(Placement.bottom)); 
			head.add(new WebMarkupContainer("pushDisabled").add(tooltipBehavior));
		} else {
			head.add(new WebMarkupContainer("pushDisabled").setVisible(false));
		}

		User user = getLoginUser();
		boolean signedIn = user != null;

		head.add(new Link<Void>("login") {

			@Override
			public void onClick() {
				throw new RestartResponseAtInterceptPageException(LoginPage.class);
			}
			
		}.setVisible(!signedIn));
		
		boolean enableSelfRegister = OneDev.getInstance(SettingManager.class).getSecuritySetting().isEnableSelfRegister();
		head.add(new ViewStateAwarePageLink<Void>("register", RegisterPage.class).setVisible(!signedIn && enableSelfRegister));
		
		if (signedIn) {
			head.add(new AvatarLink("user", user));
			head.add(new DropdownLink("userMenuTrigger", new AlignPlacement(50, 100, 50, 0, 8)) {

				@Override
				protected Component newContent(String id, FloatingPanel dropdown) {
					Fragment fragment = new Fragment(id, "userMenuFrag", LayoutPage.this);

					fragment.add(new ViewStateAwarePageLink<Void>("profile", 
							UserProfilePage.class, UserProfilePage.paramsOf(user)));
					fragment.add(new ViewStateAwarePageLink<Void>("logout", LogoutPage.class));
					
					return fragment;
				}
				
			});
		} else {  
			head.add(new WebMarkupContainer("user").setVisible(false));
			head.add(new WebMarkupContainer("userMenuTrigger").setVisible(false));
			head.add(new WebMarkupContainer("userMenu").setVisible(false));
		}
		head.setOutputMarkupId(true);
	}

	protected List<ComponentRenderer> getBreadcrumbs() {
		return new ArrayList<>();
	};
	
	@Override
	protected boolean isPermitted() {
		return SecurityUtils.canAccessPublic();
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new LayoutResourceReference()));
	}

}
