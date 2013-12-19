package com.pmease.gitop.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.pmease.commons.editable.annotation.Editable;
import com.pmease.commons.git.Git;
import com.pmease.commons.hibernate.AbstractEntity;
import com.pmease.commons.loader.AppLoader;
import com.pmease.commons.util.FileUtils;
import com.pmease.gitop.model.gatekeeper.AndGateKeeper;
import com.pmease.gitop.model.gatekeeper.GateKeeper;
import com.pmease.gitop.model.permission.object.ProtectedObject;
import com.pmease.gitop.model.permission.object.UserBelonging;
import com.pmease.gitop.model.storage.StorageManager;
import com.pmease.gitop.model.validation.ProjectName;

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
	
	private boolean forkable;
	
	@Column()
	private String defaultBranchName;
	
	@ManyToOne
	@JoinColumn(nullable=true)
	private Project forkedFrom;

	@Column(nullable=false)
	private String name;
	
	private String description;

	@Lob
	@Column(nullable=false)
	private ArrayList<GateKeeper> gateKeepers = new ArrayList<GateKeeper>();
	
	@Column(nullable=false)
	private Date createdAt = new Date();

	@OneToMany(mappedBy="project", cascade=CascadeType.REMOVE)
	private Collection<Authorization> authorizations = new ArrayList<Authorization>();

    @OneToMany(mappedBy="project", cascade=CascadeType.REMOVE)
    private Collection<Branch> branches = new ArrayList<Branch>();

    @OneToMany(mappedBy="forkedFrom", cascade=CascadeType.REMOVE)
	private Collection<Project> forks = new ArrayList<Project>();

	public User getOwner() {
		return owner;
	}

	public void setOwner(User owner) {
		this.owner = owner;
	}

	@Editable(order=100, description=
			"Specify name of the project. It will be used to identify the project when accessing via Git.")
	@ProjectName
	@NotEmpty
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Editable(order=200, description="Specify description of the project.")
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

    @Editable(order=450, description="Whether or not this project can be forked.")
    public boolean isForkable() {
        return forkable;
    }

    public void setForkable(boolean forkable) {
        this.forkable = forkable;
    }

    @Editable(name="Accept Merge Requests If", order=500,
			description="Optionally define gate keeper to accept merge requests under certain condition.")
    @NotNull
	@Valid
	public List<GateKeeper> getGateKeepers() {
		return gateKeepers;
	}

	public void setGateKeepers(ArrayList<GateKeeper> gateKeepers) {
		this.gateKeepers = gateKeepers;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public String getDefaultBranchName() {
        return defaultBranchName;
    }

    public void setDefaultBranchName(String defaultBranchName) {
        this.defaultBranchName = defaultBranchName;
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

	/**
	 * Get branches for this project from database. The result might be 
	 * different from actual branches in repository. To get actual 
	 * branches in repository, call {@link BranchManager#listBranches(Project)} 
	 * instead.
	 * 
	 * @return
	 *         collection of branches available in database for this project 
	 */
	public Collection<Branch> getBranches() {
        return branches;
    }

    public void setBranches(Collection<Branch> branches) {
        this.branches = branches;
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

	public String getPathName() {
		return getOwner().getName() + "/" + getName();
	}
	
	@Override
	public String toString() {
		return getPathName();
	}
	
	public Git code() {
		return new Git(AppLoader.getInstance(StorageManager.class).getStorage(this).ofCode());
	}

	/**
	 * Whether or not the code repository is valid. This can be used to tell apart a Gitop 
	 * repository from some other Git repositories.
	 * 
	 * @return
	 * 			<tt>true</tt> if valid; <tt>false</tt> otherwise
	 */
	public static boolean isCode(Git git) {
        File preReceiveHook = new File(git.repoDir(), "hooks/pre-receive");
        if (!preReceiveHook.exists()) 
        	return false;
        
        try {
			String content = FileUtils.readFileToString(preReceiveHook);
			return content.contains("GITOP_USER_ID");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	public GateKeeper getCompositeGateKeeper() {
		AndGateKeeper andGateKeeper = new AndGateKeeper();
		for (GateKeeper each: getGateKeepers())
			andGateKeeper.getGateKeepers().add(each);
		return andGateKeeper;
	}
	
}
