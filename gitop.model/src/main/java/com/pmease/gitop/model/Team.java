package com.pmease.gitop.model;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.validator.constraints.NotEmpty;

import com.google.common.base.Objects;
import com.pmease.commons.hibernate.AbstractEntity;
import com.pmease.gitop.model.permission.operation.GeneralOperation;
import com.pmease.gitop.model.validation.TeamName;

@Entity
@Table(uniqueConstraints={
		@UniqueConstraint(columnNames={"owner", "name"})
})
@SuppressWarnings("serial")
public class Team extends AbstractEntity {

	public static final String ANONYMOUS = "Anonymous";
	public static final String OWNERS = "Owners";
	public static final String LOGGEDIN = "Logged-In";
	
	@ManyToOne
	@JoinColumn(nullable=false)
	private User owner;

	@Column(nullable=false)
	private String name;
	
	@Column(nullable=false)
	private GeneralOperation authorizedOperation = GeneralOperation.NO_ACCESS;
	
	@OneToMany(mappedBy="team", cascade=CascadeType.REMOVE)
	private Collection<Membership> memberships = new ArrayList<Membership>();
	
	@OneToMany(mappedBy="team", cascade=CascadeType.REMOVE)
	private Collection<Authorization> authorizations = new ArrayList<Authorization>();

	public User getOwner() {
		return owner;
	}

	public void setOwner(User owner) {
		this.owner = owner;
	}

	@TeamName
	@NotEmpty
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public GeneralOperation getAuthorizedOperation() {
		return authorizedOperation;
	}

	public void setAuthorizedOperation(GeneralOperation authorizedOeration) {
		this.authorizedOperation = authorizedOeration;
	}

	public Collection<Membership> getMemberships() {
		return memberships;
	}

	public void setMemberships(Collection<Membership> memberships) {
		this.memberships = memberships;
	}

	public Collection<Authorization> getAuthorizations() {
		return authorizations;
	}

	public void setAuthorizations(Collection<Authorization> authorizations) {
		this.authorizations = authorizations;
	}

	public boolean isAnonymous() {
		return ANONYMOUS.equalsIgnoreCase(getName());
	}
	
	public boolean isOwners() {
		return OWNERS.equalsIgnoreCase(getName());
	}
	
	public boolean isLoggedIn() {
		return LOGGEDIN.equalsIgnoreCase(getName());
	}
	
	public boolean isBuiltIn() {
		return isAnonymous() || isOwners() || isLoggedIn();
	}
	
	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("name", getName())
				.add("owner", getOwner().getName())
				.toString();
	}

}
