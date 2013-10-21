package com.pmease.gitop.core.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.Valid;

import com.pmease.commons.editable.annotation.Editable;
import com.pmease.commons.hibernate.AbstractEntity;
import com.pmease.gitop.core.Gitop;
import com.pmease.gitop.core.gatekeeper.GateKeeper;
import com.pmease.gitop.core.manager.UserManager;
import com.pmease.gitop.core.permission.ObjectPermission;
import com.pmease.gitop.core.permission.object.ProtectedObject;
import com.pmease.gitop.core.permission.object.UserBelonging;
import com.pmease.gitop.core.permission.operation.GeneralOperation;
import com.pmease.gitop.core.validation.ProjectName;

@Entity
@Table(uniqueConstraints={
		@UniqueConstraint(columnNames={"owner", "name"})
})
@SuppressWarnings("serial")
@Editable
public class Project extends AbstractEntity implements UserBelonging {
	
	@ManyToOne
	@JoinColumn(nullable=false)
	private User owner;
	
	@ManyToOne
	@JoinColumn(nullable=true)
	private Project forkedFrom;

	@Column(nullable=false)
	private String name;
	
	private String description;

	private boolean publiclyAccessible;
	
	@Column(nullable=false)
	private GeneralOperation defaultAuthorizedOperation = GeneralOperation.NO_ACCESS;
	
	private GateKeeper gateKeeper;
	
	@Column(nullable=false)
	private Date createdAt = new Date();

	@OneToMany(mappedBy="project", cascade=CascadeType.REMOVE)
	private Collection<Authorization> authorizations = new ArrayList<Authorization>();

	@OneToMany(mappedBy="forkedFrom", cascade=CascadeType.REMOVE)
	private Collection<Project> forks = new ArrayList<Project>();

	public User getOwner() {
		return owner;
	}

	public void setOwner(User owner) {
		this.owner = owner;
	}

	@Editable(description=
			"Specify name of the project. It will be used to identify the project when accessing via Git.")
	@ProjectName
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Editable(description="Specify description of the project.")
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Editable(name="Public", description=
			"If a project is made public, it will be able to be browsed/pulled by anonymous users.")
	public boolean isPubliclyAccessible() {
		return publiclyAccessible;
	}

	public void setPubliclyAccessible(boolean publiclyAccessible) {
		this.publiclyAccessible = publiclyAccessible;
	}

	public GeneralOperation getDefaultAuthorizedOperation() {
		return defaultAuthorizedOperation;
	}

	public void setDefaultAuthorizedOperation(
			GeneralOperation defaultAuthorizedOperation) {
		this.defaultAuthorizedOperation = defaultAuthorizedOperation;
	}

	@Editable(
			name="Accept Merge Requests If", 
			description="Optionally define gate keeper to accept merge requests under certain condition.")
	@Valid
	public GateKeeper getGateKeeper() {
		return gateKeeper;
	}

	public void setGateKeeper(GateKeeper gateKeeper) {
		this.gateKeeper = gateKeeper;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	@Override
	public User getUser() {
		return getOwner();
	}

	public Collection<Authorization> getAuthorizations() {
		return authorizations;
	}

	public void setAuthorizations(Collection<Authorization> authorizations) {
		this.authorizations = authorizations;
	}

	public Project getForkedFrom() {
		return forkedFrom;
	}

	public void setForkedFrom(Project forkedFrom) {
		this.forkedFrom = forkedFrom;
	}

	public Collection<Project> getForks() {
		return forks;
	}

	public void setForks(Collection<Project> forks) {
		this.forks = forks;
	}

	@Override
	public boolean has(ProtectedObject object) {
		if (object instanceof Project) {
			Project project = (Project) object;
			return project.getId().equals(getId());
		} else {
			return false;
		}
	}

	public Collection<User> findAuthorizedUsers(GeneralOperation operation) {
		Set<User> authorizedUsers = new HashSet<User>();
		for (User user: Gitop.getInstance(UserManager.class).query()) {
			if (user.asSubject().isPermitted(new ObjectPermission(this, operation)))
				authorizedUsers.add(user);
		}
		return authorizedUsers;
	}
	
	@Override
	public String toString() {
		return getOwner() + "/" + getName();
	}
}
