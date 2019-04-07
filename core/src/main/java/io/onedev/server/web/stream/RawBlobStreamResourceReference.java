package io.onedev.server.web.stream;

import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;

public class RawBlobStreamResourceReference extends ResourceReference {

	private static final long serialVersionUID = 1L;

	public RawBlobStreamResourceReference() {
		super("rawblob");
	}

	@Override
	public IResource getResource() {
		return new RawBlobStreamResource();
	}

}
