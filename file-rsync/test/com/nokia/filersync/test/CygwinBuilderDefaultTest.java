package com.nokia.filersync.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.nokia.filersync.builder.CygwinBuilder;
import com.nokia.filersync.command.FileMapping;
import com.nokia.filersync.properties.FileRsyncConfig;

public class CygwinBuilderDefaultTest {

	List<IResource> resources = new ArrayList<IResource>();
	FileRsyncConfig config = new FileRsyncConfig();
	RunnerMock runner = new RunnerMock();
	CygwinBuilder builder = new CygwinBuilder(runner);
	
	ArrayList<String> commands = new ArrayList<String>();
	String commandHead;
	String commandIncludeFiles; 
	String commandExcludeFiles;
	String commandTail;
	@Before
	public void setup() {
		resources.add(new MyResource("", "src/hello/123.txt"));
		config.setCygwinHome("D:/cygwin64");
		config.setTargetHost("hzling513.china.nsn-net.net");
		config.setTargetUsername("cguo005");
		FileMapping mapping = new FileMapping(
				new Path("src"), 
				new Path("D:/runtime-EclipseApplication/test/src"), 
				new Path("/var/fpwork/cguo005/trunk"), 
				new IPath[0], new IPath[0], new Path(""));
		List<FileMapping> mappings = Arrays.asList(mapping);
		config.setMappings(mappings);
		
		commands.add("D:/cygwin64/bin/bash");
		commands.add("--login");
		commands.add("-c");
		commandHead = "cd D:/runtime-EclipseApplication/test/src; rsync -taz --delete -v ";
		commandIncludeFiles = "--include=hello/123.txt --include=hello ";
		commandExcludeFiles = "--exclude=* ";
		commandTail = "./ cguo005@hzling513.china.nsn-net.net:/var/fpwork/cguo005/trunk";
	}
	
	private String getFullCommand() {
		return commandHead + commandIncludeFiles + commandExcludeFiles + commandTail;
	}
	
	@After
	public void teardown() {
		runner.verifyCount();
	}
	
	@Test
	public void testDefaltConfig() throws IOException {
		commands.add(getFullCommand());
		runner.setExpectedCommand(commands);
		builder.buildIncremental(resources, config, System.out);
	}
	
	@Test
	public void testExcludeTeamFiles() throws IOException {
		String commandPart = "-C ";
		config.setExludeTeamFiles(true);
		runner.setExpectedCommandPart(commandPart);
		builder.buildIncremental(resources, config, System.out);
	}
	
	@Test
	public void testSkipNewerFiles() throws IOException {
		String commandPart = "-u ";
		config.setSkipNewerFiles(true);
		runner.setExpectedCommandPart(commandPart);
		builder.buildIncremental(resources, config, System.out);
	}
	
	@Test
	public void testInclude2Resrouces() throws IOException {
		resources.add(new MyResource("", "src/hello/456.txt"));
		commandIncludeFiles += "--include=hello/456.txt ";
		commands.add(getFullCommand());
		runner.setExpectedCommand(commands);
		builder.buildIncremental(resources, config, System.out);
	}
	
	@Test
	public void testFullBuildWithDefaultConfig() throws IOException {
		commandHead = "cd D:/runtime-EclipseApplication/test/src; rsync -taz --delete --stats ";
		commandIncludeFiles = "";
		commandExcludeFiles = "";
		commands.add(getFullCommand());
		runner.setExpectedCommand(commands);
		builder.buildFull(config, System.out);
	}
	
	@Test
	public void testFullBuildWithInclude() throws IOException {
		FileMapping map = config.getMappings().get(0);
		map.setInclusionPatterns(new IPath[]{new Path("**/*.txt"), new Path("build")});
		String commandPart = "--include=*/ --include=**/*.txt --include=build --exclude=*";
		commands.add(getFullCommand());
		runner.setExpectedCommandPart(commandPart);
		builder.buildFull(config, System.out);
	}
	
	@Test
	public void testFullBuildWithExclude() throws IOException {
		FileMapping map = config.getMappings().get(0);
		map.setExclusionPatterns(new IPath[]{new Path("**/*.txt"), new Path("build")});
		String commandPart = "--exclude=**/*.txt --exclude=build ";
		commands.add(getFullCommand());
		runner.setExpectedCommandPart(commandPart);
		builder.buildFull(config, System.out);
	}
	
	@Test
	public void testFullBuildWithBothIncludeAndExclude() throws IOException {
		FileMapping map = config.getMappings().get(0);
		map.setInclusionPatterns(new IPath[]{new Path("**/*.txt")});
		map.setExclusionPatterns(new IPath[]{new Path("build")});
		String commandPart = "--include=*/ --include=**/*.txt --exclude=build ";
		commands.add(getFullCommand());
		runner.setExpectedCommandPart(commandPart);
		builder.buildFull(config, System.out);
	}
	
}
