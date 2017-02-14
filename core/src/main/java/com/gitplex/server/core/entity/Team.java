package com.gitplex.server.core.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.validator.constraints.NotEmpty;

import com.gitplex.commons.hibernate.AbstractEntity;
import com.gitplex.commons.wicket.editable.annotation.Editable;
import com.gitplex.commons.wicket.editable.annotation.Markdown;
import com.gitplex.server.core.util.validation.DepotName;

@Entity
@Table(
		indexes={@Index(columnList="g_organization_id"), @Index(columnList="name")},
		uniqueConstraints={@UniqueConstraint(columnNames={"g_organization_id", "name"})})
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
@Editable
public class Team extends AbstractEntity {

	private static final long serialVersionUID = 1L;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(nullable=false)
	private Account organization;

	@Column(nullable=false)
	private String name;
	
	private String description;

	@OneToMany(mappedBy="team", cascade=CascadeType.REMOVE)
	private Collection<TeamAuthorization> authorizations = new ArrayList<>();
	
	@OneToMany(mappedBy="team", cascade=CascadeType.REMOVE)
	private Collection<TeamMembership> memberships = new ArrayList<>();
	
	private transient Collection<Account> members;
	
	public Account getOrganization() {
		return organization;
	}

	public void setOrganization(Account organization) {
		this.organization = organization;
	}

	@Editable(order=100)
	@DepotName
	@NotEmpty
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Editable(order=200, description="Optionally describe the team")
	@Markdown
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Collection<TeamAuthorization> getAuthorizations() {
		return authorizations;
	}

	public void setAuthorizations(Collection<TeamAuthorization> authorizations) {
		this.authorizations = authorizations;
	}

	public Collection<TeamMembership> getMemberships() {
		return memberships;
	}

	public void setMemberships(Collection<TeamMembership> memberships) {
		this.memberships = memberships;
	}

	public boolean matches(@Nullable String searchTerm) {
		if (searchTerm == null)
			searchTerm = "";
		else
			searchTerm = searchTerm.toLowerCase().trim();
		
		return getName().toLowerCase().contains(searchTerm);
	}
	
	public Collection<Account> getMembers() {
		if (members == null) {
			members = new HashSet<>();
			for (TeamMembership membership: getMemberships()) {
				members.add(membership.getUser());
			}
		}
		return members;
	}

	@Override
	public int compareTo(AbstractEntity entity) {
		Team team = (Team) entity;
		return getName().compareTo(team.getName());
	}
	
}
