package io.onedev.server.manager;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import io.onedev.server.entityquery.EntityCriteria;
import io.onedev.server.entityquery.EntityQuery;
import io.onedev.server.model.Project;
import io.onedev.server.model.PullRequest;
import io.onedev.server.model.User;
import io.onedev.server.model.support.ProjectAndBranch;
import io.onedev.server.model.support.pullrequest.MergePreview;
import io.onedev.server.model.support.pullrequest.MergeStrategy;
import io.onedev.server.persistence.dao.EntityManager;

public interface PullRequestManager extends EntityManager<PullRequest> {
    
    @Nullable 
    PullRequest findEffective(ProjectAndBranch target, ProjectAndBranch source);
    
    @Nullable 
    PullRequest findOpen(ProjectAndBranch target, ProjectAndBranch source);
    
    Collection<PullRequest> findAllOpenTo(ProjectAndBranch target);

    Collection<PullRequest> findAllOpen(ProjectAndBranch sourceOrTarget);
    
    @Nullable
    PullRequest find(Project targetProject, long number);
    
	@Nullable
	PullRequest find(String uuid);
	
	@Nullable
	PullRequest findLatest(Project targetProject, User submitter);
	
	Collection<PullRequest> findOpenByCommit(String commitHash);
    
    void discard(PullRequest request, @Nullable String note);
    
    void reopen(PullRequest request, @Nullable String note);

    void check(PullRequest request);
    
	/**
     * Preview merge of this pull request.
     * 
     * @param request
     * 			pull request to preview merge of
     * @return
     * 			merge preview, or <tt>null</tt> if preview is being calculating
     */
    MergePreview previewMerge(PullRequest request);

    /**
     * Delete git refs of this pull request and all its updates.
     * 
     * @param request
     *			pull request whose git refs and update refs to be deleted 	
     */
    void deleteRefs(PullRequest request);
    
    void open(PullRequest request);
    
    PullRequest open(ProjectAndBranch source, ProjectAndBranch target, MergeStrategy mergeStrategy, User submitter, String title);
    
    void delete(PullRequest request);
    
	void deleteSourceBranch(PullRequest request, @Nullable String note);
	
	void restoreSourceBranch(PullRequest request, @Nullable String note);
	
	int countOpen(Project targetProject);
	
	void checkQuality(PullRequest request);
	
	List<PullRequest> query(Project targetProject, EntityQuery<PullRequest> requestQuery, int firstResult, int maxResults);
	
	int count(Project targetProject, EntityCriteria<PullRequest> requestCriteria);
	
	List<PullRequest> query(Project targetProject, String term, int count);
	
}
