package com.pmease.gitplex.rest.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.hibernate.criterion.Restrictions;

import com.google.common.collect.Lists;
import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.commons.hibernate.dao.EntityCriteria;
import com.pmease.commons.jersey.ValidQueryParams;
import com.pmease.gitplex.core.manager.RepositoryManager;
import com.pmease.gitplex.core.model.Repository;
import com.pmease.gitplex.core.permission.ObjectPermission;

@Path("/repositories")
@Consumes(MediaType.WILDCARD)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public class RepositoryResource {

	private final Dao dao;
	
	private final RepositoryManager repositoryManager;
	
	@Inject
	public RepositoryResource(Dao dao, RepositoryManager repositoryManager) {
		this.dao = dao;
		this.repositoryManager = repositoryManager;
	}
	
	@Path("/{id}")
    @GET
    public Repository get(@PathParam("id") Long id) {
    	Repository repository = dao.load(Repository.class, id);

    	if (!SecurityUtils.getSubject().isPermitted(ObjectPermission.ofRepositoryRead(repository)))
    		throw new UnauthenticatedException();
    	else
    		return repository;
    }
    
	@ValidQueryParams
	@GET
	public Collection<Repository> query(@QueryParam("user") Long userId, @QueryParam("name") String name, 
			@QueryParam("path") String path) {
		EntityCriteria<Repository> criteria = EntityCriteria.of(Repository.class);
		if (path != null) {
			Repository repository = repositoryManager.findBy(path);
			if (repository != null)
				return Lists.newArrayList(repository);
			else
				return new ArrayList<>();
		}
		
		if (userId != null)
			criteria.add(Restrictions.eq("owner.id", userId));
		if (name != null)
			criteria.add(Restrictions.eq("name", name));
		List<Repository> repositories = dao.query(criteria);
		
		for (Repository repository: repositories) {
			if (!SecurityUtils.getSubject().isPermitted(ObjectPermission.ofRepositoryRead(repository))) {
				throw new UnauthorizedException("Unauthorized access to repository " + repository.getFullName());
			}
		}
		return repositories;
	}

	@POST
    public Long save(@NotNull @Valid Repository repository) {
    	if (!SecurityUtils.getSubject().isPermitted(ObjectPermission.ofRepositoryAdmin(repository)))
    		throw new UnauthorizedException();
    	
    	dao.persist(repository);
    	return repository.getId();
    }

    @DELETE
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) {
    	Repository repository = dao.load(Repository.class, id);

    	if (!SecurityUtils.getSubject().isPermitted(ObjectPermission.ofRepositoryAdmin(repository)))
    		throw new UnauthorizedException();
    	
    	dao.remove(repository);
    }
    
}
