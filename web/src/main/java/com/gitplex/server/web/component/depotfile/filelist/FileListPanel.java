package com.gitplex.server.web.component.depotfile.filelist;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

import com.google.common.base.Preconditions;
import com.gitplex.server.git.Blob;
import com.gitplex.server.git.BlobIdent;
import com.gitplex.server.model.Depot;
import com.gitplex.server.security.SecurityUtils;
import com.gitplex.server.web.component.PreventDefaultAjaxLink;
import com.gitplex.server.web.component.depotfile.blobview.BlobViewContext.Mode;
import com.gitplex.server.web.component.markdown.MarkdownPanel;
import com.gitplex.server.web.page.depot.file.DepotFilePage;

@SuppressWarnings("serial")
public abstract class FileListPanel extends Panel {

	public static final String README_NAME = "readme";
	
	private final IModel<Depot> depotModel;
	
	private final BlobIdent directory;
	
	private final IModel<List<BlobIdent>> childrenModel = new LoadableDetachableModel<List<BlobIdent>>() {

		@Override
		protected List<BlobIdent> load() {
			Repository repository = depotModel.getObject().getRepository();			
			try (RevWalk revWalk = new RevWalk(repository)) {
				RevTree revTree = revWalk.parseCommit(getCommitId()).getTree();
				TreeWalk treeWalk;
				if (directory.path != null) {
					treeWalk = Preconditions.checkNotNull(TreeWalk.forPath(repository, directory.path, revTree));
					treeWalk.enterSubtree();
				} else {
					treeWalk = new TreeWalk(repository);
					treeWalk.addTree(revTree);
				}
				List<BlobIdent> children = new ArrayList<>();
				while (treeWalk.next())
					children.add(new BlobIdent(directory.revision, treeWalk.getPathString(), treeWalk.getRawMode(0)));
				for (int i=0; i<children.size(); i++) {
					BlobIdent child = children.get(i);
					while (child.isTree()) {
						treeWalk = TreeWalk.forPath(repository, child.path, revTree);
						Preconditions.checkNotNull(treeWalk);
						treeWalk.enterSubtree();
						if (treeWalk.next()) {
							BlobIdent grandChild = new BlobIdent(directory.revision, 
									treeWalk.getPathString(), treeWalk.getRawMode(0));
							if (treeWalk.next() || !grandChild.isTree()) 
								break;
							else
								child = grandChild;
						} else {
							break;
						}
					}
					children.set(i, child);
				}
				
				Collections.sort(children);
				return children;
			} catch (IOException e) {
				throw new RuntimeException(e);
			} 
		}
		
	};
	
	private final IModel<BlobIdent> readmeModel = new LoadableDetachableModel<BlobIdent>() {

		@Override
		protected BlobIdent load() {
			for (BlobIdent blobIdent: childrenModel.getObject()) {
				String readme = StringUtils.substringBefore(blobIdent.getName(), ".");
				if (readme.equalsIgnoreCase(README_NAME))
					return blobIdent;
			}
			return null;
		}
		
	};
	
	public FileListPanel(String id, IModel<Depot> depotModel, BlobIdent directory) {
		super(id);

		this.depotModel = depotModel;
		this.directory = directory;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();

		WebMarkupContainer parent = new WebMarkupContainer("parent") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(directory.path != null);
			}
			
		};
		
		final BlobIdent parentIdent;
		if (directory.path == null) {
			parentIdent = null;
		} else if (directory.path.indexOf('/') != -1) {
			parentIdent = new BlobIdent(
					directory.revision, 
					StringUtils.substringBeforeLast(directory.path, "/"), 
					FileMode.TREE.getBits());
		} else {
			parentIdent = new BlobIdent(directory.revision, null, FileMode.TREE.getBits());
		}
		parent.add(new PreventDefaultAjaxLink<Void>("link") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				onSelect(target, parentIdent);
			}

			@Override
			protected void onComponentTag(ComponentTag tag) {
				super.onComponentTag(tag);
				
				DepotFilePage.State state = new DepotFilePage.State();
				state.blobIdent = parentIdent;
				PageParameters params = DepotFilePage.paramsOf(depotModel.getObject(), state); 
				tag.put("href", urlFor(DepotFilePage.class, params));
			}
			
		});
		add(parent);
		
		add(new ListView<BlobIdent>("children", childrenModel) {

			@Override
			protected void populateItem(ListItem<BlobIdent> item) {
				final BlobIdent blobIdent = item.getModelObject();
				
				WebMarkupContainer pathIcon = new WebMarkupContainer("pathIcon");
				String iconClass;
				if (blobIdent.isTree())
					iconClass = "fa fa-folder-o";
				else if (blobIdent.isGitLink()) 
					iconClass = "fa fa-ext fa-folder-submodule-o";
				else if (blobIdent.isSymbolLink()) 
					iconClass = "fa fa-ext fa-folder-symbol-link-o";
				else  
					iconClass = "fa fa-file-text-o";
				pathIcon.add(AttributeModifier.append("class", iconClass));
				
				item.add(pathIcon);
				
				AjaxLink<Void> pathLink = new PreventDefaultAjaxLink<Void>("pathLink") {

					@Override
					protected void onComponentTag(ComponentTag tag) {
						super.onComponentTag(tag);
						
						DepotFilePage.State state = new DepotFilePage.State();
						state.blobIdent = blobIdent;
						PageParameters params = DepotFilePage.paramsOf(depotModel.getObject(), state); 
						tag.put("href", urlFor(DepotFilePage.class, params));
					}

					@Override
					public void onClick(AjaxRequestTarget target) {
						onSelect(target, blobIdent);
					}
					
				}; 
				
				if (directory.path != null)
					pathLink.add(new Label("label", blobIdent.path.substring(directory.path.length()+1)));
				else
					pathLink.add(new Label("label", blobIdent.path));
				item.add(pathLink);
				
				if (item.getIndex() == 0)
					item.add(new Label("lastCommit", "Loading last commit info..."));
				else
					item.add(new Label("lastCommit"));
			}
			
		});
		
		WebMarkupContainer readmeContainer = new WebMarkupContainer("readme") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				
				setVisible(readmeModel.getObject() != null);
			}
			
		};
		readmeContainer.add(new Label("title", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return readmeModel.getObject().getName();
			}
			
		}));
		
		readmeContainer.add(new AjaxLink<Void>("edit") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				DepotFilePage.State state = new DepotFilePage.State();
				state.blobIdent = readmeModel.getObject();
				state.mode = Mode.EDIT;
				setResponsePage(DepotFilePage.class, DepotFilePage.paramsOf(depotModel.getObject(), state));
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				Depot depot = depotModel.getObject();
				BlobIdent blobIdent = readmeModel.getObject();
				setVisible(depot.getBranchRef(blobIdent.revision) != null 
						&& SecurityUtils.canModify(depotModel.getObject(), blobIdent.revision, blobIdent.path));
			}
			
		});
		readmeContainer.add(new MarkdownPanel("body", new LoadableDetachableModel<String>() {

			@Override
			protected String load() {
				Blob blob = depotModel.getObject().getBlob(readmeModel.getObject());
				Blob.Text text = blob.getText();
				if (text != null)
					return text.getContent();
				else
					return "This seems like a binary file!";
			}
			
		}, null));
		
		add(readmeContainer);
		
		setOutputMarkupId(true);
	}
	
	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		
		response.render(JavaScriptHeaderItem.forReference(new FileListResourceReference()));

		PageParameters params = LastCommitsResource.paramsOf(depotModel.getObject(), 
				directory.revision, directory.path); 
		String lastCommitsUrl = urlFor(new LastCommitsResourceReference(), params).toString();
		response.render(OnDomReadyHeaderItem.forScript(
				String.format("gitplex.server.filelist.init('%s', '%s')", getMarkupId(), lastCommitsUrl)));
	}

	protected abstract void onSelect(AjaxRequestTarget target, BlobIdent blobIdent);
	
	private ObjectId getCommitId() {
		return depotModel.getObject().getObjectId(directory.revision);
	}

	@Override
	protected void onDetach() {
		childrenModel.detach();
		readmeModel.detach();		
		depotModel.detach();
		
		super.onDetach();
	}

}
