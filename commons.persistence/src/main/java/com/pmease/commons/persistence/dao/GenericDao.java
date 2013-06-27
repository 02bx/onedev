package com.pmease.commons.persistence.dao;

import java.util.List;

import javax.annotation.Nullable;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;

import com.pmease.commons.persistence.AbstractEntity;


public interface GenericDao<T extends AbstractEntity> {
	/**
	 * Get the entity with the specified type and id from the datastore.
	 * If none is found, return null.
	 */
	public T find(Long entityId);

	/**
	 * Get a reference to the entity with the specified type and id from the
	 * datastore.
	 * 
	 * This does not require a call to the datastore and does not populate any
	 * of the entity's values. Values may be fetched lazily at a later time.
	 * This increases performance if a another entity is being saved that should
	 * reference this entity but the values of this entity are not needed.
	 * 
	 * @throws a
	 *             HibernateException if no matching entity is found
	 */
	public T getReference(Long entityId);

	/**
	 * If the id of the entity is null or zero, add it to the datastore and
	 * assign it an id; otherwise, update the corresponding entity in the
	 * datastore with the properties of this entity. In either case the entity
	 * passed to this method will be attached to the session.
	 * 
	 * If an entity to update is already attached to the session, this method
	 * will have no effect. If an entity to update has the same id as another
	 * instance already attached to the session, an error will be thrown.
	 * 
	 */
	public void save(T entity);

	/**
	 * Remove the specified entity from the datastore.
	 * 
	 */
	public void delete(T entity);
	
	/**
	 * Delete entity of specified identifier without actually loading the entity.
	 * @param entityClass
	 * 			class of the entity
	 * @param entityId
	 * 			identifier of the entity
	 */
	void deleteById(Long entityId);

	/**
	 * Search entity with specified hibernate criterions and orders. 
	 * @param criterions
	 * 			Hibernate criterions to restrict search result. No restrictions will be set 
	 * 			if pass a null value or empty array.
	 * @param orders
	 * 			Hibernate orders to order search result. No orders will be set if pass a 
	 * 			null value or empty array.
	 * @param firstResult
	 * 			First result of the query. Set to 0 if this value should be ignored.
	 * @param maxResults
	 * 			Max number of returned entities. Set to 0 if no limit of number of returned 
	 * 			entities should be set.
	 * @return
	 * 			List of matched entities.
	 */
	List<T> search(@Nullable Criterion[] criterions, @Nullable Order[] orders, int firstResult, int maxResults);
	
	/**
	 * Count entity matching specified hibernate criterions. 
	 * @param entityClass
	 * 			Class of entity to count.
	 * @param criterias
	 * 			Hibernate criterions to restrict entities to be counted. No retrictions will be 
	 * 			set if pass a null value or empty array.
	 * @return
	 * 			Number of entities matching specified criterions.
	 */
	int count(@Nullable Criterion[] criterions);
}
