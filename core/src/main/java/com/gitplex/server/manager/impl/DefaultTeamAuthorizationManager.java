package com.gitplex.server.manager.impl;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;

import com.google.common.base.Preconditions;
import com.gitplex.launcher.loader.Listen;
import com.gitplex.server.entity.Account;
import com.gitplex.server.entity.TeamAuthorization;
import com.gitplex.server.event.depot.DepotTransferred;
import com.gitplex.server.manager.TeamAuthorizationManager;
import com.gitplex.server.persistence.annotation.Sessional;
import com.gitplex.server.persistence.annotation.Transactional;
import com.gitplex.server.persistence.dao.AbstractEntityManager;
import com.gitplex.server.persistence.dao.Dao;
import com.gitplex.server.persistence.dao.EntityCriteria;
import com.gitplex.server.security.privilege.DepotPrivilege;

@Singleton
public class DefaultTeamAuthorizationManager extends AbstractEntityManager<TeamAuthorization> 
		implements TeamAuthorizationManager {

	@Inject
	public DefaultTeamAuthorizationManager(Dao dao) {
		super(dao);
	}

	@Transactional
	@Override
	public void save(TeamAuthorization entity) {
		DepotPrivilege privilege = entity.getPrivilege();
		
		/*
		 * Admin privilege is not allowed for team to make permission management consistent. 
		 * That is: the person able to administer a depot can also administer the whole 
		 * account, so that they can edit authorizations either from depot side, or from 
		 * team side 
		 */
		Preconditions.checkArgument(privilege == DepotPrivilege.READ || privilege == DepotPrivilege.WRITE);
		dao.persist(entity);
	}

	@Transactional
	@Listen
	public void on(DepotTransferred event) {
		Query query = getSession().createQuery("delete from TeamAuthorization where depot=:depot");
		query.setParameter("depot", event.getDepot());
		query.executeUpdate();
	}

	@Transactional
	@Override
	public void delete(Collection<TeamAuthorization> authorizations) {
		for (TeamAuthorization authorization: authorizations)
			dao.remove(authorization);
	}

	@Sessional
	@Override
	public Collection<TeamAuthorization> findAll(Account organization) {
		EntityCriteria<TeamAuthorization> criteria = newCriteria(); 
		criteria.createCriteria("depot").add(Restrictions.eq("account", organization));
		return findAll(criteria);
	}

}
