package com.gitplex.server.web.behavior.markdown;

import java.io.InputStream;
import java.io.Serializable;
import java.util.List;

public interface AttachmentSupport extends Serializable {
	
	long getAttachmentMaxSize();
	
	List<String> getAttachments();
	
	String getAttachmentUrl(String attachmentName);
	
	String saveAttachment(String suggestedAttachmentName, InputStream attachmentStream);
	
	void deleteAttachemnt(String attachmentName);
	
}
