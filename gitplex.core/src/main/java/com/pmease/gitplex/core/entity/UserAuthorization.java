package com.pmease.gitplex.core.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.pmease.commons.hibernate.AbstractEntity;
import com.pmease.gitplex.core.permission.privilege.DepotPrivilege;

@Entity
@Table(uniqueConstraints={
		@UniqueConstraint(columnNames={"g_user_id", "g_depot_id"})
})
public class UserAuthorization extends AbstractEntity {

	private static final long serialVersionUID = 1L;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(nullable=false)
	private Depot depot;
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(nullable=false)
	private Account user;
	
	@Column(nullable=false)
	private DepotPrivilege privilege = DepotPrivilege.READ;
	
	public Depot getDepot() {
		return depot;
	}

	public void setDepot(Depot depot) {
		this.depot = depot;
	}

	public DepotPrivilege getPrivilege() {
		return privilege;
	}

	public void setPrivilege(DepotPrivilege privilege) {
		this.privilege = privilege;
	}

}
