package com.pmease.gitop.web.model;

import org.apache.wicket.model.LoadableDetachableModel;

import com.google.common.base.Preconditions;
import com.pmease.commons.hibernate.AbstractEntity;
import com.pmease.commons.hibernate.dao.GeneralDao;
import com.pmease.commons.loader.AppLoader;
import com.pmease.commons.util.JavassistUtils;

public class EntityModel<T extends AbstractEntity> extends LoadableDetachableModel<T> {

	private static final long serialVersionUID = 1L;

	private final Class<T> entityClass;

	private T entity;
	
	private Long id;
	
	protected GeneralDao getDao() {
		return AppLoader.getInstance(GeneralDao.class);
	}

	@SuppressWarnings("unchecked")
	public EntityModel(T entity) {
		Preconditions.checkNotNull(entity, "entity");
		
		this.entityClass = (Class<T>) JavassistUtils.unproxy(entity.getClass());
		
		setObject(entity);
	}

	@Override
	protected T load() {
		if (id != null) {
			return getDao().load(entityClass, id);
		} else {
			return entity;
		}
	}

	@Override
	public void setObject(T object) {
		super.setObject(object);
		if (object.isNew())
			entity = object;
		else
			id = object.getId();
	}
	
}
