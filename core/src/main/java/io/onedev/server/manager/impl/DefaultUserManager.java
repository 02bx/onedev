package io.onedev.server.manager.impl;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.shiro.authc.credential.PasswordService;
import org.eclipse.jgit.lib.PersonIdent;
import org.hibernate.ReplicationMode;
import org.hibernate.query.Query;

import io.onedev.launcher.loader.Listen;
import io.onedev.launcher.loader.ListenerRegistry;
import io.onedev.server.event.lifecycle.SystemStarted;
import io.onedev.server.exception.InUseException;
import io.onedev.server.manager.CacheManager;
import io.onedev.server.manager.IssueFieldManager;
import io.onedev.server.manager.ProjectManager;
import io.onedev.server.manager.UserManager;
import io.onedev.server.model.Project;
import io.onedev.server.model.User;
import io.onedev.server.model.support.BranchProtection;
import io.onedev.server.model.support.TagProtection;
import io.onedev.server.persistence.annotation.Sessional;
import io.onedev.server.persistence.annotation.Transactional;
import io.onedev.server.persistence.dao.AbstractEntityManager;
import io.onedev.server.persistence.dao.Dao;
import io.onedev.server.persistence.dao.EntityPersisted;
import io.onedev.server.util.UsageUtils;
import io.onedev.utils.StringUtils;

@Singleton
public class DefaultUserManager extends AbstractEntityManager<User> implements UserManager {

    private final PasswordService passwordService;
    
    private final ProjectManager projectManager;
    
    private final IssueFieldManager issueFieldManager;
    
    private final CacheManager cacheManager;
    
    private final ListenerRegistry listenerRegistry;
    
	@Inject
    public DefaultUserManager(Dao dao, ProjectManager projectManager, 
    		IssueFieldManager issueFieldManager, CacheManager cacheManager, 
    		PasswordService passwordService, ListenerRegistry listenerRegistry) {
        super(dao);
        
        this.passwordService = passwordService;
        this.projectManager = projectManager;
        this.issueFieldManager = issueFieldManager;
        this.cacheManager = cacheManager;
        this.listenerRegistry = listenerRegistry;
    }

    @Transactional
    @Override
	public void save(User user, String oldName) {
    	if (user.isRoot()) {
    		getSession().replicate(user, ReplicationMode.OVERWRITE);
    		listenerRegistry.post(new EntityPersisted(user, false));
    	} else {
    		dao.persist(user);
    	}

    	if (oldName != null && !oldName.equals(user.getName())) {
    		for (Project project: projectManager.findAll()) {
    			for (BranchProtection protection: project.getBranchProtections())
    				protection.onRenameUser(oldName, user.getName());
    			for (TagProtection protection: project.getTagProtections())
    				protection.onRenameUser(oldName, user.getName());
    			project.getIssueWorkflow().onRenameUser(oldName, user.getName());
    		}
    		
    		issueFieldManager.onRenameUser(oldName, user.getName());
    	}
    }
    
    @Override
    public void save(User user) {
    	save(user, null);
    }
    
    @Sessional
    @Override
    public User getRoot() {
    	return load(User.ROOT_ID);
    }

    @Transactional
    @Override
	public void delete(User user) {
    	Query<?> query = getSession().createQuery("update PullRequest set submitter=null, submitterName=:submitterName "
    			+ "where submitter=:submitter");
    	query.setParameter("submitter", user);
    	query.setParameter("submitterName", user.getDisplayName());
    	query.executeUpdate();
    	
    	query = getSession().createQuery("update Issue set reporter=null, reporterName=:reporterName "
    			+ "where reporter=:reporter");
    	query.setParameter("reporter", user);
    	query.setParameter("reporterName", user.getDisplayName());
    	query.executeUpdate();
    	
    	query = getSession().createQuery("update PullRequest set closeInfo.closedBy=null, "
    			+ "closeInfo.closedByName=:closedByName where closeInfo.closedBy=:closedBy");
    	query.setParameter("closedBy", user);
    	query.setParameter("closedByName", user.getDisplayName());
    	query.executeUpdate();
    	
    	query = getSession().createQuery("update PullRequest set lastEvent.user=null, "
    			+ "lastEvent.userName=:lastEventUserName where lastEvent.user=:lastEventUser");
    	query.setParameter("lastEventUser", user);
    	query.setParameter("lastEventUserName", user.getDisplayName());
    	query.executeUpdate();

    	query = getSession().createQuery("update PullRequestStatusChange set user=null, userName=:userName where user=:user");
    	query.setParameter("user", user);
    	query.setParameter("userName", user.getDisplayName());
    	query.executeUpdate();
    	
    	query = getSession().createQuery("update PullRequestComment set user=null, userName=:userName where user=:user");
    	query.setParameter("user", user);
    	query.setParameter("userName", user.getDisplayName());
    	query.executeUpdate();
    	
    	query = getSession().createQuery("update PullRequestReference set user=null, userName=:userName where user=:user");
    	query.setParameter("user", user);
    	query.setParameter("userName", user.getDisplayName());
    	query.executeUpdate();
    	
    	query = getSession().createQuery("update CodeComment set user=null, userName=:userName where user=:user");
    	query.setParameter("user", user);
    	query.setParameter("userName", user.getDisplayName());
    	query.executeUpdate();
    	
    	query = getSession().createQuery("update CodeComment set lastEvent.user=null, "
    			+ "lastEvent.userName=:lastEventUserName where lastEvent.user=:lastEventUser");
    	query.setParameter("lastEventUser", user);
    	query.setParameter("lastEventUserName", user.getDisplayName());
    	query.executeUpdate();
    	
    	query = getSession().createQuery("update CodeCommentReply set user=null, userName=:userName where user=:user");
    	query.setParameter("user", user);
    	query.setParameter("userName", user.getDisplayName());
    	query.executeUpdate();
    	
		dao.remove(user);

		List<String> usages = new ArrayList<>();
		for (Project project: projectManager.findAll()) {
			for (BranchProtection protection: project.getBranchProtections()) 
				usages.addAll(UsageUtils.prependCategory("Project '" + project.getName() + "' / Branch Protection", protection.onDeleteUser(user.getName())));
			for (TagProtection protection: project.getTagProtections()) 
				usages.addAll(UsageUtils.prependCategory("Project '" + project.getName() + "' / Tag Protection", protection.onDeleteUser(user.getName())));
			usages.addAll(UsageUtils.prependCategory("Project '" + project.getName(), project.getIssueWorkflow().onDeleteUser(user.getName())));
		}
		
		if (!usages.isEmpty())
			throw new InUseException("User '" + user.getName() + "'", usages);
	}

	@Sessional
    @Override
    public User findByName(String userName) {
		Long id = cacheManager.getUserIdByName(userName);
		if (id != null) 
			return load(id);
		else
			return null;
    }

	@Sessional
    @Override
    public User findByEmail(String email) {
		Long id = cacheManager.getUserIdByEmail(email);
		if (id != null) 
			return load(id);
		else
			return null;
    }
	
    @Sessional
    @Override
    public User find(PersonIdent person) {
    	return findByEmail(person.getEmailAddress());
    }
    
    @Override
	public User getCurrent() {
		Long userId = User.getCurrentId();
		if (userId != 0L) {
			User user = get(userId);
			if (user != null)
				return user;
		}
		return null;
	}

	@Listen
	public void on(SystemStarted event) {
		for (User user: findAll()) {
			// Fix a critical issue that password of self-registered users are not hashed
			if (StringUtils.isNotBlank(user.getPassword()) && !user.getPassword().startsWith("$2a$10") 
					&& !user.getPassword().startsWith("@hash^prefix@")) {
				user.setPassword(passwordService.encryptPassword(user.getPassword()));
				save(user);
			}
		}
	}
	
}