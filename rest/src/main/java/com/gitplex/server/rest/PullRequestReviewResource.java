package com.gitplex.server.rest;

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
import javax.ws.rs.core.MediaType;

import org.apache.shiro.authz.UnauthorizedException;

import com.gitplex.server.manager.AccountManager;
import com.gitplex.server.manager.PullRequestReviewManager;
import com.gitplex.server.model.Account;
import com.gitplex.server.model.PullRequestReview;
import com.gitplex.server.security.SecurityUtils;

@Path("/pullrequest_reviews")
@Consumes(MediaType.WILDCARD)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public class PullRequestReviewResource {

	private final PullRequestReviewManager reviewManager;
	
	private final AccountManager accountManager;
	
	@Inject
	public PullRequestReviewResource(PullRequestReviewManager reviewManager, AccountManager accountManager) {
		this.reviewManager = reviewManager;
		this.accountManager = accountManager;
	}
	
    @GET
    @Path("/{id}")
    public PullRequestReview get(@PathParam("id") Long id) {
    	PullRequestReview review = reviewManager.load(id);
    	if (!SecurityUtils.canRead(review.getRequest().getTargetDepot()))
    		throw new UnauthorizedException();
    	return review;
    }
    
    @DELETE
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) {
    	PullRequestReview review = reviewManager.load(id);
    	Account currentUser = accountManager.getCurrent();
    	if (review.getUser().equals(currentUser) || SecurityUtils.canManage(review.getRequest().getTargetDepot())) 
    		reviewManager.delete(review);
    	else
    		throw new UnauthorizedException();
    }

    @POST
    public Long save(@NotNull @Valid PullRequestReview review) {
    	if (!SecurityUtils.canWrite(review.getRequest().getTargetDepot()))
    		throw new UnauthorizedException();
    	
    	review.setUser(accountManager.getCurrent());
    	
    	reviewManager.save(review);
    	
    	return review.getId();
    }
    
}
