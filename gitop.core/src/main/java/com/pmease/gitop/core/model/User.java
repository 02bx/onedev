package com.pmease.gitop.core.model;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

import org.apache.shiro.authz.Permission;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;

import com.google.common.base.Objects;
import com.pmease.commons.editable.annotation.Editable;
import com.pmease.commons.editable.annotation.Password;
import com.pmease.commons.loader.AppLoader;
import com.pmease.commons.shiro.AbstractUser;
import com.pmease.gitop.core.Gitop;
import com.pmease.gitop.core.manager.ProjectManager;
import com.pmease.gitop.core.manager.UserManager;
import com.pmease.gitop.core.permission.ObjectPermission;
import com.pmease.gitop.core.permission.object.ProtectedObject;
import com.pmease.gitop.core.permission.object.UserBelonging;
import com.pmease.gitop.core.permission.operation.GeneralOperation;
import com.pmease.gitop.core.validation.UserName;

@SuppressWarnings("serial")
@Entity
@Editable
public class User extends AbstractUser implements ProtectedObject {

	public static final User ANONYMOUS = new User();
	
	static {
		ANONYMOUS.setId(0L);
		ANONYMOUS.setName("Guest");
	}
	
	@Column(nullable=false)
	private String email;
	
	private String displayName;
	
	private String avatarUrl;
	
	private boolean admin;
	
	@OneToMany(mappedBy="user")
	private Collection<Membership> memberships = new ArrayList<Membership>();
	
	@OneToMany(mappedBy="submitter")
	private Collection<MergeRequest> mergeRequests = new ArrayList<MergeRequest>();
	
	@OneToMany(mappedBy="owner")
	private Collection<Project> repositories = new ArrayList<Project>();

	@OneToMany(mappedBy="owner")
	private Collection<Team> teams = new ArrayList<Team>();
	
	@OneToMany(mappedBy="voter")
	private Collection<Vote> votes = new ArrayList<Vote>();
	
	@OneToMany(mappedBy="voter")
	private Collection<VoteInvitation> voteVitations = new ArrayList<VoteInvitation>();

	@Editable(order=100)
	@UserName
	@Override
	public String getName() {
		return super.getName();
	}

	@Editable(order=200)
	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	@Editable(order=300)
	@NotEmpty
	@Email
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@Editable(name="Password", order=400)
	@Password(confirmative=true)
	@NotEmpty
	@Override
	public String getPasswordHash() {
		return super.getPasswordHash();
	}

	public boolean isAdmin() {
		return admin;
	}

	public void setAdmin(boolean admin) {
		this.admin = admin;
	}

	public String getAvatarUrl() {
		return avatarUrl;
	}

	public void setAvatarUrl(String avatarUrl) {
		this.avatarUrl = avatarUrl;
	}

	public Collection<Membership> getMemberships() {
		return memberships;
	}

	public void setMemberships(Collection<Membership> memberships) {
		this.memberships = memberships;
	}

	public Collection<Project> getRepositories() {
		return repositories;
	}

	public void setRepositories(Collection<Project> repositories) {
		this.repositories = repositories;
	}

	public Collection<Team> getTeams() {
		return teams;
	}

	public void setTeams(Collection<Team> teams) {
		this.teams = teams;
	}

	public Collection<MergeRequest> getMergeRequests() {
		return mergeRequests;
	}

	public void setMergeRequests(Collection<MergeRequest> mergeRequests) {
		this.mergeRequests = mergeRequests;
	}

	public Collection<Vote> getVotes() {
		return votes;
	}

	public void setVotes(Collection<Vote> votes) {
		this.votes = votes;
	}

	public Collection<VoteInvitation> getVoteInvitations() {
		return voteVitations;
	}

	public void setVoteInvitations(Collection<VoteInvitation> voteInvitations) {
		this.voteVitations = voteInvitations;
	}

	public Collection<VoteInvitation> getVoteVitations() {
		return voteVitations;
	}

	public void setVoteVitations(Collection<VoteInvitation> voteVitations) {
		this.voteVitations = voteVitations;
	}

	@Override
	public boolean has(ProtectedObject object) {
		if (object instanceof User) {
			User user = (User) object;
			return user.equals(this);
		} else if (object instanceof UserBelonging) {
			UserBelonging userBelonging = (UserBelonging) object;
			return userBelonging.getUser().equals(this);
		} else {
			return false;
		}
	}
	
	public Vote.Result checkVoteSince(MergeRequestUpdate update) {
		if (update.getRequest().getSubmitter().equals(this))
			return Vote.Result.ACCEPT;
		
		for (Vote vote: update.listVotesOnwards()) {
			if (vote.getVoter().equals(this)) {
				return vote.getResult();
			}
		}
		
		return null;
	}

	public static User getCurrent() {
		Long userId = getCurrentId();
		if (userId != 0L) {
			return AppLoader.getInstance(UserManager.class).load(userId);
		} else {
			return User.ANONYMOUS;
		}
	}
	
	@Override
	public boolean implies(Permission permission) {
		// Administrator can do anything
		if (isRoot() || isAdmin()) 
			return true;
		
		if (permission instanceof ObjectPermission) {
			ObjectPermission objectPermission = (ObjectPermission) permission;
			if (!isAnonymous()) {
				// One can do anything against its belongings
				if (has(objectPermission.getObject()))
					return true;
				
				for (Team team: getTeams()) {
					if (team.implies(objectPermission))
						return true;
				}
	
				for (Project each: Gitop.getInstance(ProjectManager.class).query()) {
					ObjectPermission projectPermission = new ObjectPermission(each, each.getDefaultAuthorizedOperation());
					if (projectPermission.implies(objectPermission))
						return true;
				}
			} 
			
			// check if is public access
			for (Project each: Gitop.getInstance(ProjectManager.class).findPublic()) {
				ObjectPermission projectPermission = new ObjectPermission(each, GeneralOperation.READ);
				if (projectPermission.implies(objectPermission))
					return true;
			}
		} 
		return false;
	}
	
	public boolean isRoot() {
		return Gitop.getInstance(UserManager.class).getRootUser().equals(this);
	}

	public boolean isAnonymous() {
		return getId() == 0L;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("name", getName())
				.toString();
	}
}
