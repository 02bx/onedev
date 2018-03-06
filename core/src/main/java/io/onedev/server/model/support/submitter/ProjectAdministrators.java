package io.onedev.server.model.support.submitter;

import io.onedev.server.model.Project;
import io.onedev.server.model.User;
import io.onedev.server.security.ProjectPrivilege;
import io.onedev.server.security.permission.ProjectPermission;
import io.onedev.server.util.editable.annotation.Editable;

@Editable(order=400, name="Project administrators")
public class ProjectAdministrators implements Submitter {

	private static final long serialVersionUID = 1L;

	@Override
	public boolean matches(Project project, User user) {
		return user.asSubject().isPermitted(new ProjectPermission(project.getFacade(), ProjectPrivilege.ADMIN));
	}

}
