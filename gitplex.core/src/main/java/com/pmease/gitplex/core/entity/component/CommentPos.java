package com.pmease.gitplex.core.entity.component;

import java.io.Serializable;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.OptimisticLock;

@Embeddable
public class CommentPos implements Serializable {

	private static final long serialVersionUID = 1L;

	@OptimisticLock(excluded=true)
	@Column(nullable=false)
	private String commit;

	@OptimisticLock(excluded=true)
	private String path;
	
	@OptimisticLock(excluded=true)
	private TextRange range;

	public String getCommit() {
		return commit;
	}

	public void setCommit(String commit) {
		this.commit = commit;
	}

	@Nullable
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	@Nullable
	public TextRange getRange() {
		return range;
	}

	public void setRange(TextRange range) {
		this.range = range;
	}

	public CommentPos() {
	}
	
	public CommentPos(String commit, @Nullable String path, @Nullable TextRange mark) {
		this.commit = commit;
		this.path = path;
		this.range = mark;
	}
	
	@Override
	public String toString() {
		if (range != null) 
			return commit + ":" + path + ":" + range;
		else if (path != null)
			return commit + ":" + path;
		else
			return commit;
	}
	
	public static CommentPos fromString(String str) {
		String commit = StringUtils.substringBefore(str, ":");
		String path = null;
		TextRange mark = null;
		String pathAndMark = StringUtils.substringAfter(str, ":");
		if (pathAndMark.length() != 0) {
			path = StringUtils.substringBefore(pathAndMark, ":");
			String markStr = StringUtils.substringAfter(pathAndMark, ":");
			if (markStr.length() != 0)
				mark = new TextRange(markStr);
		}
		return new CommentPos(commit, path, mark);
	}
	
	public static CommentPos of(@Nullable String str) {
		if (str != null)
			return fromString(str);
		else
			return null;
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof CommentPos))
			return false;
		if (this == other)
			return true;
		CommentPos otherPos = (CommentPos) other;
		return new EqualsBuilder()
				.append(commit, otherPos.commit)
				.append(path, otherPos.path)
				.append(range, otherPos.range)
				.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
				.append(commit)
				.append(path)
				.append(range)
				.toHashCode();
	}
	
}
