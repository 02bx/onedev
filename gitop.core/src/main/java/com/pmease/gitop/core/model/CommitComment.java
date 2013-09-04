package com.pmease.gitop.core.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.FetchMode;

import com.pmease.commons.persistence.AbstractEntity;

@SuppressWarnings("serial")
@Entity
public class CommitComment extends AbstractEntity {

	@ManyToOne(fetch=FetchType.EAGER)
	@org.hibernate.annotations.Fetch(FetchMode.SELECT)
	@JoinColumn(nullable=false)
	private User user;
	
	@ManyToOne(fetch=FetchType.EAGER)
	@org.hibernate.annotations.Fetch(FetchMode.SELECT)
	@JoinColumn(nullable=false)
	private Commit commit;
	
	@Column(nullable=false)
	private Date date;
	
	@Column(nullable=false)
	private String content;
	
	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Commit getCommit() {
		return commit;
	}

	public void setCommit(Commit commit) {
		this.commit = commit;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
	
}
