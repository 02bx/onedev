package com.pmease.gitop.web.page.project.source.component;

import java.util.Collections;
import java.util.List;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.pmease.gitop.model.Repository;
import com.pmease.gitop.web.util.UrlUtils;

public class AbstractSourcePagePanel extends Panel {
	private static final long serialVersionUID = 1L;

	protected final IModel<Repository> repoModel;
	protected final IModel<String> revisionModel;
	protected final IModel<List<String>> pathsModel;
	
	public AbstractSourcePagePanel(String id,
			IModel<Repository> projectModel,
			IModel<String> revisionModel,
			IModel<List<String>> pathsModel) {
		super(id);
		
		this.repoModel = projectModel;
		this.revisionModel = revisionModel;
		this.pathsModel = pathsModel;
	}

	public Repository getRepo() {
		return repoModel.getObject();
	}
	
	public String getRevision() {
		return revisionModel.getObject();
	}
	
	public List<String> getPaths() {
		if (pathsModel == null) {
			return Collections.emptyList();
		}
		
		return pathsModel.getObject();
	}
	
	public String getFilePath() {
		if (getPaths().isEmpty()) {
			return null;
		} else {
			return UrlUtils.concatSegments(getPaths());
		}
	}
	
	@Override
	public void onDetach() {
		if (revisionModel != null) {
			revisionModel.detach();
		}
		
		if (pathsModel!=null) {
			pathsModel.detach();
		}

		if (repoModel != null) {
			repoModel.detach();
		}
		
		super.onDetach();
	}
}
