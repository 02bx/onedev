package com.pmease.commons.git.command;

import java.io.File;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.annotation.Nullable;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pmease.commons.git.GitVersion;
import com.pmease.commons.util.FileUtils;
import com.pmease.commons.util.execution.Commandline;
import com.pmease.commons.util.execution.LineConsumer;

public abstract class GitCommand<V> implements Callable<V> {

	protected static final Logger logger = LoggerFactory.getLogger(GitCommand.class);

	private static final String GIT_EXE = "git";
	
	private static final String MIN_VERSION = "1.8.0";
	
	protected final File repoDir;
	
	private final Map<String, String> environments;
	
	protected static final LineConsumer debugLogger = new LineConsumer() {

		@Override
		public void consume(String line) {
			logger.debug(line);
		}
		
	};
	
	protected static final LineConsumer infoLogger = new LineConsumer() {

		@Override
		public void consume(String line) {
			logger.info(line);
		}
		
	};
	
	protected static final LineConsumer warnLogger = new LineConsumer() {

		@Override
		public void consume(String line) {
			logger.warn(line);
		}
		
	};
	
	protected static final LineConsumer errorLogger = new LineConsumer() {

		@Override
		public void consume(String line) {
			logger.error(line);
		}
		
	};
	
	protected static final LineConsumer traceLogger = new LineConsumer() {

		@Override
		public void consume(String line) {
			logger.trace(line);
		}
		
	};
	
	public GitCommand(File repoDir, @Nullable Map<String, String> environments) {
		this.repoDir = repoDir;
		this.environments = environments;
		
		if (!repoDir.exists())
		    FileUtils.createDir(repoDir);
	}

	public GitCommand(File repoDir) {
		this(repoDir, null);
	}

	/**
	 * Check if there are any errors with git command line. 
	 *
	 * @return
	 * 			error message if failed to check git command line, 
	 * 			or <tt>null</tt> otherwise
	 * 			
	 */
	public static String checkError() {
		try {
			final String[] version = new String[]{null};
			
			new Commandline(GIT_EXE).addArgs("--version").execute(new LineConsumer() {
	
				@Override
				public void consume(String line) {
					if (line.trim().length() != 0)
						version[0] = line.trim();
				}
				
			}, new LineConsumer.ErrorLogger()).checkReturnCode();
	
			if (version[0] == null)
				throw new RuntimeException("Unable to determine git version.");
			
			GitVersion gitVersion = new GitVersion(version[0]);
			
			if (gitVersion.isOlderThan(new GitVersion(MIN_VERSION)))
				throw new RuntimeException("Git version should be at least " + MIN_VERSION);
			
			return null;
			
		} catch (Exception e) {
			return ExceptionUtils.getMessage(e);
		}
	}
	
	public Commandline cmd() {
		Commandline cmd = new Commandline(GIT_EXE).workingDir(repoDir);
		if (environments != null)
			cmd.environment(environments);
		return cmd;
	}

	@Override
	public abstract V call();
	
	protected void debug(String line) {
		logger.debug(line);
	}
	
	protected void trace(String line) {
		logger.trace(line);
	}

	protected void info(String line) {
		logger.info(line);
	}

	protected void warn(String line) {
		logger.warn(line);
	}

	protected void error(String line) {
		logger.error(line);
	}
	
}
