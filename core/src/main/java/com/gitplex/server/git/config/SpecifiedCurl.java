package com.gitplex.server.git.config;

import org.hibernate.validator.constraints.NotEmpty;

import com.gitplex.server.util.editable.annotation.Editable;

@Editable(name="Use Specified curl", order=200)
public class SpecifiedCurl extends CurlConfig {

	private static final long serialVersionUID = 1L;
	
	private String curlPath;
	
	@Editable(description="Specify path to curl executable, for instance: <tt>/usr/bin/curl</tt>")
	@NotEmpty
	public String getCurlPath() {
		return curlPath;
	}

	public void setCurlPath(String curlPath) {
		this.curlPath = curlPath;
	}

	@Override
	public String getExecutable() {
		return curlPath;
	}

}
