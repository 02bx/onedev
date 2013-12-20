package com.pmease.gitop.core;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.quartz.ScheduleBuilder;
import org.quartz.SimpleScheduleBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pmease.commons.git.GitConfig;
import com.pmease.commons.git.command.GitCommand;
import com.pmease.commons.loader.AbstractPlugin;
import com.pmease.commons.loader.AppLoader;
import com.pmease.commons.loader.AppName;
import com.pmease.commons.schedule.SchedulableTask;
import com.pmease.commons.schedule.TaskScheduler;
import com.pmease.commons.util.init.InitStage;
import com.pmease.commons.util.init.ManualConfig;
import com.pmease.gitop.core.manager.DataManager;
import com.pmease.gitop.core.setting.ServerConfig;

public class Gitop extends AbstractPlugin {

	private static final Logger logger = LoggerFactory.getLogger(Gitop.class);
	
	private final DataManager dataManager;
	
	private final ServerConfig serverConfig;

	private final String appName;
	
	private volatile InitStage initStage;
	
	private final TaskScheduler taskScheduler;
	
	private final Provider<GitConfig> gitConfigProvider;
	
	private volatile String gitError;
	
	private String gitCheckTaskId;
	
	@Inject
	public Gitop(ServerConfig serverConfig, DataManager dataManager, TaskScheduler taskScheduler, 
			Provider<GitConfig> gitConfigProvider, @AppName String appName) {
		this.dataManager = dataManager;
		this.serverConfig = serverConfig;
		this.taskScheduler = taskScheduler;
		this.gitConfigProvider = gitConfigProvider;
		
		this.appName = appName;
		
		initStage = new InitStage("Server is Starting...");
	}
	
	@Override
	public void start() {
		List<ManualConfig> manualConfigs = dataManager.init();
		
		if (!manualConfigs.isEmpty()) {
			logger.warn("Please set up the server at " + Gitop.getInstance().guessServerUrl() + ".");
			initStage = new InitStage("Server Setup", manualConfigs);
			
			initStage.waitFor();
		}
		
		gitCheckTaskId = taskScheduler.schedule(new SchedulableTask() {
			
			@Override
			public ScheduleBuilder<?> getScheduleBuilder() {
				return SimpleScheduleBuilder.repeatMinutelyForever();
			}
			
			@Override
			public void execute() {
				checkGit();
			}
			
		});
	}
	
	public void checkGit() {
		gitError = GitCommand.checkError(gitConfigProvider.get().getExecutable());
	}
	
	@Override
	public void postStart() {
		initStage = null;
		
		logger.info("Server is ready at " + guessServerUrl() + ".");
	}

	public String guessServerUrl() {
		String hostName;
		try {
			hostName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
		
		String serverUrl;
		if (serverConfig.getHttpPort() != 0)
			serverUrl = "http://" + hostName + ":" + serverConfig.getHttpPort();
		else 
			serverUrl = "https://" + hostName + ":" + serverConfig.getSslConfig().getPort();

		return serverUrl + serverConfig.getContextPath();
	}
	
	/**
	 * Get context aware servlet request path given path inside context. For instance if 
	 * pathInContext is &quot;/images/ok.png&quot;, this method will return 
	 * &quot;/gitop/images/ok.png&quot; if Gitop web UI is configured to run under 
	 * context path &quot;/gitop&quot;
	 *  
	 * @param pathInContext
	 * 			servlet request path inside servlet context. It does not matter whether or 
	 * 			not this path starts with slash 
	 * @return
	 * 			absolute path prepending servlet context path
	 */
	public String getContextAwarePath(String pathInContext) {
		String contextAwarePath = serverConfig.getContextPath();
		contextAwarePath = StringUtils.stripEnd(contextAwarePath, "/");
		if (pathInContext.startsWith("/"))
			return contextAwarePath + pathInContext;
		else
			return contextAwarePath + "/" + pathInContext;
	}
	
	public String getAppName() {
		return appName;
	}
	
	/**
	 * This method can be called from different UI threads, so we clone initStage to 
	 * make it thread-safe.
	 * <p>
	 * @return
	 * 			cloned initStage, or <tt>null</tt> if system initialization is completed
	 */
	public @Nullable InitStage getInitStage() {
		if (initStage != null) {
			return initStage.clone();
		} else {
			return null;
		}
	}
	
	public boolean isReady() {
		return initStage == null;
	}
	
	public static Gitop getInstance() {
		return AppLoader.getInstance(Gitop.class);
	}
	
	public static <T> T getInstance(Class<T> type) {
		return AppLoader.getInstance(type);
	}

	public <T> Set<T> getExtensions(Class<T> extensionPoint) {
		return AppLoader.getExtensions(extensionPoint);
	}

	@Override
	public void stop() {
		taskScheduler.unschedule(gitCheckTaskId);
	}
	
	public String getGitError() {
		return gitError;
	}
	
}
