package com.pmease.gitplex.web.component.filenavigator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider;
import org.apache.wicket.extensions.markup.html.repeater.tree.NestedTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.theme.HumanTheme;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.pmease.commons.git.BlobIdent;
import com.pmease.commons.git.Git;
import com.pmease.commons.util.StringUtils;
import com.pmease.commons.wicket.behavior.dropdown.DropdownBehavior;
import com.pmease.commons.wicket.behavior.dropdown.DropdownPanel;
import com.pmease.gitplex.core.model.Repository;
import com.pmease.gitplex.web.component.blobview.BlobNameChangeCallback;
import com.pmease.gitplex.web.page.repository.file.RepoFilePage;

@SuppressWarnings("serial")
public abstract class FileNavigator extends Panel {

	private final static String LAST_SEGMENT_ID = "lastSegment";
	
	private final IModel<Repository> repoModel;
	
	private final BlobIdent file;
	
	private final BlobNameChangeCallback callback;
	
	public FileNavigator(String id, IModel<Repository> repoModel, BlobIdent file, 
			@Nullable BlobNameChangeCallback callback) {
		super(id);

		this.repoModel = repoModel;
		this.file = file;
		this.callback = callback;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();

		add(new ListView<BlobIdent>("treeSegments", new LoadableDetachableModel<List<BlobIdent>>() {

			@Override
			protected List<BlobIdent> load() {
				List<BlobIdent> treeSegments = new ArrayList<>();
				treeSegments.add(new BlobIdent(file.revision, null, FileMode.TREE.getBits()));
				
				if (file.path != null) {
					List<String> segments = Splitter.on('/').omitEmptyStrings().splitToList(file.path);
					
					for (int i=0; i<segments.size(); i++) { 
						BlobIdent parent = treeSegments.get(treeSegments.size()-1);
						int treeMode = FileMode.TREE.getBits();
						if (i<segments.size()-1 || file.mode == treeMode) {
							String treePath = parent.path + "/" + segments.get(i);
							treeSegments.add(new BlobIdent(file.revision, treePath, treeMode));
						}
					}
				}
				
				return treeSegments;
			}
			
		}) {

			@Override
			protected void populateItem(final ListItem<BlobIdent> item) {
				final BlobIdent blobIdent = item.getModelObject();
				AjaxLink<Void> link = new AjaxLink<Void>("link") {

					@Override
					public void onClick(AjaxRequestTarget target) {
						onSelect(target, blobIdent);
					}

					@Override
					protected void onComponentTag(ComponentTag tag) {
						super.onComponentTag(tag);
						PageParameters params = RepoFilePage.paramsOf(repoModel.getObject(), blobIdent);
						tag.put("href", urlFor(RepoFilePage.class, params));
					}
					
				};
				link.setEnabled(item.getIndex() != getViewSize()-1);
				
				if (blobIdent.path != null) {
					if (blobIdent.path.indexOf('/') != -1)
						link.add(new Label("label", StringUtils.substringAfterLast(blobIdent.path, "/")));
					else
						link.add(new Label("label", blobIdent.path));
				} else {
					link.add(new Label("label", repoModel.getObject().getName()));
				}
				
				item.add(link);
				
				WebMarkupContainer subtreeDropdownTrigger = new WebMarkupContainer("subtreeDropdownTrigger");
				
				DropdownPanel subtreeDropdown = new DropdownPanel("subtreeDropdown", true) {

					@Override
					protected Component newContent(String id) {
						return new NestedTree<BlobIdent>(id, new ITreeProvider<BlobIdent>() {

							@Override
							public void detach() {
							}

							@Override
							public Iterator<? extends BlobIdent> getRoots() {
								try (	FileRepository jgitRepo = repoModel.getObject().openAsJGitRepo();
										RevWalk revWalk = new RevWalk(jgitRepo)) {
									RevTree revTree = revWalk.parseCommit(getCommitId()).getTree();
									TreeWalk treeWalk;
									if (blobIdent.path != null) {
										treeWalk = Preconditions.checkNotNull(TreeWalk.forPath(jgitRepo, blobIdent.path, revTree));
										treeWalk.enterSubtree();
									} else {
										treeWalk = new TreeWalk(jgitRepo);
										treeWalk.addTree(revTree);
									}
									treeWalk.setRecursive(false);
									
									List<BlobIdent> roots = new ArrayList<>();
									while (treeWalk.next()) 
										roots.add(new BlobIdent(file.revision, treeWalk.getPathString(), treeWalk.getRawMode(0)));
									Collections.sort(roots);
									return roots.iterator();
								} catch (IOException e) {
									throw new RuntimeException(e);
								}
							}

							@Override
							public boolean hasChildren(BlobIdent blobIdent) {
								return blobIdent.isTree();
							}

							@Override
							public Iterator<? extends BlobIdent> getChildren(BlobIdent blobIdent) {
								return FileNavigator.this.getChildren(blobIdent).iterator();
							}

							@Override
							public IModel<BlobIdent> model(BlobIdent blobIdent) {
								return Model.of(blobIdent);
							}
							
						}) {

							@Override
							protected void onInitialize() {
								super.onInitialize();
								add(new HumanTheme());				
							}

							@Override
							public void expand(BlobIdent blobIdent) {
								super.expand(blobIdent);
								
								List<BlobIdent> children = getChildren(blobIdent);
								if (children.size() == 1 && children.get(0).isTree()) 
									expand(children.get(0));
							}

							@Override
							protected Component newContentComponent(String id, final IModel<BlobIdent> model) {
								BlobIdent blobIdent = model.getObject();
								Fragment fragment = new Fragment(id, "treeNodeFrag", FileNavigator.this);
								
								WebMarkupContainer icon = new WebMarkupContainer("icon");
								String iconClass;
								if (blobIdent.isTree())
									iconClass = "fa fa-folder-o";
								else if (blobIdent.isGitLink()) 
									iconClass = "fa fa-ext fa-folder-submodule-o";
								else if (blobIdent.isSymbolLink()) 
									iconClass = "fa fa-ext fa-folder-symbol-link-o";
								else  
									iconClass = "fa fa-file-text-o";
								
								icon.add(AttributeModifier.append("class", iconClass));
								fragment.add(icon);
								
								AjaxLink<Void> link = new AjaxLink<Void>("link") {

									@Override
									public void onClick(AjaxRequestTarget target) {
										onSelect(target, model.getObject());
										hide(target);
									}

									@Override
									protected void onComponentTag(ComponentTag tag) {
										super.onComponentTag(tag);
										
										PageParameters params = RepoFilePage.paramsOf(repoModel.getObject(), model.getObject());
										tag.put("href", urlFor(RepoFilePage.class, params));
									}
									
								};
								if (blobIdent.path.indexOf('/') != -1)
									link.add(new Label("label", StringUtils.substringAfterLast(blobIdent.path, "/")));
								else
									link.add(new Label("label", blobIdent.path));
								fragment.add(link);
								
								return fragment;
							}
							
						};		
					}
					
				};
				item.add(subtreeDropdown);
				subtreeDropdownTrigger.add(new DropdownBehavior(subtreeDropdown));
				item.add(subtreeDropdownTrigger);
			}
			
		});
		
		WebMarkupContainer lastSegment;
		if (callback != null) {
			lastSegment = new Fragment(LAST_SEGMENT_ID, "nameEditFrag", this);
			final TextField<String> nameInput = new TextField<String>("name");
			lastSegment.add(nameInput);
			nameInput.add(new OnChangeAjaxBehavior() {

				@Override
				protected void onUpdate(AjaxRequestTarget target) {
					callback.onChange(target, nameInput.getInput());
				}
				
			});
		} else if (file.isTree()) {
			lastSegment = new Fragment(LAST_SEGMENT_ID, "addFileFrag", this) {

				@Override
				protected void onComponentTag(ComponentTag tag) {
					super.onComponentTag(tag);
					
					if (!repoModel.getObject().getRefs(Git.REFS_HEADS).containsKey(file.revision))
						tag.put("title", "Must on a branch to add or propose add of a file");
				}
				
			};
			lastSegment.add(new AjaxLink<Void>("addFile") {

				@Override
				public void onClick(AjaxRequestTarget target) {
					
				}
				
				@Override
				protected void onComponentTag(ComponentTag tag) {
					super.onComponentTag(tag);
					
					if (!repoModel.getObject().getRefs(Git.REFS_HEADS).containsKey(file.revision))
						tag.put("disabled", "disabled");
				}
				
			});
		} else {
			lastSegment = new Fragment(LAST_SEGMENT_ID, "blobNameFrag", this);
			lastSegment.add(new Label("label", StringUtils.substringAfterLast(file.path, "/")));
		}
		add(lastSegment);
		
		setOutputMarkupId(true);
	}
	
	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new CssResourceReference(FileNavigator.class, "file-navigator.css")));
	}

	private ObjectId getCommitId() {
		return repoModel.getObject().getObjectId(file.revision, true);
	}

	protected abstract void onSelect(AjaxRequestTarget target, BlobIdent blobIdent);
	
	private List<BlobIdent> getChildren(BlobIdent blobIdent) {
		try (	FileRepository jgitRepo = repoModel.getObject().openAsJGitRepo(); 
				RevWalk revWalk = new RevWalk(jgitRepo)) {
			RevTree revTree = revWalk.parseCommit(getCommitId()).getTree();
			TreeWalk treeWalk = TreeWalk.forPath(jgitRepo, blobIdent.path, revTree);
			treeWalk.setRecursive(false);
			treeWalk.enterSubtree();
			
			List<BlobIdent> children = new ArrayList<>();
			while (treeWalk.next()) 
				children.add(new BlobIdent(file.revision, treeWalk.getPathString(), treeWalk.getRawMode(0)));
			Collections.sort(children);
			return children;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void onDetach() {
		repoModel.detach();
		
		super.onDetach();
	}
	
}
