package com.pmease.gitplex.core.setting;

import java.io.Serializable;

import javax.validation.ConstraintValidatorContext;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;

import com.pmease.commons.editable.annotation.Editable;
import com.pmease.commons.git.GitConfig;
import com.pmease.commons.validation.ClassValidating;
import com.pmease.commons.validation.Directory;
import com.pmease.commons.validation.Validatable;

@Editable
@ClassValidating
public class SystemSetting implements Serializable, Validatable {
	
	private static final long serialVersionUID = 1;

	private String serverUrl;
	
	private String storagePath;
	
	private GitConfig gitConfig = new SystemGit();
	
	private boolean gravatarEnabled = true;
	
	@Editable(name="Server URL", order=90, description="Specify root URL to access this server. GitPlex uses this url "
			+ "to construct various links in notification email.")
	@NotEmpty
	public String getServerUrl() {
		return serverUrl;
	}

	public void setServerUrl(String serverUrl) {
		this.serverUrl = serverUrl;
	}

	@Editable(name="Storage Directory", order=100, description="Specify directory to store GitPlex data such as Git repositories.")
	@Directory
	@NotEmpty
	public String getStoragePath() {
		return storagePath;
	}

	public void setStoragePath(String storagePath) {
		this.storagePath = storagePath;
	}

	@Editable(order=200, description="GitPlex relies on git command line to operate managed repositories. The minimum "
			+ "required version is 1.8.0.")
	@Valid
	@NotNull
	public GitConfig getGitConfig() {
		return gitConfig;
	}

	public void setGitConfig(GitConfig gitConfig) {
		this.gitConfig = gitConfig;
	}

	@Editable(order=300, description="Whether or not to enable user gravatar.")
	public boolean isGravatarEnabled() {
		return gravatarEnabled;
	}

	public void setGravatarEnabled(boolean gravatarEnabled) {
		this.gravatarEnabled = gravatarEnabled;
	}

	@Override
	public boolean isValid(ConstraintValidatorContext context) {
		if (serverUrl != null)
			serverUrl = StringUtils.stripEnd(serverUrl, "/\\");
		return true;
	}

}
