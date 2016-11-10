package com.gitplex.commons.hibernate.command;

import java.io.File;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hibernate.Interceptor;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gitplex.commons.hibernate.DefaultPersistManager;
import com.gitplex.commons.hibernate.EntityValidator;
import com.gitplex.commons.hibernate.HibernateProperties;
import com.gitplex.commons.hibernate.IdManager;
import com.gitplex.commons.hibernate.ModelProvider;
import com.gitplex.commons.hibernate.dao.Dao;
import com.gitplex.commons.hibernate.migration.Migrator;
import com.google.common.base.Throwables;
import com.gitplex.commons.bootstrap.Bootstrap;
import com.gitplex.commons.bootstrap.BootstrapUtils;
import com.gitplex.commons.util.FileUtils;

@Singleton
public class DefaultBackupCommand extends DefaultPersistManager {

	private static final Logger logger = LoggerFactory.getLogger(DefaultBackupCommand.class);
	
	@Inject
	public DefaultBackupCommand(Set<ModelProvider> modelProviders, PhysicalNamingStrategy physicalNamingStrategy,
			HibernateProperties properties, Migrator migrator, Interceptor interceptor, 
			IdManager idManager, Dao dao, EntityValidator validator) {
		super(modelProviders, physicalNamingStrategy, properties, migrator, interceptor, idManager, dao, validator);
	}

	@Override
	public void start() {
		if (Bootstrap.command.getArgs().length == 0) {
			logger.error("Missing backup file parameter. Usage: {} <path to database backup file>", Bootstrap.command.getScript());
			System.exit(1);
		}
		File backupFile = new File(Bootstrap.command.getArgs()[0]);
		if (!backupFile.isAbsolute())
			backupFile = new File(Bootstrap.getBinDir(), backupFile.getPath());
		
		if (backupFile.exists()) {
			logger.error("Backup file already exists: {}", backupFile.getAbsolutePath());
			System.exit(1);
		}
		
		if (Bootstrap.isServerRunning() && getDialect().toLowerCase().contains("hsql")) {
			logger.error("Please stop server before backing up database");
			System.exit(1);
		}
		
		checkDataVersion(false);
		
		logger.info("Backing up database to {}...", backupFile.getAbsolutePath());
		
		Metadata metadata = buildMetadata();
		sessionFactory = metadata.getSessionFactoryBuilder().applyInterceptor(interceptor).build();

		File tempDir = BootstrapUtils.createTempDir("backup");
		try {
			exportData(tempDir);
			BootstrapUtils.zip(tempDir, backupFile);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		} finally {
			FileUtils.deleteDir(tempDir);
		}

		sessionFactory.close();
		logger.info("Database is successfully backed up to {}", backupFile.getAbsolutePath());
		
		System.exit(0);
	}

}
