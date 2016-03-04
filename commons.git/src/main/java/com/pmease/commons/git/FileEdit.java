package com.pmease.commons.git;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TreeFormatter;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.pmease.commons.git.exception.NotFileException;
import com.pmease.commons.git.exception.NotTreeException;
import com.pmease.commons.git.exception.ObjectAlreadyExistException;
import com.pmease.commons.git.exception.ObjectNotExistException;
import com.pmease.commons.git.exception.ObsoleteCommitException;
import com.pmease.commons.git.exception.RefUpdateException;

public class FileEdit implements Serializable {
	
	private static final long serialVersionUID = 1L;

	private final String oldPath;
	
	private final PathAndContent newFile;
	
	public FileEdit(@Nullable String oldPath, @Nullable PathAndContent newFile) {
		this.oldPath = GitUtils.normalizePath(oldPath);
		this.newFile = newFile;

		Preconditions.checkArgument(this.oldPath != null || this.newFile != null, 
				"Either oldPath or newFile should be defined");
	}

	public String getOldPath() {
		return oldPath;
	}

	public PathAndContent getNewFile() {
		return newFile;
	}

	private ObjectId insertTree(RevTree revTree, TreeWalk treeWalk, ObjectInserter inserter, 
			@Nullable String currentOldPath, @Nullable String currentNewPath, FileMode newFileMode) {
        try {
    		boolean newPathFound = false;
    		
    		List<TreeFormatterEntry> entries = new ArrayList<>();
    		while (treeWalk.next()) {
				String name = treeWalk.getNameString();
				if (name.equals(currentOldPath)) {
					if (name.equals(currentNewPath)) {
						newPathFound = true;
						ObjectId blobId = inserter.insert(Constants.OBJ_BLOB, newFile.getContent());
						entries.add(new TreeFormatterEntry(name, newFileMode, blobId));
					}
				} else if (name.equals(currentNewPath)) {
					throw new ObjectAlreadyExistException("Path already exist: " + treeWalk.getPathString());
				} else if (currentOldPath != null && currentOldPath.startsWith(name + "/")) {
					TreeWalk childTreeWalk = TreeWalk.forPath(treeWalk.getObjectReader(), treeWalk.getPathString(), revTree);
					Preconditions.checkNotNull(childTreeWalk);
					childTreeWalk.enterSubtree();
					String childOldPath = currentOldPath.substring(name.length()+1);
					String childNewPath;
					if (currentNewPath != null && currentNewPath.startsWith(name + "/")) {
						childNewPath = currentNewPath.substring(name.length()+1);
						newPathFound = true;
					} else {
						childNewPath = null;
					}
					ObjectId childTreeId = insertTree(revTree, childTreeWalk, inserter, 
							childOldPath, childNewPath, newFileMode);
					if (childTreeId != null) 
						entries.add(new TreeFormatterEntry(name, FileMode.TREE, childTreeId));
				} else if (currentNewPath != null && currentNewPath.startsWith(name + "/")) {
					if ((treeWalk.getFileMode(0).getBits() & FileMode.TYPE_TREE) == 0)
						throw new NotTreeException("Path does not represent a tree: " + treeWalk.getPathString());
					newPathFound = true;
					TreeWalk childTreeWalk = TreeWalk.forPath(treeWalk.getObjectReader(), 
							treeWalk.getPathString(), revTree);
					Preconditions.checkNotNull(childTreeWalk);
					childTreeWalk.enterSubtree();
					String childNewPath = currentNewPath.substring(name.length()+1);
					ObjectId childTreeId = insertTree(revTree, childTreeWalk, inserter, null, childNewPath, newFileMode);
					if (childTreeId != null)  
						entries.add(new TreeFormatterEntry(name, treeWalk.getFileMode(0), childTreeId));
				} else {
					entries.add(new TreeFormatterEntry(name, treeWalk.getFileMode(0), treeWalk.getObjectId(0)));
				}
			}
			
			if (currentNewPath != null && !newPathFound) {
				List<String> splitted = Splitter.on('/').splitToList(currentNewPath);
				
				ObjectId childId = null;
				FileMode childMode = null;
				String childName = null;
				
				for (int i=splitted.size()-1; i>=0; i--) {
					if (childId == null) {
						childName = splitted.get(i);
						childId = inserter.insert(Constants.OBJ_BLOB, newFile.getContent());
						childMode = newFileMode;
					} else {
						TreeFormatter childFormatter = new TreeFormatter();
						childFormatter.append(childName, childMode, childId);
						childName = splitted.get(i);
						childId = inserter.insert(childFormatter);
						childMode = FileMode.TREE;
					}
				}

				Preconditions.checkState(childId!=null && childMode != null && childName != null);
				entries.add(new TreeFormatterEntry(childName, childMode, childId));
			}
			if (!entries.isEmpty()) {
				TreeFormatter formatter = new TreeFormatter();
				Collections.sort(entries);
				for (TreeFormatterEntry entry: entries)
					formatter.append(entry.name, entry.mode, entry.id);
				return inserter.insert(formatter);
			} else {
				return null;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Commit specified file into specified repository.
	 * 
	 * @param repo 
	 * 			repository to make the new commit
	 * @param refName
	 * 			ref name to associate the new commit with
	 * @param expectedOldCommitId
	 * 			expected old commit id of above ref
	 * @param parentCommitId
	 * 			parent commit id of the new commit
	 * @param authorAndCommitter
	 * 			author and committer person ident for the new commit
	 * @param commitMessage
	 * 			commit message for the new commit
	 * @return 
	 * 			id of new commit
	 * @throws ObsoleteCommitException 
	 * 			if expected old commit id of the ref does not equal to 
	 * 			expectedOldCommitId, or if expectedOldCommitId is specified as <tt>null</tt> and 
	 * 			ref exists  
	 * @throws ObjectNotExistException 
	 * 			if file to delete does not exist when oldPath!=null&&newFile==null 
	 * @throws ObjectAlreadyExistException 
	 * 			if added/renamed file already exists when newFile!=null && (oldPath==null || !oldPath.equals(newFile.getPath()))
	 * 			 
	 */
	public ObjectId commit(Repository repo, String refName, 
			ObjectId expectedOldCommitId, ObjectId parentCommitId, 
			PersonIdent authorAndCommitter, String commitMessage) {
		
		try (	RevWalk revWalk = new RevWalk(repo); 
				TreeWalk treeWalk = new TreeWalk(repo);
				ObjectInserter inserter = repo.newObjectInserter();) {

	        CommitBuilder commit = new CommitBuilder();
	        
	        commit.setAuthor(authorAndCommitter);
	        commit.setCommitter(authorAndCommitter);
	        commit.setParentId(parentCommitId);
	        commit.setMessage(commitMessage);
	        
			RevTree revTree = revWalk.parseCommit(parentCommitId).getTree();
			treeWalk.addTree(revTree);

			FileMode newFileMode;
			if (oldPath != null) {
				TreeWalk oldPathTreeWalk = TreeWalk.forPath(repo, oldPath, revTree);
				if (oldPathTreeWalk == null)
					throw new ObjectNotExistException("Path not exist: " + oldPath);
				newFileMode = oldPathTreeWalk.getFileMode(0);
				
				if ((newFileMode.getBits() & FileMode.TYPE_FILE) == 0)
					throw new NotFileException("Path does not represent a file: " + oldPath);
			} else {
				newFileMode = FileMode.REGULAR_FILE;
			}

			String newPath;
			if (newFile != null)
				newPath = GitUtils.normalizePath(newFile.getPath());
			else
				newPath = null;
	        ObjectId treeId = insertTree(revTree, treeWalk, inserter, oldPath, newPath, newFileMode);
	        
	        if (treeId != null)
	        	commit.setTreeId(treeId);
	        else 
	        	commit.setTreeId(inserter.insert(new TreeFormatter()));
	        
	        ObjectId commitId = inserter.insert(commit);
	        inserter.flush();
	        RefUpdate ru = repo.updateRef(refName);
	        ru.setRefLogIdent(authorAndCommitter);
	        ru.setNewObjectId(commitId);
	        ru.setExpectedOldObjectId(expectedOldCommitId);
	        RefUpdate.Result result = ru.update();
	        if (result == RefUpdate.Result.LOCK_FAILURE 
	        		&& !expectedOldCommitId.equals(ru.getOldObjectId())) {
	        	throw new ObsoleteCommitException(ru.getOldObjectId());
	        } else if (result != RefUpdate.Result.FAST_FORWARD) {
	        	throw new RefUpdateException(result);
	        } else {
	        	return commitId;
	        }
		} catch (RevisionSyntaxException | IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static class TreeFormatterEntry implements Comparable<TreeFormatterEntry> {

		String name;
		
		FileMode mode;
		
		ObjectId id;
		
		public TreeFormatterEntry(String name, FileMode mode, ObjectId id) {
			this.name = name;
			this.mode = mode;
			this.id = id;
		}

		@Override
		public int compareTo(TreeFormatterEntry entry) {
			return name.compareTo(entry.name);
		}

	}
}
