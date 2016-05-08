package com.pmease.gitplex.web.component.depotfile.blobview;

import java.io.Serializable;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.wicket.ajax.AjaxRequestTarget;

import com.pmease.commons.git.BlobIdent;
import com.pmease.commons.lang.extractors.TokenPosition;
import com.pmease.commons.wicket.component.menu.MenuItem;
import com.pmease.commons.wicket.component.menu.MenuLink;
import com.pmease.gitplex.core.entity.Depot;
import com.pmease.gitplex.core.entity.PullRequest;
import com.pmease.gitplex.search.hit.QueryHit;
import com.pmease.gitplex.web.page.depot.file.Mark;

public interface BlobViewContext extends Serializable {

	public enum Mode {BLAME, EDIT, DELETE}
	
	Depot getDepot();

	@Nullable
	PullRequest getPullRequest();
	
	BlobIdent getBlobIdent();
	
	@Nullable
	Mark getMark();
	
	Mode getMode();
	
	boolean isOnBranch();
	
	boolean isAtSourceBranchHead();
	
	void onSelect(AjaxRequestTarget target, BlobIdent blobIdent, @Nullable TokenPosition tokenPos);
	
	void onSearchComplete(AjaxRequestTarget target, List<QueryHit> hits);
	
	void onBlameChange(AjaxRequestTarget target, @Nullable String viewState);
	
	void onDelete(AjaxRequestTarget target);
	
	void onEdit(AjaxRequestTarget target, @Nullable String viewState);

	List<MenuItem> getMenuItems(MenuLink menuLink);
	
}
