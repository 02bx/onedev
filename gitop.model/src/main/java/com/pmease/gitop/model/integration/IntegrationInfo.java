package com.pmease.gitop.model.integration;

import java.io.Serializable;

import javax.annotation.Nullable;
import javax.persistence.Embeddable;

@SuppressWarnings("serial")
@Embeddable
public class IntegrationInfo implements Serializable {
	
	private String branchHead;
	
	private String requestHead;
	
	private String integrationHead;
	
	private IntegrationStrategy integrationStrategy;
	
	@SuppressWarnings("unused")
	private IntegrationInfo() {
	}
	
	public IntegrationInfo(String branchHead, String requestHead, @Nullable String integrationHead, 
			IntegrationStrategy integrationStrategy) {
		this.branchHead = branchHead;
		this.requestHead = requestHead;
		this.integrationHead = integrationHead;
		this.integrationStrategy = integrationStrategy;
	}

	public String getBranchHead() {
		return branchHead;
	}

	public String getRequestHead() {
		return requestHead;
	}

	/**
	 * Get integration commit. 
	 *  
	 * @return
	 * 			integration commit. <tt>null</tt> will be returned if there are integration conflicts
	 */
	public @Nullable String getIntegrationHead() {
		return integrationHead;
	}

	/**
	 * Get strategy used to do integration. 
	 * 
	 * @return
	 * 			strategy used to do integration. <tt>null</tt> will be returned if integration approach 
	 * 			is unknown, for instance when request head is already ancestor of branch head while the 
	 * 			request is being refreshed
	 */
	public @Nullable IntegrationStrategy getIntegrationStrategy() {
		return integrationStrategy;
	}

}