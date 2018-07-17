package io.onedev.server.model;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.CascadeType;
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

import io.onedev.server.web.editable.annotation.Editable;

@Entity
@Table(
		indexes={@Index(columnList="g_project_id"), @Index(columnList="name")},
		uniqueConstraints={@UniqueConstraint(columnNames={"g_project_id", "name"})}
)
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
@Editable
public class Configuration extends AbstractEntity {

	private static final long serialVersionUID = 1L;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(nullable=false)
	private Project project;
	
	private String name;

	@OneToMany(mappedBy="configuration", cascade=CascadeType.REMOVE)
	private Collection<PullRequestBuild> pullRequestBuilds = new ArrayList<>();
	
	@OneToMany(mappedBy="configuration", cascade=CascadeType.REMOVE)
	private Collection<Build> builds = new ArrayList<>();
	
	public Project getProject() {
		return project;
	}

	public void setProject(Project project) {
		this.project = project;
	}

	@Editable
	@NotEmpty
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Collection<PullRequestBuild> getPullRequestBuilds() {
		return pullRequestBuilds;
	}

	public void setPullRequestBuilds(Collection<PullRequestBuild> pullRequestBuilds) {
		this.pullRequestBuilds = pullRequestBuilds;
	}

	public Collection<Build> getBuilds() {
		return builds;
	}

	public void setBuilds(Collection<Build> builds) {
		this.builds = builds;
	}
	
}
