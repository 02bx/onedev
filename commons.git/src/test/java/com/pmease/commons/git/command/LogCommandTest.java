package com.pmease.commons.git.command;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

import org.junit.Test;

import com.pmease.commons.git.Commit;
import com.pmease.commons.git.FileChange;
import com.pmease.commons.git.Git;
import com.pmease.commons.util.FileUtils;

public class LogCommandTest {

	@Test
	public void shouldParseLogCorrectly() {
	    assertTrue(GitCommand.checkError() == null);
	    File tempDir = FileUtils.createTempDir();
	    
	    try {
		    Git workGit = new Git(new File(tempDir, "work"));
		    workGit.init(false);
		        
    		FileUtils.touchFile(new File(workGit.repoDir(), "a"));
    		workGit.add("a");
    		workGit.commit("add a", false);
    		
    		FileUtils.touchFile(new File(workGit.repoDir(), "b"));
    		workGit.add("b");
    		workGit.commit("add b", false);
    		
    		FileUtils.touchFile(new File(workGit.repoDir(), "c"));
    		workGit.add("c");
    		workGit.commit("add c", false);
    		
    		FileUtils.touchFile(new File(workGit.repoDir(), "d"));
    		workGit.add("d");
    		workGit.commit("add d", false);
    		
    		FileUtils.writeFile(new File(workGit.repoDir(), "a"), "a");
    		workGit.add("a");
    		workGit.commit("modify a", false);
    		
    		FileUtils.createDir(new File(workGit.repoDir(), "dir"));
    		FileUtils.writeFile(new File(workGit.repoDir(), "dir/file"), "hello world");
    		FileUtils.writeFile(new File(workGit.repoDir(), "dir/file2"), "hello world");
    		workGit.add("dir/file");
    		workGit.add("dir/file2");
    		workGit.commit("add dir/file\nadd dir/file to test files under a directory", false);
    		
    		workGit.checkout("dev", true);
    		workGit.remove("dir/file");
    		workGit.commit("remove dir/file", false);
    		
    		Git bareGit = new Git(new File(tempDir, "bare"));
    		bareGit.clone(workGit.repoDir().getAbsolutePath(), true);

    		bareGit.addNote("master", "hello\nworld");

    		List<Commit> commits = bareGit.log(null, "master", null, 0);
    		assertEquals(commits.size(), 6);
    		assertEquals(commits.get(0).getSummary(), "add dir/file");
    		assertEquals(commits.get(0).getMessage(), "add dir/file to test files under a directory");
    		assertEquals("hello\nworld", commits.get(0).getNote());
    		assertEquals(commits.get(0).getFileChanges().size(), 2);
    		assertEquals(commits.get(0).getFileChanges().get(0).getPath(), "dir/file");
    		assertEquals(commits.get(0).getFileChanges().get(0).getAction(), FileChange.Action.ADD);
    		assertEquals(commits.get(0).getParentHashes().size(), 1);
    		assertEquals(commits.get(0).getParentHashes().iterator().next(), commits.get(1).getHash());
    		
    		assertEquals(null, commits.get(1).getNote());
    		
    		workGit.checkout("master", false).remove("a").commit("remove a", false);
    		FileUtils.writeFile(new File(workGit.repoDir(), "dir/file2"), "file2");
    		workGit.add("dir/file2").commit("add dir/file2", false);
    		workGit.merge("dev");
    		
    		commits = workGit.log(null, "master", null, 0);
    		
    		assertEquals(commits.get(0).getParentHashes().size(), 2);
    		
    		commits = workGit.log(null, "master", "a", 0);
    		assertEquals(commits.size(), 3);
    		
    		commits = workGit.log("dev", "master", "dir", 0);
    		assertEquals(commits.size(), 2);

    		assertEquals(workGit.resolveCommit(commits.get(0).getHash()).getHash(), commits.get(0).getHash()); 
    		assertEquals(workGit.resolveCommit(commits.get(1).getHash()).getHash(), commits.get(1).getHash()); 
	    } finally {
	        FileUtils.deleteDir(tempDir);
	    }
	}

}
