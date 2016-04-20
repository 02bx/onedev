package com.pmease.gitplex.core.entity;

import java.util.Date;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Version;

import org.hibernate.annotations.DynamicUpdate;

import com.pmease.commons.hibernate.AbstractEntity;
import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.gitplex.core.GitPlex;

/*
 * @DynamicUpdate annotation here along with various @OptimisticLock annotations
 * on certain fields tell Hibernate not to perform version check on those fields
 * which can be updated from background thread.
 */
@Entity
@DynamicUpdate 
public class PullRequestComment extends AbstractEntity {
	
	private static final long serialVersionUID = 1L;

	public static final int DIFF_CONTEXT_SIZE = 3;

	@Version
	private long version;
	
	@ManyToOne(fetch=FetchType.LAZY)
	private PullRequest request;
	
	@ManyToOne(fetch=FetchType.LAZY)
	private Account user;
	
	@Lob
	@Column(nullable=false, length=65535)
	private String content;
	
	@Column(nullable=false)
	private Date date = new Date();
	
	public PullRequest getRequest() {
		return request;
	}

	public void setRequest(PullRequest request) {
		this.request = request;
	}

	@Nullable
	public Account getUser() {
		return user;
	}

	public void setUser(@Nullable Account user) {
		this.user = user;
	}

	public long getVersion() {
		return version;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Date getDate() {
		return date;
	}

	public Depot getDepot() {
		return request.getTargetDepot();
	}

	public void delete() {
		GitPlex.getInstance(Dao.class).remove(this);
	}

}
