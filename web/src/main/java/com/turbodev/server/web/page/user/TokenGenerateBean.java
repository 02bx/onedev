package com.turbodev.server.web.page.user;

import java.io.Serializable;

import org.hibernate.validator.constraints.NotEmpty;

import com.turbodev.server.util.editable.annotation.Editable;
import com.turbodev.server.util.editable.annotation.Password;

@Editable
public class TokenGenerateBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private String password;
	
	@Editable(order=100, description="Input password of this user")
	@CurrentPassword
	@Password
	@NotEmpty
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
