package com.pmease.gitplex.core.manager.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.jgit.lib.PersonIdent;
import org.hibernate.Query;
import org.hibernate.ReplicationMode;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.pmease.commons.hibernate.Sessional;
import com.pmease.commons.hibernate.Transactional;
import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.commons.util.StringUtils;
import com.pmease.gitplex.core.listeners.LifecycleListener;
import com.pmease.gitplex.core.manager.RepositoryManager;
import com.pmease.gitplex.core.manager.UserManager;
import com.pmease.gitplex.core.model.Membership;
import com.pmease.gitplex.core.model.Repository;
import com.pmease.gitplex.core.model.Team;
import com.pmease.gitplex.core.model.User;
import com.pmease.gitplex.core.permission.operation.RepositoryOperation;

@Singleton
public class DefaultUserManager implements UserManager, LifecycleListener {

    private final Dao dao;

    private final RepositoryManager repositoryManager;
    
    private final ReadWriteLock idLock = new ReentrantReadWriteLock();
    		
	private final Map<String, Set<Long>> emailToIds = new HashMap<>();
	
	private final BiMap<String, Long> nameToId = HashBiMap.create();
	
	@Inject
    public DefaultUserManager(Dao dao, RepositoryManager repositoryManager) {
        this.dao = dao;
        this.repositoryManager = repositoryManager;
    }

    @Transactional
    @Override
	public void save(final User user) {
    	boolean isNew;
    	if (user.isRoot()) {
    		isNew = dao.get(User.class, User.ROOT_ID) == null;
    		dao.getSession().replicate(user, ReplicationMode.OVERWRITE);
    	} else {
    		isNew = user.isNew();
    		dao.persist(user);
    	}
    	
    	if (isNew) {
        	Team team = new Team();
        	team.setOwner(user);
        	team.setAuthorizedOperation(RepositoryOperation.NO_ACCESS);
        	team.setName(Team.ANONYMOUS);
        	dao.persist(team);
        	
        	team = new Team();
        	team.setOwner(user);
        	team.setName(Team.LOGGEDIN);
        	team.setAuthorizedOperation(RepositoryOperation.NO_ACCESS);
        	dao.persist(team);
        	
        	team = new Team();
        	team.setOwner(user);
        	team.setName(Team.OWNERS);
        	team.setAuthorizedOperation(RepositoryOperation.ADMIN);
        	dao.persist(team);
        	
        	Membership membership = new Membership();
        	membership.setTeam(team);
        	membership.setUser(user);
        	dao.persist(membership);
    	}
    	
    	dao.afterCommit(new Runnable() {

			@Override
			public void run() {
				idLock.writeLock().lock();
				try {
					Set<Long> ids = emailToIds.get(user.getEmail());
					if (ids == null) {
						ids = new HashSet<>();
						emailToIds.put(user.getEmail(), ids);
					}
					ids.add(user.getId());
					nameToId.inverse().put(user.getId(), user.getName());
				} finally {
					idLock.writeLock().unlock();
				}
			}
    		
    	});
    }

    @Sessional
    @Override
    public User getRoot() {
    	return dao.load(User.class, User.ROOT_ID);
    }

    @Transactional
    @Override
	public void delete(final User user) {
    	Query query = dao.getSession().createQuery("update PullRequest set submitter=null where submitter=:submitter");
    	query.setParameter("submitter", user);
    	query.executeUpdate();
    	
    	query = dao.getSession().createQuery("update PullRequest set assignee.id=:rootId where assignee=:assignee");
    	query.setParameter("rootId", User.ROOT_ID);
    	query.setParameter("assignee", user);
    	query.executeUpdate();
    	
    	query = dao.getSession().createQuery("update PullRequest set closedBy=null where closedBy=:closedBy");
    	query.setParameter("closedBy", user);
    	query.executeUpdate();
    	
    	query = dao.getSession().createQuery("update PullRequestActivity set user=null where user=:user");
    	query.setParameter("user", user);
    	query.executeUpdate();
    	
    	query = dao.getSession().createQuery("update PullRequestActivity set user=null where user=:user");
    	query.setParameter("user", user);
    	query.executeUpdate();
    	
    	query = dao.getSession().createQuery("update Comment set user=null where user=:user");
    	query.setParameter("user", user);
    	query.executeUpdate();
    	
    	query = dao.getSession().createQuery("update CommentReply set user=null where user=:user");
    	query.setParameter("user", user);
    	query.executeUpdate();
    	
    	for (Repository repository: user.getRepositories())
    		repositoryManager.delete(repository);
    	
		dao.remove(user);
		
		dao.afterCommit(new Runnable() {

			@Override
			public void run() {
				idLock.writeLock().lock();
				try {
					for (Iterator<Map.Entry<String, Set<Long>>> it = emailToIds.entrySet().iterator(); it.hasNext();) {
						Map.Entry<String, Set<Long>> entry = it.next();
						entry.getValue().remove(user.getId());
						if (entry.getValue().isEmpty())
							it.remove();
					}
					nameToId.inverse().remove(user.getId());
				} finally {
					idLock.writeLock().unlock();
				}
			}
			
		});
	}

	@Sessional
    @Override
    public User findByName(String userName) {
    	idLock.readLock().lock();
    	try {
    		Long id = nameToId.get(userName);
    		if (id != null)
    			return dao.load(User.class, id);
    		else
    			return null;
    	} finally {
    		idLock.readLock().unlock();
    	}
    }

    @Sessional
    @Override
    public User findByPerson(PersonIdent person) {
    	idLock.readLock().lock();
    	try {
    		Set<Long> ids = emailToIds.get(person.getEmailAddress());
    		if (ids != null) {
    			if (ids.size() > 1) {
    				String personName = person.getName().toLowerCase();
    				int minDistance = Integer.MAX_VALUE;
    				User minDistanceUser = null;
	    			for (Long id: ids) {
	    				User user = dao.load(User.class, id);
	    				int distance;
	    				if (user.getFullName() != null) {
	    					int distance1 = StringUtils.calcLevenshteinDistance(personName, user.getFullName().toLowerCase());
	    					if (distance1 == 0)
	    						return user;
	    					int distance2 = StringUtils.calcLevenshteinDistance(personName, user.getName().toLowerCase());
	    					if (distance2 == 0)
	    						return user;
	    					distance = distance1<distance2?distance1:distance2;
	    				}  else {
	    					distance = StringUtils.calcLevenshteinDistance(personName, user.getName().toLowerCase());
	    					if (distance == 0)
	    						return user;
	    				}
	    				if (distance<minDistance) {
	    					distance = minDistance;
	    					minDistanceUser = user;
	    				}
	    			}
	    			return Preconditions.checkNotNull(minDistanceUser);
    			} else {
    				return dao.load(User.class, ids.iterator().next());
    			}
    		} else {
    			return null;
    		}
    	} finally {
    		idLock.readLock().unlock();
    	}
    }
    
    @Override
	public User getCurrent() {
		Long userId = User.getCurrentId();
		if (userId != 0L) {
			User user = dao.get(User.class, userId);
			if (user != null)
				return user;
		}
		return null;
	}

	@Override
	public void trim(Collection<Long> userIds) {
		for (Iterator<Long> it = userIds.iterator(); it.hasNext();) {
			if (dao.get(User.class, it.next()) == null)
				it.remove();
		}
	}

	@Override
	public User getPrevious() {
		Long userId = User.getPreviousId();
		if (userId != 0L) {
			User user = dao.get(User.class, userId);
			if (user != null)
				return user;
		}
		return null;
	}

	@Sessional
	@Override
	public void systemStarting() {
        for (User user: dao.allOf(User.class)) {
        	Set<Long> ids = emailToIds.get(user.getEmail());
        	if (ids == null) {
        		ids = new HashSet<>();
        		emailToIds.put(user.getEmail(), ids);
        	}
        	ids.add(user.getId());
        	
        	nameToId.inverse().put(user.getId(), user.getName());
        }
	}

	@Override
	public void systemStarted() {
	}

	@Override
	public void systemStopping() {
	}

	@Override
	public void systemStopped() {
	}

}