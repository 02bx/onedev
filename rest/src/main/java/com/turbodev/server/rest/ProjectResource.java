package com.turbodev.server.rest;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.shiro.authz.UnauthorizedException;
import org.hibernate.criterion.Restrictions;

import com.turbodev.server.manager.ProjectManager;
import com.turbodev.server.model.Project;
import com.turbodev.server.persistence.dao.EntityCriteria;
import com.turbodev.server.rest.jersey.ValidQueryParams;
import com.turbodev.server.security.SecurityUtils;

@Path("/projects")
@Consumes(MediaType.WILDCARD)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public class ProjectResource {

	private final ProjectManager projectManager;
	
	@Inject
	public ProjectResource(ProjectManager projectManager) {
		this.projectManager = projectManager;
	}
	
	@ValidQueryParams
	@GET
    public Response query(@QueryParam("name") String projectName, @QueryParam("per_page") Integer perPage, 
    		@QueryParam("page") Integer page, @Context UriInfo uriInfo) {
		EntityCriteria<Project> criteria = projectManager.newCriteria();
		if (projectName != null)
			criteria.add(Restrictions.eq("name", projectName));
		
    	if (page == null)
    		page = 1;
    	
    	if (perPage == null || perPage > RestConstants.PAGE_SIZE) 
    		perPage = RestConstants.PAGE_SIZE;

    	int totalCount = projectManager.count(criteria);

    	Collection<Project> projects = projectManager.findRange(criteria, (page-1)*perPage, perPage);
		for (Project project: projects) {
			if (!SecurityUtils.canRead(project))
				throw new UnauthorizedException("Unable to access project '" + project.getName() + "'");
		}
		
		return Response
				.ok(projects, RestConstants.JSON_UTF8)
				.links(PageUtils.getNavLinks(uriInfo, totalCount, perPage, page))
				.build();
		
    }
    
	@Path("/{projectId}")
    @GET
    public Project get(@PathParam("projectId") Long projectId) {
    	Project project = projectManager.load(projectId);
    	if (!SecurityUtils.canRead(project))
			throw new UnauthorizedException("Unauthorized access to project " + project.getName());
    	else
    		return project;
    }
	
}
