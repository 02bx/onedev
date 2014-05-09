package com.pmease.commons.jersey;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.UnauthorizedException;

import com.google.inject.Key;
import com.pmease.commons.loader.AppLoader;
import com.pmease.commons.loader.AppName;

@Provider
public class UnauthorizedExceptionMapper implements ExceptionMapper<UnauthorizedException> {
	
    private final String appName = AppLoader.injector.getInstance(Key.<String>get(String.class, AppName.class));
	
	@Override
    public Response toResponse(UnauthorizedException exception) {
		ResponseBuilder builder;
		if (!SecurityUtils.getSubject().isAuthenticated()) {
			builder = Response.status(Response.Status.UNAUTHORIZED);
			builder.header("WWW-Authenticate", HttpServletRequest.BASIC_AUTH + " realm=\"" + appName + "\"");
		} else {
			builder = Response.status(Response.Status.FORBIDDEN);
		}
    	if (exception.getMessage() != null)
    		builder = builder.entity(exception.getMessage()).type("text/plain");
    	
    	return builder.build();
    }
    
}
