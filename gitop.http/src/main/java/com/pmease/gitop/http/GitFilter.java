package com.pmease.gitop.http;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.UnauthorizedException;
import org.eclipse.jgit.http.server.GitSmartHttpTools;
import org.eclipse.jgit.http.server.ServletUtils;
import org.eclipse.jgit.transport.PacketLineOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pmease.commons.git.command.AdvertiseReceiveRefsCommand;
import com.pmease.commons.git.command.AdvertiseUploadRefsCommand;
import com.pmease.commons.git.command.ReceiveCommand;
import com.pmease.commons.git.command.UploadCommand;
import com.pmease.commons.util.GeneralException;
import com.pmease.gitop.core.Gitop;
import com.pmease.gitop.core.manager.ProjectManager;
import com.pmease.gitop.model.Project;
import com.pmease.gitop.model.User;
import com.pmease.gitop.model.permission.ObjectPermission;
import com.pmease.gitop.model.storage.StorageManager;

@Singleton
public class GitFilter implements Filter {
	
	private static final Logger logger = LoggerFactory.getLogger(GitFilter.class);

	private static final String INFO_REFS = "info/refs";
	
	private final Gitop gitop;
	
	private final StorageManager storageManager;
	
	private final ProjectManager projectManager;
	
	@Inject
	public GitFilter(Gitop gitop, StorageManager storageManager, ProjectManager projectManager) {
		this.gitop = gitop;
		this.storageManager = storageManager;
		this.projectManager = projectManager;
	}
	
	private String getPathInfo(HttpServletRequest request) {
		String pathInfo = request.getRequestURI().substring(request.getContextPath().length());
		return StringUtils.stripStart(pathInfo, "/");
	}
	
	private Project getProject(HttpServletRequest request, HttpServletResponse response, String repoInfo) 
			throws IOException {
		repoInfo = StringUtils.stripStart(StringUtils.stripEnd(repoInfo, "/"), "/");
		
		String ownerName = StringUtils.substringBefore(repoInfo, "/");
		String projectName = StringUtils.substringAfter(repoInfo, "/");

		if (StringUtils.isBlank(ownerName) || StringUtils.isBlank(projectName)) {
			String url = request.getRequestURL().toString();
			String urlRoot = url.substring(0, url.length()-getPathInfo(request).length());
			throw new GeneralException("Expecting url of format %s<owner name>/<project name>", urlRoot);
		} 
		
		if (projectName.endsWith(".git"))
			projectName = projectName.substring(0, projectName.length()-".git".length());
		
		Project project = projectManager.find(ownerName, projectName);
		if (project == null) {
			throw new GeneralException("Unable to find project %s owned by %s.", projectName, ownerName);
		}
		
		return project;
	}
	
	private void doNotCache(HttpServletResponse response) {
		response.setHeader("Expires", "Fri, 01 Jan 1980 00:00:00 GMT");
		response.setHeader("Pragma", "no-cache");
		response.setHeader("Cache-Control", "no-cache, max-age=0, must-revalidate");
	}
	
	protected void processPacks(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String pathInfo = getPathInfo(request);
		
		String service = StringUtils.substringAfterLast(pathInfo, "/");

		String repoInfo = StringUtils.substringBeforeLast(pathInfo, "/");
		Project project = getProject(request, response, repoInfo);
		
		doNotCache(response);
		response.setHeader("Content-Type", "application/x-" + service + "-result");			

		Map<String, String> environments = new HashMap<>();
		environments.put("GITOP_USER_ID", User.getCurrentId().toString());
		File gitDir = storageManager.getStorage(project).ofCode();

		if (GitSmartHttpTools.isUploadPack(request)) {
			if (!SecurityUtils.getSubject().isPermitted(ObjectPermission.ofProjectRead(project))) {
				throw new UnauthorizedException("You do not have permission to pull from this project.");
			}
			new UploadCommand(gitDir, environments).input(ServletUtils.getInputStream(request)).output(response.getOutputStream()).call();
		} else {
			if (!SecurityUtils.getSubject().isPermitted(ObjectPermission.ofProjectWrite(project))) {
				throw new UnauthorizedException("You do not have permission to push to this project.");
			}
			new ReceiveCommand(gitDir, environments).input(ServletUtils.getInputStream(request)).output(response.getOutputStream()).call();
		}
	}
	
	private void writeInitial(HttpServletResponse response, String service) throws IOException {
		doNotCache(response);
		response.setHeader("Content-Type", "application/x-" + service + "-advertisement");			
		
		PacketLineOut pack = new PacketLineOut(response.getOutputStream());
		pack.setFlushOnEnd(false);
		pack.writeString("# service=" + service + "\n");
		pack.end();
	}
	
	protected void processRefs(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String pathInfo = request.getRequestURI().substring(request.getContextPath().length());
		pathInfo = StringUtils.stripStart(pathInfo, "/");

		String repoInfo = pathInfo.substring(0, pathInfo.length() - INFO_REFS.length());
		Project project = getProject(request, response, repoInfo);
		String service = request.getParameter("service");
		
		File gitDir = storageManager.getStorage(project).ofCode();

		if (service.contains("upload")) {
			if (!SecurityUtils.getSubject().isPermitted(ObjectPermission.ofProjectRead(project))) {
				throw new UnauthorizedException("You do not have permission to pull from this project.");
			}
			writeInitial(response, service);
			new AdvertiseUploadRefsCommand(gitDir).output(response.getOutputStream()).call();
		} else {
			if (!SecurityUtils.getSubject().isPermitted(ObjectPermission.ofProjectWrite(project))) {
				throw new UnauthorizedException("You do not have permission to push to this project.");
			}
			writeInitial(response, service);
			new AdvertiseReceiveRefsCommand(gitDir).output(response.getOutputStream()).call();
		}
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;

		try {
			if (GitSmartHttpTools.isInfoRefs(httpRequest)) {
				if (gitop.isReady())
					processRefs(httpRequest, httpResponse);
				else
					throw new GeneralException("Server is not ready");
			} else if (GitSmartHttpTools.isReceivePack(httpRequest) || GitSmartHttpTools.isUploadPack(httpRequest)) {
				if (gitop.isReady())
					processPacks(httpRequest, httpResponse);
				else
					throw new GeneralException("Server is not ready");
			} else {
				chain.doFilter(request, response);
			}
		} catch (GeneralException e) {
			logger.error("Error serving git request", e);
			GitSmartHttpTools.sendError(httpRequest, httpResponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	@Override
	public void destroy() {
	}
	
}
 