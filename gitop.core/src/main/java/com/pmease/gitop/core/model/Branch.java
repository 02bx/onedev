package com.pmease.gitop.core.model;

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

import com.google.common.base.Objects;
import com.pmease.commons.hibernate.AbstractEntity;

@SuppressWarnings("serial")
@Entity
@Table(uniqueConstraints={
		@UniqueConstraint(columnNames={"project", "name"})
})
public class Branch extends AbstractEntity {
	
	@ManyToOne
	@JoinColumn(nullable=false)
	private Project project;

	@Column(nullable=false)
	private String name;

	@OneToMany(mappedBy="to", cascade=CascadeType.REMOVE)
	private Collection<AutoPull> autoPulls = new ArrayList<AutoPull>();

	@OneToMany(mappedBy="from", cascade=CascadeType.REMOVE)
	private Collection<AutoPush> autoPushes = new ArrayList<AutoPush>();

	public Project getProject() {
		return project;
	}

	public void setProject(Project project) {
		this.project = project;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public Collection<AutoPull> getAutoPulls() {
		return autoPulls;
	}

	public void setAutoPulls(Collection<AutoPull> autoPulls) {
		this.autoPulls = autoPulls;
	}

	public Collection<AutoPush> getAutoPushes() {
		return autoPushes;
	}

	public void setAutoPushes(Collection<AutoPush> autoPushes) {
		this.autoPushes = autoPushes;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("name", getName())
				.add("project", getProject())
				.toString();
	}
}
