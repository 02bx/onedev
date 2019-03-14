package io.onedev.server.manager.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import io.onedev.commons.launcher.loader.Listen;
import io.onedev.server.event.entity.EntityPersisted;
import io.onedev.server.event.entity.EntityRemoved;
import io.onedev.server.event.system.SystemStarted;
import io.onedev.server.manager.CacheManager;
import io.onedev.server.model.Build;
import io.onedev.server.model.Configuration;
import io.onedev.server.model.Group;
import io.onedev.server.model.GroupAuthorization;
import io.onedev.server.model.Issue;
import io.onedev.server.model.Membership;
import io.onedev.server.model.Project;
import io.onedev.server.model.User;
import io.onedev.server.model.UserAuthorization;
import io.onedev.server.persistence.annotation.Sessional;
import io.onedev.server.persistence.annotation.Transactional;
import io.onedev.server.persistence.dao.Dao;
import io.onedev.server.util.facade.BuildFacade;
import io.onedev.server.util.facade.ConfigurationFacade;
import io.onedev.server.util.facade.EntityFacade;
import io.onedev.server.util.facade.GroupAuthorizationFacade;
import io.onedev.server.util.facade.GroupFacade;
import io.onedev.server.util.facade.IssueFacade;
import io.onedev.server.util.facade.MembershipFacade;
import io.onedev.server.util.facade.ProjectFacade;
import io.onedev.server.util.facade.UserAuthorizationFacade;
import io.onedev.server.util.facade.UserFacade;

@Singleton
public class DefaultCacheManager implements CacheManager {

	private final Logger logger = LoggerFactory.getLogger(DefaultCacheManager.class);
	
	private final Dao dao;
	
	private final Map<Long, UserFacade> users = new HashMap<>();
	
	private final BiMap<String, Long> userIdsByName = HashBiMap.create();
	
	private final BiMap<String, Long> userIdsByEmail = HashBiMap.create();
	
	private final ReadWriteLock usersLock = new ReentrantReadWriteLock();
	
	private final Map<Long, ProjectFacade> projects = new HashMap<>();
	
	private final BiMap<String, Long> projectIdsByName = HashBiMap.create();
	
	private final ReadWriteLock projectsLock = new ReentrantReadWriteLock();
	
	private final Map<Long, GroupFacade> groups = new HashMap<>();
	
	private final BiMap<String, Long> groupIdsByName = HashBiMap.create();
	
	private final ReadWriteLock groupsLock = new ReentrantReadWriteLock();
	
	private final Map<Long, MembershipFacade> memberships = new HashMap<>();
	
	private final ReadWriteLock membershipsLock = new ReentrantReadWriteLock();

	private final Map<Long, ConfigurationFacade> configurations = new HashMap<>();
	
	private final ReadWriteLock configurationsLock = new ReentrantReadWriteLock();
	
	private final Map<Long, BuildFacade> builds= new HashMap<>();
	
	private final ReadWriteLock buildsLock = new ReentrantReadWriteLock();
	
	private final Map<Long, IssueFacade> issues = new HashMap<>();
	
	private final ReadWriteLock issuesLock = new ReentrantReadWriteLock();
	
	private final Map<Long, GroupAuthorizationFacade> groupAuthorizations = new HashMap<>(); 
	
	private final ReadWriteLock groupAuthorizationsLock = new ReentrantReadWriteLock();
	
	private final Map<Long, UserAuthorizationFacade> userAuthorizations = new HashMap<>();
	
	private final ReadWriteLock userAuthorizationsLock = new ReentrantReadWriteLock();
	
	@Inject
	public DefaultCacheManager(Dao dao) {
		this.dao = dao;
	}
	
	@SuppressWarnings("unchecked")
	@Sessional
	@Listen
	public void on(SystemStarted event) {
		logger.info("Caching meta data...");
		for (Project project: dao.query(Project.class)) {
			projects.put(project.getId(), project.getFacade());
			projectIdsByName.inverse().put(project.getId(), project.getName());
		}
		for (User user: dao.query(User.class)) {
			users.put(user.getId(), user.getFacade());
			userIdsByName.inverse().put(user.getId(), user.getName());
			if (user.getEmail() != null)
				userIdsByEmail.inverse().put(user.getId(), user.getEmail());
		}
		for (Group group: dao.query(Group.class)) {
			groups.put(group.getId(), group.getFacade());
			groupIdsByName.inverse().put(group.getId(), group.getName());
		}
		
		for (Membership membership: dao.query(Membership.class))
			memberships.put(membership.getId(), membership.getFacade());
		
		for (Configuration configuration: dao.query(Configuration.class))
			configurations.put(configuration.getId(), configuration.getFacade());
		
		Query<?> query = dao.getSession().createQuery("select id, configuration.id, commitHash from Build");
		for (Object[] fields: (List<Object[]>)query.list()) {
			Long buildId = (Long) fields[0];
			builds.put(buildId, new BuildFacade(buildId, (Long)fields[1], (String)fields[2]));
		}
		
		query = dao.getSession().createQuery("select id, project.id, number from Issue");
		for (Object[] fields: (List<Object[]>)query.list()) {
			Long issueId = (Long) fields[0];
			issues.put(issueId, new IssueFacade(issueId, (Long)fields[1], (Long)fields[2]));
		}
		
		for (GroupAuthorization groupAuthorization: dao.query(GroupAuthorization.class))
			groupAuthorizations.put(groupAuthorization.getId(), groupAuthorization.getFacade());
		for (UserAuthorization userAuthorization: dao.query(UserAuthorization.class))
			userAuthorizations.put(userAuthorization.getId(), userAuthorization.getFacade());
	}
	
	@Transactional
	@Listen
	public void on(EntityPersisted event) {
		EntityFacade facade;
		
		if (event.getEntity() instanceof Project) {
			facade = ((Project) event.getEntity()).getFacade();
		} else if (event.getEntity() instanceof User) {
			facade = ((User) event.getEntity()).getFacade();
		} else if (event.getEntity() instanceof Group) {
			facade = ((Group) event.getEntity()).getFacade();
		} else if (event.getEntity() instanceof Membership) {
			facade = ((Membership) event.getEntity()).getFacade();
		} else if (event.getEntity() instanceof Build) {
			facade = ((Build)event.getEntity()).getFacade();
		} else if (event.getEntity() instanceof Issue) {
			facade = ((Issue)event.getEntity()).getFacade();
		} else if (event.getEntity() instanceof Configuration) {
			facade = ((Configuration)event.getEntity()).getFacade();
		} else if (event.getEntity() instanceof UserAuthorization) {
			facade = ((UserAuthorization) event.getEntity()).getFacade();
		} else if (event.getEntity() instanceof GroupAuthorization) {
			facade = ((GroupAuthorization) event.getEntity()).getFacade();
		} else {
			facade = null;
		}
		
		dao.doAfterCommit(new Runnable() {

			@Override
			public void run() {
				if (facade instanceof ProjectFacade) {
					ProjectFacade project = (ProjectFacade) facade;
					projectsLock.writeLock().lock();
					try {
						projects.put(project.getId(), project);
						projectIdsByName.inverse().put(project.getId(), project.getName());
					} finally {
						projectsLock.writeLock().unlock();
					}
				} else if (facade instanceof UserFacade) {
					UserFacade user = (UserFacade) facade;
					usersLock.writeLock().lock();
					try {
						users.put(user.getId(), user);
						userIdsByName.inverse().put(user.getId(), user.getName());
						if (user.getEmail() != null)
							userIdsByEmail.inverse().put(user.getId(), user.getEmail());
					} finally {
						usersLock.writeLock().unlock();
					}
				} else if (facade instanceof GroupFacade) {
					GroupFacade group = (GroupFacade) facade;
					groupsLock.writeLock().lock();
					try {
						groups.put(group.getId(), group);
						groupIdsByName.inverse().put(group.getId(), group.getName());
					} finally {
						groupsLock.writeLock().unlock();
					}
				} else if (facade instanceof ConfigurationFacade) {
					ConfigurationFacade configuration = (ConfigurationFacade) facade;
					configurationsLock.writeLock().lock();
					try {
						configurations.put(configuration.getId(), configuration);
					} finally {
						configurationsLock.writeLock().unlock();
					}
				} else if (facade instanceof IssueFacade) {
					IssueFacade issue = (IssueFacade) facade;
					issuesLock.writeLock().lock();
					try {
						issues.put(issue.getId(), issue);
					} finally {
						issuesLock.writeLock().unlock();
					}
				} else if (facade instanceof BuildFacade) {
					BuildFacade build = (BuildFacade) facade;
					buildsLock.writeLock().lock();
					try {
						builds.put(build.getId(), build);
					} finally {
						buildsLock.writeLock().unlock();
					}
				} else if (facade instanceof MembershipFacade) {
					MembershipFacade membership = (MembershipFacade) facade;
					membershipsLock.writeLock().lock();
					try {
						memberships.put(membership.getId(), membership);
					} finally {
						membershipsLock.writeLock().unlock();
					}
				} else if (facade instanceof UserAuthorizationFacade) {
					UserAuthorizationFacade userAuthorization = (UserAuthorizationFacade) facade;
					userAuthorizationsLock.writeLock().lock();
					try {
						userAuthorizations.put(userAuthorization.getId(), userAuthorization);
					} finally {
						userAuthorizationsLock.writeLock().unlock();
					}
				} else if (facade instanceof GroupAuthorizationFacade) {
					GroupAuthorizationFacade groupAuthorization = (GroupAuthorizationFacade) facade;
					groupAuthorizationsLock.writeLock().lock();
					try {
						groupAuthorizations.put(groupAuthorization.getId(), groupAuthorization);
					} finally {
						groupAuthorizationsLock.writeLock().unlock();
					}
				}
			}
			
		});
		
	}
	
	@Transactional
	@Listen
	public void on(EntityRemoved event) {
		Long id = event.getEntity().getId();
		Class<?> clazz = event.getEntity().getClass();
		
		dao.doAfterCommit(new Runnable() {

			@Override
			public void run() {
				if (Project.class.isAssignableFrom(clazz)) {
					projectsLock.writeLock().lock();
					try {
						projects.remove(id);
						projectIdsByName.inverse().remove(id);
					} finally {
						projectsLock.writeLock().unlock();
					}
					userAuthorizationsLock.writeLock().lock();
					try {
						for (Iterator<Map.Entry<Long, UserAuthorizationFacade>> it = userAuthorizations.entrySet().iterator(); it.hasNext();) {
							if (it.next().getValue().getProjectId().equals(id))
								it.remove();
						}
					} finally {
						userAuthorizationsLock.writeLock().unlock();
					}
					groupAuthorizationsLock.writeLock().lock();
					try {
						for (Iterator<Map.Entry<Long, GroupAuthorizationFacade>> it = groupAuthorizations.entrySet().iterator(); it.hasNext();) {
							if (it.next().getValue().getProjectId().equals(id))
								it.remove();
						}
					} finally {
						groupAuthorizationsLock.writeLock().unlock();
					}
					configurationsLock.writeLock().lock();
					try {
						for (Iterator<Map.Entry<Long, ConfigurationFacade>> it = configurations.entrySet().iterator(); it.hasNext();) {
							ConfigurationFacade configuration = it.next().getValue();
							if (configuration.getProjectId().equals(id)) {
								it.remove();
								buildsLock.writeLock().lock();
								try {
									for (Iterator<Map.Entry<Long, BuildFacade>> it2 = builds.entrySet().iterator(); it2.hasNext();) {
										if (it2.next().getValue().getConfigurationId().equals(configuration.getId()))
											it2.remove();
									}
								} finally {
									buildsLock.writeLock().unlock();
								}
							}
						}
					} finally {
						configurationsLock.writeLock().unlock();
					}
					issuesLock.writeLock().lock();
					try {
						for (Iterator<Map.Entry<Long, IssueFacade>> it = issues.entrySet().iterator(); it.hasNext();) {
							IssueFacade issue = it.next().getValue();
							if (issue.getProjectId().equals(id))
								it.remove();
						}
					} finally {
						issuesLock.writeLock().unlock();
					}
				} else if (User.class.isAssignableFrom(clazz)) {
					usersLock.writeLock().lock();
					try {
						users.remove(id);
						userIdsByName.inverse().remove(id);
						userIdsByEmail.inverse().remove(id);
					} finally {
						usersLock.writeLock().unlock();
					}
					userAuthorizationsLock.writeLock().lock();
					try {
						for (Iterator<Map.Entry<Long, UserAuthorizationFacade>> it = userAuthorizations.entrySet().iterator(); it.hasNext();) {
							if (it.next().getValue().getUserId().equals(id))
								it.remove();
						}
					} finally {
						userAuthorizationsLock.writeLock().unlock();
					}
					membershipsLock.writeLock().lock();
					try {
						for (Iterator<Map.Entry<Long, MembershipFacade>> it = memberships.entrySet().iterator(); it.hasNext();) {
							if (it.next().getValue().getUserId().equals(id))
								it.remove();
						}
					} finally {
						membershipsLock.writeLock().unlock();
					}
				} else if (Group.class.isAssignableFrom(clazz)) {
					groupsLock.writeLock().lock();
					try {
						groups.remove(id);
						groupIdsByName.inverse().remove(id);
					} finally {
						groupsLock.writeLock().unlock();
					}
					groupAuthorizationsLock.writeLock().lock();
					try {
						for (Iterator<Map.Entry<Long, GroupAuthorizationFacade>> it = groupAuthorizations.entrySet().iterator(); it.hasNext();) {
							if (it.next().getValue().getGroupId().equals(id))
								it.remove();
						}
					} finally {
						groupAuthorizationsLock.writeLock().unlock();
					}
					membershipsLock.writeLock().lock();
					try {
						for (Iterator<Map.Entry<Long, MembershipFacade>> it = memberships.entrySet().iterator(); it.hasNext();) {
							if (it.next().getValue().getGroupId().equals(id))
								it.remove();
						}
					} finally {
						membershipsLock.writeLock().unlock();
					}
				} else if (Configuration.class.isAssignableFrom(clazz)) {
					configurationsLock.writeLock().lock();
					try {
						configurations.remove(id);
					} finally {
						configurationsLock.writeLock().unlock();
					}
					buildsLock.writeLock().lock();
					try {
						for (Iterator<Map.Entry<Long, BuildFacade>> it = builds.entrySet().iterator(); it.hasNext();) {
							if (it.next().getValue().getConfigurationId().equals(id))
								it.remove();
						}
					} finally {
						buildsLock.writeLock().unlock();
					}
				} else if (Build.class.isAssignableFrom(clazz)) {
					buildsLock.writeLock().lock();
					try {
						builds.remove(id);
					} finally {
						buildsLock.writeLock().unlock();
					}
				} else if (Issue.class.isAssignableFrom(clazz)) {
					issuesLock.writeLock().lock();
					try {
						issues.remove(id);
					} finally {
						issuesLock.writeLock().unlock();
					}
				} else if (Membership.class.isAssignableFrom(clazz)) {
					membershipsLock.writeLock().lock();
					try {
						memberships.remove(id);
					} finally {
						membershipsLock.writeLock().unlock();
					}
				} else if (UserAuthorization.class.isAssignableFrom(clazz)) {
					userAuthorizationsLock.writeLock().lock();
					try {
						userAuthorizations.remove(id);
					} finally {
						userAuthorizationsLock.writeLock().unlock();
					}
				} else if (GroupAuthorization.class.isAssignableFrom(clazz)) {
					groupAuthorizationsLock.writeLock().lock();
					try {
						groupAuthorizations.remove(id);
					} finally {
						groupAuthorizationsLock.writeLock().unlock();
					}
				}
			}
		});
	}

	@Override
	public Map<Long, ProjectFacade> getProjects() {
		projectsLock.readLock().lock();
		try {
			return new HashMap<>(projects);
		} finally {
			projectsLock.readLock().unlock();
		}
	}

	@Override
	public Map<Long, UserFacade> getUsers() {
		usersLock.readLock().lock();
		try {
			return new HashMap<>(users);
		} finally {
			usersLock.readLock().unlock();
		}
	}

	public Map<Long, GroupFacade> getGroups() {
		groupsLock.readLock().lock();
		try {
			return new HashMap<>(groups);
		} finally {
			groupsLock.readLock().unlock();
		}
	}
	
	@Override
	public Map<Long, MembershipFacade> getMemberships() {
		membershipsLock.readLock().lock();
		try {
			return new HashMap<>(memberships);
		} finally {
			membershipsLock.readLock().unlock();
		}
	}

	@Override
	public ProjectFacade getProject(Long id) {
		projectsLock.readLock().lock();
		try {
			return projects.get(id);
		} finally {
			projectsLock.readLock().unlock();
		}
	}

	@Override
	public UserFacade getUser(Long id) {
		usersLock.readLock().lock();
		try {
			return users.get(id);
		} finally {
			usersLock.readLock().unlock();
		}
	}
	
	@Override
	public Long getUserIdByName(String name) {
		usersLock.readLock().lock();
		try {
			return userIdsByName.get(name);
		} finally {
			usersLock.readLock().unlock();
		}
	}

	@Override
	public Long getUserIdByEmail(String email) {
		usersLock.readLock().lock();
		try {
			return userIdsByEmail.get(email);
		} finally {
			usersLock.readLock().unlock();
		}
	}
	
	@Override
	public Long getProjectIdByName(String name) {
		projectsLock.readLock().lock();
		try {
			return projectIdsByName.get(name);
		} finally {
			projectsLock.readLock().unlock();
		}
	}
	
	@Override
	public Map<String, Long> getProjectIds() {
		projectsLock.readLock().lock();
		try {
			return new HashMap<>(projectIdsByName);
		} finally {
			projectsLock.readLock().unlock();
		}
	}
	
	@Override
	public Long getGroupIdByName(String name) {
		groupsLock.readLock().lock();
		try {
			return groupIdsByName.get(name);
		} finally {
			groupsLock.readLock().unlock();
		}
	}
	
	@Override
	public GroupFacade getGroup(Long id) {
		groupsLock.readLock().lock();
		try {
			return groups.get(id);
		} finally {
			groupsLock.readLock().unlock();
		}
	}

	@Override
	public MembershipFacade getMembership(Long id) {
		membershipsLock.readLock().lock();
		try {
			return memberships.get(id);
		} finally {
			membershipsLock.readLock().unlock();
		}
	}

	@Override
	public Collection<Long> getIssueNumbers(Long projectId) {
		issuesLock.readLock().lock();
		try {
			Collection<Long> issueNumbers = new HashSet<>();
			for (IssueFacade issue: issues.values()) {
				if (projectId.equals(issue.getProjectId()))
					issueNumbers.add(issue.getNumber());
			}
			return issueNumbers;
		} finally {
			issuesLock.readLock().unlock();
		}
	}

	@Override
	public Collection<Long> getBuildIdsByProject(Long projectId) {
		buildsLock.readLock().lock();
		configurationsLock.readLock().lock();
		try {
			Collection<Long> buildIds = new HashSet<>();
			for (BuildFacade build: builds.values()) {
				ConfigurationFacade configuration = configurations.get(build.getConfigurationId());
				if (configuration != null && configuration.getProjectId().equals(projectId))
					buildIds.add(build.getId());
			}
			return buildIds;
		} finally {
			configurationsLock.readLock().unlock();
			buildsLock.readLock().unlock();
		}
	}

	@Override
	public Collection<Long> getBuildIdsByConfiguration(Long configurationId) {
		buildsLock.readLock().lock();
		try {
			Collection<Long> buildIds = new HashSet<>();
			for (BuildFacade build: builds.values()) {
				if (build.getConfigurationId().equals(configurationId))
					buildIds.add(build.getId());
			}
			return buildIds;
		} finally {
			buildsLock.readLock().unlock();
		}
	}
	
	@Override
	public Collection<Long> filterBuildIds(Long projectId, Collection<String> commitHashes) {
		buildsLock.readLock().lock();
		configurationsLock.readLock().lock();
		try {
			Collection<Long> buildIds = new HashSet<>();
			for (BuildFacade build: builds.values()) {
				ConfigurationFacade configuration = configurations.get(build.getConfigurationId());
				if (configuration != null && configuration.getProjectId().equals(projectId) 
						&& commitHashes.contains(build.getCommitHash())) {
					buildIds.add(build.getId());
				}
			}
			return buildIds;
		} finally {
			configurationsLock.readLock().unlock();
			buildsLock.readLock().unlock();
		}
	}

	@Override
	public Map<Long, ConfigurationFacade> getConfigurations() {
		configurationsLock.readLock().lock();
		try {
			return new HashMap<>(configurations);
		} finally {
			configurationsLock.readLock().unlock();
		}
	}

	@Override
	public Map<Long, UserAuthorizationFacade> getUserAuthorizations() {
		userAuthorizationsLock.readLock().lock();
		try {
			return new HashMap<>(userAuthorizations);
		} finally {
			userAuthorizationsLock.readLock().unlock();
		}
	}

	@Override
	public Map<Long, GroupAuthorizationFacade> getGroupAuthorizations() {
		groupAuthorizationsLock.readLock().lock();
		try {
			return new HashMap<>(groupAuthorizations);
		} finally {
			groupAuthorizationsLock.readLock().unlock();
		}
	}
	
	@Override
	public UserAuthorizationFacade getUserAuthorization(Long id) {
		userAuthorizationsLock.readLock().lock();
		try {
			return userAuthorizations.get(id);
		} finally {
			userAuthorizationsLock.readLock().unlock();
		}
	}

	@Override
	public GroupAuthorizationFacade getGroupAuthorization(Long id) {
		groupAuthorizationsLock.readLock().lock();
		try {
			return groupAuthorizations.get(id);
		} finally {
			groupAuthorizationsLock.readLock().unlock();
		}
	}

}
