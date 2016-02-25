package com.pmease.gitplex.core.manager;

import javax.annotation.Nullable;

import org.eclipse.jgit.lib.PersonIdent;

import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.gitplex.core.entity.User;

public interface UserManager extends Dao {
	
	/**
	 * Save user properties. Note that name of the user should not be changed 
	 * via this method. Call the {@link this#rename(Long, String, String)} to 
	 * rename an user.
	 */
	void save(User user);
	
	void delete(User user);
	
	/**
	 * Find root account in the system. 
	 * 
	 * @return
	 * 			root account of the system. Never be <tt>null</tt>
	 */
	User getRoot();

	/**
	 * Find user of specified name.
	 * <p>
	 * @param userName
	 * 			name of the user
	 * @return
	 * 			matching user, or <tt>null</tt> if not found 
	 */
	@Nullable User findByName(String userName);
	
	/**
	 * Find user by person
	 * <p>
	 * @param person
	 * 			Git person representation 
	 * @return
	 * 			matching user, or <tt>null</tt> if not found 
	 */
	@Nullable User findByPerson(PersonIdent person);
	
	/**
	 * Get current authenticated user in Shiro context, or <tt>null</tt> if not 
	 * authenticated. 
	 * 
	 * @return 
	 *         current authenticated user, or <tt>null</tt> for anonymous access
	 */
	@Nullable User getCurrent();
	
	/**
	 * Get previous user in case current user is a run-as user
	 * 
	 * @return
	 * 			previous user if current user is a run-as user, or <tt>null</tt> 
	 * 			if current user is not a run-as user. 
	 */
	@Nullable User getPrevious();
	
	void rename(Long userId, String oldName, String newName);
}
