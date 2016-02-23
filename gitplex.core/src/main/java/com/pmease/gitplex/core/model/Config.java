package com.pmease.gitplex.core.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.pmease.commons.hibernate.AbstractEntity;

/**
 * This entity stores object in serialized form, with one entity 
 * representing one object. 
 *
 */
@SuppressWarnings("serial")
@Entity
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
public class Config extends AbstractEntity {

	public enum Key {SYSTEM, MAIL, QOS};
	
	@Column(nullable=false, unique=true)
	private Key key;
	
	/* 
	 * This field is allowed to be null to indicate particular setting is not 
	 * available (the record will always be available after interactive setup
	 * to indicate that the setting has been prompted (although the user may
	 * skipped the setting), so we can not use existence of record to indicate
	 * a null setting.
	 */
	@Lob
	@Column(length=65535)
	private Serializable setting;

	public Key getKey() {
		return key;
	}

	public void setKey(Key key) {
		this.key = key;
	}

	public Serializable getSetting() {
		return setting;
	}

	public void setSetting(Serializable setting) {
		this.setting = setting;
	}

}
