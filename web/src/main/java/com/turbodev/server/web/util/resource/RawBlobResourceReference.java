package com.turbodev.server.web.util.resource;

import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;

public class RawBlobResourceReference extends ResourceReference {

	private static final long serialVersionUID = 1L;

	public RawBlobResourceReference() {
		super("rawblob");
	}

	@Override
	public IResource getResource() {
		return new RawBlobResource();
	}

}
