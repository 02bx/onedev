package com.pmease.commons.git;

import java.io.Serializable;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.eclipse.jgit.lib.FileMode;

public class BlobIdent implements Serializable, Comparable<BlobIdent> {
	
	private static final long serialVersionUID = 1L;

	@Nullable
	public String revision;
	
	@Nullable
	public String path;
	
	@Nullable
	public Integer mode;
	
	public BlobIdent() {
	}
	
	public BlobIdent(@Nullable String revision, @Nullable String path, @Nullable Integer mode) {
		this.revision = revision;
		this.path = path;
		this.mode = mode;
	}
	
	public boolean isTree() {
		return (FileMode.TYPE_MASK & mode) == FileMode.TYPE_TREE;
	}
	
	public boolean isGitLink() {
		return (FileMode.TYPE_MASK & mode) == FileMode.TYPE_GITLINK;
	}
	
	public boolean isSymbolLink() {
		return (FileMode.TYPE_MASK & mode) == FileMode.TYPE_SYMLINK;
	}

	public boolean isFile() {
		return (FileMode.TYPE_MASK & mode) == FileMode.TYPE_FILE;
	}
	
	public String getName() {
		if (path != null) {
			if (path.contains("/"))
				return StringUtils.substringAfterLast(path, "/");
			else
				return path;
		} else {
			return null;
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof BlobIdent)) 
			return false;
		if (this == obj)
			return true;
		BlobIdent otherIdent = (BlobIdent) obj;
		return new EqualsBuilder()
			.append(revision, otherIdent.revision)
			.append(path, otherIdent.path)
			.append(mode, otherIdent.mode)
			.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
			.append(revision)
			.append(path)
			.append(mode)
			.toHashCode();
	}		

	@Override
	public int compareTo(BlobIdent ident) {
		if (isTree()) {
			if (ident.isTree()) 
				return GitUtils.comparePath(path, ident.path);
			else
				return -1;
		} else if (ident.isTree()) {
			return 1;
		} else {
			return GitUtils.comparePath(path, ident.path);
		}
	}

}