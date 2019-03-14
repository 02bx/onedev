package io.onedev.server.command;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.shiro.authc.credential.PasswordService;
import org.hibernate.Interceptor;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.onedev.commons.launcher.bootstrap.Bootstrap;
import io.onedev.server.manager.UserManager;
import io.onedev.server.model.User;
import io.onedev.server.persistence.DefaultPersistManager;
import io.onedev.server.persistence.HibernateProperties;
import io.onedev.server.persistence.IdManager;
import io.onedev.server.persistence.dao.Dao;
import io.onedev.server.util.validation.EntityValidator;

@Singleton
public class ResetAdminPasswordCommand extends DefaultPersistManager {

	private static final Logger logger = LoggerFactory.getLogger(ResetAdminPasswordCommand.class);
	
	private final UserManager userManager;
	
	private final PasswordService passwordService;
	
	@Inject
	public ResetAdminPasswordCommand(PhysicalNamingStrategy physicalNamingStrategy, HibernateProperties properties, 
			Interceptor interceptor, IdManager idManager, Dao dao, 
			EntityValidator validator, UserManager userManager, PasswordService passwordService) {
		super(physicalNamingStrategy, properties, interceptor, idManager, dao, validator);
		this.userManager = userManager;
		this.passwordService = passwordService;
	}

	@Override
	public void start() {
		if (Bootstrap.command.getArgs().length == 0) {
			logger.error("Missing password parameter. Usage: {} <new password>", Bootstrap.command.getScript());
			System.exit(1);
		}
		if (Bootstrap.isServerRunning(Bootstrap.installDir)) {
			logger.error("Please stop server before resetting admin password");
			System.exit(1);
		}

		checkDataVersion(false);

		Metadata metadata = buildMetadata();
		sessionFactory = metadata.getSessionFactoryBuilder().applyInterceptor(interceptor).build();
		
		User root = userManager.get(User.ROOT_ID);
		if (root == null) {
			logger.error("Server not set up yet");
			System.exit(1);
		}
		String password = Bootstrap.command.getArgs()[0];
		root.setPassword(passwordService.encryptPassword(password));
		userManager.save(root);
		
		// wait for a short period to have embedded db flushing data
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		sessionFactory.close();
		
		logger.info("Password of user '" + root.getName() + "' has been reset to: " + password);
		System.exit(0);
	}

}
