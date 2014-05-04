package com.pmease.gitop.rest.resource;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.pmease.gitop.core.manager.BranchManager;
import com.pmease.gitop.model.Branch;

@Path("/branches")
@Produces(MediaType.APPLICATION_JSON)
public class BranchResource {

	private final BranchManager branchManager;
	
	@Inject
	public BranchResource(BranchManager branchManager) {
		this.branchManager = branchManager;
	}
	
    @GET
    @Path("/{branchId}")
    public Branch get(@PathParam("branchId") Long branchId) {
    	return branchManager.load(branchId);
    }
    
    @POST
    public Long save(@Valid Branch branch) {
    	branchManager.save(branch);
    	return branch.getId();
    }
    
}
