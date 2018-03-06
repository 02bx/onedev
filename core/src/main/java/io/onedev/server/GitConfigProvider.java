package io.onedev.server;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import io.onedev.server.git.config.GitConfig;
import io.onedev.server.manager.ConfigManager;

@Singleton
public class GitConfigProvider implements Provider<GitConfig> {

	private final ConfigManager configManager;
	
	@Inject
	public GitConfigProvider(ConfigManager configManager) {
		this.configManager = configManager;
	}
	
	@Override
	public GitConfig get() {
		return configManager.getSystemSetting().getGitConfig();
	}

}
