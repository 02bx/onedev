package com.pmease.gitplex.core.model;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.pmease.commons.git.AbstractGitTest;
import com.pmease.commons.git.Git;
import com.pmease.commons.util.FileUtils;

public class PullRequestTest extends AbstractGitTest {

    private File repoDir;
    
    private Git git;
    
    private Repository repository;

    @Override
    public void setup() {
    	super.setup();
    	
        repoDir = FileUtils.createTempDir();
        
        git = new Git(new File(repoDir, "code"));
        git.init(false);
        
        repository = Mockito.mock(Repository.class);
        Mockito.when(repository.git()).thenReturn(git);
    }

    @Override
    public void teardown() {
        FileUtils.deleteDir(repoDir);
        super.teardown();
    }

    @Test
    public void shouldReturnAllUpdatesAsEffectiveIfTheyAreFastForward() {
        PullRequest request = new PullRequest();
        Branch target = new Branch();
        target.setName("master");
        target.setRepository(repository);
        request.setTarget(target);

        FileUtils.touchFile(new File(git.repoDir(), "a"));
        git.add("a");
        git.commit("commit", false, false);
        
        git.checkout("head", "dev");
        
        FileUtils.touchFile(new File(git.repoDir(), "b"));
        git.add("b");
        git.commit("commit", false, false);

        target.setHeadCommit(git.parseRevision("master", true));
        
        PullRequestUpdate update0 = new PullRequestUpdate();
        update0.setId(1L);
        update0.setRequest(request);
        git.updateRef(update0.getHeadRef(), "master", null, null);
        update0.setHeadCommit(git.parseRevision(update0.getHeadRef(), true));
        request.getUpdates().add(update0);

        PullRequestUpdate update1 = new PullRequestUpdate();
        update1.setId(2L);
        update1.setRequest(request);
        git.updateRef(update1.getHeadRef(), "HEAD", null, null);
        update1.setHeadCommit(git.parseRevision(update1.getHeadRef(), true));
        request.getUpdates().add(update1);

        FileUtils.touchFile(new File(git.repoDir(), "c"));
        git.add("c");
        git.commit("commit", false, false);
        
        PullRequestUpdate update2 = new PullRequestUpdate();
        update2.setId(3L);
        update2.setRequest(request);
        git.updateRef(update2.getHeadRef(), "HEAD", null, null);
        update2.setHeadCommit(git.parseRevision(update2.getHeadRef(), true));
        request.getUpdates().add(update2);
        
        Assert.assertEquals(request.getEffectiveUpdates().size(), 2);
    }

    @Test
    public void shouldReturnLatestUpdateAsEffectiveIfAllOthersHaveBeenMerged() {
        PullRequest request = new PullRequest();
        Branch target = new Branch();
        target.setName("master");
        target.setRepository(repository);
        request.setTarget(target);

        FileUtils.touchFile(new File(git.repoDir(), "a"));
        git.add("a");
        git.commit("master:1", false, false);
        
        git.checkout("head", "dev");
        
        FileUtils.touchFile(new File(git.repoDir(), "b"));
        git.add("b");
        git.commit("dev:2", false, false);
        
        PullRequestUpdate update0 = new PullRequestUpdate();
        update0.setId(1L);
        update0.setRequest(request);
        git.updateRef(update0.getHeadRef(), "master", null, null);
        update0.setHeadCommit(git.parseRevision(update0.getHeadRef(), true));
        request.getUpdates().add(update0);
        
        FileUtils.touchFile(new File(git.repoDir(), "c"));
        git.add("c");
        git.commit("dev:3", false, false);
        
        PullRequestUpdate update1 = new PullRequestUpdate();
        update1.setId(2L);
        update1.setRequest(request);
        git.updateRef(update1.getHeadRef(), "HEAD", null, null);
        update1.setHeadCommit(git.parseRevision(update1.getHeadRef(), true));
        String secondRef = update1.getHeadRef();
        request.getUpdates().add(update1);

        FileUtils.touchFile(new File(git.repoDir(), "d"));
        git.add("d");
        git.commit("dev:4", false, false);
        
        PullRequestUpdate update2 = new PullRequestUpdate();
        update2.setId(3L);
        update2.setRequest(request);
        git.updateRef(update2.getHeadRef(), "HEAD", null, null);
        update2.setHeadCommit(git.parseRevision(update2.getHeadRef(), true));
        request.getUpdates().add(update2);
        
        git.checkout("master", null);
        
        FileUtils.touchFile(new File(git.repoDir(), "e"));
        git.add("e");
        git.commit("master:5", false, false);
        
        git.merge(secondRef, null, null, null, null);

        target.setHeadCommit(git.parseRevision("master", true));

        Assert.assertEquals(1, request.getEffectiveUpdates().size());
        Assert.assertEquals(3L, request.getEffectiveUpdates().get(0).getId().longValue());
    }

}