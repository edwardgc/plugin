package com.nokia.filersync.builder;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

import com.nokia.filersync.command.FileMapping;
import com.nokia.filersync.properties.FileRsyncConfig;

public class CygwinBuilder {

	private IRunner runner;
	private final String blank = " ";
	private final String slash = "/";
	
	private final String verboseOption = "-v ";
	private final String statsOption = "--stats ";
	
	public CygwinBuilder(IRunner runner) {
		this.runner = runner;
	}

	public void buildIncremental(List<IResource> resources, FileRsyncConfig config, OutputStream os) throws IOException{
		if(resources == null || resources.size()==0) {
			return;
		}
		List<String> commands = getCygwinBash(config);
		Set<FileMapping> mappings = findFileMap(resources, config);
		for(FileMapping mapping : mappings) {
			commands.add(getRsyncHead(config, mapping) + verboseOption +
					     getIncrementalBuildFilesOption(resources, mapping) +
					     getRsyncTail(config, mapping));
			run(commands, os);
		}
	}
	
	public void buildFull(FileRsyncConfig config, OutputStream os) throws IOException{
		for(FileMapping mapping : config.getMappings()) {
			List<String> commands = getCygwinBash(config);
			commands.add(getRsyncHead(config, mapping) + statsOption +
					     getFullBuildFilesOption(mapping) + 
					     getRsyncTail(config, mapping));
			run(commands, os);
		}
	}
	
	public void run(List<String> commands, OutputStream os) throws IOException{
		PrintWriter pw = new PrintWriter(os, true);
		pw.println(commands.toString());
		try {
			runner.run(commands, os);
		} catch(IOException e) {
			pw.println(e.getMessage());
		}
		pw.println();
		os.flush();
	}

	private List<String> getCygwinBash(FileRsyncConfig config) {
		ArrayList<String> commands = new ArrayList<String>();
		commands.add(config.getCygwinHome() + "/bin/bash");
		commands.add("--login");
		commands.add("-c");
		return commands;
	}
	
	private String getRsyncHead(FileRsyncConfig config, FileMapping mapping) {
		String command = "cd " + mapping.getSourceLocaiton() + "; ";
		command += "rsync -taz --delete ";
		if(config.isExludeTeamFiles()) {
			command += "-C ";
		}
		if(config.isSkipNewerFiles()) {
			command += "-u ";
		}
		return command;
	}
	
	private String getIncrementalBuildFilesOption(List<IResource> resources, FileMapping mapping) {
		Set<String> includeSet = new HashSet<String>();
		for(IResource resource : resources) {
			IPath sourcePath = mapping.getSourcePath();
			IPath resourcePath = resource.getProjectRelativePath();
			if(sourcePath.isPrefixOf(resourcePath)) {
				IPath relativePath = resourcePath.removeFirstSegments(sourcePath.segmentCount());
				while(relativePath.segmentCount() > 0) {
					includeSet.add("--include=" + relativePath + blank);
					relativePath = relativePath.removeLastSegments(1);
				}
			}
		}
		String command = String.join("", includeSet);
		command += "--exclude=* ";
		return command;
	}
	
	private String getFullBuildFilesOption(FileMapping mapping) {
		String command = "";
		IPath[] includes = mapping.getInclusionPatterns();
		IPath[] excludes = mapping.getExclusionPatterns();
		if(includes.length > 0) {
			command += "--include=*/ ";
			for(IPath include : includes) {
				command += "--include=" + include + blank;
			}
			if(excludes.length==0) {
				command += "--exclude=* ";
			}
		}
		for(IPath exclude : excludes) {
			command += "--exclude=" + exclude + blank;
		}
		return command;
	}
	
	private Set<FileMapping> findFileMap (List<IResource> resources, FileRsyncConfig config) {
		Set<FileMapping> mappings = new HashSet<FileMapping>();
		for(IResource resource : resources) {
			for(FileMapping mapping : config.getMappings()) {
				if(mapping.getSourcePath().isPrefixOf(resource.getProjectRelativePath())) {
					if(!mappings.contains(mapping))mappings.add(mapping);
				}
			}
		}
		return mappings;
	}
	
	private String getRsyncTail(FileRsyncConfig config, FileMapping mapping) {
		 String command = "." + slash + blank;
		 if(config.getTargetUsername()!=null && !config.getTargetUsername().isEmpty()) {
			 command += config.getTargetUsername() + "@";
		 }
		 command += config.getTargetHost() + ":" + mapping.getDestinationPath();
		 return command;
	}	
}
