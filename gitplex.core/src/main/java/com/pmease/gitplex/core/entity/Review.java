package com.pmease.gitplex.core.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.pmease.commons.hibernate.AbstractEntity;

@Entity
@Table(uniqueConstraints={
		@UniqueConstraint(columnNames={"g_reviewer_id", "g_update_id"})
})
public class Review extends AbstractEntity {

	private static final long serialVersionUID = 1L;

	public static enum Result {APPROVE, DISAPPROVE};
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(nullable=false)
	private Account reviewer;

	@Column(nullable=false)
	private Date date = new Date();
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(nullable=false)
	private PullRequestUpdate update;
	
	@Column(nullable=false)
	private Result result;
	
	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Account getReviewer() {
		return reviewer;
	}

	public void setReviewer(Account reviewer) {
		this.reviewer = reviewer;
	}
	
	public PullRequestUpdate getUpdate() {
		return update;
	}

	public void setUpdate(PullRequestUpdate update) {
		this.update = update;
	}

	public Result getResult() {
		return result;
	}

	public void setResult(Result result) {
		this.result = result;
	}

}
