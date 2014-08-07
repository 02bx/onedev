package com.pmease.gitplex.web.extensionpoint;

import javax.annotation.Nullable;
import org.apache.tika.mime.MediaType;
import com.pmease.commons.loader.ExtensionPoint;

@ExtensionPoint
public interface MediaRendererProvider {
	@Nullable MediaRenderer getMediaRenderer(MediaType mediaType);
}
