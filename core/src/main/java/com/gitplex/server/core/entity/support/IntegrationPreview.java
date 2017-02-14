package com.gitplex.server.core.entity.support;

import java.io.Serializable;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.eclipse.jgit.lib.ObjectId;

import com.gitplex.server.core.entity.PullRequest;
import com.gitplex.server.core.entity.PullRequest.IntegrationStrategy;

@SuppressWarnings("serial")
@Embeddable
public class IntegrationPreview implements Serializable {
	
	@Column(name="PREVIEW_TARGET_HEAD")
	private String targetHead;
	
	@Column(name="PREVIEW_REQUEST_HEAD")
	private String requestHead;
	
	@Column(name="PREVIEW_INTEGRATION_STRATEGY")
	private IntegrationStrategy integrationStrategy;
	
	@Column(name="PREVIEW_INTEGRATED")
	private String integrated;
	
	@SuppressWarnings("unused")
	private IntegrationPreview() {
	}
	
	public IntegrationPreview(String targetHead, String requestHead, 
			IntegrationStrategy integrationStrategy, @Nullable String integrated) {
		this.targetHead = targetHead;
		this.requestHead = requestHead;
		this.integrationStrategy = integrationStrategy;
		this.integrated = integrated;
	}

	public String getTargetHead() {
		return targetHead;
	}

	public String getRequestHead() {
		return requestHead;
	}

	public IntegrationStrategy getIntegrationStrategy() {
		return integrationStrategy;
	}

	public void setTargetHead(String targetHead) {
		this.targetHead = targetHead;
	}

	public void setRequestHead(String requestHead) {
		this.requestHead = requestHead;
	}

	public void setIntegrationStrategy(IntegrationStrategy integrationStrategy) {
		this.integrationStrategy = integrationStrategy;
	}

	/**
	 * Integrated commit hash 
	 * 
	 * @return
	 * 			null if there are conflicts
	 */
	@Nullable
	public String getIntegrated() {
		return integrated;
	}

	public void setIntegrated(String integrated) {
		this.integrated = integrated;
	}

	public boolean isObsolete(PullRequest request) {
		if (getRequestHead().equals(request.getHeadCommitHash())
				&& getTargetHead().equals(request.getTarget().getObjectName())
				&& getIntegrationStrategy() == request.getIntegrationStrategy()
				&& (getIntegrated() == null || ObjectId.fromString(getIntegrated()).equals((request.getTargetDepot().getObjectId(request.getIntegrateRef(), false))))) {
			return false;
		} else {
			return true;
		}
	}
	
}