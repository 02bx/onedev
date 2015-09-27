package com.pmease.gitplex.web.resource;

import java.io.IOException;
import java.io.InputStream;

import javax.persistence.EntityNotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.tika.io.IOUtils;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.AbstractResource;
import org.eclipse.jgit.lib.Constants;

import com.google.common.base.Preconditions;
import com.pmease.commons.git.Blob;
import com.pmease.commons.git.BlobIdent;
import com.pmease.commons.git.GitUtils;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.manager.RepositoryManager;
import com.pmease.gitplex.core.model.Repository;
import com.pmease.gitplex.core.security.SecurityUtils;

public class BlobResource extends AbstractResource {

	private static final long serialVersionUID = 1L;

	private static final String PARAM_USER = "user";
	
	private static final String PARAM_REPO = "repo";
	
	private static final String PARAM_REVISION = "revision";
	
	private static final String PARAM_PATH = "path";
	
	@Override
	protected ResourceResponse newResourceResponse(Attributes attributes) {
		PageParameters params = attributes.getParameters();

		String userName = params.get(PARAM_USER).toString();
		if (StringUtils.isBlank(userName))
			throw new IllegalArgumentException("account name has to be specified");
		
		String repoName = Preconditions.checkNotNull(params.get(PARAM_REPO).toString());
		if (StringUtils.isBlank(repoName))
			throw new IllegalArgumentException("repository name has to be specified");
		
		if (repoName.endsWith(Constants.DOT_GIT_EXT))
			repoName = repoName.substring(0, repoName.length() - Constants.DOT_GIT_EXT.length());
		
		final Repository repository = GitPlex.getInstance(RepositoryManager.class).findBy(userName, repoName);
		
		if (repository == null) 
			throw new EntityNotFoundException("Unable to find repository " + userName + "/" + repoName);
		
		String revision = params.get(PARAM_REVISION).toString();
		if (StringUtils.isBlank(revision))
			throw new IllegalArgumentException("revision parameter has to be specified");
		
		String path = params.get(PARAM_PATH).toString();
		if (StringUtils.isBlank(path))
			throw new IllegalArgumentException("path parameter has to be specified");

		if (!SecurityUtils.canPull(repository)) 
			throw new UnauthorizedException();

		final Blob blob = repository.getBlob(new BlobIdent(revision, path, 0));
		
		ResourceResponse response = new ResourceResponse();
		response.setContentLength(blob.getSize());
		response.setContentType(blob.getMediaType().toString());
		
		if (!GitUtils.isHash(revision))
			response.disableCaching();
		
		response.setFileName(blob.getIdent().getName());
		response.setWriteCallback(new WriteCallback() {

			@Override
			public void writeData(Attributes attributes) throws IOException {
				if (blob.isPartial()) {
					try (InputStream is = repository.getInputStream(blob.getIdent());) {
						IOUtils.copy(is, attributes.getResponse().getOutputStream());
					}
				} else {
					attributes.getResponse().getOutputStream().write(blob.getBytes());
				}
			}
			
		});

		return response;
	}

	public static PageParameters paramsOf(Repository repository, BlobIdent blobIdent) {
		PageParameters params = new PageParameters();
		params.add(PARAM_USER, repository.getOwner().getName());
		params.set(PARAM_REPO, repository.getName());
		params.set(PARAM_REVISION, blobIdent.revision);
		params.set(PARAM_PATH, blobIdent.path);
		
		return params;
	}
	
}
