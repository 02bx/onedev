package com.gitplex.server.core.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.shiro.authz.Permission;

import com.gitplex.commons.shiro.AbstractRealm;
import com.gitplex.commons.shiro.AbstractUser;
import com.gitplex.server.core.GitPlex;
import com.gitplex.server.core.entity.Account;
import com.gitplex.server.core.entity.Depot;
import com.gitplex.server.core.entity.OrganizationMembership;
import com.gitplex.server.core.entity.Team;
import com.gitplex.server.core.entity.TeamAuthorization;
import com.gitplex.server.core.entity.TeamMembership;
import com.gitplex.server.core.entity.UserAuthorization;
import com.gitplex.server.core.manager.AccountManager;
import com.gitplex.server.core.manager.OrganizationMembershipManager;
import com.gitplex.server.core.manager.TeamMembershipManager;
import com.gitplex.server.core.manager.UserAuthorizationManager;
import com.gitplex.server.core.security.privilege.AccountPrivilege;
import com.gitplex.server.core.security.privilege.DepotPrivilege;

@Singleton
public class SecurityRealm extends AbstractRealm {

    private final AccountManager accountManager;
    
    private final OrganizationMembershipManager organizationMembershipManager;
    
    private final TeamMembershipManager teamMembershipManager;
    
    @Inject
    public SecurityRealm(AccountManager userManager, 
    		OrganizationMembershipManager organizationMembershipManager, 
    		TeamMembershipManager teamMembershipManager) {
    	this.accountManager = userManager;
    	this.organizationMembershipManager = organizationMembershipManager;
    	this.teamMembershipManager = teamMembershipManager;
    }

    @Override
    protected AbstractUser getUserByName(String userName) {
    	return accountManager.findByName(userName);
    }

    @Override
    protected Collection<Permission> permissionsOf(Long userId) {
        Collection<Permission> permissions = new ArrayList<>();

        /*
         * Instead of returning all permissions of the user, we return a customized
         * permission object so that we can control the authorization procedure for 
         * optimization purpose. For instance, we may check information contained 
         * in the permission being checked and if it means authorization of certain 
         * object, we can then only load authorization information of that object.
         */
        permissions.add(new Permission() {

            @Override
            public boolean implies(Permission permission) {
            	if (permission instanceof ObjectPermission) {
            		ObjectPermission objectPermission = (ObjectPermission) permission;
            		Depot checkDepot = getDepot(objectPermission);
            		if (checkDepot != null 
            				&& checkDepot.isPublicRead() 
            				&& DepotPrivilege.READ.can(objectPermission.getPrivilege())) {
            			return true;
            		}
	                if (userId != 0L) {
	                    Account user = accountManager.get(userId);
	                    if (user != null) {
		                    // administrator can do anything
		                    if (user.isAdministrator())
		                    	return true;

		                    Account checkAccount = getAccount(objectPermission);
		                    
		                    // if permission is to check privilege of account belongings		                    
		                    if (checkAccount != null) {  
		                    	// I can do anything against my own account
		                    	if (checkAccount.equals(user)) 
		                    		return true;

		                    	OrganizationMembership organizationMembership = 
		                    			organizationMembershipManager.find(checkAccount, user);
		                    	if (organizationMembership != null) {
		                    		AccountPrivilege accountPrivilege;
		                    		if (organizationMembership.isAdmin())
		                    			accountPrivilege = AccountPrivilege.ADMIN;
		                    		else
		                    			accountPrivilege = AccountPrivilege.ACCESS;
		                    		if (accountPrivilege.can(objectPermission.getPrivilege()))
		                    			return true;
		                    	}
			                    if (checkDepot != null) {
			                    	if (organizationMembership != null 
			                    			&& checkAccount.getDefaultPrivilege().can(objectPermission.getPrivilege())) {
			                    		return true;
			                    	}
			                    	UserAuthorizationManager userAuthorizationManager = 
			                    			GitPlex.getInstance(UserAuthorizationManager.class);
			                    	UserAuthorization userAuthorization = userAuthorizationManager.find(user, checkDepot);
			                    	if (userAuthorization != null 
			                    			&& userAuthorization.getPrivilege().can(objectPermission.getPrivilege())) {
			                    		return true;
			                    	}
			                    		
	                				Set<Team> teams = new HashSet<>();
	                				for (TeamMembership teamMembership: 
	                						teamMembershipManager.findAll(checkAccount, user)) {
	                					teams.add(teamMembership.getTeam());
	                				}
		                			for (TeamAuthorization authorization: checkDepot.getAuthorizedTeams()) {
		                				if (authorization.getPrivilege().can(objectPermission.getPrivilege())
		                						&& teams.contains(authorization.getTeam())) {
		                					return true;
		                				}
		                			}
			                    }
		                    }
	                    }
	                }
            	} 
            	return false;
            }
        });

        return permissions;        
    }
    
    private Account getAccount(ObjectPermission permission) {
        if (permission.getObject() instanceof Depot) {
        	Depot depot = (Depot) permission.getObject();
        	return depot.getAccount();
        } else if (permission.getObject() instanceof Account) {
        	return (Account) permission.getObject();
        } else {
        	return null;
        }
    }

    private Depot getDepot(ObjectPermission permission) {
        if (permission.getObject() instanceof Depot) {
        	Depot depot = (Depot) permission.getObject();
        	return depot;
        } else {
        	return null;
        }
    }
    
}
