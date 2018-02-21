package com.nokia.filersync.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.OutputStream;
import java.util.List;

import com.nokia.filersync.builder.IRunner;

public class RunnerMock implements IRunner{

	private List<String> expectedCommands;
	private String expectedCommandPart;
	private int expectedCount;
	private int count = 0;
	
	@Override
	public void run(List<String> commands, OutputStream os) {
		if(expectedCommands!=null) {
			assertEquals(expectedCommands, commands);
		}
		if(expectedCommandPart!=null) {
			assertTrue(commands.toString() + " has not " + expectedCommandPart, 
					   commands.get(commands.size()-1).indexOf(expectedCommandPart)>=0);
		}
		++count;
	}

	public void setExpectedCommand(List<String> expectedCommand) {
		this.expectedCommands = expectedCommand;
		++expectedCount;
	}
	
	public void setExpectedCommandPart(String expectedCommandPart) {
		this.expectedCommandPart = expectedCommandPart;
		++expectedCount;
	}
	
	public void verifyCount() {
		assertEquals(expectedCount, count);
	}

}
