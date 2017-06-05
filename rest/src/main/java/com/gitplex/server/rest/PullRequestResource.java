package com.gitplex.server.rest;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.shiro.authz.UnauthorizedException;
import org.hibernate.criterion.Restrictions;

import com.gitplex.server.model.PullRequest;
import com.gitplex.server.persistence.dao.Dao;
import com.gitplex.server.persistence.dao.EntityCriteria;
import com.gitplex.server.rest.jersey.ValidQueryParams;
import com.gitplex.server.security.SecurityUtils;

@Path("/pullrequests")
@Consumes(MediaType.WILDCARD)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public class PullRequestResource {

	private static final String OPEN = "open";
	
	private static final String CLOSED = "closed";
	
	private final Dao dao;
	
	@Inject
	public PullRequestResource(Dao dao) {
		this.dao = dao;
	}

    @GET
    @Path("/{id}")
    public PullRequest get(@PathParam("id") Long id) {
    	PullRequest request = dao.load(PullRequest.class, id);
    	
    	if (!SecurityUtils.canRead(request.getTargetProject()))
    		throw new UnauthorizedException();
    	
    	return request;
    }
        
    @ValidQueryParams
    @GET
    public Collection<PullRequest> query(
    		@QueryParam("targetProjectId") Long targetProjectId, @QueryParam("targetBranch") String targetBranch,
    		@QueryParam("sourceProjectId") Long sourceProjectId, @QueryParam("sourceBranch") String sourceBranch, 
    		@QueryParam("submitterId") Long submitterId, @QueryParam("status") String status, 
    		@QueryParam("beginDate") Date beginDate, @QueryParam("endDate") Date endDate) {
    	
    	EntityCriteria<PullRequest> criteria = EntityCriteria.of(PullRequest.class);

    	if (targetProjectId != null)
    		criteria.add(Restrictions.eq("targetProject.id", targetProjectId));
    	if (targetBranch != null)
    		criteria.add(Restrictions.eq("targetBranch", targetBranch));
    		
    	if (sourceProjectId != null)
    		criteria.add(Restrictions.eq("sourceProject.id", targetProjectId));
    	if (sourceBranch != null)
    		criteria.add(Restrictions.eq("sourceBranch", sourceBranch));
		
		if (OPEN.equalsIgnoreCase(status)) 
			criteria.add(PullRequest.CriterionHelper.ofOpen());
		else if (CLOSED.equalsIgnoreCase(status)) 
			criteria.add(PullRequest.CriterionHelper.ofClosed());
		
		if (submitterId != null)
			criteria.add(Restrictions.eq("submitter.id", submitterId));
		if (beginDate != null)
			criteria.add(Restrictions.ge("submitDate", beginDate));
		if (endDate != null)
			criteria.add(Restrictions.le("submitDate", endDate));

		List<PullRequest> requests = dao.findAll(criteria);
		
		for (PullRequest request: requests) {
	    	if (!SecurityUtils.canRead(request.getTarget().getProject()))
	    		throw new UnauthorizedException("Unauthorized access to pull request " + request.getTarget() + "/" + request.getId());
		}
		return requests;
    }
    
}