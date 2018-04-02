package io.onedev.server.model.support.issueworkflow.action;

import org.hibernate.validator.constraints.NotEmpty;

import io.onedev.server.util.editable.annotation.Editable;
import io.onedev.server.util.editable.annotation.OmitName;

@Editable(order=200, name="Pull request is opened against")
public class OpenPullRequest implements IssueAction {

	private static final long serialVersionUID = 1L;

	private String targetBranch;

	@Editable
	@OmitName
	@NotEmpty
	public String getTargetBranch() {
		return targetBranch;
	}

	public void setTargetBranch(String targetBranch) {
		this.targetBranch = targetBranch;
	}

	@Override
	public Button getButton() {
		return null;
	}
	
}
