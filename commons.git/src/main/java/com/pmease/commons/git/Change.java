package com.pmease.commons.git;

import java.io.Serializable;
import java.util.StringTokenizer;

import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.util.QuotedString;

import com.google.common.base.Preconditions;

@SuppressWarnings("serial")
public class Change implements Comparable<Change>, Serializable {

	public enum Status {ADDED, MODIFIED, DELETED, RENAMED, UNCHANGED}
	
	private final Status status;
	
	private final String oldPath;
	
	private final String newPath;
	
	private final int oldMode;
	
	private final int newMode;
	
	public Change(Status status, String oldPath, String newPath, int oldMode, int newMode) {
		this.status = status;
		this.oldPath = oldPath;
		this.newPath = newPath;
		this.oldMode = oldMode;
		this.newMode = newMode;
	}
	
	public Change(Change change) {
		this.status = change.status;
		this.oldPath = change.oldPath;
		this.newPath = change.newPath;
		this.oldMode = change.oldMode;
		this.newMode = change.newMode;
	}

	public String getOldPath() {
		return oldPath;
	}
	
	public String getNewPath() {
		return newPath;
	}

	public int getOldMode() {
		return oldMode;
	}

	public int getNewMode() {
		return newMode;
	}

	public Status getStatus() {
		return status;
	}

	@Override
	public String toString() {
		if (status == Status.RENAMED)
			return status.name() + "\t" + oldPath + "->" + newPath;
		else if (status == Status.DELETED)
			return status.name() + "\t" + oldPath;
		else 
			return status.name() + "\t" + newPath;
	}
	
	public boolean isFolder() {
		return oldMode == FileMode.TYPE_TREE || newMode == FileMode.TYPE_TREE;
	}
	
	@Override
	public int compareTo(Change other) {
		if (isFolder()) {
			if (other.isFolder())
				return getPath().compareTo(other.getPath());
			else
				return -1;
		} else if (other.isFolder()) {
			return 1;
		} else {
			return getPath().compareTo(other.getPath());
		}
	}
	
	public String getPath() {
		if (status == Status.DELETED)
			return oldPath;
		else
			return newPath;
	}

	private static String dequoteFileName(String quotedFileName) {
		return QuotedString.GIT_PATH.dequote(quotedFileName);
	}

	public static Change parseRawLine(String rawLine) {
		Preconditions.checkArgument(rawLine.startsWith(":"));
		
		StringTokenizer tokenizer = new StringTokenizer(rawLine.substring(1));
		int oldMode = Integer.parseInt(tokenizer.nextToken(), 8);
		int newMode = Integer.parseInt(tokenizer.nextToken(), 8);
		tokenizer.nextToken();
		tokenizer.nextToken();
		String statusCode = tokenizer.nextToken();
		if (statusCode.startsWith("R")) {
			String oldPath = dequoteFileName(tokenizer.nextToken("\t"));
			String newPath = dequoteFileName(tokenizer.nextToken("\t"));
			return new Change(Change.Status.RENAMED, oldPath, newPath, oldMode, newMode);
		} else if (statusCode.equals("M") || statusCode.equals("T")) {
			String oldPath, newPath;
			oldPath = newPath = dequoteFileName(tokenizer.nextToken("\t"));
			return new Change(Change.Status.MODIFIED, oldPath, newPath, oldMode, newMode);
		} else if (statusCode.equals("D")) {
			String oldPath = dequoteFileName(tokenizer.nextToken("\t"));
			return new Change(Change.Status.DELETED, oldPath, null, oldMode, newMode);
		} else if (statusCode.equals("A")) {
			String newPath = dequoteFileName(tokenizer.nextToken("\t"));
			return new Change(Change.Status.ADDED, null, newPath, oldMode, newMode);
		} else {
			throw new RuntimeException("Unexpected status code: " + statusCode);
		}
	}
}
