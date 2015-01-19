package com.pmease.gitplex.core.manager;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.annotation.Nullable;

import com.pmease.commons.bootstrap.Lifecycle;
import com.pmease.gitplex.core.extensionpoint.ConfigListener;
import com.pmease.gitplex.core.model.Branch;
import com.pmease.gitplex.core.model.IntegrationPreview;
import com.pmease.gitplex.core.model.PullRequest;
import com.pmease.gitplex.core.model.PullRequest.IntegrationStrategy;
import com.pmease.gitplex.core.model.Repository;

public interface PullRequestManager extends Lifecycle, ConfigListener {
    
    @Nullable PullRequest findOpen(Branch target, Branch source);
    
    Collection<PullRequest> findOpenTo(Branch target, Repository source);

    Collection<PullRequest> findOpenFrom(Branch source, Repository target);
   
    Collection<PullRequest> findOpen(Branch sourceOrTarget);

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

    void onTargetBranchUpdate(PullRequest request);
    
    void onSourceBranchUpdate(PullRequest request, boolean notify);
    
    void onAssigneeChange(PullRequest request);
    
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
    
    void open(PullRequest request, @Nullable Object listenerData);
    
    void delete(PullRequest request);
    
	List<IntegrationStrategy> getApplicableIntegrationStrategies(PullRequest request);

	void restoreSource(PullRequest request);
	
	/**
	 * Get last visit date of specified pull request for current user.
	 * 
	 * @return
	 * 			last visit date of specified pull request for current user, or <tt>null</tt>
	 * 			if current user is anonymous or has never visited the pull request
	 */
	@Nullable
	Date getLastVisitDate(PullRequest request);
}
