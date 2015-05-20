package com.pmease.gitplex.web.page.repository;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import com.pmease.commons.wicket.component.tabbable.PageTab;

@SuppressWarnings("serial")
public class RepoTab extends PageTab {

	public RepoTab(IModel<String> titleModel, Class<? extends RepositoryPage> mainPageClass) {
		super(titleModel, mainPageClass);
	}

	public RepoTab(IModel<String> titleModel, Class<? extends RepositoryPage> mainPageClass, 
			Class<? extends RepositoryPage> additionalPageClass1) {
		super(titleModel, mainPageClass, additionalPageClass1);
	}
	
	public RepoTab(IModel<String> titleModel, 
			Class<? extends RepositoryPage> mainPageClass, 
			Class<? extends RepositoryPage> additionalPageClass1, 
			Class<? extends RepositoryPage> additionalPageClass2) {
		super(titleModel, mainPageClass, additionalPageClass1, additionalPageClass2);
	}
	
	public RepoTab(IModel<String> titleModel, 
			Class<? extends RepositoryPage> mainPageClass, 
			Class<? extends RepositoryPage> additionalPageClass1, 
			Class<? extends RepositoryPage> additionalPageClass2,
			Class<? extends RepositoryPage> additionalPageClass3) {
		super(titleModel, mainPageClass, additionalPageClass1, additionalPageClass2, additionalPageClass3);
	}
	
	public RepoTab(IModel<String> titleModel, 
			Class<? extends RepositoryPage> mainPageClass, 
			Class<? extends RepositoryPage> additionalPageClass1, 
			Class<? extends RepositoryPage> additionalPageClass2,
			Class<? extends RepositoryPage> additionalPageClass3,
			Class<? extends RepositoryPage> additionalPageClass4) {
		super(titleModel, mainPageClass, additionalPageClass1, additionalPageClass2, 
				additionalPageClass3, additionalPageClass4);
	}
	
	public RepoTab(IModel<String> titleModel, 
			Class<? extends RepositoryPage> mainPageClass, 
			Class<? extends RepositoryPage> additionalPageClass1, 
			Class<? extends RepositoryPage> additionalPageClass2,
			Class<? extends RepositoryPage> additionalPageClass3,
			Class<? extends RepositoryPage> additionalPageClass4, 
			Class<? extends RepositoryPage> additionalPageClass5) {
		super(titleModel, mainPageClass, additionalPageClass1, additionalPageClass2, 
				additionalPageClass3, additionalPageClass4, additionalPageClass5);
	}
	
	@Override
	public Component render(String componentId) {
		return new RepoTabLink(componentId, this);
	}

}
