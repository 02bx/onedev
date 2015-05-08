package com.pmease.gitplex.web.component.pathnavigator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider;
import org.apache.wicket.extensions.markup.html.repeater.tree.NestedTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.theme.HumanTheme;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.CssResourceReference;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.pmease.commons.git.GitPath;
import com.pmease.commons.util.StringUtils;
import com.pmease.commons.wicket.behavior.dropdown.DropdownBehavior;
import com.pmease.commons.wicket.behavior.dropdown.DropdownMode;
import com.pmease.commons.wicket.behavior.dropdown.DropdownPanel;
import com.pmease.gitplex.core.model.Repository;

@SuppressWarnings("serial")
public class PathNavigator extends GenericPanel<String> {

	private final IModel<Repository> repoModel;
	
	private final IModel<String> revModel;
	
	public PathNavigator(String id, IModel<Repository> repoModel, IModel<String> revModel, IModel<String> pathModel) {
		super(id, pathModel);

		this.repoModel = repoModel;
		this.revModel = revModel;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();

		add(new ListView<String>("paths", new LoadableDetachableModel<List<String>>() {

			@Override
			protected List<String> load() {
				List<String> paths = new ArrayList<>();
				paths.add("");
				
				String path = PathNavigator.this.getModelObject();
				if (path != null) {
					for (String segment: Splitter.on('/').omitEmptyStrings().split(path)) { 
						String parent = paths.get(paths.size()-1);
						if (parent.length() != 0)
							paths.add(parent + "/" + segment);
						else
							paths.add(segment);
					}
				}
				
				return paths;
			}
			
		}) {

			@Override
			protected void populateItem(final ListItem<String> item) {
				final String path = item.getModelObject();
				
				AjaxLink<Void> link = new AjaxLink<Void>("link") {

					@Override
					public void onClick(AjaxRequestTarget target) {
						selectPath(target, path);
					}
					
				};
				link.setEnabled(item.getIndex() != getViewSize()-1);
				
				if (path.length() != 0) {
					if (path.indexOf('/') != -1)
						link.add(new Label("label", StringUtils.substringAfterLast(path, "/")));
					else
						link.add(new Label("label", path));
				} else {
					link.add(new Label("label", repoModel.getObject().getName()));
				}
				
				item.add(link);
				
				WebMarkupContainer subtreeDropdownTrigger = new WebMarkupContainer("subtreeDropdownTrigger");
				
				if (path.length() != 0 && item.getIndex() == size()-1) {
					org.eclipse.jgit.lib.Repository jgitRepo = repoModel.getObject().openAsJGitRepo();
					try {
						RevTree revTree = new RevWalk(jgitRepo).parseCommit(getCommitId()).getTree();
						TreeWalk treeWalk = TreeWalk.forPath(jgitRepo, path, revTree);
						if (!treeWalk.isSubtree())
							subtreeDropdownTrigger.setVisible(false);
					} catch (IOException e) {
						throw new RuntimeException(e);
					} finally {
						jgitRepo.close();
					}
				}
				
				DropdownPanel subtreeDropdown = new DropdownPanel("subtreeDropdown", true) {

					@Override
					protected Component newContent(String id) {
						return new NestedTree<GitPath>(id, new ITreeProvider<GitPath>() {

							@Override
							public void detach() {
							}

							@Override
							public Iterator<? extends GitPath> getRoots() {
								org.eclipse.jgit.lib.Repository jgitRepo = repoModel.getObject().openAsJGitRepo();
								try {
									RevTree revTree = new RevWalk(jgitRepo).parseCommit(getCommitId()).getTree();
									TreeWalk treeWalk;
									if (path.length() != 0) {
										treeWalk = TreeWalk.forPath(jgitRepo, path, revTree);
										treeWalk.enterSubtree();
									} else {
										treeWalk = new TreeWalk(jgitRepo);
										treeWalk.addTree(revTree);
									}
									treeWalk.setRecursive(false);
									
									List<GitPath> roots = new ArrayList<>();
									while (treeWalk.next()) 
										roots.add(new GitPath(treeWalk.getPathString(), treeWalk.getRawMode(0)));
									Collections.sort(roots);
									return roots.iterator();
								} catch (IOException e) {
									throw new RuntimeException(e);
								} finally {
									jgitRepo.close();
								}
							}

							@Override
							public boolean hasChildren(GitPath path) {
								return FileMode.TREE.equals(path.getMode());
							}

							@Override
							public Iterator<? extends GitPath> getChildren(GitPath path) {
								return PathNavigator.this.getChildren(path).iterator();
							}

							@Override
							public IModel<GitPath> model(GitPath path) {
								return Model.of(path);
							}
							
						}) {

							@Override
							protected void onInitialize() {
								super.onInitialize();
								add(new HumanTheme());				
							}

							@Override
							public void expand(GitPath pathInfo) {
								super.expand(pathInfo);
								
								List<GitPath> children = getChildren(pathInfo);
								if (children.size() == 1 && FileMode.TREE.equals(children.get(0).getMode())) 
									expand(children.get(0));
							}

							@Override
							protected Component newContentComponent(String id, final IModel<GitPath> model) {
								GitPath pathInfo = model.getObject();
								Fragment fragment = new Fragment(id, "treeNodeFrag", PathNavigator.this);
								
								WebMarkupContainer icon = new WebMarkupContainer("icon");
								String iconClass;
								if (FileMode.TREE.equals(pathInfo.getMode()))
									iconClass = "fa fa-folder-o";
								else if (FileMode.GITLINK.equals(pathInfo.getMode())) 
									iconClass = "fa fa-ext fa-submodule-o";
								else if (FileMode.SYMLINK.equals(pathInfo.getMode())) 
									iconClass = "fa fa-ext fa-symbol-link";
								else  
									iconClass = "fa fa-file-text-o";
								
								icon.add(AttributeModifier.append("class", iconClass));
								fragment.add(icon);
								
								AjaxLink<Void> link = new AjaxLink<Void>("link") {

									@Override
									public void onClick(AjaxRequestTarget target) {
										selectPath(target, model.getObject().getName());
										hide(target);
									}
									
								};
								if (pathInfo.getName().indexOf('/') != -1)
									link.add(new Label("label", StringUtils.substringAfterLast(pathInfo.getName(), "/")));
								else
									link.add(new Label("label", pathInfo.getName()));
								fragment.add(link);
								
								return fragment;
							}
							
						};		
					}
					
				};
				item.add(subtreeDropdown);
				subtreeDropdownTrigger.add(new DropdownBehavior(subtreeDropdown).mode(new DropdownMode.Hover()));
				item.add(subtreeDropdownTrigger);
			}
			
		});
		
		setOutputMarkupId(true);
	}
	
	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new CssResourceReference(PathNavigator.class, "path-navigator.css")));
	}

	private ObjectId getCommitId() {
		return Preconditions.checkNotNull(repoModel.getObject().resolveRevision(revModel.getObject()));
	}

	private void selectPath(AjaxRequestTarget target, String path) {
		if (path.length() == 0)
			path = null;
		
		setModelObject(path);
		target.add(this);
	}
	
	private List<GitPath> getChildren(GitPath pathInfo) {
		org.eclipse.jgit.lib.Repository jgitRepo = repoModel.getObject().openAsJGitRepo();
		try {
			RevTree revTree = new RevWalk(jgitRepo).parseCommit(getCommitId()).getTree();
			TreeWalk treeWalk = TreeWalk.forPath(jgitRepo, pathInfo.getName(), revTree);
			treeWalk.setRecursive(false);
			treeWalk.enterSubtree();
			
			List<GitPath> children = new ArrayList<>();
			while (treeWalk.next()) 
				children.add(new GitPath(treeWalk.getPathString(), treeWalk.getRawMode(0)));
			Collections.sort(children);
			return children;
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			jgitRepo.close();
		}
	}

	@Override
	protected void onDetach() {
		repoModel.detach();
		revModel.detach();
		
		super.onDetach();
	}
	
}
