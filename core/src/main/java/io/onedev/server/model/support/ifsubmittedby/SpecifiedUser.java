package io.onedev.server.model.support.ifsubmittedby;

import org.hibernate.validator.constraints.NotEmpty;

import com.google.common.base.Preconditions;

import io.onedev.server.OneDev;
import io.onedev.server.manager.UserManager;
import io.onedev.server.model.Project;
import io.onedev.server.model.User;
import io.onedev.server.util.editable.annotation.Editable;
import io.onedev.server.util.editable.annotation.OmitName;
import io.onedev.server.util.editable.annotation.UserChoice;

@Editable(order=200, name="User")
public class SpecifiedUser implements IfSubmittedBy {

	private static final long serialVersionUID = 1L;
	
	private String userName;

	@Editable(name="User")
	@UserChoice
	@OmitName
	@NotEmpty
	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	@Override
	public boolean matches(Project project, User user) {
		UserManager userManager = OneDev.getInstance(UserManager.class);
		User specifiedUser = Preconditions.checkNotNull(userManager.findByName(userName));
		return specifiedUser.equals(user);
	}

}
