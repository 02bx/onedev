package io.onedev.server.command;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.onedev.launcher.bootstrap.Bootstrap;
import io.onedev.server.persistence.DefaultPersistManager;
import io.onedev.server.persistence.HibernateProperties;
import io.onedev.server.persistence.IdManager;
import io.onedev.server.persistence.dao.Dao;
import io.onedev.server.util.validation.EntityValidator;

@Singleton
public class CheckDataVersionCommand extends DefaultPersistManager {

	private static final Logger logger = LoggerFactory.getLogger(CheckDataVersionCommand.class);
	
	@Inject
	public CheckDataVersionCommand(PhysicalNamingStrategy physicalNamingStrategy,
			HibernateProperties properties, Interceptor interceptor, 
			IdManager idManager, Dao dao, EntityValidator validator) {
		super(physicalNamingStrategy, properties, interceptor, idManager, dao, validator);
	}

	@Override
	public void start() {
		if (Bootstrap.isServerRunning(Bootstrap.installDir) && getDialect().toLowerCase().contains("hsql")) {
			logger.error("Please stop server before checking data version");
			System.exit(1);
		}
		
		// Use system.out in case logger is suppressed by user as this output is important to 
		// upgrade procedure
		System.out.println("Data version: " + checkDataVersion(false));
		System.exit(0);
	}

	@Override
	public SessionFactory getSessionFactory() {
		throw new UnsupportedOperationException();
	}

}
