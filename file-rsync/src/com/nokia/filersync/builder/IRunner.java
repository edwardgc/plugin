package com.nokia.filersync.builder;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public interface IRunner {
	void run(List<String> commands, OutputStream os) throws IOException;
}
