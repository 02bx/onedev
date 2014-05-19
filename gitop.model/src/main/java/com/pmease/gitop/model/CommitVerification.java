package com.pmease.gitop.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.pmease.commons.hibernate.AbstractEntity;

@SuppressWarnings("serial")
@Entity
@Table(uniqueConstraints={
		@UniqueConstraint(columnNames={"branch", "commit", "configuration"})
})
public class CommitVerification extends AbstractEntity {
	
	public enum Status {ONGOING, PASSED, NOT_PASSED}
	
	@ManyToOne
	@JoinColumn(nullable=false)
	private Branch branch;
	
	@Column(nullable=false)
	private String commit;

	@Column(nullable=false)
	private String configuration;
	
	@Column(nullable=false)
	private String message;
	
	private Status status;

	public String getCommit() {
		return commit;
	}

	public void setCommit(String commit) {
		this.commit = commit;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public Branch getBranch() {
		return branch;
	}

	public void setBranch(Branch branch) {
		this.branch = branch;
	}

	public String getConfiguration() {
		return configuration;
	}

	public void setConfiguration(String configuration) {
		this.configuration = configuration;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
}
