package com.pmease.gitplex.core.gatekeeper;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.pmease.commons.git.AbstractGitTest;
import com.pmease.commons.git.Git;
import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.commons.loader.AppLoader;
import com.pmease.commons.util.FileUtils;
import com.pmease.gitplex.core.model.Branch;
import com.pmease.gitplex.core.model.Membership;
import com.pmease.gitplex.core.model.PullRequest;
import com.pmease.gitplex.core.model.PullRequestUpdate;
import com.pmease.gitplex.core.model.Repository;
import com.pmease.gitplex.core.model.Team;
import com.pmease.gitplex.core.model.User;
import com.pmease.gitplex.core.model.Vote;

public class IfApprovedByMajoritiesOfSpecifiedTeamTest extends AbstractGitTest {

	@Mock
	private Dao dao;
	
	@Override
	protected void setup() {
		super.setup();
		
		Mockito.when(AppLoader.getInstance(Dao.class)).thenReturn(dao);
		Branch branch1 = new Branch();
		branch1.setId(1L);
		branch1.setName("branch1");
		Mockito.when(dao.load(Branch.class, 1L)).thenReturn(branch1);
		
		Branch branch2 = new Branch();
		branch2.setId(2L);
		branch2.setName("branch2");
		Mockito.when(dao.load(Branch.class, 2L)).thenReturn(branch2);
		
		Team team = new Team();
		team.setName("team");
		team.setId(1L);
		Membership membership = new Membership();
		membership.setTeam(team);
		membership.setUser(new User());
		membership.getUser().setId(1L);
		membership.getUser().setName("user1");
		team.getMemberships().add(membership);
		membership = new Membership();
		membership.setTeam(team);
		membership.setUser(new User());
		membership.getUser().setId(2L);
		membership.getUser().setName("user2");
		team.getMemberships().add(membership);
		membership = new Membership();
		membership.setTeam(team);
		membership.setUser(new User());
		membership.getUser().setId(3L);
		membership.getUser().setName("user3");
		team.getMemberships().add(membership);
		
		Mockito.when(dao.load(Team.class, 1L)).thenReturn(team);
	}

	@Test
	public void testCheckFile() {
		IfApprovedByMajoritiesOfSpecifiedTeam gateKeeper = new IfApprovedByMajoritiesOfSpecifiedTeam();
		gateKeeper.setTeamId(1L);
		
		User user1 = new User();
		user1.setId(1L);
		Branch branch1 = new Branch();
		branch1.setId(1L);
		Assert.assertTrue(gateKeeper.checkFile(user1, branch1, "src/file").isPending());
	}

	@SuppressWarnings("serial")
	@Test
	public void testCheckRequest() {
		final Git git = new Git(FileUtils.createTempDir());
		try {
			git.init(false);

			FileUtils.touchFile(new File(git.repoDir(), "file1"));
			git.add("file1");
			git.commit("add file1", false, false);
			
			git.checkout("master", "dev");
			FileUtils.touchFile(new File(git.repoDir(), "file2"));
			git.add("file2");
			git.commit("add file2", false, false);
			
			IfApprovedByMajoritiesOfSpecifiedTeam gateKeeper = new IfApprovedByMajoritiesOfSpecifiedTeam();
			gateKeeper.setTeamId(1L);

			PullRequest request = new PullRequest();
			request.setId(1L);
			request.setTarget(new Branch());
			request.getTarget().setName("master");
			request.getTarget().setHeadCommit(git.parseRevision("master", true));
			request.getTarget().setRepository(new Repository() {

				@Override
				public Git git() {
					return git;
				}
				
			});
			request.getTarget().getRepository().setId(1L);
			request.getTarget().getRepository().setOwner(new User());
			request.getTarget().getRepository().getOwner().setId(1L);
			
			request.setSource(new Branch());
			request.getSource().setName("dev");
			request.getSource().setHeadCommit(git.parseRevision("dev", true));
			request.getSource().setRepository(new Repository() {

				@Override
				public Git git() {
					return git;
				}
				
			});
			
			request.setSubmitter(new User());
			request.getSubmitter().setId(2L);
			request.getSubmitter().setName("user2");
			
			PullRequestUpdate update0 = new PullRequestUpdate();
			update0.setHeadCommit(git.calcMergeBase("dev", "master"));
			update0.setId(1L);
			update0.setRequest(request);
			request.getUpdates().add(update0);

			PullRequestUpdate update1 = new PullRequestUpdate();
			update1.setHeadCommit(git.parseRevision("dev", true));
			update1.setId(2L);
			update1.setRequest(request);
			request.getUpdates().add(update1);
			
			Assert.assertTrue(gateKeeper.checkRequest(request).isPending());
			Collection<User> candidates = new HashSet<>();
			User user = new User();
			user.setId(1L);
			user.setName("user1");
			candidates.add(user);
			user = new User();
			user.setId(2L);
			user.setName("user2");
			candidates.add(user);
			user = new User();
			user.setId(3L);
			user.setName("user3");
			candidates.add(user);
			Assert.assertEquals("user1", request.getVoteInvitations().iterator().next().getVoter().getName());
			
			Vote vote = new Vote();
			vote.setId(1L);
			vote.setResult(Vote.Result.APPROVE);
			vote.setUpdate(update1);
			vote.setVoter(new User());
			vote.getVoter().setId(1L);
			vote.getVoter().setName("user1");
			update1.getVotes().add(vote);
			
			Assert.assertTrue(gateKeeper.checkRequest(request).isApproved());
		} finally {
			FileUtils.deleteDir(git.repoDir());
		}
	}

	@Override
	protected void teardown() {
		super.teardown();
	}

}
