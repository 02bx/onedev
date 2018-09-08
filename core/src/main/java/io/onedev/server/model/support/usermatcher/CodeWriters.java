package io.onedev.server.model.support.usermatcher;

import io.onedev.server.model.Project;
import io.onedev.server.model.User;
import io.onedev.server.security.permission.ProjectPermission;
import io.onedev.server.security.permission.ProjectPrivilege;
import io.onedev.server.web.editable.annotation.Editable;

@Editable(order=300, name="Users allowed to write code")
public class CodeWriters implements UserMatcher {

	private static final long serialVersionUID = 1L;

	@Override
	public boolean matches(Project project, User user) {
		return user.asSubject().isPermitted(new ProjectPermission(project.getFacade(), ProjectPrivilege.CODE_WRITE));
	}

}
