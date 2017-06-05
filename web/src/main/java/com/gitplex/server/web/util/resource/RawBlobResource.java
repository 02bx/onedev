package com.gitplex.server.web.util.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityNotFoundException;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.tika.io.IOUtils;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.AbstractResource;
import org.eclipse.jgit.lib.Constants;

import com.gitplex.server.GitPlex;
import com.gitplex.server.git.Blob;
import com.gitplex.server.git.BlobIdent;
import com.gitplex.server.git.GitUtils;
import com.gitplex.server.manager.ProjectManager;
import com.gitplex.server.model.Project;
import com.gitplex.server.security.SecurityUtils;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;

public class RawBlobResource extends AbstractResource {

	private static final long serialVersionUID = 1L;

	private static final String PARAM_DEPOT = "project";
	
	private static final String PARAM_REVISION = "revision";
	
	private static final String PARAM_PATH = "path";
	
	@Override
	protected ResourceResponse newResourceResponse(Attributes attributes) {
		PageParameters params = attributes.getParameters();

		String projectName = Preconditions.checkNotNull(params.get(PARAM_DEPOT).toString());
		if (StringUtils.isBlank(projectName))
			throw new IllegalArgumentException("project name has to be specified");
		
		if (projectName.endsWith(Constants.DOT_GIT_EXT))
			projectName = projectName.substring(0, projectName.length() - Constants.DOT_GIT_EXT.length());
		
		Project project = GitPlex.getInstance(ProjectManager.class).find(projectName);
		
		if (project == null) 
			throw new EntityNotFoundException("Unable to find project " + projectName);

		List<String> revisionAndPathSegments = new ArrayList<>();
		String segment = params.get(PARAM_REVISION).toString();
		if (segment.length() != 0)
			revisionAndPathSegments.add(segment);
		segment = params.get(PARAM_PATH).toString();
		if (segment.length() != 0)
			revisionAndPathSegments.add(segment);
		
		for (int i=0; i<params.getIndexedCount(); i++) {
			segment = params.get(i).toString();
			if (segment.length() != 0)
				revisionAndPathSegments.add(segment);
		}
		
		BlobIdent blobIdent = new BlobIdent(project, revisionAndPathSegments);
		
		String revision = blobIdent.revision;
		String path = blobIdent.path;
		if (StringUtils.isBlank(path))
			throw new IllegalArgumentException("path parameter has to be specified");

		if (!SecurityUtils.canRead(project)) 
			throw new UnauthorizedException();

		final Blob blob = project.getBlob(new BlobIdent(revision, path, 0));
		
		ResourceResponse response = new ResourceResponse();
		response.setContentLength(blob.getSize());
		response.setContentType(blob.getMediaType().toString());
		if (response.getContentType().equals(MediaType.TEXT_HTML)) {
			response.setContentType(MediaType.TEXT_PLAIN);
		}
		
		if (!GitUtils.isHash(revision))
			response.disableCaching();
		
		try {
			response.setFileName(URLEncoder.encode(blob.getIdent().getName(), Charsets.UTF_8.name()));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		
		response.setWriteCallback(new WriteCallback() {

			@Override
			public void writeData(Attributes attributes) throws IOException {
				if (blob.isPartial()) {
					try (InputStream is = project.getInputStream(blob.getIdent());) {
						IOUtils.copy(is, attributes.getResponse().getOutputStream());
					}
				} else {
					attributes.getResponse().getOutputStream().write(blob.getBytes());
				}
			}
			
		});

		return response;
	}

	public static PageParameters paramsOf(Project project, BlobIdent blobIdent) {
		PageParameters params = new PageParameters();
		params.set(PARAM_DEPOT, project.getName());
		params.set(PARAM_REVISION, blobIdent.revision);
		params.set(PARAM_PATH, blobIdent.path);
		
		return params;
	}
	
}
