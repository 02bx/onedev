package com.gitplex.server.core.setting;

import java.io.Serializable;

import com.gitplex.commons.wicket.editable.annotation.Editable;

@Editable
public class SecuritySetting implements Serializable {

	private static final long serialVersionUID = 1L;

	private boolean enableSelfRegister = true;

	@Editable(description="User can self-register accounts if this option is enabled")
	public boolean isEnableSelfRegister() {
		return enableSelfRegister;
	}

	public void setEnableSelfRegister(boolean enableSelfRegister) {
		this.enableSelfRegister = enableSelfRegister;
	}
	
}
