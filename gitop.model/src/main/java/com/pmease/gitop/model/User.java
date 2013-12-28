package com.pmease.gitop.model;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;

import com.pmease.commons.editable.annotation.Editable;
import com.pmease.commons.editable.annotation.Password;
import com.pmease.commons.shiro.AbstractUser;
import com.pmease.gitop.model.permission.object.ProtectedObject;
import com.pmease.gitop.model.permission.object.UserBelonging;
import com.pmease.gitop.model.validation.UserName;

@SuppressWarnings("serial")
@Entity
@Editable
public class User extends AbstractUser implements ProtectedObject {

	@Column(nullable=false)
	private String email;
	
	private String displayName;
	
	private String avatarUrl;
	
	private boolean admin;
	
	@OneToMany(mappedBy="user", cascade=CascadeType.REMOVE)
	private Collection<Membership> memberships = new ArrayList<Membership>();
	
	@OneToMany(mappedBy="owner", cascade=CascadeType.REMOVE)
	private Collection<Project> projects = new ArrayList<Project>();

	@OneToMany(mappedBy="owner", cascade=CascadeType.REMOVE)
	private Collection<Team> teams = new ArrayList<Team>();
	
	@OneToMany(mappedBy="voter", cascade=CascadeType.REMOVE)
	private Collection<Vote> votes = new ArrayList<Vote>();
	
	@OneToMany(mappedBy="voter", cascade=CascadeType.REMOVE)
	private Collection<VoteInvitation> voteInvitations = new ArrayList<VoteInvitation>();

	@Editable(order=100)
	@UserName
	@NotEmpty
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

	@Editable(order=400)
	@Password(confirmative=true)
	@NotEmpty
	public String getPassword() {
		return getPasswordHash();
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

	public Collection<Project> getProjects() {
		return projects;
	}

	public void setProjects(Collection<Project> repositories) {
		this.projects = repositories;
	}

	public Collection<Team> getTeams() {
		return teams;
	}

	public void setTeams(Collection<Team> teams) {
		this.teams = teams;
	}

	public Collection<Vote> getVotes() {
		return votes;
	}

	public void setVotes(Collection<Vote> votes) {
		this.votes = votes;
	}

	public Collection<VoteInvitation> getVoteInvitations() {
		return voteInvitations;
	}

	public void setVoteInvitations(Collection<VoteInvitation> voteInvitations) {
		this.voteInvitations = voteInvitations;
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
	
	public Vote.Result checkVoteSince(PullRequestUpdate update) {
		if (update.getRequest().getSubmitter().equals(this))
			return Vote.Result.ACCEPT;
		
		for (Vote vote: update.listVotesOnwards()) {
			if (vote.getVoter().equals(this)) {
				return vote.getResult();
			}
		}
		
		return null;
	}
	
	@Override
	public String toString() {
		return getName();
	}
}
