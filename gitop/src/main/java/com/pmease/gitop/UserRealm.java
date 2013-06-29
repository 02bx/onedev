package com.pmease.gitop;

import java.util.Arrays;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.shiro.authc.credential.CredentialsMatcher;

import com.pmease.commons.persistence.dao.GeneralDao;
import com.pmease.commons.security.AbstractRealm;
import com.pmease.gitop.model.User;

@Singleton
public class UserRealm extends AbstractRealm<User> {

	@Inject
	public UserRealm(Provider<GeneralDao> generalDaoProvider, CredentialsMatcher credentialsMatcher) {
		super(generalDaoProvider, credentialsMatcher);
	}

	@Override
	protected Collection<String> doGetPermissions(Long userId) {
		if (userId == 0L)
			return Arrays.asList("read");
		else
			return Arrays.asList("*");
	}

}
