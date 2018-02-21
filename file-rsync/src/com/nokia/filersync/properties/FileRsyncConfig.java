package com.nokia.filersync.properties;

import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.nokia.filersync.command.FileMapping;

public class FileRsyncConfig {
	private List<FileMapping> mappings;
	private boolean exludeTeamFiles;
	private boolean skipNewerFiles;
	private IPath cygwinHome;
	private String targetHost;
	private String targetUsername;

	public List<FileMapping> getMappings() {
		return mappings;
	}

	public void setMappings(List<FileMapping> mappings) {
		this.mappings = mappings;
	}

	public boolean isExludeTeamFiles() {
		return exludeTeamFiles;
	}

	public void setExludeTeamFiles(boolean exludeTeamFiles) {
		this.exludeTeamFiles = exludeTeamFiles;
	}

	public boolean isSkipNewerFiles() {
		return skipNewerFiles;
	}

	public void setSkipNewerFiles(boolean skipNewerFiles) {
		this.skipNewerFiles = skipNewerFiles;
	}

	public IPath getCygwinHome() {
		return cygwinHome;
	}

	public void setCygwinHome(IPath cygwinHome) {
		this.cygwinHome = cygwinHome;
	}
	
	public void setCygwinHome(String cygwinHome) {
		if(cygwinHome == null || cygwinHome.isEmpty())
			this.cygwinHome = null;
		else
			this.cygwinHome = new Path(cygwinHome).makeAbsolute();
	}

	public String getTargetHost() {
		return targetHost;
	}

	public void setTargetHost(String targetHost) {
		this.targetHost = targetHost;
	}

	public String getTargetUsername() {
		return targetUsername;
	}

	public void setTargetUsername(String targetUsername) {
		this.targetUsername = targetUsername;
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof FileRsyncConfig))
			return false;
		
		FileRsyncConfig other = (FileRsyncConfig)obj;
		return (mappings == other.mappings || (mappings!=null && mappings.equals(other.mappings)))
				&& exludeTeamFiles == other.exludeTeamFiles
				&& skipNewerFiles == other.skipNewerFiles
				&& (cygwinHome == other.cygwinHome || (cygwinHome!=null && cygwinHome.equals(other.cygwinHome)))
				&& (targetHost == other.targetHost || (targetHost!=null && targetHost.equals(other.targetHost)))
				&& (targetUsername == other.targetUsername || (targetUsername!=null && targetUsername.equals(other.targetUsername)));
	}
}
