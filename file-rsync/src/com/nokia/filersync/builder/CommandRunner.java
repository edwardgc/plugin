package com.nokia.filersync.builder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class CommandRunner implements IRunner {

	@Override
	public void run(List<String> commands, OutputStream os) throws IOException {
		ProcessBuilder pb = new ProcessBuilder(commands);
		pb.redirectErrorStream(true);
		pb.redirectOutput();
		InputStream in = null;
		PrintWriter pw = new PrintWriter(os, true);
		Process process = pb.start();
		in = process.getInputStream();
		byte[] re = new byte[1024];
		int len = 0;
		while ((len = in.read(re)) != -1) {
			pw.print(new String(re, 0, len, "UTF-8"));
		}
		pw.println();
	}

	public static void main(String args[]) throws IOException{
		CommandRunner runner = new CommandRunner();
		runner.run(Arrays.asList("D:/cygwin64/bin/bash", "--login", "-c", "ls"), System.out);
	}

}
