package com.gitplex.server.entity.support;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.OptimisticLock;

import com.gitplex.server.entity.Account;

@Embeddable
public class CloseInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	public enum Status {INTEGRATED, DISCARDED};
	
	@OptimisticLock(excluded=true)
	@ManyToOne(fetch=FetchType.LAZY)
	private Account closedBy;

	@OptimisticLock(excluded=true)
	private Date closeDate;
	
	@OptimisticLock(excluded=true)
	private Status closeStatus;

	public Account getClosedBy() {
		return closedBy;
	}

	public void setClosedBy(Account closedBy) {
		this.closedBy = closedBy;
	}

	public Date getCloseDate() {
		return closeDate;
	}

	public void setCloseDate(Date closeDate) {
		this.closeDate = closeDate;
	}

	public Status getCloseStatus() {
		return closeStatus;
	}

	public void setCloseStatus(Status closeStatus) {
		this.closeStatus = closeStatus;
	}
	
}
