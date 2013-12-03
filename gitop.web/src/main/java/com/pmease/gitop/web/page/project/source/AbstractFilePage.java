package com.pmease.gitop.web.page.project.source;

import java.util.List;

import javax.persistence.EntityNotFoundException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.pmease.commons.git.Git;
import com.pmease.gitop.web.page.PageSpec;
import com.pmease.gitop.web.page.project.ProjectCategoryPage;

@SuppressWarnings("serial")
public abstract class AbstractFilePage extends ProjectCategoryPage {

	protected final IModel<String> revisionModel;
	protected final IModel<List<String>> pathsModel;
	
	public AbstractFilePage(PageParameters params) {
		super(params);
		
		revisionModel = new LoadableDetachableModel<String>() {

			@Override
			public String load() {
				PageParameters params = AbstractFilePage.this.getPageParameters();
				String objectId = params.get(PageSpec.OBJECT_ID).toString();
				String rev;
				if (Strings.isNullOrEmpty(objectId)) {
					String branchName = getProject().getDefaultBranchName();
					if (Strings.isNullOrEmpty(branchName)) {
						rev = "master";
					} else {
						rev = branchName;
					}
				} else {
					rev = objectId;
				}
				
				Git git = getProject().code();
				String hash = git.resolveRef(rev, false);
				if (hash == null) {
					throw new EntityNotFoundException("Ref " + rev + " doesn't exist");
				} else {
					return rev;
				}
			}
		};
		
		pathsModel = new LoadableDetachableModel<List<String>>() {

			@Override
			public List<String> load() {
				PageParameters params = AbstractFilePage.this.getPageParameters();
				int count = params.getIndexedCount();
				List<String> paths = Lists.newArrayList();
				for (int i = 0; i < count; i++) {
					String p = params.get(i).toString();
					if (!Strings.isNullOrEmpty(p)) {
						paths.add(p);
					}
				}
				
				return paths;
			}
		};
	}

	protected String getRevision() {
		return revisionModel.getObject();
	}
	
	protected List<String> getPaths() {
		return pathsModel.getObject();
	}
	
	@Override
	protected Category getCategory() {
		return Category.CODE;
	}

	@Override
	public void onDetach() {
		if (revisionModel != null) {
			revisionModel.detach();
		}
		
		if (pathsModel != null) {
			pathsModel.detach();
		}
		
		super.onDetach();
	}
}
