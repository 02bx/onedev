package com.pmease.commons.git;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.google.common.base.Objects;

@SuppressWarnings("serial")
public class BlobInfo implements Serializable {

	private final String path;
	
	private final String revision;
	
	private final int mode;

	public BlobInfo(String revision, String path, int mode) {
		this.revision = revision;
		this.path = path;
		this.mode = mode;
	}
	
	public String getPath() {
		return path;
	}

	public String getRevision() {
		return revision;
	}

	public int getMode() {
		return mode;
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof BlobInfo)) 
			return false;
		if (this == other)
			return true;
		BlobInfo otherInfo = (BlobInfo) other;
		return new EqualsBuilder()
			.append(revision, otherInfo.getRevision())
			.append(path, otherInfo.getPath())
			.append(mode, otherInfo.mode)
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
	public String toString() {
		return Objects.toStringHelper(BlobInfo.class)
				.add("revision", revision)
				.add("path", path)
				.add("mode", mode)
				.toString(); 
	}

}
