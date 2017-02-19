package com.gitplex.server.web.util.resource;

import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;

public class BlobResourceReference extends ResourceReference {

	private static final long serialVersionUID = 1L;

	public BlobResourceReference() {
		super("rawblob");
	}

	@Override
	public IResource getResource() {
		return new BlobResource();
	}

}
