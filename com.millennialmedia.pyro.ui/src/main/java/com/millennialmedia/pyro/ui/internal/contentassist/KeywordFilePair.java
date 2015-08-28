package com.millennialmedia.pyro.ui.internal.contentassist;


public class KeywordFilePair implements Comparable<KeywordFilePair> {
	private String keywordName;
	private String fileName;
	
	public String getKeywordName() {
		return keywordName;
	}

	public void setKeywordName(String keywordName) {
		this.keywordName = keywordName;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	@Override
	public int compareTo(KeywordFilePair o) {
		return keywordName.compareTo(o.getKeywordName());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
		result = prime * result + ((keywordName == null) ? 0 : keywordName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		KeywordFilePair other = (KeywordFilePair) obj;
		if (fileName == null) {
			if (other.fileName != null)
				return false;
		} else if (!fileName.equals(other.fileName))
			return false;
		if (keywordName == null) {
			if (other.keywordName != null)
				return false;
		} else if (!keywordName.equals(other.keywordName))
			return false;
		return true;
	}
	
}
