package io.onedev.server.web.util.avatar;

import java.io.File;

import javax.annotation.Nullable;

import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.eclipse.jgit.lib.PersonIdent;

import io.onedev.server.model.User;
import io.onedev.server.util.facade.UserFacade;

public interface AvatarManager {
	
	/**
	 * Get URL of user avatar image. 
	 *  
	 * @param user
	 * 			user to get avatar for
	 * @return
	 * 			url of avatar image. This url will be relative to context root if gravatar is disabled
	 */
	String getAvatarUrl(@Nullable UserFacade user);
	
	/**
	 * Get URL of avatar image of specified person. 
	 *  
	 * @param person
	 * 			person to get avatar for
	 * @return
	 * 			url of avatar image. This url will be relative to context root if gravatar is disabled
	 */
	String getAvatarUrl(PersonIdent person);

	void useAvatar(User user, @Nullable FileUpload avatar);
	
	File getUploaded(UserFacade user);
	
}

