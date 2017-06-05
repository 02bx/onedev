package com.gitplex.server.web.component.projectprivilege.privilegesource;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import com.gitplex.server.GitPlex;
import com.gitplex.server.manager.ProjectManager;
import com.gitplex.server.manager.UserManager;
import com.gitplex.server.model.Group;
import com.gitplex.server.model.GroupAuthorization;
import com.gitplex.server.model.Project;
import com.gitplex.server.model.User;
import com.gitplex.server.security.ProjectPrivilege;
import com.gitplex.server.web.page.group.GroupAuthorizationsPage;
import com.gitplex.server.web.page.group.GroupProfilePage;

@SuppressWarnings("serial")
public class PrivilegeSourcePanel extends Panel {

	private final IModel<User> userModel;
	
	private final IModel<Project> projectModel;
	
	private final ProjectPrivilege privilege;
	
	public PrivilegeSourcePanel(String id, User user, Project project, ProjectPrivilege privilege) {
		super(id);
	
		Long userId = user.getId();
		userModel = new LoadableDetachableModel<User>() {

			@Override
			protected User load() {
				return GitPlex.getInstance(UserManager.class).load(userId);
			}
			
		};
		
		Long projectId = project.getId();
		projectModel = new LoadableDetachableModel<Project>() {

			@Override
			protected Project load() {
				return GitPlex.getInstance(ProjectManager.class).load(projectId);
			}
			
		};
		
		this.privilege = privilege;
	}
	
	@Override
	public void onInitialize() {
		super.onInitialize();
		
		User user = userModel.getObject();
		Project project = projectModel.getObject();
		add(new Label("user", user.getDisplayName()));
		add(new Label("project", project.getName()));
		add(new Label("privilege", privilege.name()));
		
		add(new WebMarkupContainer("userIsRoot").setVisible(user.isRoot()));
		add(new WebMarkupContainer("projectIsPublic")
				.setVisible(privilege == ProjectPrivilege.READ && project.isPublicRead()));
		add(new ListView<Group>("groups", new LoadableDetachableModel<List<Group>>() {

			@Override
			protected List<Group> load() {
				List<Group> groups = new ArrayList<>();
				for (Group group: user.getGroups()) {
					if (group.isAdministrator()) {
						groups.add(group);
					} else {
						for (GroupAuthorization authorization: group.getAuthorizations()) {
							if (authorization.getProject().equals(project) 
									&& authorization.getPrivilege() == privilege) {
								groups.add(group);
								break;
							}
						}
					}
				}
				return groups;
			}
			
		}) {

			@Override
			protected void populateItem(ListItem<Group> item) {
				Group group = item.getModelObject();
				if (group.isAdministrator()) {
					item.add(new BookmarkablePageLink<Void>("group", GroupProfilePage.class, 
							GroupAuthorizationsPage.paramsOf(group)) {

						@Override
						public IModel<?> getBody() {
							return Model.of(item.getModelObject().getName());
						}
						
					});
				} else {
					item.add(new BookmarkablePageLink<Void>("group", GroupAuthorizationsPage.class, 
							GroupAuthorizationsPage.paramsOf(group)) {

						@Override
						public IModel<?> getBody() {
							return Model.of(item.getModelObject().getName());
						}
						
					});
				}
			}

		});
	}

	@Override
	protected void onDetach() {
		userModel.detach();
		projectModel.detach();
		
		super.onDetach();
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		
		response.render(CssHeaderItem.forReference(new PrivilegeSourceCssResourceReference()));
	}

}
