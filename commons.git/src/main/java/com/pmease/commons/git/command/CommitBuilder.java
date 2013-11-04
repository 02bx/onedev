package com.pmease.commons.git.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.pmease.commons.git.Commit;
import com.pmease.commons.git.FileChange;

class CommitBuilder {
    
    private Date date;
    
    private String author;
    
    private String committer;
    
    private String hash;
    
    private String subject;
    
    private String body;
    
    private List<String> parentHashes = new ArrayList<>();
    
    private List<FileChange> fileChanges = new ArrayList<>();

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCommitter() {
        return committer;
    }

    public void setCommitter(String committer) {
        this.committer = committer;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}
    
	public Collection<String> getParentHashes() {
		return parentHashes;
	}
	
	public List<FileChange> getFileChanges() {
		return fileChanges;
	}
	
	public Commit build() {
		return new Commit(date, author, committer, hash, subject.trim(), 
				body!=null?body.trim():null, parentHashes, fileChanges);
	}
}
