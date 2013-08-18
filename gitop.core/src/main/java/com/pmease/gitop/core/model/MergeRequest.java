package com.pmease.gitop.core.model;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.annotations.FetchMode;

import com.pmease.commons.persistence.AbstractEntity;

@SuppressWarnings("serial")
@Entity
public class MergeRequest extends AbstractEntity {

	@Column(nullable=false)
	private String title;

	@ManyToOne(fetch=FetchType.EAGER)
	@org.hibernate.annotations.Fetch(FetchMode.SELECT)
	@JoinColumn(nullable=false)
	private User user;
	
	@ManyToOne(fetch=FetchType.EAGER)
	@org.hibernate.annotations.Fetch(FetchMode.SELECT)
	@JoinColumn(nullable=false)
	private InvolvedBranch targetBranch;

	@ManyToOne(fetch=FetchType.EAGER)
	@org.hibernate.annotations.Fetch(FetchMode.SELECT)
	@JoinColumn(nullable=false)
	private InvolvedBranch sourceBranch;

	@OneToMany(mappedBy="request")
	private Collection<MergeRequestUpdate> updates;
	
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public InvolvedBranch getTargetBranch() {
		return targetBranch;
	}

	public void setTargetBranch(InvolvedBranch targetBranch) {
		this.targetBranch = targetBranch;
	}

	public InvolvedBranch getSourceBranch() {
		return sourceBranch;
	}

	public void setSourceBranch(InvolvedBranch sourceBranch) {
		this.sourceBranch = sourceBranch;
	}

	public Collection<MergeRequestUpdate> getUpdates() {
		return updates;
	}

	public void setUpdates(Collection<MergeRequestUpdate> updates) {
		this.updates = updates;
	}

	public Collection<String> getTouchedFiles() {
		return new ArrayList<String>();
	}
	
}
