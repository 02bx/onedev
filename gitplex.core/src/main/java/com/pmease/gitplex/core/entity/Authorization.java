package com.pmease.gitplex.core.entity;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.pmease.commons.hibernate.AbstractEntity;
import com.pmease.gitplex.core.permission.operation.DepotOperation;

@SuppressWarnings("serial")
@Entity
@Table(uniqueConstraints={
		@UniqueConstraint(columnNames={"g_team_id", "g_depot_id"})
})
public class Authorization extends AbstractEntity {

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(nullable=false)
	private Team team;	

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(nullable=false)
	private Depot depot;
	
	private DepotOperation operation = DepotOperation.PULL;
	
	public DepotOperation getOperation() {
		return operation;
	}

	public void setOperation(DepotOperation operation) {
		this.operation = operation;
	}

	public Team getTeam() {
		return team;
	}

	public void setTeam(Team team) {
		this.team = team;
	}
	
	public Depot getDepot() {
		return depot;
	}

	public void setDepot(Depot depot) {
		this.depot = depot;
	}

}
