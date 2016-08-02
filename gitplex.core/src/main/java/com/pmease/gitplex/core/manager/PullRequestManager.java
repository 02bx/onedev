package com.pmease.gitplex.core.manager;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import com.pmease.commons.hibernate.dao.EntityManager;
import com.pmease.gitplex.core.entity.Depot;
import com.pmease.gitplex.core.entity.PullRequest;
import com.pmease.gitplex.core.entity.PullRequest.IntegrationStrategy;
import com.pmease.gitplex.core.entity.support.DepotAndBranch;
import com.pmease.gitplex.core.entity.support.IntegrationPreview;

public interface PullRequestManager extends EntityManager<PullRequest> {
    
    @Nullable 
    PullRequest findOpen(DepotAndBranch target, DepotAndBranch source);
    
    Collection<PullRequest> findAllOpenTo(DepotAndBranch target, @Nullable Depot sourceDepot);

    Collection<PullRequest> findAllOpenFrom(DepotAndBranch source, @Nullable Depot targetDepot);
   
    Collection<PullRequest> findAllOpen(DepotAndBranch sourceOrTarget);
    
    @Nullable
    PullRequest find(Depot target, long number);
    
	@Nullable
	PullRequest find(String uuid);
    
    boolean canIntegrate(PullRequest request);
    
    /**
     * Integrate specified pull request.
     * 
     * @param request
     * 			pull request to be integrated
     * @param comment
     * 			comment for the integration
     */
    void integrate(PullRequest request, @Nullable String comment);
    
    void discard(PullRequest request, @Nullable String comment);
    
    void reopen(PullRequest request, @Nullable String comment);

    void changeAssignee(PullRequest request);
    
    void check(PullRequest request);
    
	/**
     * Preview integration of this pull request.
     * 
     * @param request
     * 			pull request to preview integration of
     * @return
     * 			integration preview, or <tt>null</tt> if preview is being calculating
     */
    IntegrationPreview previewIntegration(PullRequest request);

    /**
     * Delete git refs of this pull request and all its updates.
     * 
     * @param request
     *			pull request whose git refs and update refs to be deleted 	
     */
    void deleteRefs(PullRequest request);
    
    void open(PullRequest request);
    
    void delete(PullRequest request);
    
	List<IntegrationStrategy> getApplicableIntegrationStrategies(PullRequest request);

	void deleteSourceBranch(PullRequest request);
	
	void restoreSourceBranch(PullRequest request);
	
	void checkSanity();
	
	int countOpen(Depot depot);
}
