package com.pmease.gitop.web.page.repository.source;

import java.util.List;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.pmease.gitop.model.Repository;
import com.pmease.gitop.web.page.repository.RepositoryPage;
import com.pmease.gitop.web.page.repository.RepositoryInfoPage;
import com.pmease.gitop.web.page.repository.api.IRevisionAware;

@SuppressWarnings("serial")
public abstract class AbstractFilePage extends RepositoryInfoPage implements IRevisionAware {

	protected final IModel<List<String>> pathsModel;
	
	public static PageParameters paramsOf(Repository repository, String revision, List<String> paths) {
		PageParameters params = paramsOf(repository);
		params.set(RepositoryPage.PARAM_OBJECT_ID, revision);
		for (int i = 0; i < paths.size(); i++) {
			params.set(i, paths.get(i));
		}
		
		return params;
	}
	
	public AbstractFilePage(PageParameters params) {
		super(params);
		
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
	
	protected List<String> getPaths() {
		return pathsModel.getObject();
	}
	
	@Override
	public void onDetach() {
		if (pathsModel != null) {
			pathsModel.detach();
		}
		
		super.onDetach();
	}
}
